package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import androidx.compose.runtime.Immutable
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyProtocol

data class KillSwitchState(
    val host: String,
    val port: Int,
    val protocol: ProxyProtocol,
    val draftHost: String,
    val draftPortText: String,
    val draftProtocol: ProxyProtocol,
    val isEnabled: Boolean,
    val isConnecting: Boolean = false,
    val proxyCheck: ProxyCheck = ProxyCheck.Idle,
    val hasSavedProxies: Boolean = false,
) {
    fun isDraftReadyToSave(): Boolean {
        val draftPort = draftPortText.toIntOrNull()
        val isDraftChanged =
            draftHost != host ||
                draftPortText != port.toString() ||
                draftProtocol != protocol
        val isVerified =
            proxyCheck is ProxyCheck.Succeeded &&
                proxyCheck.host == draftHost &&
                proxyCheck.port == draftPort &&
                proxyCheck.protocol == draftProtocol
        return isDraftChanged && isVerified && ProxyEndpoint.isValid(host = draftHost, port = draftPort)
    }

    sealed interface ProxyCheck {
        data object Idle : ProxyCheck

        data object InProgress : ProxyCheck

        data class Succeeded(
            val host: String,
            val port: Int,
            val protocol: ProxyProtocol,
        ) : ProxyCheck

        data object Failed : ProxyCheck
    }
}

@Immutable
data class KillSwitchUiState(
    val activeEndpointText: String,
    val hostText: String,
    val portText: String,
    val isEnabled: Boolean,
    val isConnecting: Boolean,
    val isToggleEnabled: Boolean,
    val isSaveEnabled: Boolean,
    val isCheckEnabled: Boolean,
    val isChecking: Boolean,
    val checkStatusText: String,
    val connectionStatusText: String,
)
