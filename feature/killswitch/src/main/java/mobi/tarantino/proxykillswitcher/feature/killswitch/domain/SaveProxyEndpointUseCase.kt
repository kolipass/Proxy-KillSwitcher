package mobi.tarantino.proxykillswitcher.feature.killswitch.domain

import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchRepository
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.VpnConnectionState

@Inject
class SaveProxyEndpointUseCase
    constructor(
        private val killSwitchRepository: KillSwitchRepository,
        private val observeKillSwitchStateUseCase: ObserveKillSwitchStateUseCase,
    ) {
        operator fun invoke(
            host: String,
            port: Int,
            protocol: ProxyProtocol,
            replacedEndpoint: SavedProxyEndpoint? = null,
        ): Boolean {
            if (observeKillSwitchStateUseCase.state.connectionState is VpnConnectionState.Connected) {
                return false
            }
            killSwitchRepository.saveEndpoint(
                savedEndpoint = SavedProxyEndpoint(host = host.trim(), port = port, protocol = protocol),
                replacedEndpoint = replacedEndpoint,
            )
            return true
        }
    }
