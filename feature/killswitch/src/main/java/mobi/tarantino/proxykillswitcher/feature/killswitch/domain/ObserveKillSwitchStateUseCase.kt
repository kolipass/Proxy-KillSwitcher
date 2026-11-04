package mobi.tarantino.proxykillswitcher.feature.killswitch.domain

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchRepository
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchSettings
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.VpnConnectionState
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.VpnConnectionStateHolder

@Inject
class ObserveKillSwitchStateUseCase
    constructor(
        private val killSwitchRepository: KillSwitchRepository,
        private val vpnConnectionStateHolder: VpnConnectionStateHolder,
    ) {
        val stateFlow: Flow<State> =
            combine(
                killSwitchRepository.settingsFlow,
                vpnConnectionStateHolder.connectionStateFlow,
                transform = ::mapState,
            )

        val state: State
            get() = mapState(killSwitchRepository.settingsFlow.value, vpnConnectionStateHolder.connectionState)

        private fun mapState(
            settings: KillSwitchSettings,
            connectionState: VpnConnectionState,
        ): State {
            return State(settings = settings, connectionState = connectionState)
        }

        data class State(
            val settings: KillSwitchSettings,
            val connectionState: VpnConnectionState,
        )
    }
