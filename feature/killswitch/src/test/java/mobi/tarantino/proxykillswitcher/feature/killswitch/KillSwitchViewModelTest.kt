package mobi.tarantino.proxykillswitcher.feature.killswitch

import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.CHANGED_DRAFT_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.CHECKING_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.CHECK_FAILED_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.CHECK_SUCCEEDED_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.CONNECTED_OBSERVED_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.CONNECTING_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.DISCONNECTED_OBSERVED_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.ENABLED_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.FAILED_OBSERVED_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.INITIAL_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.PORT
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.PROTOCOL
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.UPDATED_HOST
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.UPDATED_HOST_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.UPDATED_SETTINGS
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ObserveKillSwitchStateUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SaveProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ToggleKillSwitchUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.VerifyProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.KillSwitchIntent
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.KillSwitchSideEffect
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.KillSwitchUiStateMapper
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.KillSwitchViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.orbitmvi.orbit.test.test

class KillSwitchViewModelTest {
    private val testScope = TestScope(StandardTestDispatcher())
    private val killSwitchUiStateMapper: KillSwitchUiStateMapper = mockk()
    private val toggleKillSwitchUseCase: ToggleKillSwitchUseCase = mockk()
    private val saveProxyEndpointUseCase: SaveProxyEndpointUseCase = mockk()
    private val verifyProxyEndpointUseCase: VerifyProxyEndpointUseCase = mockk()
    private val observeKillSwitchStateUseCase: ObserveKillSwitchStateUseCase = mockk()
    private val stateFlow = MutableStateFlow(DISCONNECTED_OBSERVED_STATE)

