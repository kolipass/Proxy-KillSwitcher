package mobi.tarantino.proxykillswitcher.feature.killswitch

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.CONNECTED_OBSERVED_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.DISCONNECTED_OBSERVED_STATE
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.HOST
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.PORT
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.PROTOCOL
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.SAVED_ENDPOINT
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.DeleteSavedProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ObserveKillSwitchStateUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SelectSavedProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.VerifyProxyEndpointUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.ProxyCheckStatus
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyListSideEffect
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyListState
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyListUiStateMapper
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyListViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.orbitmvi.orbit.test.OrbitTestContext
import org.orbitmvi.orbit.test.test

class SavedProxyListViewModelTest {
    private val testScope = TestScope(StandardTestDispatcher())
    private val uiStateMapper: SavedProxyListUiStateMapper = mockk()
    private val selectSavedProxyEndpointUseCase: SelectSavedProxyEndpointUseCase = mockk()
    private val deleteSavedProxyEndpointUseCase: DeleteSavedProxyEndpointUseCase = mockk()
    private val observeKillSwitchStateUseCase: ObserveKillSwitchStateUseCase = mockk()
    private val verifyProxyEndpointUseCase: VerifyProxyEndpointUseCase = mockk()
    private val disconnectedObservedState =
        DISCONNECTED_OBSERVED_STATE.copy(
            settings =
                DISCONNECTED_OBSERVED_STATE.settings.copy(
                    savedEndpoints = listOf(SAVED_ENDPOINT, OTHER_ENDPOINT),
                ),
        )
    private val stateFlow = MutableStateFlow(disconnectedObservedState)

    private val sut by lazy {
        SavedProxyListViewModel(
            uiStateMapper = uiStateMapper,
            selectSavedProxyEndpointUseCase = selectSavedProxyEndpointUseCase,
            deleteSavedProxyEndpointUseCase = deleteSavedProxyEndpointUseCase,
            observeKillSwitchStateUseCase = observeKillSwitchStateUseCase,
            verifyProxyEndpointUseCase = verifyProxyEndpointUseCase,
        )
    }

    @BeforeEach
    fun setUp() {
        every { observeKillSwitchStateUseCase.state } returns disconnectedObservedState
        every { observeKillSwitchStateUseCase.stateFlow } returns stateFlow
    }

    @AfterEach
    fun tearDown() {
        testScope.cancel()
        unmockkAll()
    }

    @Test
    suspend fun `When screen opens with vpn disconnected, Then saved proxies are checked`() {
        coEvery { verifyProxyEndpointUseCase(any(), any(), any()) } returns true

        sut.test(testScope = testScope, initialState = INITIAL_LIST_STATE) {
            val onCreateJob = runOnCreate()
            testScope.advanceUntilIdleAll()
            expectState {
                INITIAL_LIST_STATE.copy(
                    checks =
                        mapOf(
                            SAVED_ENDPOINT to ProxyCheckStatus.InProgress,
                            OTHER_ENDPOINT to ProxyCheckStatus.InProgress,
                        ),
                )
            }
            awaitFinalChecks(
                this,
                mapOf(
                    SAVED_ENDPOINT to ProxyCheckStatus.Available,
                    OTHER_ENDPOINT to ProxyCheckStatus.Available,
                ),
            )
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }

        coVerify(exactly = 1) { verifyProxyEndpointUseCase(host = HOST, port = PORT, protocol = PROTOCOL) }
        coVerify(exactly = 1) { verifyProxyEndpointUseCase(host = OTHER_HOST, port = OTHER_PORT, protocol = PROTOCOL) }
    }

    @Test
    suspend fun `When proxy check succeeds, Then available status is stored`() {
        coEvery { verifyProxyEndpointUseCase(any(), any(), any()) } returns true

        sut.test(testScope = testScope, initialState = INITIAL_LIST_STATE) {
            val onCreateJob = runOnCreate()
            testScope.advanceUntilIdleAll()
            awaitFinalChecks(
                this,
                mapOf(
                    SAVED_ENDPOINT to ProxyCheckStatus.Available,
                    OTHER_ENDPOINT to ProxyCheckStatus.Available,
                ),
            )
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }
    }

    @Test
    suspend fun `When proxy check fails, Then unavailable status is stored`() {
        coEvery { verifyProxyEndpointUseCase(any(), any(), any()) } returns false

        sut.test(testScope = testScope, initialState = INITIAL_LIST_STATE) {
            val onCreateJob = runOnCreate()
            testScope.advanceUntilIdleAll()
            awaitFinalChecks(
                this,
                mapOf(
                    SAVED_ENDPOINT to ProxyCheckStatus.Unavailable,
                    OTHER_ENDPOINT to ProxyCheckStatus.Unavailable,
                ),
            )
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }
    }

    private suspend fun awaitFinalChecks(
        context: OrbitTestContext<SavedProxyListState, SavedProxyListSideEffect, SavedProxyListViewModel>,
        expectedChecks: Map<SavedProxyEndpoint, ProxyCheckStatus>,
    ) {
        var state = context.awaitState()
        while (state.checks != expectedChecks) {
            state = context.awaitState()
        }
        assertEquals(expectedChecks, state.checks)
    }

    @Test
    suspend fun `When screen opens with vpn connected, Then saved proxies are not checked`() {
        val connectedObservedState =
            disconnectedObservedState.copy(connectionState = CONNECTED_OBSERVED_STATE.connectionState)
        every { observeKillSwitchStateUseCase.state } returns connectedObservedState

        sut.test(testScope = testScope, initialState = CONNECTED_LIST_STATE) {
            val onCreateJob = runOnCreate()
            testScope.advanceUntilIdleAll()
            cancelAndIgnoreRemainingItems()
            onCreateJob.cancel()
            testScope.advanceUntilIdleAll()
        }

        coVerify(exactly = 0) { verifyProxyEndpointUseCase(any(), any(), any()) }
    }

    private companion object {
        const val OTHER_HOST = "10.0.0.1"
        const val OTHER_PORT = 8080
        val OTHER_ENDPOINT = SavedProxyEndpoint(host = OTHER_HOST, port = OTHER_PORT, protocol = PROTOCOL)
        val INITIAL_LIST_STATE =
            SavedProxyListState(
                savedEndpoints = listOf(SAVED_ENDPOINT, OTHER_ENDPOINT),
                activeEndpoint = SAVED_ENDPOINT,
                isEnabled = false,
            )
        val CONNECTED_LIST_STATE = INITIAL_LIST_STATE.copy(isEnabled = true)
    }
}
