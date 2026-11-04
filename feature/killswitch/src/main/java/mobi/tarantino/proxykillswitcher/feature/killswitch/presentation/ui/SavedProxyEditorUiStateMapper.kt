package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import androidx.compose.runtime.Stable
import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyEditorState.ProxyCheck

@Stable
@Inject
class SavedProxyEditorUiStateMapper {
    fun map(state: SavedProxyEditorState): SavedProxyEditorUiState {
        val draftEndpoint = mapDraftEndpoint(state)
        val isChecking = state.proxyCheck is ProxyCheck.InProgress
        val isVerified =
            state.proxyCheck is ProxyCheck.Succeeded &&
                state.proxyCheck.savedEndpoint == draftEndpoint
        return SavedProxyEditorUiState(
            hostText = state.host,
            portText = state.portText,
            protocolText = state.protocol.storedValue,
            isFrozen = state.isEnabled,
            isChecking = isChecking,
            isCheckEnabled = draftEndpoint != null && !state.isEnabled && !isChecking,
            isSaveEnabled = isVerified && !state.isEnabled && !isChecking,
            checkStatusText = mapStatusText(state.proxyCheck, isVerified),
        )
    }

    private fun mapDraftEndpoint(state: SavedProxyEditorState): SavedProxyEndpoint? {
        val port = state.portText.toIntOrNull()
        if (!ProxyEndpoint.isValid(host = state.host, port = port) || port == null) {
            return null
        }
        return SavedProxyEndpoint(host = state.host, port = port, protocol = state.protocol)
    }

    private fun mapStatusText(
        proxyCheck: ProxyCheck,
        isVerified: Boolean,
    ): String {
        return when (proxyCheck) {
            ProxyCheck.Idle -> ""
            ProxyCheck.InProgress -> "Checking proxy…"
            is ProxyCheck.Succeeded -> if (isVerified) "Proxy check succeeded" else ""
            ProxyCheck.Failed -> "Proxy check failed"
        }
    }
}