    private val sut by lazy {
        KillSwitchViewModel(
            uiStateMapper = killSwitchUiStateMapper,
            toggleKillSwitchUseCase = toggleKillSwitchUseCase,
            saveProxyEndpointUseCase = saveProxyEndpointUseCase,
            verifyProxyEndpointUseCase = verifyProxyEndpointUseCase,
            observeKillSwitchStateUseCase = observeKillSwitchStateUseCase,
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
    suspend fun `When service reports connected, Then enabled state is emitted`() {
        sut.test(testScope = testScope, initialState = INITIAL_STATE) {
            val onCreateJob = runOnCreate()
            stateFlow.value = CONNECTED_OBSERVED_STATE
            testScope.advanceUntilIdleAll()
            expectState { ENABLED_STATE }
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }
    }

    @Test
    suspend fun `When service reports failure, Then connection failure is announced`() {
        sut.test(testScope = testScope, initialState = INITIAL_STATE) {
            val onCreateJob = runOnCreate()
            stateFlow.value = FAILED_OBSERVED_STATE
            testScope.advanceUntilIdleAll()
            expectSideEffect(KillSwitchSideEffect.ConnectionFailed)
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }
    }

    @Test
    suspend fun `When failure repeats without reconnect, Then connection failure is announced once`() {
        sut.test(testScope = testScope, initialState = INITIAL_STATE) {
            val onCreateJob = runOnCreate()
            stateFlow.value = FAILED_OBSERVED_STATE
            testScope.advanceUntilIdleAll()
            expectSideEffect(KillSwitchSideEffect.ConnectionFailed)
            stateFlow.value = FAILED_OBSERVED_STATE.copy(settings = UPDATED_SETTINGS)
            testScope.advanceUntilIdleAll()
            expectState { UPDATED_HOST_STATE }
            expectNoItems()
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }
    }

    @Test
    suspend fun `When toggle clicked, Then vpn preparation is requested`() {
        sut.test(testScope = testScope, initialState = INITIAL_STATE) {
            val onCreateJob = runOnCreate()
            sut.onIntent(KillSwitchIntent.ToggleClick)
            testScope.advanceUntilIdleAll()
            expectState { CONNECTING_STATE }
            expectSideEffect(KillSwitchSideEffect.PrepareVpn)
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }
    }

    @Test
    suspend fun `When service reports connected while connecting, Then progress is stopped`() {
        sut.test(testScope = testScope, initialState = CONNECTING_STATE) {
            val onCreateJob = runOnCreate()
            stateFlow.value = CONNECTED_OBSERVED_STATE
            testScope.advanceUntilIdleAll()
            expectState { ENABLED_STATE }
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }
    }

    @Test
    suspend fun `When service reports failure while connecting, Then progress is stopped`() {
        sut.test(testScope = testScope, initialState = CONNECTING_STATE) {
            val onCreateJob = runOnCreate()
            stateFlow.value = FAILED_OBSERVED_STATE
            testScope.advanceUntilIdleAll()
            expectState { INITIAL_STATE }
            expectSideEffect(KillSwitchSideEffect.ConnectionFailed)
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }
    }

    @Test
    suspend fun `When vpn permission denied, Then disabled state is emitted`() {
        sut.runActionAndCheckResultState(
            testScope = testScope,
            initialState = ENABLED_STATE,
            expectedState = INITIAL_STATE,
        ) {
            sut.onIntent(KillSwitchIntent.VpnPermissionDenied)
        }

        verify(exactly = 1) { toggleKillSwitchUseCase(false) }
    }

    @Test
    suspend fun `When host changed, Then draft host is updated`() {
        sut.runActionAndCheckResultState(
            testScope = testScope,
            initialState = INITIAL_STATE,
            expectedState = CHANGED_DRAFT_STATE,
        ) {
            sut.onIntent(KillSwitchIntent.HostChanged(UPDATED_HOST))
        }
    }

    @Test
    suspend fun `When proxy check succeeds, Then succeeded status is stored`() {
        coEvery {
            verifyProxyEndpointUseCase(host = UPDATED_HOST, port = PORT, protocol = PROTOCOL)
        } returns true

        sut.test(testScope = testScope, initialState = CHANGED_DRAFT_STATE) {
            val onCreateJob = runOnCreate()
            sut.onIntent(KillSwitchIntent.CheckProxy)
            testScope.advanceUntilIdleAll()
            expectState { CHECKING_STATE }
            expectState { CHECK_SUCCEEDED_STATE }
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }
    }

    @Test
    suspend fun `When proxy check fails, Then failed status is stored`() {
        coEvery {
            verifyProxyEndpointUseCase(host = UPDATED_HOST, port = PORT, protocol = PROTOCOL)
        } returns false

        sut.test(testScope = testScope, initialState = CHANGED_DRAFT_STATE) {
            val onCreateJob = runOnCreate()
            sut.onIntent(KillSwitchIntent.CheckProxy)
            testScope.advanceUntilIdleAll()
            expectState { CHECKING_STATE }
            expectState { CHECK_FAILED_STATE }
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }
    }

    @Test
    suspend fun `When save clicked without check, Then endpoint is not persisted`() {
        sut.test(testScope = testScope, initialState = CHANGED_DRAFT_STATE) {
            val onCreateJob = runOnCreate()
            sut.onIntent(KillSwitchIntent.SaveProxy)
            testScope.advanceUntilIdleAll()
            expectSideEffect(KillSwitchSideEffect.ProxyRejected)
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }

        verify(exactly = 0) { saveProxyEndpointUseCase(host = UPDATED_HOST, port = PORT, protocol = PROTOCOL) }
    }

    @Test
    suspend fun `When save clicked after successful check, Then endpoint is persisted`() {
        every {
            saveProxyEndpointUseCase(host = UPDATED_HOST, port = PORT, protocol = PROTOCOL)
        } returns true

        sut.test(testScope = testScope, initialState = CHECK_SUCCEEDED_STATE) {
            val onCreateJob = runOnCreate()
            sut.onIntent(KillSwitchIntent.SaveProxy)
            testScope.advanceUntilIdleAll()
            expectSideEffect(KillSwitchSideEffect.ProxySaved)
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }

        verify(exactly = 1) {
            saveProxyEndpointUseCase(host = UPDATED_HOST, port = PORT, protocol = PROTOCOL)
        }
    }

    private fun enableAllConditions() {
        every { observeKillSwitchStateUseCase.state } returns DISCONNECTED_OBSERVED_STATE
        every { observeKillSwitchStateUseCase.stateFlow } returns stateFlow
        every { toggleKillSwitchUseCase(any()) } just runs
    }
}
