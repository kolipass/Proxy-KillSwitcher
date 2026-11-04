package mobi.tarantino.proxykillswitcher

import android.app.Application
import dev.zacsweers.metro.createGraphFactory
import mobi.tarantino.proxykillswitcher.core.tunnel.TunnelEngine
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchRepository
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ObserveKillSwitchStateUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.VerifyProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.KillSwitchGraphOwner
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.VpnConnectionStateHolder

class ProxyKillSwitcherApp :
    Application(),
    KillSwitchGraphOwner {
    val graph: AppGraph by lazy {
        createGraphFactory<AppGraph.Factory>().create(this)
    }

    override val killSwitchRepository: KillSwitchRepository by lazy {
        graph.killSwitchRepository
    }

    override val observeKillSwitchStateUseCase: ObserveKillSwitchStateUseCase by lazy {
        graph.observeKillSwitchStateUseCase
    }

    override val verifyProxyEndpointUseCase: VerifyProxyEndpointUseCase by lazy {
        graph.verifyProxyEndpointUseCase
    }

    override val vpnConnectionStateHolder: VpnConnectionStateHolder by lazy {
        graph.vpnConnectionStateHolder
    }

    override val tunnelEngine: TunnelEngine by lazy {
        graph.tunnelEngine
    }
}
