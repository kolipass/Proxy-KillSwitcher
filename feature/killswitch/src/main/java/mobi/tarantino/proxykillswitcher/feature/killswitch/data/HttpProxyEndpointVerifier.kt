package mobi.tarantino.proxykillswitcher.feature.killswitch.data

import android.util.Log
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyEndpointVerifier
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyProtocol
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket

@Inject
class HttpProxyEndpointVerifier : ProxyEndpointVerifier {
    override suspend fun verify(
        host: String,
        port: Int,
        protocol: ProxyProtocol,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            verifyOnIo(host = host, port = port)
        }
    }

    private fun verifyOnIo(
        host: String,
        port: Int,
    ): Boolean {
        return try {
            Socket().use { socket ->
                connectHandshakeAccepted(host = host, port = port, socket = socket)
            }
        } catch (ioException: IOException) {
            Log.w(TAG, "Proxy check failed for $host:$port", ioException)
            false
        }
    }

    private fun connectHandshakeAccepted(
        host: String,
        port: Int,
        socket: Socket,
    ): Boolean {
        socket.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT_MS)
        socket.soTimeout = SOCKET_TIMEOUT_MS
        val outputStream = socket.getOutputStream()
        val inputStream = socket.getInputStream()
        outputStream.write(CONNECT_PROBE_REQUEST)
        outputStream.flush()
        val statusLine = readAsciiLine(inputStream) ?: return logFailure(host, port, "empty HTTP status line")
        if (!isHttpProxyStatusLine(statusLine)) {
            return logFailure(host, port, "unexpected status line: $statusLine")
        }
        if (isProxyAuthRequired(statusLine)) {
            return logFailure(host, port, statusLine)
        }
        return true
    }

    private fun readAsciiLine(inputStream: InputStream): String? {
        val lineBuilder = StringBuilder()
        while (lineBuilder.length < MAX_STATUS_LINE_LENGTH) {
            val readByte = inputStream.read()
            if (readByte == -1) {
                return null
            }
            if (readByte == '\n'.code) {
                return lineBuilder.toString().trimEnd('\r')
            }
            lineBuilder.append(readByte.toChar())
        }
        return null
    }

    private fun isHttpProxyStatusLine(statusLine: String): Boolean {
        return statusLine.startsWith(HTTP_VERSION_PREFIX)
    }

    private fun isProxyAuthRequired(statusLine: String): Boolean {
        return statusLine.substringAfter(" ").substringBefore(" ") == HTTP_PROXY_AUTH_REQUIRED_CODE
    }

    private fun logFailure(
        host: String,
        port: Int,
        reason: String,
    ): Boolean {
        Log.w(TAG, "Proxy check failed for $host:$port: $reason")
        return false
    }

    companion object {
        private const val TAG = "HttpProxyEndpointVerifier"
        private const val SOCKET_TIMEOUT_MS = 5_000
        private const val MAX_STATUS_LINE_LENGTH = 256
        private const val HTTP_VERSION_PREFIX = "HTTP/"
        private const val HTTP_PROXY_AUTH_REQUIRED_CODE = "407"
        private val CONNECT_PROBE_REQUEST =
            "CONNECT 1.1.1.1:443 HTTP/1.1\r\nHost: 1.1.1.1:443\r\n\r\n".toByteArray()
    }
}
