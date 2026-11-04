package mobi.tarantino.proxykillswitcher.core.di.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel

/**
 * Every screen reaches its ViewModel through this instead of touching the Metro graph directly —
 * provided once, near the graph root, by whatever holds the app's [ViewModelFactoryProvider].
 */
val LocalViewModelFactoryProvider =
    staticCompositionLocalOf<ViewModelFactoryProvider> {
        error("No ViewModelFactoryProvider provided in this composition")
    }

@Composable
inline fun <reified VM : ViewModel> viewModel(): VM {
    val provider = LocalViewModelFactoryProvider.current
    return composeViewModel(factory = provider.viewModelFactory)
}

@Composable
inline fun <reified AF : AssistedViewModelFactory, reified VM : ViewModel> assistedViewModel(
    key: String? = null,
    crossinline create: (AF) -> VM,
): VM {
    val provider = LocalViewModelFactoryProvider.current
    val factory =
        remember(key) {
            @Suppress("UNCHECKED_CAST")
            val assistedFactory =
                provider.assistedFactories[AF::class] as? AF
                    ?: error("No AssistedViewModelFactory bound for ${AF::class}")
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = create(assistedFactory) as T
            }
        }
    return composeViewModel(factory = factory, key = key)
}
