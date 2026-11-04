package mobi.tarantino.proxykillswitcher.core.preloading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * One independent thing that must finish before the app is considered ready. Implement this per
 * data source that needs to preload, and contribute it via `@IntoSet` into the
 * `Set<LoadingStateRepository>` that [AsyncPreloadingUseCase] combines.
 */
abstract class LoadingStateRepository(
    private val scope: CoroutineScope,
) {
    enum class State { INITIAL, LOADING, SUCCESS, FAILURE }

    private val _loadingState = MutableStateFlow(State.INITIAL)
    val loadingState: StateFlow<State> = _loadingState.asStateFlow()

    fun loadAsyncIfNeeded() {
        if (_loadingState.value != State.INITIAL) return
        _loadingState.value = State.LOADING
        scope.launch {
            _loadingState.value =
                runCatching { load() }.fold(
                    onSuccess = { State.SUCCESS },
                    onFailure = { State.FAILURE },
                )
        }
    }

    protected abstract suspend fun load()
}
