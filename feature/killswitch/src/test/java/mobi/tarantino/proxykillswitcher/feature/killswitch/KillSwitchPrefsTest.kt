package mobi.tarantino.proxykillswitcher.feature.killswitch

import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchPrefs
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class KillSwitchPrefsTest {
    private val sut = KillSwitchPrefs(RuntimeEnvironment.getApplication())

    @Test
    fun `When values saved, Then another prefs instance reads them`() {
        sut.host = EXPECTED_HOST
        sut.port = EXPECTED_PORT
        sut.protocol = EXPECTED_PROTOCOL
        sut.savedEndpoints = EXPECTED_SAVED_ENDPOINTS

        val actual = KillSwitchPrefs(RuntimeEnvironment.getApplication())

        assertEquals(EXPECTED_HOST, actual.host)
        assertEquals(EXPECTED_PORT, actual.port)
        assertEquals(EXPECTED_PROTOCOL, actual.protocol)
        assertEquals(EXPECTED_SAVED_ENDPOINTS, actual.savedEndpoints)
    }

    companion object {
        private const val EXPECTED_HOST = "192.168.1.10"
        private const val EXPECTED_PORT = 1080
        private const val EXPECTED_PROTOCOL = "http"
        private const val EXPECTED_SAVED_ENDPOINTS = "$EXPECTED_HOST|$EXPECTED_PORT|$EXPECTED_PROTOCOL"
    }
}
