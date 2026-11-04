package mobi.tarantino.proxykillswitcher.feature.killswitch.domain

import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchRepository
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.VpnConnectionState

@Inject
class DeleteSavedProxyEndpointUseCase
    constructor(
        private val killSwitchRepository: KillSwitchRepository,
        private val observeKillSwitchStateUseCase: ObserveKillSwitchStateUseCase,
    ) {
        operator fun invoke(savedEndpoint: SavedProxyEndpoint): Boolean {
            if (observeKillSwitchStateUseCase.state.connectionState is VpnConnectionState.Connected) {
                return false
            }
            killSwitchRepository.deleteEndpoint(savedEndpoint)
            return true
        }
    }
