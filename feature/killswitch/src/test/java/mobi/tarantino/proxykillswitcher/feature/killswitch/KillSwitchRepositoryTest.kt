package mobi.tarantino.proxykillswitcher.feature.killswitch

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.HOST
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.PORT
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.PROTOCOL
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.SAVED_ENDPOINT
import mobi.tarantino.proxykillswitcher.feature.killswitch.KillSwitchStateGenerator.UPDATED_HOST
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchPrefs
import mobi.tarantino.proxykillswitcher.feature.killswitch.data.KillSwitchRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KillSwitchRepositoryTest {
    private val killSwitchPrefs: KillSwitchPrefs = mockk()

    private val sut by lazy { KillSwitchRepository(killSwitchPrefs) }

    @BeforeEach
    fun setUp() {
        enableAllConditions()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `When repository created, Then persisted endpoint is exposed`() {
        val actual = sut.settingsFlow.value

        assertEquals(HOST, actual.host)
        assertEquals(PORT, actual.port)
        assertEquals(PROTOCOL, actual.protocol)
    }

    @Test
    fun `When desired state changes, Then primitive preference is updated`() {
        every { killSwitchPrefs.desiredEnabled = true } just runs

        sut.setDesiredEnabled(true)

        io.mockk.verify(exactly = 1) { killSwitchPrefs.desiredEnabled = true }
    }

    @Test
    fun `When endpoint saved, Then settings flow updates endpoint`() {
        every { killSwitchPrefs.host = UPDATED_HOST } just runs
        every { killSwitchPrefs.host } returnsMany listOf(HOST, UPDATED_HOST)

        sut.setHost(UPDATED_HOST)

        assertEquals(UPDATED_HOST, sut.settingsFlow.value.host)
    }

    @Test
    fun `When catalog is empty and active endpoint is valid, Then active endpoint is seeded`() {
        every { killSwitchPrefs.savedEndpoints } returns ""
        every { killSwitchPrefs.savedEndpoints = "$HOST|$PORT|${PROTOCOL.storedValue}" } just runs

        val actual = sut.settingsFlow.value.savedEndpoints

        assertEquals(listOf(SAVED_ENDPOINT), actual)
        verify(exactly = 1) { killSwitchPrefs.savedEndpoints = "$HOST|$PORT|${PROTOCOL.storedValue}" }
    }

    @Test
    fun `When stored catalog is malformed, Then catalog is empty`() {
        every { killSwitchPrefs.savedEndpoints } returns "malformed"

        val actual = sut.settingsFlow.value.savedEndpoints

        assertTrue(actual.isEmpty())
    }

    @Test
    fun `When existing endpoint is saved again, Then catalog stays deduplicated`() {
        every { killSwitchPrefs.savedEndpoints = any() } just runs
        every { killSwitchPrefs.host = HOST } just runs
        every { killSwitchPrefs.port = PORT } just runs
        every { killSwitchPrefs.protocol = PROTOCOL.storedValue } just runs

        sut.saveEndpoint(SAVED_ENDPOINT)

        assertEquals(listOf(SAVED_ENDPOINT), sut.settingsFlow.value.savedEndpoints)
    }

    @Test
    fun `When active endpoint is deleted, Then active host is cleared`() {
        every { killSwitchPrefs.savedEndpoints = "" } just runs
        every { killSwitchPrefs.host = "" } just runs
        every { killSwitchPrefs.host } returnsMany listOf(HOST, "")

        sut.deleteEndpoint(SAVED_ENDPOINT)

        assertEquals("", sut.settingsFlow.value.host)
        assertTrue(
            sut.settingsFlow.value.savedEndpoints
                .isEmpty(),
        )
    }

    private fun enableAllConditions() {
        every { killSwitchPrefs.host } returns HOST
        every { killSwitchPrefs.port } returns PORT
        every { killSwitchPrefs.protocol } returns PROTOCOL.storedValue
        every { killSwitchPrefs.savedEndpoints } returns "$HOST|$PORT|${PROTOCOL.storedValue}"
    }
}
