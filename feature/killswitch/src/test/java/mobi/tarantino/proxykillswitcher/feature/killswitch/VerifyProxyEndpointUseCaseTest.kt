package mobi.tarantino.proxykillswitcher.feature.killswitch

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyEndpointVerifier
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyProtocol
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.VerifyProxyEndpointUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerifyProxyEndpointUseCaseTest {
    private val proxyEndpointVerifier: ProxyEndpointVerifier = mockk()

    private val sut = VerifyProxyEndpointUseCase(proxyEndpointVerifier)

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    suspend fun `When verifier accepts endpoint, Then use case returns true`() {
        coEvery {
            proxyEndpointVerifier.verify(host = HOST, port = PORT, protocol = PROTOCOL)
        } returns true

        val actual = sut(host = " $HOST ", port = PORT, protocol = PROTOCOL)

        assertTrue(actual)
        coVerify(exactly = 1) {
            proxyEndpointVerifier.verify(host = HOST, port = PORT, protocol = PROTOCOL)
        }
    }

    companion object {
        private const val HOST = "192.168.1.10"
        private const val PORT = 1080
        private val PROTOCOL = ProxyProtocol.Http
    }
}
