package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import androidx.compose.runtime.Stable
import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint

@Stable
@Inject
class SavedProxyListUiStateMapper {
    fun map(state: SavedProxyListState): SavedProxyListUiState {
        return SavedProxyListUiState(
            endpoints = state.savedEndpoints.map { savedEndpoint -> mapEndpoint(savedEndpoint, state) },
            isFrozen = state.isEnabled,
        )
    }

    private fun mapEndpoint(
        savedEndpoint: SavedProxyEndpoint,
        state: SavedProxyListState,
    ): SavedProxyListUiState.Endpoint {
        val (checkStatusText, checkStatusKind) = mapCheckStatus(state.checks[savedEndpoint])
        return SavedProxyListUiState.Endpoint(
            savedEndpoint = savedEndpoint,
            addressText = "${savedEndpoint.protocol.storedValue}://${savedEndpoint.host}:${savedEndpoint.port}",
            isActive = savedEndpoint == state.activeEndpoint,
            isSelecting = savedEndpoint == state.selectingEndpoint,
            checkStatusText = checkStatusText,
            checkStatusKind = checkStatusKind,
        )
    }

    private fun mapCheckStatus(status: ProxyCheckStatus?): Pair<String, SavedProxyListUiState.CheckStatusKind> {
        return when (status) {
            ProxyCheckStatus.InProgress -> {
                CHECKING_STATUS_TEXT to SavedProxyListUiState.CheckStatusKind.InProgress
            }

            ProxyCheckStatus.Available -> {
                AVAILABLE_STATUS_TEXT to SavedProxyListUiState.CheckStatusKind.Available
            }

            ProxyCheckStatus.Unavailable -> {
                UNAVAILABLE_STATUS_TEXT to SavedProxyListUiState.CheckStatusKind.Unavailable
            }

            null -> {
                "" to SavedProxyListUiState.CheckStatusKind.None
            }
        }
    }

    companion object {
        const val CHECKING_STATUS_TEXT = "Checking proxy…"
        const val AVAILABLE_STATUS_TEXT = "Proxy available"
        const val UNAVAILABLE_STATUS_TEXT = "Proxy unavailable"
    }
}
