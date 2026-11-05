package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import androidx.compose.runtime.Immutable
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.DiscoveredProxy
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.LanScanState
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyProtocol
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint

data class SavedProxyEditorState(
    val host: String = "",
    val portText: String = "",
    val protocol: ProxyProtocol = ProxyProtocol.Http,
    val originalEndpoint: SavedProxyEndpoint? = null,
    val isEnabled: Boolean,
    val isInitialized: Boolean = false,
    val proxyCheck: ProxyCheck = ProxyCheck.Idle,
    val lanScan: LanScanState = LanScanState.Idle,
) {
    sealed interface ProxyCheck {
        data object Idle : ProxyCheck

        data object InProgress : ProxyCheck

        data class Succeeded(
            val savedEndpoint: SavedProxyEndpoint,
        ) : ProxyCheck

        data object Failed : ProxyCheck
    }
}

@Immutable
data class SavedProxyEditorUiState(
    val hostText: String,
    val portText: String,
    val protocolText: String,
    val isFrozen: Boolean,
    val isChecking: Boolean,
    val isCheckEnabled: Boolean,
    val isSaveEnabled: Boolean,
    val checkStatusText: String,
    val lanScanUiState: LanScanUiState,
)

@Immutable
sealed interface LanScanUiState {
    data object Hidden : LanScanUiState

    data object NotOnWifi : LanScanUiState

    data object Scanning : LanScanUiState

    data class Completed(
        val progressText: String,
        val elapsedText: String,
        val found: List<DiscoveredProxy>,
    ) : LanScanUiState
}
