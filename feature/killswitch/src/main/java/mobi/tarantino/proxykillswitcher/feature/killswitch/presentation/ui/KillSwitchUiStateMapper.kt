package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import androidx.compose.runtime.Stable
import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyProtocol
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.KillSwitchState.ProxyCheck

@Stable
@Inject
class KillSwitchUiStateMapper {
    fun map(state: KillSwitchState): KillSwitchUiState {
        val draftPort = state.draftPortText.toIntOrNull()
        val isDraftValid = ProxyEndpoint.isValid(host = state.draftHost, port = draftPort)
        val isDraftChanged =
            state.draftHost != state.host ||
                state.draftPortText != state.port.toString() ||
                state.draftProtocol != state.protocol
        val isVerified =
            isSucceededForDraft(
                proxyCheck = state.proxyCheck,
                host = state.draftHost,
                port = draftPort,
                protocol = state.draftProtocol,
            )
        val isChecking = state.proxyCheck is ProxyCheck.InProgress
        return KillSwitchUiState(
            activeEndpointText =
                if (ProxyEndpoint.isValid(host = state.host, port = state.port)) {
                    "${state.protocol.storedValue}://${state.host}:${state.port}"
                } else {
                    NO_ACTIVE_ENDPOINT_TEXT
                },
            hostText = state.draftHost,
            portText = state.draftPortText,
            isEnabled = state.isEnabled,
            isConnecting = state.isConnecting,
            isToggleEnabled = ProxyEndpoint.isValid(host = state.host, port = state.port),
            isSaveEnabled = isDraftValid && isDraftChanged && isVerified && !isChecking,
            isCheckEnabled = isDraftValid && !isChecking,
            isChecking = isChecking,
            checkStatusText = mapCheckStatusText(proxyCheck = state.proxyCheck, isVerified = isVerified),
            connectionStatusText =
                mapConnectionStatusText(isConnecting = state.isConnecting, isEnabled = state.isEnabled),
        )
    }

    private fun isSucceededForDraft(
        proxyCheck: ProxyCheck,
        host: String,
        port: Int?,
        protocol: ProxyProtocol,
    ): Boolean {
        return proxyCheck is ProxyCheck.Succeeded &&
            proxyCheck.host == host &&
            proxyCheck.port == port &&
            proxyCheck.protocol == protocol
    }

    private fun mapCheckStatusText(
        proxyCheck: ProxyCheck,
        isVerified: Boolean,
    ): String {
        return when (proxyCheck) {
            ProxyCheck.Idle -> ""
            ProxyCheck.InProgress -> CHECKING_STATUS_TEXT
            is ProxyCheck.Succeeded -> if (isVerified) SUCCEEDED_STATUS_TEXT else ""
            ProxyCheck.Failed -> FAILED_STATUS_TEXT
        }
    }

    private fun mapConnectionStatusText(
        isConnecting: Boolean,
        isEnabled: Boolean,
    ): String {
        return when {
            isConnecting -> CONNECTING_STATUS_TEXT
            isEnabled -> PROXY_ENABLED_STATUS_TEXT
            else -> DIRECT_CONNECTION_STATUS_TEXT
        }
    }

    companion object {
        private const val CHECKING_STATUS_TEXT = "Checking proxy…"
        private const val SUCCEEDED_STATUS_TEXT = "Proxy check succeeded"
        private const val FAILED_STATUS_TEXT = "Proxy check failed"
        private const val NO_ACTIVE_ENDPOINT_TEXT = "No proxy selected"
        private const val CONNECTING_STATUS_TEXT = "Connecting…"
        private const val PROXY_ENABLED_STATUS_TEXT = "Proxy enabled"
        private const val DIRECT_CONNECTION_STATUS_TEXT = "Direct connection"
    }
}
