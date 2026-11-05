package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import androidx.compose.runtime.Immutable
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint

data class SavedProxyListState(
    val savedEndpoints: List<SavedProxyEndpoint>,
    val activeEndpoint: SavedProxyEndpoint?,
    val isEnabled: Boolean,
    val selectingEndpoint: SavedProxyEndpoint? = null,
    val checks: Map<SavedProxyEndpoint, ProxyCheckStatus> = emptyMap(),
)

sealed interface ProxyCheckStatus {
    data object InProgress : ProxyCheckStatus

    data object Available : ProxyCheckStatus

    data object Unavailable : ProxyCheckStatus
}

@Immutable
data class SavedProxyListUiState(
    val endpoints: List<Endpoint>,
    val isFrozen: Boolean,
) {
    @Immutable
    data class Endpoint(
        val savedEndpoint: SavedProxyEndpoint,
        val addressText: String,
        val isActive: Boolean,
        val isSelecting: Boolean,
        val checkStatusText: String,
        val checkStatusKind: CheckStatusKind,
    )

    enum class CheckStatusKind {
        None,
        InProgress,
        Available,
        Unavailable,
    }
}
