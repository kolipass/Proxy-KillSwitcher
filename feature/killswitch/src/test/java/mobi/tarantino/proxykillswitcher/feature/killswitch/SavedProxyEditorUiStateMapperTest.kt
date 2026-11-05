package mobi.tarantino.proxykillswitcher.feature.killswitch

import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.DiscoveredProxy
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.LanScanState
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyProtocol
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ScanArea
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.LanScanUiState
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyEditorState
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyEditorUiStateMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedProxyEditorUiStateMapperTest {
    private val mapper = SavedProxyEditorUiStateMapper()

    private companion object {
        val TEST_SCAN_AREA =
            ScanArea(firstHost = "<LITELLM-HIDDEN-IP-19604>", lastHost = "<LITELLM-HIDDEN-IP-19608>", hostsCount = 254)
    }

    @Test
    fun `lan scan block is hidden when editing existing endpoint`() {
        val state =
            SavedProxyEditorState(
                originalEndpoint = SavedProxyEndpoint(host = "1.1.1.1", port = 8080, protocol = ProxyProtocol.Http),
                isEnabled = false,
                lanScan = LanScanState.NotOnWifi,
            )

        assertEquals(LanScanUiState.Hidden, mapper.map(state).lanScanUiState)
    }

    @Test
    fun `lan scan block is hidden while scan is idle`() {
        val state = SavedProxyEditorState(isEnabled = false, lanScan = LanScanState.Idle)

        assertEquals(LanScanUiState.Hidden, mapper.map(state).lanScanUiState)
    }

    @Test
    fun `lan scan block shows wifi hint when not on wifi`() {
        val state = SavedProxyEditorState(isEnabled = false, lanScan = LanScanState.NotOnWifi)

        assertEquals(LanScanUiState.NotOnWifi, mapper.map(state).lanScanUiState)
    }

    @Test
    fun `lan scan block shows spinner while scanning without results`() {
        val state =
            SavedProxyEditorState(
                isEnabled = false,
                lanScan =
                    LanScanState.InProgress(
                        scanArea = TEST_SCAN_AREA,
                        scanned = 10,
                        total = 1778,
                        elapsedMs = 2500,
                        found = emptyList(),
                    ),
            )

        assertEquals(LanScanUiState.Scanning, mapper.map(state).lanScanUiState)
    }

    @Test
    fun `lan scan block shows found proxies while scanning`() {
        val found = listOf(DiscoveredProxy(host = "192.168.1.10", port = 3128))
        val state =
            SavedProxyEditorState(
                isEnabled = false,
                lanScan =
                    LanScanState.InProgress(
                        scanArea = TEST_SCAN_AREA,
                        scanned = 100,
                        total = 1778,
                        elapsedMs = 2500,
                        found = found,
                    ),
            )

        val uiState = mapper.map(state).lanScanUiState

        assertTrue(uiState is LanScanUiState.Completed)
        uiState as LanScanUiState.Completed
        assertEquals(found, uiState.found)
        assertEquals("Scanning 100/1778", uiState.progressText)
        assertEquals("0:02", uiState.elapsedText)
    }

    @Test
    fun `lan scan block shows elapsed time when completed`() {
        val state =
            SavedProxyEditorState(
                isEnabled = false,
                lanScan =
                    LanScanState.Completed(
                        scanArea = TEST_SCAN_AREA,
                        elapsedMs = 65000,
                        found = emptyList(),
                    ),
            )

        val uiState = mapper.map(state).lanScanUiState

        assertTrue(uiState is LanScanUiState.Completed)
        uiState as LanScanUiState.Completed
        assertEquals("1:05", uiState.elapsedText)
        assertEquals("Scan finished", uiState.progressText)
    }
}
