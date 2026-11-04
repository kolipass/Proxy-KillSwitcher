package mobi.tarantino.proxykillswitcher.core.common.loading

/** Common shape for an Orbit state that starts empty, loads once, and either fills or fails. */
sealed interface LoadingState<out T> {
    data object Loading : LoadingState<Nothing>

    data class Success<T>(
        val data: T,
    ) : LoadingState<T>

    data class Failure(
        val error: Throwable,
    ) : LoadingState<Nothing>
}
