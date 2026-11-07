package mobi.tarantino.proxykillswitcher.feature.update

import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateInstallMode
import mobi.tarantino.proxykillswitcher.feature.update.presentation.ui.UpdatePhase
import mobi.tarantino.proxykillswitcher.feature.update.presentation.ui.UpdatePrimaryActionKind
import mobi.tarantino.proxykillswitcher.feature.update.presentation.ui.UpdateState
import mobi.tarantino.proxykillswitcher.feature.update.presentation.ui.UpdateUiStateMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateUiStateMapperTest {
    private val sut = UpdateUiStateMapper()

    @Test
    fun `When update is missing, Then block is hidden`() {
        val actual = sut.map(UpdateState())

        assertFalse(actual.isVisible)
        assertEquals("", actual.titleText)
    }

    @Test
    fun `When update is downloading, Then percent progress is shown`() {
        val actual =
            sut.map(
                UpdateState(
                    update = UpdateGenerator.AVAILABLE_UPDATE,
                    phase = UpdatePhase.Downloading,
                    downloadedBytes = DOWNLOADED_BYTES,
                    totalBytes = TOTAL_BYTES,
                ),
            )

        assertTrue(actual.isVisible)
        assertTrue(actual.isProgressVisible)
        assertEquals(0.5f, actual.progressFraction, PROGRESS_DELTA)
        assertEquals("Downloading 50%", actual.statusText)
        assertFalse(actual.isPrimaryActionVisible)
        assertTrue(actual.isPostponeVisible)
    }

    @Test
    fun `When update is ready for self install, Then install action is shown`() {
        val actual =
            sut.map(
                UpdateState(
                    update = UpdateGenerator.AVAILABLE_UPDATE,
                    phase = UpdatePhase.Ready,
                    installMode = UpdateInstallMode.SelfInstall,
                ),
            )

        assertTrue(actual.isPrimaryActionVisible)
        assertEquals(UpdatePrimaryActionKind.Install, actual.primaryActionKind)
        assertEquals("Install", actual.primaryActionText)
        assertTrue(actual.isPostponeVisible)
        assertFalse(actual.isRetryVisible)
    }

    @Test
    fun `When build cannot install itself, Then release page action is shown`() {
        val actual =
            sut.map(
                UpdateState(
                    update = UpdateGenerator.AVAILABLE_UPDATE,
                    phase = UpdatePhase.Ready,
                    installMode = UpdateInstallMode.OpenReleasePage,
                ),
            )

        assertEquals(UpdatePrimaryActionKind.OpenReleasePage, actual.primaryActionKind)
        assertEquals("Open on GitHub", actual.primaryActionText)
    }

    @Test
    fun `When update failed, Then retry is visible and primary action is hidden`() {
        val actual =
            sut.map(
                UpdateState(
                    update = UpdateGenerator.AVAILABLE_UPDATE,
                    phase = UpdatePhase.Failed,
                ),
            )

        assertFalse(actual.isPrimaryActionVisible)
        assertTrue(actual.isRetryVisible)
        assertFalse(actual.isProgressVisible)
    }

    @Test
    fun `When update is postponed, Then block is hidden`() {
        val actual =
            sut.map(
                UpdateState(
                    update = UpdateGenerator.AVAILABLE_UPDATE,
                    phase = UpdatePhase.Ready,
                    isPostponed = true,
                ),
            )

        assertFalse(actual.isVisible)
    }

    companion object {
        private const val DOWNLOADED_BYTES = 512L
        private const val TOTAL_BYTES = 1_024L
        private const val PROGRESS_DELTA = 0.001f
    }
}
