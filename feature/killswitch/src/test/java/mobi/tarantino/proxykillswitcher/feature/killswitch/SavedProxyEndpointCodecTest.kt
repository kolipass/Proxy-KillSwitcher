package mobi.tarantino.proxykillswitcher.feature.killswitch

import mobi.tarantino.proxykillswitcher.feature.killswitch.data.SavedProxyEndpointCodec
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyProtocol
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SavedProxyEndpointCodecTest {
    private val savedEndpoint =
        SavedProxyEndpoint(
            host = HOST,
            port = PORT,
            protocol = ProxyProtocol.Http,
        )

    private val sut = SavedProxyEndpointCodec

    @Test
    fun `When endpoints encoded and decoded, Then triples are preserved`() {
        val actual = sut.decode(sut.encode(listOf(savedEndpoint)))

        assertEquals(listOf(savedEndpoint), actual)
    }

    @Test
    fun `When stored catalog contains duplicates and malformed rows, Then only one valid endpoint remains`() {
        val actual =
            sut.decode(
                """
                $HOST|$PORT|http
                malformed
                $HOST|$PORT|http
                other|bad-port|http
                other|8080|unsupported
                """.trimIndent(),
            )

        assertEquals(listOf(savedEndpoint), actual)
    }

    companion object {
        private const val HOST = "192.168.1.10"
        private const val PORT = 1080
    }
}
