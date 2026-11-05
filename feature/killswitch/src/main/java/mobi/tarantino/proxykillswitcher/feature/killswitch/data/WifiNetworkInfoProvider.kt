package mobi.tarantino.proxykillswitcher.feature.killswitch.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.getSystemService
import dev.zacsweers.metro.Inject
import java.net.Inet4Address
import java.nio.ByteBuffer

@Inject
class WifiNetworkInfoProvider
    constructor(
        context: Context,
    ) {
        private val connectivityManager = context.getSystemService<ConnectivityManager>()

        fun wifiScanArea(): ScanAreaInfo? {
            val manager = connectivityManager ?: return null
            val network = manager.activeNetwork ?: return null
            val capabilities = manager.getNetworkCapabilities(network) ?: return null
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return null
            }
            val linkProperties = manager.getLinkProperties(network) ?: return null
            return scanAreaFromLinkProperties(linkProperties)
        }

        private fun scanAreaFromLinkProperties(linkProperties: LinkProperties): ScanAreaInfo? {
            return linkProperties.linkAddresses
                .asSequence()
                .mapNotNull { linkAddress ->
                    val address = linkAddress.address as? Inet4Address ?: return@mapNotNull null
                    scanAreaFromAddress(address, linkAddress.prefixLength)
                }.firstOrNull()
        }

        private fun scanAreaFromAddress(
            address: Inet4Address,
            prefixLength: Int,
        ): ScanAreaInfo? {
            if (prefixLength !in MIN_PREFIX_LENGTH..MAX_PREFIX_LENGTH) {
                return null
            }
            val hostBits = IPV4_BITS - prefixLength
            val hostsCount = 1 shl hostBits
            if (hostsCount <= MIN_HOSTS) {
                return null
            }
            val addressInt = ByteBuffer.wrap(address.address).int
            val mask = -1 shl hostBits
            val networkInt = addressInt and mask
            val firstHost = if (hostsCount > MIN_HOSTS) networkInt + 1 else networkInt
            val lastHost = networkInt + hostsCount - 1
            Log.d(
                TAG,
                "Wi-Fi scan area resolved: ${address.hostAddress}/$prefixLength " +
                    "range=${intToIp(firstHost)}..${intToIp(lastHost)}",
            )
            return ScanAreaInfo(firstHostInt = firstHost, lastHostInt = lastHost)
        }

        private fun intToIp(value: Int): String {
            return listOf(
                (value ushr 24) and 0xFF,
                (value ushr 16) and 0xFF,
                (value ushr 8) and 0xFF,
                value and 0xFF,
            ).joinToString(".")
        }

        private companion object {
            const val TAG = "WifiNetworkInfoProvider"
            const val IPV4_BITS = 32
            const val MIN_PREFIX_LENGTH = 8
            const val MAX_PREFIX_LENGTH = 30
            const val MIN_HOSTS = 2
        }
    }

data class ScanAreaInfo(
    val firstHostInt: Int,
    val lastHostInt: Int,
) {
    val hostsCount: Int get() = lastHostInt - firstHostInt + 1

    fun hostAt(index: Int): String {
        val value = firstHostInt + index
        return listOf(
            (value ushr 24) and 0xFF,
            (value ushr 16) and 0xFF,
            (value ushr 8) and 0xFF,
            value and 0xFF,
        ).joinToString(".")
    }
}
