package mobi.tarantino.proxykillswitcher.feature.killswitch.domain

data class DiscoveredProxy(
    val host: String,
    val port: Int,
    val protocol: ProxyProtocol = ProxyProtocol.Http,
    val isAlreadySaved: Boolean = false,
)

data class ScanArea(
    val firstHost: String,
    val lastHost: String,
    val hostsCount: Int,
)

sealed interface LanScanState {
    data object Idle : LanScanState

    data object NotOnWifi : LanScanState

    data class InProgress(
        val scanArea: ScanArea,
        val scanned: Int,
        val total: Int,
        val elapsedMs: Long,
        val found: List<DiscoveredProxy>,
    ) : LanScanState

    data class Completed(
        val scanArea: ScanArea,
        val elapsedMs: Long,
        val found: List<DiscoveredProxy>,
    ) : LanScanState
}
