package mobi.tarantino.proxykillswitcher.feature.killswitch.domain

import kotlinx.coroutines.flow.Flow

fun interface LanProxyScanner {
    fun scan(): Flow<LanScanState>
}
