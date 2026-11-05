package mobi.tarantino.proxykillswitcher.feature.killswitch.data

import android.util.Log
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.DiscoveredProxy
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.LanProxyScanner
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.LanScanState
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpointsProvider
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ScanArea
import java.util.concurrent.atomic.AtomicInteger

@Inject
class DefaultLanProxyScanner
    constructor(
        private val wifiNetworkInfoProvider: WifiNetworkInfoProvider,
        private val proxyPortProber: ProxyPortProber,
        private val savedProxyEndpointsProvider: SavedProxyEndpointsProvider,
    ) : LanProxyScanner {
        private val scanDispatcher = Dispatchers.IO.limitedParallelism(SCAN_PARALLELISM)

        override fun scan(): Flow<LanScanState> {
            val scanAreaInfo = wifiNetworkInfoProvider.wifiScanArea()
            if (scanAreaInfo == null) {
                Log.d(TAG, "Scan skipped: no Wi-Fi scan area")
                return channelFlow {
                    send(LanScanState.NotOnWifi)
                    awaitClose()
                }
            }
            Log.d(
                TAG,
                "Scan started: range=${scanAreaInfo.hostAt(0)}..${scanAreaInfo.hostAt(scanAreaInfo.hostsCount - 1)}",
            )
            return scanArea(scanAreaInfo)
        }

        private fun scanArea(scanAreaInfo: ScanAreaInfo): Flow<LanScanState> {
            return channelFlow {
                val scanArea =
                    ScanArea(
                        firstHost = scanAreaInfo.hostAt(0),
                        lastHost = scanAreaInfo.hostAt(scanAreaInfo.hostsCount - 1),
                        hostsCount = scanAreaInfo.hostsCount,
                    )
                val targets = buildTargets(scanAreaInfo)
                val total = targets.size
                val scannedCount = AtomicInteger(0)
                val startTimeMs = System.currentTimeMillis()
                val found = mutableListOf<DiscoveredProxy>()
                val progressLock = Any()

                fun elapsedMs(): Long = System.currentTimeMillis() - startTimeMs

                suspend fun publishProgress() {
                    val snapshot = synchronized(progressLock) { found.toList().sortedBy { "${it.host}:${it.port}" } }
                    send(
                        LanScanState.InProgress(
                            scanArea = scanArea,
                            scanned = scannedCount.get(),
                            total = total,
                            elapsedMs = elapsedMs(),
                            found = snapshot,
                        ),
                    )
                }

                send(
                    LanScanState.InProgress(
                        scanArea = scanArea,
                        scanned = 0,
                        total = total,
                        elapsedMs = 0,
                        found = emptyList(),
                    ),
                )

                val tickerJob =
                    launch {
                        while (true) {
                            delay(PROGRESS_TICK_MS)
                            publishProgress()
                        }
                    }

                launchScanJobs(targets, scannedCount, found, progressLock).forEach { job -> job.join() }
                tickerJob.cancel()

                val finalFound = synchronized(progressLock) { found.toList().sortedBy { "${it.host}:${it.port}" } }
                send(
                    LanScanState.Completed(
                        scanArea = scanArea,
                        elapsedMs = elapsedMs(),
                        found = finalFound,
                    ),
                )
                awaitClose()
            }
        }

        private fun kotlinx.coroutines.channels.ProducerScope<LanScanState>.launchScanJobs(
            targets: List<Pair<String, Int>>,
            scannedCount: AtomicInteger,
            found: MutableList<DiscoveredProxy>,
            progressLock: Any,
        ) = targets.map { (host, port) ->
            launch(scanDispatcher) {
                val isProxy = proxyPortProber.probe(host = host, port = port)
                if (isProxy) {
                    logFound(host, port)
                    synchronized(progressLock) {
                        found +=
                            DiscoveredProxy(
                                host = host,
                                port = port,
                                isAlreadySaved = isAlreadySaved(host, port),
                            )
                    }
                }
                scannedCount.incrementAndGet()
            }
        }

        private fun isAlreadySaved(
            host: String,
            port: Int,
        ): Boolean {
            return savedProxyEndpointsProvider.current().any { endpoint ->
                endpoint.host == host && endpoint.port == port
            }
        }

        private fun logFound(
            host: String,
            port: Int,
        ) {
            Log.d(TAG, "Proxy discovered: $host:$port")
        }

        private fun buildTargets(scanAreaInfo: ScanAreaInfo): List<Pair<String, Int>> {
            return (0 until scanAreaInfo.hostsCount)
                .flatMap { hostIndex ->
                    PROXY_PORTS.map { port -> scanAreaInfo.hostAt(hostIndex) to port }
                }
        }

        private companion object {
            const val TAG = "DefaultLanProxyScanner"
            const val SCAN_PARALLELISM = 64
            const val PROGRESS_TICK_MS = 100L
            val PROXY_PORTS = listOf(3128, 8080, 8081, 8888, 8118, 6588, 8000, 9090)
        }
    }
