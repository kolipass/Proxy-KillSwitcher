package mobi.tarantino.proxykillswitcher.feature.killswitch.vpn

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mobi.tarantino.proxykillswitcher.core.di.AppScope

sealed interface VpnConnectionState {
    data object Disconnected : VpnConnectionState

    data object Connected : VpnConnectionState

    data object Failed : VpnConnectionState
}

@SingleIn(AppScope::class)
@Inject
class VpnConnectionStateHolder {
    private val mutableConnectionStateFlow =
        MutableStateFlow<VpnConnectionState>(VpnConnectionState.Disconnected)

    val connectionStateFlow: StateFlow<VpnConnectionState> = mutableConnectionStateFlow.asStateFlow()

    val connectionState: VpnConnectionState
        get() = connectionStateFlow.value

    fun publishConnected() {
        mutableConnectionStateFlow.value = VpnConnectionState.Connected
    }

    fun publishDisconnected() {
        mutableConnectionStateFlow.value = VpnConnectionState.Disconnected
    }

    fun publishFailed() {
        mutableConnectionStateFlow.value = VpnConnectionState.Failed
    }
}
