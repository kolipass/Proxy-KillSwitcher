package mobi.tarantino.proxykillswitcher.core.di

/**
 * Lazily creates and caches a Metro `@GraphExtension` instance, with [release] for tearing it
 * down when its scope ends (e.g. leaving a feature that owns its own graph). Not needed for the
 * single root [AppGraph][dev.zacsweers.metro.DependencyGraph] today — reach for it the first
 * time a feature needs a shorter-lived scope than the app's.
 */
class GraphContainer<T : Any>(
    private val factory: () -> T,
) {
    @Volatile
    private var instance: T? = null

    val graph: T
        get() =
            instance ?: synchronized(this) {
                instance ?: factory().also { instance = it }
            }

    fun release() {
        instance = null
    }
}
