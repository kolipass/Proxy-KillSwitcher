package mobi.tarantino.proxykillswitcher

import android.app.Application
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mobi.tarantino.proxykillswitcher.core.di.AppScope
import mobi.tarantino.proxykillswitcher.core.di.viewmodel.AssistedViewModelFactory
import mobi.tarantino.proxykillswitcher.core.di.viewmodel.ViewModelFactoryProvider
import mobi.tarantino.proxykillswitcher.core.preloading.AppInitializer
import mobi.tarantino.proxykillswitcher.core.preloading.LoadingStateRepository
import mobi.tarantino.proxykillswitcher.core.tunnel.TunnelEngine
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchRepository
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ObserveKillSwitchStateUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.VerifyProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.VpnConnectionStateHolder
import kotlin.reflect.KClass

@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class)
interface AppGraph {
    val viewModelFactoryProvider: ViewModelFactoryProvider
    val appInitializer: AppInitializer
    val killSwitchRepository: KillSwitchRepository
    val observeKillSwitchStateUseCase: ObserveKillSwitchStateUseCase
    val verifyProxyEndpointUseCase: VerifyProxyEndpointUseCase
    val vpnConnectionStateHolder: VpnConnectionStateHolder
    val tunnelEngine: TunnelEngine

    @Multibinds(allowEmpty = true)
    val assistedFactories: Map<KClass<out AssistedViewModelFactory>, AssistedViewModelFactory>

    @Multibinds(allowEmpty = true)
    val loadingStateRepositories: Set<LoadingStateRepository>

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides application: Application,
        ): AppGraph
    }
}
