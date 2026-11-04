package mobi.tarantino.proxykillswitcher.feature.killswitch

import android.service.quicksettings.Tile
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.CONNECTED_OBSERVED_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.DISCONNECTED_OBSERVED_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.mapKillSwitchTileState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KillSwitchTileServiceTest {
    @Test
    fun `When vpn is connected, Then tile is active`() {
        val actual = mapKillSwitchTileState(CONNECTED_OBSERVED_STATE)

        assertEquals(Tile.STATE_ACTIVE, actual)
    }

    @Test
    fun `When vpn is disconnected, Then tile is inactive`() {
        val actual = mapKillSwitchTileState(DISCONNECTED_OBSERVED_STATE)

        assertEquals(Tile.STATE_INACTIVE, actual)
    }
}
