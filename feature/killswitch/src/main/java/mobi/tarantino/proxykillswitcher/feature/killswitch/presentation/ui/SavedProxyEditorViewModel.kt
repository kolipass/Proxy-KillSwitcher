package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.DiscoveredProxy
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.LanProxyScanner
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ObserveKillSwitchStateUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SaveProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.VerifyProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyEditorState.ProxyCheck
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.VpnConnectionState
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.Syntax

@Stable
@Inject
class SavedProxyEditorViewModel
    constructor(
        val uiStateMapper: SavedProxyEditorUiStateMapper,
        private val saveProxyEndpointUseCase: SaveProxyEndpointUseCase,
        private val verifyProxyEndpointUseCase: VerifyProxyEndpointUseCase,
        private val observeKillSwitchStateUseCase: ObserveKillSwitchStateUseCase,
        private val lanProxyScanner: LanProxyScanner,
    ) : ViewModel(),
        ContainerHost<SavedProxyEditorState, SavedProxyEditorSideEffect> {
        override val container: Container<SavedProxyEditorState, SavedProxyEditorSideEffect> =
            viewModelScope.container(
                SavedProxyEditorState(
                    isEnabled = observeKillSwitchStateUseCase.state.connectionState is VpnConnectionState.Connected,
                ),
            ) {
                subscribeToState()
            }

        fun onIntent(intent: SavedProxyEditorIntent) {
            when (intent) {
                is SavedProxyEditorIntent.Initialize -> initialize(intent.savedEndpoint)
                is SavedProxyEditorIntent.HostChanged -> updateHost(intent.host)
                is SavedProxyEditorIntent.PortChanged -> updatePort(intent.portText)
                SavedProxyEditorIntent.CheckClick -> checkEndpoint()
                SavedProxyEditorIntent.SaveClick -> saveEndpoint()
                is SavedProxyEditorIntent.DiscoveredProxyClick -> applyDiscoveredProxy(intent.discoveredProxy)
                SavedProxyEditorIntent.RescanClick -> rescanLan()
            }
        }

        private fun initialize(savedEndpoint: SavedProxyEndpoint?) {
            intent {
                if (!state.isInitialized) {
                    reduce {
                        state.copy(
                            host = savedEndpoint?.host.orEmpty(),
                            portText = savedEndpoint?.port?.toString().orEmpty(),
                            protocol = savedEndpoint?.protocol ?: state.protocol,
                            originalEndpoint = savedEndpoint,
                            isInitialized = true,
                        )
                    }
                    if (savedEndpoint == null && !state.isEnabled) {
                        lanProxyScanner.scan().collect { scanState ->
                            reduce { state.copy(lanScan = scanState) }
                        }
                    }
                }
            }
        }

        private fun updateHost(host: String) {
            intent {
                if (!state.isEnabled) {
                    reduce { state.copy(host = host, proxyCheck = ProxyCheck.Idle) }
                }
            }
        }

        private fun updatePort(portText: String) {
            intent {
                if (!state.isEnabled) {
                    reduce { state.copy(portText = portText, proxyCheck = ProxyCheck.Idle) }
                }
            }
        }

        private fun checkEndpoint() {
            intent {
                val savedEndpoint =
                    mapDraftEndpoint() ?: run {
                        reduce { state.copy(proxyCheck = ProxyCheck.Failed) }
                        return@intent
                    }
                if (state.isEnabled) {
                    return@intent
                }
                reduce { state.copy(proxyCheck = ProxyCheck.InProgress) }
                val isVerified =
                    verifyProxyEndpointUseCase(
                        host = savedEndpoint.host,
                        port = savedEndpoint.port,
                        protocol = savedEndpoint.protocol,
                    )
                reduce {
                    if (mapDraftEndpoint() != savedEndpoint) {
                        state
                    } else {
                        state.copy(
                            proxyCheck =
                                if (isVerified) {
                                    ProxyCheck.Succeeded(savedEndpoint)
                                } else {
                                    ProxyCheck.Failed
                                },
                        )
                    }
                }
            }
        }

        private fun saveEndpoint() {
            intent {
                val savedEndpoint = mapDraftEndpoint()
                val proxyCheck = state.proxyCheck
                val isVerified =
                    proxyCheck is ProxyCheck.Succeeded &&
                        proxyCheck.savedEndpoint == savedEndpoint
                val isSaved =
                    if (savedEndpoint != null && isVerified && !state.isEnabled) {
                        saveProxyEndpointUseCase(
                            host = savedEndpoint.host,
                            port = savedEndpoint.port,
                            protocol = savedEndpoint.protocol,
                            replacedEndpoint = state.originalEndpoint,
                        )
                    } else {
                        false
                    }
                postSideEffect(
                    if (isSaved) {
                        SavedProxyEditorSideEffect.Saved
                    } else {
                        SavedProxyEditorSideEffect.Rejected
                    },
                )
            }
        }

        private fun applyDiscoveredProxy(discoveredProxy: DiscoveredProxy) {
            intent {
                if (state.isEnabled) {
                    return@intent
                }
                reduce {
                    state.copy(
                        host = discoveredProxy.host,
                        portText = discoveredProxy.port.toString(),
                        protocol = discoveredProxy.protocol,
                        proxyCheck = ProxyCheck.Idle,
                    )
                }
            }
        }

        private fun rescanLan() {
            intent {
                if (!state.isEnabled) {
                    lanProxyScanner.scan().collect { scanState ->
                        reduce { state.copy(lanScan = scanState) }
                    }
                }
            }
        }

        private suspend fun Syntax<SavedProxyEditorState, SavedProxyEditorSideEffect>.subscribeToState() {
            observeKillSwitchStateUseCase.stateFlow.collect { observedState ->
                reduce {
                    state.copy(
                        isEnabled = observedState.connectionState is VpnConnectionState.Connected,
                    )
                }
            }
        }

        private fun mapDraftEndpoint(): SavedProxyEndpoint? {
            val port =
                container.stateFlow.value.portText
                    .toIntOrNull()
            val state = container.stateFlow.value
            if (!ProxyEndpoint.isValid(host = state.host, port = port) || port == null) {
                return null
            }
            return SavedProxyEndpoint(host = state.host, port = port, protocol = state.protocol)
        }
    }

sealed interface SavedProxyEditorIntent {
    data class Initialize(
        val savedEndpoint: SavedProxyEndpoint?,
    ) : SavedProxyEditorIntent

    data class HostChanged(
        val host: String,
    ) : SavedProxyEditorIntent

    data class PortChanged(
        val portText: String,
    ) : SavedProxyEditorIntent

    data class DiscoveredProxyClick(
        val discoveredProxy: DiscoveredProxy,
    ) : SavedProxyEditorIntent

    data object RescanClick : SavedProxyEditorIntent

    data object CheckClick : SavedProxyEditorIntent

    data object SaveClick : SavedProxyEditorIntent
}

sealed interface SavedProxyEditorSideEffect {
    data object Saved : SavedProxyEditorSideEffect

    data object Rejected : SavedProxyEditorSideEffect
}
