package mobi.tarantino.proxykillswitcher.feature.killswitch

import android.content.Intent
import android.content.pm.PackageManager
import android.service.quicksettings.Tile
import io.mockk.every
import io.mockk.mockk
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.CONNECTED_OBSERVED_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.DISCONNECTED_OBSERVED_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.KillSwitchTileService
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.createOpenAddProxyIntent
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.isProxyVpnTogglable
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.mapKillSwitchTileState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KillSwitchTileServiceTest {
    @Test
    fun `When vpn is connected, Then tile is active`() {
        val actual = mapKillSwitchTileState(CONNECTED_OBSERVED_STATE)

        assertEquals(Tile.STATE_ACTIVE, actual)
    }

    @Test
    fun `When vpn is disconnected with valid proxy, Then tile is inactive`() {
        val actual = mapKillSwitchTileState(DISCONNECTED_OBSERVED_STATE)

        assertEquals(Tile.STATE_INACTIVE, actual)
    }

    @Test
    fun `When no saved proxies exist, Then tile is unavailable`() {
        val emptyState =
            DISCONNECTED_OBSERVED_STATE.copy(
                settings = DISCONNECTED_OBSERVED_STATE.settings.copy(savedEndpoints = emptyList()),
            )
        val actual = mapKillSwitchTileState(emptyState)

        assertEquals(Tile.STATE_UNAVAILABLE, actual)
    }

    @Test
    fun `When proxy endpoint is invalid, Then tile is unavailable`() {
        val invalidState =
            DISCONNECTED_OBSERVED_STATE.copy(
                settings = DISCONNECTED_OBSERVED_STATE.settings.copy(host = "", port = 0),
            )
        val actual = mapKillSwitchTileState(invalidState)

        assertEquals(Tile.STATE_UNAVAILABLE, actual)
    }

    @Test
    fun `When settings contain saved endpoints and valid endpoint, Then proxy is togglable`() {
        val actual = isProxyVpnTogglable(DISCONNECTED_OBSERVED_STATE)

        assertTrue(actual)
    }

    @Test
    fun `When settings contain zero saved endpoints, Then proxy is not togglable`() {
        val emptyState =
            DISCONNECTED_OBSERVED_STATE.copy(
                settings = DISCONNECTED_OBSERVED_STATE.settings.copy(savedEndpoints = emptyList()),
            )
        val actual = isProxyVpnTogglable(emptyState)

        assertFalse(actual)
    }

    @Test
    fun `When package manager has launch intent, Then add proxy intent is created`() {
        val packageManager: PackageManager = mockk()
        val baseIntent: Intent = mockk(relaxed = true)
        every { packageManager.getLaunchIntentForPackage(PACKAGE_NAME) } returns baseIntent

        val actual = createOpenAddProxyIntent(packageManager, PACKAGE_NAME)

        assertEquals(baseIntent, actual)
        io.mockk.verify {
            baseIntent.putExtra(KillSwitchTileService.EXTRA_OPEN_ADD_PROXY, true)
            baseIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    @Test
    fun `When package manager has no launch intent, Then null is returned`() {
        val packageManager: PackageManager = mockk()
        every { packageManager.getLaunchIntentForPackage(PACKAGE_NAME) } returns null

        val actual = createOpenAddProxyIntent(packageManager, PACKAGE_NAME)

        assertNull(actual)
    }

    companion object {
        private const val PACKAGE_NAME = "mobi.tarantino.proxykillswitcher"
    }
}
