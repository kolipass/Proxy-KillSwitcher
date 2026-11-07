package mobi.tarantino.proxykillswitcher.feature.update

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import mobi.tarantino.proxykillswitcher.feature.update.domain.CheckForUpdateUseCase
import mobi.tarantino.proxykillswitcher.feature.update.domain.DownloadUpdateUseCase
import mobi.tarantino.proxykillswitcher.feature.update.domain.InstallResult
import mobi.tarantino.proxykillswitcher.feature.update.domain.InstallUpdateUseCase
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateCheck
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateDownload
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateInstallMode
import mobi.tarantino.proxykillswitcher.feature.update.presentation.ui.UpdateIntent
import mobi.tarantino.proxykillswitcher.feature.update.presentation.ui.UpdatePhase
import mobi.tarantino.proxykillswitcher.feature.update.presentation.ui.UpdateSideEffect
import mobi.tarantino.proxykillswitcher.feature.update.presentation.ui.UpdateState
import mobi.tarantino.proxykillswitcher.feature.update.presentation.ui.UpdateUiStateMapper
import mobi.tarantino.proxykillswitcher.feature.update.presentation.ui.UpdateViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.orbitmvi.orbit.test.test

class UpdateViewModelTest {
    private val testScope = TestScope(StandardTestDispatcher())
    private val updateUiStateMapper: UpdateUiStateMapper = mockk()
    private val checkForUpdateUseCase: CheckForUpdateUseCase = mockk()
    private val downloadUpdateUseCase: DownloadUpdateUseCase = mockk()
    private val installUpdateUseCase: InstallUpdateUseCase = mockk()

    private val sut by lazy {
        UpdateViewModel(
            uiStateMapper = updateUiStateMapper,
            checkForUpdateUseCase = checkForUpdateUseCase,
            downloadUpdateUseCase = downloadUpdateUseCase,
            installUpdateUseCase = installUpdateUseCase,
        )
    }

    @BeforeEach
    fun setUp() {
        enableAllConditions()
    }

    @AfterEach
    fun tearDown() {
        testScope.cancel()
        unmockkAll()
    }

    @Test
    suspend fun `When update is available, Then download progress and ready state are emitted`() {
        coEvery { checkForUpdateUseCase(any()) } returns
            UpdateCheck.Available(
                update = UpdateGenerator.AVAILABLE_UPDATE,
                installMode = UpdateInstallMode.SelfInstall,
            )
        every { downloadUpdateUseCase(any()) } returns
            flowOf(
                UpdateDownload.InProgress(downloadedBytes = DOWNLOADED_BYTES, totalBytes = TOTAL_BYTES),
                UpdateDownload.Ready(apkFile = UpdateGenerator.APK_FILE),
            )

        sut.test(testScope = testScope, initialState = UpdateState()) {
            val onCreateJob = runOnCreate()
            testScope.advanceUntilIdleAll()
            expectState {
                UpdateState(
                    update = UpdateGenerator.AVAILABLE_UPDATE,
                    phase = UpdatePhase.Downloading,
                    totalBytes = TOTAL_BYTES,
                )
            }
            expectState {
                UpdateState(
                    update = UpdateGenerator.AVAILABLE_UPDATE,
                    phase = UpdatePhase.Downloading,
                    downloadedBytes = DOWNLOADED_BYTES,
                    totalBytes = TOTAL_BYTES,
                )
            }
            expectState {
                UpdateState(
                    update = UpdateGenerator.AVAILABLE_UPDATE,
                    phase = UpdatePhase.Ready,
                    downloadedBytes = DOWNLOADED_BYTES,
                    totalBytes = TOTAL_BYTES,
                    downloadedApk = UpdateGenerator.APK_FILE,
                )
            }
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }
    }

    @Test
    suspend fun `When install clicked and unknown sources are disabled, Then settings are requested`() {
        every { installUpdateUseCase(any()) } returns InstallResult.UnknownSourcesRequired

        sut.test(testScope = testScope, initialState = UpdateGenerator.READY_STATE) {
            val onCreateJob = runOnCreate()
            testScope.advanceUntilIdleAll()
            sut.onIntent(UpdateIntent.InstallClick)
            testScope.advanceUntilIdleAll()
            expectState { UpdateGenerator.READY_STATE.copy(phase = UpdatePhase.Installing) }
            expectState { UpdateGenerator.READY_STATE }
            expectSideEffect(UpdateSideEffect.OpenUnknownSourcesSettings)
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }
    }

    @Test
    suspend fun `When postpone clicked, Then block is hidden`() {
        sut.test(testScope = testScope, initialState = UpdateGenerator.READY_STATE) {
            val onCreateJob = runOnCreate()
            testScope.advanceUntilIdleAll()
            sut.onIntent(UpdateIntent.PostponeClick)
            testScope.advanceUntilIdleAll()
            expectState { UpdateGenerator.READY_STATE.copy(isPostponed = true) }
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }
    }

    private fun enableAllConditions() {
        coEvery { checkForUpdateUseCase(any()) } returns UpdateCheck.Throttled
        every { installUpdateUseCase(any()) } returns InstallResult.InstallerLaunched
    }

    companion object {
        private const val DOWNLOADED_BYTES = 512L
        private const val TOTAL_BYTES = 1_024L
    }
}
