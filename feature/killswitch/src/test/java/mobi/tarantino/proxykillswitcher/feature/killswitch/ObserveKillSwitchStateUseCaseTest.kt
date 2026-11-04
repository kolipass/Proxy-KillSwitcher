package mobi.tarantino.proxykillswitcher.feature.killswitch

import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.INITIAL_SETTINGS
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchRepository
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ObserveKillSwitchStateUseCase
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.VpnConnectionState
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.VpnConnectionStateHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ObserveKillSwitchStateUseCaseTest {
    private val killSwitchRepository: KillSwitchRepository = mockk()
    private val vpnConnectionStateHolder: VpnConnectionStateHolder = mockk()
    private val settingsFlow = MutableStateFlow(INITIAL_SETTINGS)
    private val connectionStateFlow = MutableStateFlow<VpnConnectionState>(VpnConnectionState.Disconnected)

    private val sut by lazy {
        ObserveKillSwitchStateUseCase(
            killSwitchRepository = killSwitchRepository,
            vpnConnectionStateHolder = vpnConnectionStateHolder,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    suspend fun `When vpn connects, Then endpoint and connected state are emitted`() {
        every { killSwitchRepository.settingsFlow } returns settingsFlow
        every { vpnConnectionStateHolder.connectionStateFlow } returns connectionStateFlow
        connectionStateFlow.value = VpnConnectionState.Connected

        val actual = sut.stateFlow.first()

        assertEquals(INITIAL_SETTINGS, actual.settings)
        assertEquals(VpnConnectionState.Connected, actual.connectionState)
    }

    @Test
    suspend fun `When vpn disconnects, Then disconnected state is emitted`() {
        every { killSwitchRepository.settingsFlow } returns settingsFlow
        every { vpnConnectionStateHolder.connectionStateFlow } returns connectionStateFlow
        connectionStateFlow.value = VpnConnectionState.Disconnected

        val actual = sut.stateFlow.first()

        assertEquals(VpnConnectionState.Disconnected, actual.connectionState)
    }
}
