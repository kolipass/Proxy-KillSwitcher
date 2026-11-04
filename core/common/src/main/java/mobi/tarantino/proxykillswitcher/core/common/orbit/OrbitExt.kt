package mobi.tarantino.proxykillswitcher.core.common.orbit

import kotlinx.coroutines.CoroutineScope
import mobi.tarantino.proxykillswitcher.core.common.loading.LoadingState
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.Syntax

/** A container that starts in [LoadingState.Loading] and runs [onCreate] once to fill it. */
fun <T, SE : Any> CoroutineScope.loadableContainer(
    onCreate: suspend Syntax<LoadingState<T>, SE>.() -> Unit,
): Container<LoadingState<T>, SE> = container(LoadingState.Loading, onCreate = onCreate)

suspend fun <T, SE : Any> Syntax<LoadingState<T>, SE>.reduceToLoading() = reduce { LoadingState.Loading }

suspend fun <T, SE : Any> Syntax<LoadingState<T>, SE>.reduceToSuccess(block: suspend () -> T) {
    val result = runCatching { block() }
    reduce {
        result.fold(
            onSuccess = { LoadingState.Success(it) },
            onFailure = { LoadingState.Failure(it) },
        )
    }
}

suspend fun <T, SE : Any> Syntax<LoadingState<T>, SE>.reduceOnSuccess(block: (T) -> T) {
    reduce {
        val current = state
        if (current is LoadingState.Success) current.copy(data = block(current.data)) else current
    }
}
