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
        return SavedProxyListUiState.Endpoint(
            savedEndpoint = savedEndpoint,
            addressText = "${savedEndpoint.protocol.storedValue}://${savedEndpoint.host}:${savedEndpoint.port}",
            isActive = savedEndpoint == state.activeEndpoint,
            isSelecting = savedEndpoint == state.selectingEndpoint,
        )
    }
}
