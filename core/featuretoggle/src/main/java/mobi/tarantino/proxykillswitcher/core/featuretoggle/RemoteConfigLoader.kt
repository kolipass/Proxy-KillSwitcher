package mobi.tarantino.proxykillswitcher.core.featuretoggle

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class RemoteConfigLoader {
    private val mutableRemoteConfigLoaderFlow = MutableSharedFlow<RemoteConfigLoader>(replay = 1)

    var exampleFeatureRemotelyEnabled: Boolean = false
        protected set

    var killSwitchRemotelyEnabled: Boolean = false
        protected set

    val remoteConfigLoaderFlow: Flow<RemoteConfigLoader> = mutableRemoteConfigLoaderFlow.asSharedFlow()

    init {
        mutableRemoteConfigLoaderFlow.tryEmit(this)
    }

    abstract suspend fun loadConfig()

    protected abstract fun getBoolean(
        key: String,
        default: Boolean = false,
    ): Boolean

    protected fun applyLoadedConfig() {
        exampleFeatureRemotelyEnabled = getBoolean("example_feature_enabled", default = false)
        killSwitchRemotelyEnabled = getBoolean("kill_switch_enabled", default = false)
        mutableRemoteConfigLoaderFlow.tryEmit(this)
    }
}
