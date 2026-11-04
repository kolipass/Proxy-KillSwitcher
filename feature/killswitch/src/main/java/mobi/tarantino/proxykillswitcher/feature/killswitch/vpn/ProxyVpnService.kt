package mobi.tarantino.proxykillswitcher.feature.killswitch.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mobi.tarantino.proxykillswitcher.core.tunnel.TunnelEngine

class ProxyVpnService : VpnService() {
    private var tunnelInterface: ParcelFileDescriptor? = null
    private var startupJob: Job? = null
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private val serviceCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val proxyVpnNotificationFactory by lazy { ProxyVpnNotificationFactory(this) }
    private val killSwitchGraphOwner by lazy { application as KillSwitchGraphOwner }
    private val killSwitchRepository by lazy { killSwitchGraphOwner.killSwitchRepository }
    private val verifyProxyEndpointUseCase by lazy { killSwitchGraphOwner.verifyProxyEndpointUseCase }
    private val vpnConnectionStateHolder by lazy { killSwitchGraphOwner.vpnConnectionStateHolder }
    private val tunnelEngine by lazy { killSwitchGraphOwner.tunnelEngine }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            disconnect()
            return START_NOT_STICKY
        }

        stopTunnel()
        startForeground(NOTIFICATION_ID, proxyVpnNotificationFactory.createOngoingNotification())

        val host = killSwitchRepository.host
        val port = killSwitchRepository.port
        if (host.isBlank() || port !in VALID_PORT_RANGE) {
            failAndDisconnect()
            return START_NOT_STICKY
        }

        startTunnelWhenProxyReachable(host = host, port = port)
        return START_NOT_STICKY
    }

    private fun startTunnelWhenProxyReachable(
        host: String,
        port: Int,
    ) {
        startupJob =
            serviceCoroutineScope.launch {
                val isReachable =
                    verifyProxyEndpointUseCase(
                        host = host,
                        port = port,
                        protocol = killSwitchRepository.protocol,
                    )
                if (!isReachable) {
                    failAndDisconnect()
                    return@launch
                }
                establishTunnel(host = host, port = port)
            }
    }

    private fun establishTunnel(
        host: String,
        port: Int,
    ) {
        val establishedInterface =
            Builder()
                .setSession(VPN_SESSION_NAME)
                .setMtu(TUN_MTU)
                .addAddress(TUN_ADDRESS, TUN_PREFIX_LENGTH)
                .addRoute(DEFAULT_ROUTE, DEFAULT_ROUTE_PREFIX_LENGTH)
                .addDnsServer(MAP_DNS_ADDRESS)
                .addDisallowedApplication(packageName)
                .setBlocking(true)
                .establish()

        if (establishedInterface == null) {
            failAndDisconnect()
            return
        }

        tunnelInterface = establishedInterface
        if (tunnelEngine.start(
                tunFileDescriptor = establishedInterface.fd,
                config =
                    TunnelEngine.Config(
                        filesDir = filesDir,
                        upstreamHost = host,
                        upstreamPort = port,
                        protectSocket = ::protect,
                        protectFileDescriptor = ::protect,
                        onUpstreamUnreachable = ::tearDownOnUpstreamLoss,
                    ),
            )
        ) {
            vpnConnectionStateHolder.publishConnected()
        } else {
            failAndDisconnect()
        }
    }

    override fun onRevoke() {
        disconnect()
        super.onRevoke()
    }

    override fun onDestroy() {
        releaseTunnel()
        serviceCoroutineScope.cancel()
        super.onDestroy()
    }

    private fun disconnect() {
        releaseTunnel()
        stopSelf()
    }

    private fun tearDownOnUpstreamLoss() {
        mainThreadHandler.post {
            if (tunnelInterface == null) {
                return@post
            }
            killSwitchRepository.setDesiredEnabled(false)
            failAndDisconnect()
        }
    }

    private fun failAndDisconnect() {
        releaseTunnel()
        vpnConnectionStateHolder.publishFailed()
        stopSelf()
    }

    private fun releaseTunnel() {
        stopTunnel()
        vpnConnectionStateHolder.publishDisconnected()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun stopTunnel() {
        startupJob?.cancel()
        startupJob = null
        tunnelEngine.stop()
        tunnelInterface?.close()
        tunnelInterface = null
    }

    companion object {
        fun start(context: Context) {
            context.startForegroundService(Intent(context, ProxyVpnService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ProxyVpnService::class.java).setAction(ACTION_DISCONNECT),
            )
        }

        private const val ACTION_DISCONNECT = "mobi.tarantino.proxykillswitcher.killswitch.DISCONNECT"

        private const val NOTIFICATION_ID = 1001
        private const val VPN_SESSION_NAME = "Proxy kill switch"
        private const val TUN_MTU = 1500
        private const val TUN_ADDRESS = "10.10.0.2"
        private const val TUN_PREFIX_LENGTH = 32
        private const val DEFAULT_ROUTE = "0.0.0.0"
        private const val DEFAULT_ROUTE_PREFIX_LENGTH = 0
        private const val MAP_DNS_ADDRESS = "10.10.0.1"
        private val VALID_PORT_RANGE = 1..65535
    }
}
