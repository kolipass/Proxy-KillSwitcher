package mobi.tarantino.proxykillswitcher.core.featuretoggle

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mobi.tarantino.proxykillswitcher.core.di.AppScope

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AllEnabledRemoteConfigLoader
    @Inject
    constructor() : RemoteConfigLoader() {
        init {
            applyLoadedConfig()
        }

        override suspend fun loadConfig() {
            applyLoadedConfig()
        }

        override fun getBoolean(
            key: String,
            default: Boolean,
        ): Boolean {
            return true
        }
    }
