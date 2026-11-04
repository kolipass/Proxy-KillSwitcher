package mobi.tarantino.proxykillswitcher.core.preloading

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Deliberately minimal: Idle -> Loading -> Ready. This is *not* a port of binomoAndroid's full
 * `Core` state machine (auth check, maintenance state, typed error codes) — that apparatus earns
 * its complexity only once there are enough independent startup dependencies to justify it. Grow
 * this into that shape if/when this project reaches that scale, not before.
 */
sealed interface AppInitState {
    data object Idle : AppInitState

    data object Loading : AppInitState

    data object Ready : AppInitState

    data class Error(
        val cause: Throwable,
    ) : AppInitState
}

@Inject
class AppInitializer(
    private val preloading: AsyncPreloadingUseCase,
) {
    private val _state = MutableStateFlow<AppInitState>(AppInitState.Idle)
    val state: StateFlow<AppInitState> = _state.asStateFlow()

    fun start(scope: CoroutineScope) {
        if (_state.value != AppInitState.Idle) return
        _state.value = AppInitState.Loading
        preloading.startIfNeeded()
        scope.launch {
            preloading.isReady.collect { ready ->
                if (ready) _state.value = AppInitState.Ready
            }
        }
    }
}
