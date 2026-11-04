package mobi.tarantino.proxykillswitcher.feature.killswitch

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchRepository
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ToggleKillSwitchUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class ToggleKillSwitchUseCaseTest {
    private val killSwitchRepository: KillSwitchRepository = mockk()

    private val sut = ToggleKillSwitchUseCase(killSwitchRepository)

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `When kill switch enabled, Then desired state is persisted`() {
        every { killSwitchRepository.setDesiredEnabled(true) } just runs

        sut(desiredEnabled = true)

        verify(exactly = 1) { killSwitchRepository.setDesiredEnabled(true) }
    }
}
