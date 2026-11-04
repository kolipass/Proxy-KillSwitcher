package mobi.tarantino.proxykillswitcher.core.di.viewmodel

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.MapKey
import kotlin.reflect.KClass

@MapKey
annotation class ViewModelKey(
    val value: KClass<out ViewModel>,
)
