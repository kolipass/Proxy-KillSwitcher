package mobi.tarantino.proxykillswitcher.core.di.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.zacsweers.metro.Inject
import kotlin.reflect.KClass

@Inject
class ViewModelFactory(
    private val providers: Map<KClass<out ViewModel>, () -> ViewModel>,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val provider =
            providers[modelClass.kotlin]
                ?: error("No ViewModel provider bound for ${modelClass.name}")
        return provider() as T
    }
}
