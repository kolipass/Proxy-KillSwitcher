package mobi.tarantino.proxykillswitcher.core.preloading

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Inject
class AsyncPreloadingUseCase(
    private val repositories: Set<LoadingStateRepository>,
) {
    val isReady: Flow<Boolean>
        get() =
            combine(repositories.map { it.loadingState }) { states ->
                states.all { it == LoadingStateRepository.State.SUCCESS || it == LoadingStateRepository.State.FAILURE }
            }

    fun startIfNeeded() {
        repositories.forEach { it.loadAsyncIfNeeded() }
    }
}
