package mobi.tarantino.proxykillswitcher.feature.killswitch.vpn

import mobi.tarantino.proxykillswitcher.core.tunnel.TunnelEngine
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchRepository
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ObserveKillSwitchStateUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.VerifyProxyEndpointUseCase

interface KillSwitchGraphOwner {
    val killSwitchRepository: KillSwitchRepository

    val observeKillSwitchStateUseCase: ObserveKillSwitchStateUseCase

    val verifyProxyEndpointUseCase: VerifyProxyEndpointUseCase

    val vpnConnectionStateHolder: VpnConnectionStateHolder

    val tunnelEngine: TunnelEngine
}
