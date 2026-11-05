package mobi.tarantino.proxykillswitcher.feature.killswitch.data

import android.util.Log
import dev.zacsweers.metro.Inject
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket

@Inject
class ProxyPortProber {
    fun probe(
        host: String,
        port: Int,
        timeoutMs: Int = SOCKET_TIMEOUT_MS,
    ): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                socket.soTimeout = timeoutMs
                socket.getOutputStream().apply {
                    write(CONNECT_PROBE_REQUEST)
                    flush()
                }
                val statusLine = readAsciiLine(socket.getInputStream())
                if (statusLine != null) {
                    Log.d(TAG, "HTTP probe $host:$port answered: $statusLine")
                }
                statusLine != null && statusLine.startsWith(HTTP_VERSION_PREFIX)
            }
        } catch (ioException: IOException) {
            Log.v(TAG, "HTTP probe failed for $host:$port: ${ioException.message}")
            false
        }
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

    private companion object {
        const val TAG = "ProxyPortProber"
        const val SOCKET_TIMEOUT_MS = 450
        const val MAX_STATUS_LINE_LENGTH = 256
        const val HTTP_VERSION_PREFIX = "HTTP/"
        val CONNECT_PROBE_REQUEST =
            "CONNECT 1.1.1.1:443 HTTP/1.1\r\nHost: 1.1.1.1:443\r\n\r\n".toByteArray()
    }
}
