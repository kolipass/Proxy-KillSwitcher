package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ObserveKillSwitchStateUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyProtocol
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SaveProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ToggleKillSwitchUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.VerifyProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.KillSwitchState.ProxyCheck
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.VpnConnectionState
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.Syntax

@Stable
@Inject
class KillSwitchViewModel
    constructor(
        val uiStateMapper: KillSwitchUiStateMapper,
        private val toggleKillSwitchUseCase: ToggleKillSwitchUseCase,
        private val saveProxyEndpointUseCase: SaveProxyEndpointUseCase,
        private val verifyProxyEndpointUseCase: VerifyProxyEndpointUseCase,
        private val observeKillSwitchStateUseCase: ObserveKillSwitchStateUseCase,
    ) : ViewModel(),
        ContainerHost<KillSwitchState, KillSwitchSideEffect> {
        override val container: Container<KillSwitchState, KillSwitchSideEffect> =
            viewModelScope.container(
                initialState =
                    mapPersistedState(
                        observedState = observeKillSwitchStateUseCase.state,
                    ),
            ) {
                subscribeToState()
            }

        fun onIntent(intent: KillSwitchIntent) {
            when (intent) {
                KillSwitchIntent.ToggleClick -> toggleKillSwitch()
                KillSwitchIntent.VpnPermissionDenied -> disableKillSwitch()
                is KillSwitchIntent.HostChanged -> updateDraftHost(intent.host)
                is KillSwitchIntent.PortChanged -> updateDraftPort(intent.portText)
                KillSwitchIntent.CheckProxy -> checkProxyEndpoint()
                KillSwitchIntent.SaveProxy -> saveProxyEndpoint()
                KillSwitchIntent.ManageProxiesClick -> openSavedProxyList()
            }
        }

        private fun toggleKillSwitch() {
            intent {
                val updatedIsEnabled = !state.isEnabled
                toggleKillSwitchUseCase(updatedIsEnabled)
                reduce { state.copy(isConnecting = updatedIsEnabled) }
                postSideEffect(
                    if (updatedIsEnabled) {
                        KillSwitchSideEffect.PrepareVpn
                    } else {
                        KillSwitchSideEffect.StopVpn
                    },
                )
            }
        }

        private fun disableKillSwitch() {
            intent {
                toggleKillSwitchUseCase(false)
                reduce { state.copy(isEnabled = false, isConnecting = false) }
            }
        }

        private fun openSavedProxyList() {
            intent {
                postSideEffect(KillSwitchSideEffect.OpenSavedProxyList)
            }
        }

        private fun updateDraftHost(host: String) {
            intent {
                reduce {
                    state.copy(
                        draftHost = host,
                        proxyCheck =
                            if (state.proxyCheck is ProxyCheck.InProgress) {
                                state.proxyCheck
                            } else {
                                ProxyCheck.Idle
                            },
                    )
                }
            }
        }

        private fun updateDraftPort(portText: String) {
            intent {
                reduce {
                    state.copy(
                        draftPortText = portText,
                        proxyCheck =
                            if (state.proxyCheck is ProxyCheck.InProgress) {
                                state.proxyCheck
                            } else {
                                ProxyCheck.Idle
                            },
                    )
                }
            }
        }

        private fun checkProxyEndpoint() {
            intent {
                val host = state.draftHost
                val port = state.draftPortText.toIntOrNull()
                if (!ProxyEndpoint.isValid(host = host, port = port) || port == null) {
                    reduce { state.copy(proxyCheck = ProxyCheck.Failed) }
                    return@intent
                }
                reduce { state.copy(proxyCheck = ProxyCheck.InProgress) }
                val protocol = state.draftProtocol
                val isReachable =
                    verifyProxyEndpointUseCase(host = host, port = port, protocol = protocol)
                reduce {
                    val isSameDraft =
                        state.draftHost == host &&
                            state.draftPortText.toIntOrNull() == port &&
                            state.draftProtocol == protocol
                    if (!isSameDraft) {
                        state
                    } else {
                        state.copy(
                            proxyCheck =
                                if (isReachable) {
                                    ProxyCheck.Succeeded(host = host, port = port, protocol = protocol)
                                } else {
                                    ProxyCheck.Failed
                                },
                        )
                    }
                }
            }
        }

        private fun saveProxyEndpoint() {
            intent {
                val host = state.draftHost
                val port = state.draftPortText.toIntOrNull()
                val protocol = state.draftProtocol
                if (port != null && state.isDraftReadyToSave()) {
                    saveProxyEndpointUseCase(host = host, port = port, protocol = protocol)
                    postSideEffect(KillSwitchSideEffect.ProxySaved)
                } else {
                    postSideEffect(KillSwitchSideEffect.ProxyRejected)
                }
            }
        }

        private suspend fun Syntax<KillSwitchState, KillSwitchSideEffect>.subscribeToState() {
            var lastConnectionState = observeKillSwitchStateUseCase.state.connectionState
            observeKillSwitchStateUseCase.stateFlow.collect { observedState ->
                val updatedConnectionState = observedState.connectionState
                val hasJustFailed =
                    updatedConnectionState is VpnConnectionState.Failed &&
                        lastConnectionState !is VpnConnectionState.Failed
                val hasConnectionStateChanged = updatedConnectionState != lastConnectionState
                lastConnectionState = updatedConnectionState
                val updatedPersistedState = mapPersistedState(observedState)
                reduce {
                    state.copy(
                        host = updatedPersistedState.host,
                        port = updatedPersistedState.port,
                        protocol = updatedPersistedState.protocol,
                        isEnabled = updatedPersistedState.isEnabled,
                        isConnecting = state.isConnecting && !hasConnectionStateChanged,
                    )
                }
                if (hasJustFailed) {
                    postSideEffect(KillSwitchSideEffect.ConnectionFailed)
                }
            }
        }

        private fun mapPersistedState(observedState: ObserveKillSwitchStateUseCase.State): KillSwitchState {
            val settings = observedState.settings
            return KillSwitchState(
                host = settings.host,
                port = settings.port,
                protocol = settings.protocol,
                draftHost = settings.host,
                draftPortText = settings.port.toString(),
                draftProtocol = settings.protocol,
                isEnabled = observedState.connectionState is VpnConnectionState.Connected,
            )
        }
    }

sealed interface KillSwitchIntent {
    data object ToggleClick : KillSwitchIntent

    data object VpnPermissionDenied : KillSwitchIntent

    data class HostChanged(
        val host: String,
    ) : KillSwitchIntent

    data class PortChanged(
        val portText: String,
    ) : KillSwitchIntent

    data object CheckProxy : KillSwitchIntent

    data object SaveProxy : KillSwitchIntent

    data object ManageProxiesClick : KillSwitchIntent
}

sealed interface KillSwitchSideEffect {
    data object PrepareVpn : KillSwitchSideEffect

    data object StopVpn : KillSwitchSideEffect

    data object ProxySaved : KillSwitchSideEffect

    data object ProxyRejected : KillSwitchSideEffect

    data object ConnectionFailed : KillSwitchSideEffect

    data object OpenSavedProxyList : KillSwitchSideEffect
}
