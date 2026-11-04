package mobi.tarantino.proxykillswitcher.feature.killswitch

import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchSettings
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ObserveKillSwitchStateUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyProtocol
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.KillSwitchState
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.KillSwitchState.ProxyCheck
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.VpnConnectionState

object KillSwitchStateGenerator {
    const val HOST = "192.168.1.10"
    const val UPDATED_HOST = "10.0.0.2"
    const val PORT = 1080
    val PROTOCOL = ProxyProtocol.Http
    val SAVED_ENDPOINT =
        SavedProxyEndpoint(
            host = HOST,
            port = PORT,
            protocol = PROTOCOL,
        )
    val INITIAL_STATE =
        KillSwitchState(
            host = HOST,
            port = PORT,
            protocol = PROTOCOL,
            draftHost = HOST,
            draftPortText = PORT.toString(),
            draftProtocol = PROTOCOL,
            isEnabled = false,
        )
    val ENABLED_STATE = INITIAL_STATE.copy(isEnabled = true)
    val CONNECTING_STATE = INITIAL_STATE.copy(isConnecting = true)
    val CHANGED_DRAFT_STATE =
        INITIAL_STATE.copy(
            draftHost = UPDATED_HOST,
        )
    val CHECKING_STATE =
        CHANGED_DRAFT_STATE.copy(
            proxyCheck = ProxyCheck.InProgress,
        )
    val CHECK_SUCCEEDED_STATE =
        CHANGED_DRAFT_STATE.copy(
            proxyCheck = ProxyCheck.Succeeded(host = UPDATED_HOST, port = PORT, protocol = PROTOCOL),
        )
    val CHECK_FAILED_STATE =
        CHANGED_DRAFT_STATE.copy(
            proxyCheck = ProxyCheck.Failed,
        )
    val UPDATED_HOST_STATE = INITIAL_STATE.copy(host = UPDATED_HOST)
    val INITIAL_SETTINGS =
        KillSwitchSettings(
            host = INITIAL_STATE.host,
            port = INITIAL_STATE.port,
            protocol = INITIAL_STATE.protocol,
        )
    val UPDATED_SETTINGS = INITIAL_SETTINGS.copy(host = UPDATED_HOST)
    val DISCONNECTED_OBSERVED_STATE =
        ObserveKillSwitchStateUseCase.State(
            settings = INITIAL_SETTINGS,
            connectionState = VpnConnectionState.Disconnected,
        )
    val CONNECTED_OBSERVED_STATE =
        DISCONNECTED_OBSERVED_STATE.copy(connectionState = VpnConnectionState.Connected)
    val FAILED_OBSERVED_STATE =
        DISCONNECTED_OBSERVED_STATE.copy(connectionState = VpnConnectionState.Failed)
}
