package mobi.tarantino.proxykillswitcher.feature.killswitch

import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.SAVED_ENDPOINT
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.ProxyCheckStatus
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyListState
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyListUiState
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyListUiStateMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SavedProxyListUiStateMapperTest {
    private val sut = SavedProxyListUiStateMapper()

    @Test
    fun `When proxy was not checked, Then status text is empty`() {
        val actual = sut.map(listState(checks = emptyMap()))

        assertEquals("", actual.endpoints.single().checkStatusText)
        assertEquals(SavedProxyListUiState.CheckStatusKind.None, actual.endpoints.single().checkStatusKind)
    }

    @Test
    fun `When proxy check is in progress, Then checking status is shown`() {
        val actual = sut.map(listState(checks = mapOf(SAVED_ENDPOINT to ProxyCheckStatus.InProgress)))

        assertEquals(CHECKING_STATUS_TEXT, actual.endpoints.single().checkStatusText)
        assertEquals(SavedProxyListUiState.CheckStatusKind.InProgress, actual.endpoints.single().checkStatusKind)
    }

    @Test
    fun `When proxy check succeeded, Then available status is shown`() {
        val actual = sut.map(listState(checks = mapOf(SAVED_ENDPOINT to ProxyCheckStatus.Available)))

        assertEquals(AVAILABLE_STATUS_TEXT, actual.endpoints.single().checkStatusText)
        assertEquals(SavedProxyListUiState.CheckStatusKind.Available, actual.endpoints.single().checkStatusKind)
    }

    @Test
    fun `When proxy check failed, Then unavailable status is shown`() {
        val actual = sut.map(listState(checks = mapOf(SAVED_ENDPOINT to ProxyCheckStatus.Unavailable)))

        assertEquals(UNAVAILABLE_STATUS_TEXT, actual.endpoints.single().checkStatusText)
        assertEquals(SavedProxyListUiState.CheckStatusKind.Unavailable, actual.endpoints.single().checkStatusKind)
    }

    private fun listState(checks: Map<SavedProxyEndpoint, ProxyCheckStatus>) =
        SavedProxyListState(
            savedEndpoints = listOf(SAVED_ENDPOINT),
            activeEndpoint = SAVED_ENDPOINT,
            isEnabled = false,
            checks = checks,
        )

    private companion object {
        const val CHECKING_STATUS_TEXT = "Checking proxy…"
        const val AVAILABLE_STATUS_TEXT = "Proxy available"
        const val UNAVAILABLE_STATUS_TEXT = "Proxy unavailable"
    }
}
