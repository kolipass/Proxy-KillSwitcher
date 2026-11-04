package mobi.tarantino.proxykillswitcher.core.tunnel

import java.io.InputStream
import java.net.Socket

internal object HttpConnectProtocol {
    fun completeConnect(
        upstream: Socket,
        destination: String,
    ): Boolean {
        val request = "CONNECT $destination HTTP/1.1\r\nHost: $destination\r\n\r\n"
        val outputStream = upstream.getOutputStream()
        outputStream.write(request.toByteArray(Charsets.US_ASCII))
        outputStream.flush()
        val header = readHeader(upstream.getInputStream()) ?: return false
        val statusLine = header.substringBefore("\r\n")
        return statusLine.startsWith(HTTP_SUCCESS_PREFIX) || statusLine.contains(" $HTTP_SUCCESS_CODE ")
    }

    private fun readHeader(inputStream: InputStream): String? {
        val headerBuilder = StringBuilder()
        while (headerBuilder.length < MAX_HTTP_HEADER_LENGTH) {
            val readByte = inputStream.read()
            if (readByte == -1) {
                return null
            }
            headerBuilder.append(readByte.toChar())
            if (headerBuilder.endsWith(HTTP_HEADER_END)) {
                return headerBuilder.toString()
            }
        }
        return null
    }

    private const val MAX_HTTP_HEADER_LENGTH = 8_192
    private const val HTTP_SUCCESS_CODE = "200"
    private const val HTTP_SUCCESS_PREFIX = "HTTP/1.1 200"
    private const val HTTP_HEADER_END = "\r\n\r\n"
}
