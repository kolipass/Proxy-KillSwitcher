package mobi.tarantino.proxykillswitcher.feature.killswitch.di

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import mobi.tarantino.proxykillswitcher.core.di.AppScope
import mobi.tarantino.proxykillswitcher.core.di.viewmodel.ViewModelKey
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.DefaultLanProxyScanner
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.HttpProxyEndpointVerifier
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchRepository
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.LanProxyScanner
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyEndpointVerifier
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpointsProvider
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.KillSwitchViewModel
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyEditorViewModel
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyListViewModel

@BindingContainer
@ContributesTo(AppScope::class)
interface KillSwitchModule {
    @Binds
    @IntoMap
    @ViewModelKey(KillSwitchViewModel::class)
    fun bindKillSwitchViewModel(viewModel: KillSwitchViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SavedProxyListViewModel::class)
    fun bindSavedProxyListViewModel(viewModel: SavedProxyListViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SavedProxyEditorViewModel::class)
    fun bindSavedProxyEditorViewModel(viewModel: SavedProxyEditorViewModel): ViewModel

    @Binds
    fun bindProxyEndpointVerifier(httpProxyEndpointVerifier: HttpProxyEndpointVerifier): ProxyEndpointVerifier

    @Binds
    fun bindLanProxyScanner(defaultLanProxyScanner: DefaultLanProxyScanner): LanProxyScanner

    @Binds
    fun bindSavedProxyEndpointsProvider(killSwitchRepository: KillSwitchRepository): SavedProxyEndpointsProvider
}
