package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.DeleteSavedProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ObserveKillSwitchStateUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SelectSavedProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.VerifyProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.VpnConnectionState
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.Syntax

@Stable
@Inject
class SavedProxyListViewModel
    constructor(
        val uiStateMapper: SavedProxyListUiStateMapper,
        private val selectSavedProxyEndpointUseCase: SelectSavedProxyEndpointUseCase,
        private val deleteSavedProxyEndpointUseCase: DeleteSavedProxyEndpointUseCase,
        private val observeKillSwitchStateUseCase: ObserveKillSwitchStateUseCase,
        private val verifyProxyEndpointUseCase: VerifyProxyEndpointUseCase,
    ) : ViewModel(),
        ContainerHost<SavedProxyListState, SavedProxyListSideEffect> {
        override val container: Container<SavedProxyListState, SavedProxyListSideEffect> =
            viewModelScope.container(mapState(observeKillSwitchStateUseCase.state)) {
                checkSavedEndpoints()
                subscribeToState()
            }

        fun onIntent(intent: SavedProxyListIntent) {
            when (intent) {
                SavedProxyListIntent.AddClick -> openEditor(null)
                is SavedProxyListIntent.EditClick -> openEditor(intent.savedEndpoint)
                is SavedProxyListIntent.SelectClick -> selectEndpoint(intent.savedEndpoint)
                is SavedProxyListIntent.DeleteClick -> deleteEndpoint(intent.savedEndpoint)
            }
        }

        private fun openEditor(savedEndpoint: SavedProxyEndpoint?) {
            intent {
                if (!state.isEnabled) {
                    postSideEffect(SavedProxyListSideEffect.OpenEditor(savedEndpoint))
                }
            }
        }

        private fun selectEndpoint(savedEndpoint: SavedProxyEndpoint) {
            intent {
                if (state.isEnabled) {
                    return@intent
                }
                reduce { state.copy(selectingEndpoint = savedEndpoint) }
                val isSelected = selectSavedProxyEndpointUseCase(savedEndpoint)
                reduce { state.copy(selectingEndpoint = null) }
                if (!isSelected) {
                    postSideEffect(SavedProxyListSideEffect.SelectionFailed)
                }
            }
        }

        private fun deleteEndpoint(savedEndpoint: SavedProxyEndpoint) {
            intent {
                if (!state.isEnabled) {
                    deleteSavedProxyEndpointUseCase(savedEndpoint)
                }
            }
        }

        private suspend fun Syntax<SavedProxyListState, SavedProxyListSideEffect>.subscribeToState() {
            observeKillSwitchStateUseCase.stateFlow.collect { observedState ->
                reduce { mapState(observedState).copy(checks = state.checks) }
            }
        }

        private suspend fun Syntax<SavedProxyListState, SavedProxyListSideEffect>.checkSavedEndpoints() {
            if (state.isEnabled) {
                return
            }
            val endpointsToCheck = state.savedEndpoints.filter { it !in state.checks }
            if (endpointsToCheck.isEmpty()) {
                return
            }
            reduce {
                state.copy(
                    checks =
                        state.checks +
                            endpointsToCheck.associateWith { ProxyCheckStatus.InProgress },
                )
            }
            coroutineScope {
                endpointsToCheck.forEach { endpoint ->
                    launch {
                        val isAvailable =
                            verifyProxyEndpointUseCase(
                                host = endpoint.host,
                                port = endpoint.port,
                                protocol = endpoint.protocol,
                            )
                        val checkStatus =
                            if (isAvailable) {
                                ProxyCheckStatus.Available
                            } else {
                                ProxyCheckStatus.Unavailable
                            }
                        reduce {
                            state.copy(checks = state.checks + (endpoint to checkStatus))
                        }
                    }
                }
            }
        }

        private fun mapState(observedState: ObserveKillSwitchStateUseCase.State): SavedProxyListState {
            return SavedProxyListState(
                savedEndpoints = observedState.settings.savedEndpoints,
                activeEndpoint = observedState.settings.activeEndpoint,
                isEnabled = observedState.connectionState is VpnConnectionState.Connected,
            )
        }
    }

sealed interface SavedProxyListIntent {
    data object AddClick : SavedProxyListIntent

    data class EditClick(
        val savedEndpoint: SavedProxyEndpoint,
    ) : SavedProxyListIntent

    data class SelectClick(
        val savedEndpoint: SavedProxyEndpoint,
    ) : SavedProxyListIntent

    data class DeleteClick(
        val savedEndpoint: SavedProxyEndpoint,
    ) : SavedProxyListIntent
}

sealed interface SavedProxyListSideEffect {
    data class OpenEditor(
        val savedEndpoint: SavedProxyEndpoint?,
    ) : SavedProxyListSideEffect

    data object SelectionFailed : SavedProxyListSideEffect
}
