package mobi.tarantino.proxykillswitcher.feature.update

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import mobi.tarantino.proxykillswitcher.feature.update.data.LatestReleaseResult
import mobi.tarantino.proxykillswitcher.feature.update.data.UpdateRepository
import mobi.tarantino.proxykillswitcher.feature.update.domain.CheckForUpdateUseCase
import mobi.tarantino.proxykillswitcher.feature.update.domain.InstalledAppInfo
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateCheck
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateClock
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateInstallMode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CheckForUpdateUseCaseTest {
    private val updateRepository: UpdateRepository = mockk()
    private val installedAppInfo: InstalledAppInfo = mockk()
    private val updateClock: UpdateClock = mockk()

    private val sut =
        CheckForUpdateUseCase(
            updateRepository = updateRepository,
            installedAppInfo = installedAppInfo,
            updateClock = updateClock,
        )

    @BeforeEach
    fun setUp() {
        enableAllConditions()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    suspend fun `When release is newer, Then available update is returned`() {
        val actual = sut()

        val expected =
            UpdateCheck.Available(
                update = UpdateGenerator.AVAILABLE_UPDATE,
                installMode = UpdateInstallMode.SelfInstall,
            )
        assertEquals(expected, actual)
    }

    @Test
    suspend fun `When release is not newer, Then no update is returned`() {
        coEvery { updateRepository.latestRelease(any(), any()) } returns
            LatestReleaseResult.Loaded(UpdateGenerator.LATEST_RELEASE.copy(tagName = "v0.0.1"))

        val actual = sut()

        assertEquals(UpdateCheck.NoUpdate, actual)
    }

    @Test
    suspend fun `When repository is throttled, Then throttled is returned`() {
        coEvery { updateRepository.latestRelease(any(), any()) } returns LatestReleaseResult.Throttled

        val actual = sut()

        assertEquals(UpdateCheck.Throttled, actual)
    }

    @Test
    suspend fun `When repository fails, Then failed check is returned`() {
        coEvery { updateRepository.latestRelease(any(), any()) } returns LatestReleaseResult.Failed

        val actual = sut()

        assertEquals(UpdateCheck.Failed, actual)
    }

    @Test
    suspend fun `When release tag is malformed, Then no update is returned`() {
        coEvery { updateRepository.latestRelease(any(), any()) } returns
            LatestReleaseResult.Loaded(UpdateGenerator.LATEST_RELEASE.copy(tagName = "nightly"))

        val actual = sut()

        assertEquals(UpdateCheck.NoUpdate, actual)
    }

    @Test
    suspend fun `When build is debuggable, Then release page install mode is returned`() {
        every { installedAppInfo.isDebuggable } returns true

        val actual = sut()

        assertEquals(UpdateInstallMode.OpenReleasePage, (actual as UpdateCheck.Available).installMode)
    }

    @Test
    suspend fun `When check is forced, Then repository bypasses the throttle`() {
        sut(force = true)

        coVerify(exactly = 1) { updateRepository.latestRelease(CURRENT_TIME_MILLIS, true) }
    }

    private fun enableAllConditions() {
        every { updateClock.nowMillis() } returns CURRENT_TIME_MILLIS
        every { installedAppInfo.versionCode } returns UpdateGenerator.INSTALLED_VERSION_CODE
        every { installedAppInfo.isDebuggable } returns false
        coEvery { updateRepository.latestRelease(any(), any()) } returns
            LatestReleaseResult.Loaded(UpdateGenerator.LATEST_RELEASE)
    }

    companion object {
        private const val CURRENT_TIME_MILLIS = 1_000_000L
    }
}
