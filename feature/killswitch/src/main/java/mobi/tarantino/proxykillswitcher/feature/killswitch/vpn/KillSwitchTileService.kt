package mobi.tarantino.proxykillswitcher.feature.killswitch.vpn

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ObserveKillSwitchStateUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyEndpoint

class KillSwitchTileService : TileService() {
    private val killSwitchGraphOwner by lazy { application as KillSwitchGraphOwner }
    private var tileListeningScope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val startedListeningScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        tileListeningScope = startedListeningScope
        startedListeningScope.launch {
            killSwitchGraphOwner.observeKillSwitchStateUseCase.stateFlow
                .collect { state -> renderTile(mapKillSwitchTileState(state)) }
        }
    }

    override fun onStopListening() {
        tileListeningScope?.cancel()
        tileListeningScope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun(::toggleProxyVpn)
        } else {
            toggleProxyVpn()
        }
    }

    private fun toggleProxyVpn() {
        if (killSwitchGraphOwner.observeKillSwitchStateUseCase.state.connectionState is VpnConnectionState.Connected) {
            killSwitchGraphOwner.killSwitchRepository.setDesiredEnabled(false)
            ProxyVpnService.stop(this)
        } else {
            requestProxyVpnStart()
        }
    }

    private fun requestProxyVpnStart() {
        val state = killSwitchGraphOwner.observeKillSwitchStateUseCase.state
        val isTogglable = isProxyVpnTogglable(state)
        if (!isTogglable) {
            return
        }
        killSwitchGraphOwner.killSwitchRepository.setDesiredEnabled(true)
        val startProxyVpnIntent =
            Intent(this, StartProxyVpnActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    START_PROXY_VPN_REQUEST_CODE,
                    startProxyVpnIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(startProxyVpnIntent)
        }
    }

    private fun renderTile(tileState: Int) {
        val tile = qsTile ?: return
        tile.state = tileState
        tile.updateTile()
    }

    private fun isProxyVpnTogglable(state: ObserveKillSwitchStateUseCase.State): Boolean {
        return ProxyEndpoint.isValid(host = state.settings.host, port = state.settings.port)
    }

    companion object {
        private const val START_PROXY_VPN_REQUEST_CODE = 2001
    }
}

internal fun mapKillSwitchTileState(state: ObserveKillSwitchStateUseCase.State): Int {
    return when {
        !ProxyEndpoint.isValid(host = state.settings.host, port = state.settings.port) -> Tile.STATE_UNAVAILABLE
        state.connectionState is VpnConnectionState.Connected -> Tile.STATE_ACTIVE
        else -> Tile.STATE_INACTIVE
    }
}
