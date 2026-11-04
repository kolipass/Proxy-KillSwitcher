package mobi.tarantino.proxykillswitcher.core.di.viewmodel

import dev.zacsweers.metro.Inject
import kotlin.reflect.KClass

@Inject
class ViewModelFactoryProvider(
    val viewModelFactory: ViewModelFactory,
    val assistedFactories: Map<KClass<out AssistedViewModelFactory>, AssistedViewModelFactory>,
)
