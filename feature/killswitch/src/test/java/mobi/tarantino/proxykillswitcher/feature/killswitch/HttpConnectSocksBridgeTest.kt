package mobi.tarantino.proxykillswitcher.feature.killswitch

import mobi.tarantino.proxykillswitcher.core.tunnel.HttpConnectSocksBridge
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.DataInputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class HttpConnectSocksBridgeTest {
    private val httpProxyExecutor = Executors.newSingleThreadExecutor()
    private var httpProxyServerSocket: ServerSocket? = null
    private val upstreamUnreachableLatch = CountDownLatch(1)

    private val sut =
        HttpConnectSocksBridge(
            protectSocket = { true },
            onUpstreamUnreachable = upstreamUnreachableLatch::countDown,
        )

    @AfterEach
    fun tearDown() {
        sut.stop()
        httpProxyServerSocket?.close()
        httpProxyExecutor.shutdownNow()
    }

    @Test
    fun `When socks connect is requested, Then http connect is forwarded`() {
        val connectLatch = CountDownLatch(1)
        val capturedDestination = arrayOfNulls<String>(1)
        val httpProxyPort = startHttpConnectProxy(capturedDestination, connectLatch)
        val bridgePort = sut.start(upstreamHost = LOOPBACK_HOST, upstreamPort = httpProxyPort)

        Socket(LOOPBACK_HOST, bridgePort).use { clientSocket ->
            val outputStream = clientSocket.getOutputStream()
            val inputStream = DataInputStream(clientSocket.getInputStream())
            outputStream.write(SOCKS5_GREETING)
            outputStream.flush()
            assertEquals(0x05, inputStream.read())
            assertEquals(0x00, inputStream.read())
            outputStream.write(SOCKS5_CONNECT_REQUEST)
            outputStream.flush()
            val reply = ByteArray(SOCKS5_REPLY_LENGTH)
            inputStream.readFully(reply)
            assertEquals(0x05, reply[0].toInt())
            assertEquals(SOCKS5_SUCCESS_REPLY_CODE, reply[1].toInt())
            clientSocket.getOutputStream().write(PAYLOAD)
            clientSocket.getOutputStream().flush()
            val echoed = ByteArray(PAYLOAD.size)
            inputStream.readFully(echoed)
            assertTrue(echoed.contentEquals(PAYLOAD))
        }

        assertTrue(connectLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        assertEquals("8.8.8.8:443", capturedDestination[0])
    }

    @Test
    fun `When upstream proxy refuses connection, Then upstream loss is reported`() {
        val bridgePort = sut.start(upstreamHost = LOOPBACK_HOST, upstreamPort = reserveClosedPort())

        Socket(LOOPBACK_HOST, bridgePort).use { clientSocket ->
            val outputStream = clientSocket.getOutputStream()
            val inputStream = DataInputStream(clientSocket.getInputStream())
            outputStream.write(SOCKS5_GREETING)
            outputStream.flush()
            assertEquals(0x05, inputStream.read())
            assertEquals(0x00, inputStream.read())
            outputStream.write(SOCKS5_CONNECT_REQUEST)
            outputStream.flush()
            val reply = ByteArray(SOCKS5_REPLY_LENGTH)
            inputStream.readFully(reply)
            assertEquals(0x05, reply[0].toInt())
            assertEquals(SOCKS5_FAILURE_REPLY_CODE, reply[1].toInt())
        }

        assertTrue(upstreamUnreachableLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS))
    }

    private fun reserveClosedPort(): Int {
        val probeServerSocket = ServerSocket(0, 0, InetAddress.getByName(LOOPBACK_HOST))
        val reservedPort = probeServerSocket.localPort
        probeServerSocket.close()
        return reservedPort
    }

    private fun startHttpConnectProxy(
        capturedDestination: Array<String?>,
        connectLatch: CountDownLatch,
    ): Int {
        val serverSocket = ServerSocket(0, 0, InetAddress.getByName(LOOPBACK_HOST))
        httpProxyServerSocket = serverSocket
        httpProxyExecutor.execute {
            serverSocket.accept().use { proxyClient ->
                val header = readAsciiUntilHeaderEnd(proxyClient.getInputStream())
                capturedDestination[0] = header.substringAfter("CONNECT ").substringBefore(" ")
                proxyClient.getOutputStream().write(HTTP_CONNECTED_RESPONSE)
                proxyClient.getOutputStream().flush()
                connectLatch.countDown()
                val payload = ByteArray(PAYLOAD.size)
                DataInputStream(proxyClient.getInputStream()).readFully(payload)
                proxyClient.getOutputStream().write(payload)
                proxyClient.getOutputStream().flush()
            }
        }
        return serverSocket.localPort
    }

    private fun readAsciiUntilHeaderEnd(inputStream: InputStream): String {
        val headerBuilder = StringBuilder()
        while (headerBuilder.length < MAX_HEADER_LENGTH) {
            val readByte = inputStream.read()
            if (readByte == -1) {
                break
            }
            headerBuilder.append(readByte.toChar())
            if (headerBuilder.endsWith(HTTP_HEADER_END)) {
                return headerBuilder.toString()
            }
        }
        return headerBuilder.toString()
    }

    companion object {
        private const val LOOPBACK_HOST = "127.0.0.1"
        private const val MAX_HEADER_LENGTH = 1024
        private const val HTTP_HEADER_END = "\r\n\r\n"
        private const val SOCKS5_REPLY_LENGTH = 10
        private const val SOCKS5_SUCCESS_REPLY_CODE = 0x00
        private const val SOCKS5_FAILURE_REPLY_CODE = 0x01
        private const val AWAIT_TIMEOUT_SECONDS = 5L
        private val SOCKS5_GREETING = byteArrayOf(0x05, 0x01, 0x00)
        private val SOCKS5_CONNECT_REQUEST =
            byteArrayOf(0x05, 0x01, 0x00, 0x01, 8, 8, 8, 8, 0x01, 0xBB.toByte())
        private val PAYLOAD = byteArrayOf(1, 2, 3, 4)
        private val HTTP_CONNECTED_RESPONSE = "HTTP/1.1 200 Connection established\r\n\r\n".toByteArray()
    }
}
