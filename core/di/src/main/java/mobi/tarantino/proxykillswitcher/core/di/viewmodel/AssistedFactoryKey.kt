package mobi.tarantino.proxykillswitcher.core.di.viewmodel

import dev.zacsweers.metro.MapKey
import kotlin.reflect.KClass

@MapKey
annotation class AssistedFactoryKey(
    val value: KClass<out AssistedViewModelFactory>,
)
