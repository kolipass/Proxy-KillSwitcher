package mobi.tarantino.proxykillswitcher.feature.killswitch

import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.KillSwitchUiStateMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KillSwitchUiStateMapperTest {
    private val sut = KillSwitchUiStateMapper()

    @Test
    fun `When draft matches saved endpoint, Then save is disabled`() {
        val actual = sut.map(KillSwitchStateGenerator.INITIAL_STATE)

        assertFalse(actual.isSaveEnabled)
        assertTrue(actual.isCheckEnabled)
        assertEquals("", actual.checkStatusText)
    }

    @Test
    fun `When draft changed without check, Then save is disabled`() {
        val actual = sut.map(KillSwitchStateGenerator.CHANGED_DRAFT_STATE)

        assertFalse(actual.isSaveEnabled)
        assertTrue(actual.isCheckEnabled)
    }

    @Test
    fun `When draft changed and check succeeded, Then save is enabled`() {
        val actual = sut.map(KillSwitchStateGenerator.CHECK_SUCCEEDED_STATE)

        assertTrue(actual.isSaveEnabled)
        assertTrue(actual.isCheckEnabled)
        assertEquals(SUCCEEDED_STATUS_TEXT, actual.checkStatusText)
    }

    @Test
    fun `When check is in progress, Then save and check are disabled`() {
        val actual = sut.map(KillSwitchStateGenerator.CHECKING_STATE)

        assertFalse(actual.isSaveEnabled)
        assertFalse(actual.isCheckEnabled)
        assertTrue(actual.isChecking)
    }

    companion object {
        private const val SUCCEEDED_STATUS_TEXT = "Proxy check succeeded"
    }
}
