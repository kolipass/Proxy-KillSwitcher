package mobi.tarantino.proxykillswitcher.core.tunnel

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress

internal object Socks5ConnectProtocol {
    fun completeGreeting(
        clientInput: InputStream,
        clientOutput: OutputStream,
    ): Boolean {
        val version = clientInput.read()
        val methodCount = clientInput.read()
        if (version != SOCKS5_VERSION || methodCount <= 0) {
            return false
        }
        val skippedByteCount = clientInput.skip(methodCount.toLong())
        if (skippedByteCount != methodCount.toLong()) {
            return false
        }
        clientOutput.write(byteArrayOf(SOCKS5_VERSION.toByte(), SOCKS5_NO_AUTH_METHOD.toByte()))
        clientOutput.flush()
        return true
    }

    fun readConnectDestination(clientInput: InputStream): String? {
        val version = clientInput.read()
        val command = clientInput.read()
        val reserved = clientInput.read()
        val addressType = clientInput.read()
        if (version != SOCKS5_VERSION || command != SOCKS5_CONNECT_COMMAND || reserved != 0) {
            return null
        }
        val host = readHost(clientInput = clientInput, addressType = addressType) ?: return null
        val portHigh = clientInput.read()
        val portLow = clientInput.read()
        if (portHigh < 0 || portLow < 0) {
            return null
        }
        val port = (portHigh shl 8) or portLow
        return "$host:$port"
    }

    fun writeSuccess(clientOutput: OutputStream) {
        clientOutput.write(SOCKS5_SUCCESS_REPLY)
        clientOutput.flush()
    }

    fun writeFailure(clientOutput: OutputStream) {
        try {
            clientOutput.write(SOCKS5_FAILURE_REPLY)
            clientOutput.flush()
        } catch (_: IOException) {
            return
        }
    }

    private fun readHost(
        clientInput: InputStream,
        addressType: Int,
    ): String? {
        return when (addressType) {
            SOCKS5_IPV4_ADDRESS_TYPE -> {
                val addressBytes = readExact(clientInput, IPV4_BYTE_COUNT) ?: return null
                InetAddress.getByAddress(addressBytes).hostAddress
            }

            SOCKS5_DOMAIN_ADDRESS_TYPE -> {
                val domainLength = clientInput.read()
                if (domainLength <= 0) {
                    return null
                }
                val domainBytes = readExact(clientInput, domainLength) ?: return null
                String(domainBytes, Charsets.US_ASCII)
            }

            SOCKS5_IPV6_ADDRESS_TYPE -> {
                val addressBytes = readExact(clientInput, IPV6_BYTE_COUNT) ?: return null
                "[${InetAddress.getByAddress(addressBytes).hostAddress}]"
            }

            else -> {
                null
            }
        }
    }

    private fun readExact(
        inputStream: InputStream,
        byteCount: Int,
    ): ByteArray? {
        val buffer = ByteArray(byteCount)
        var offset = 0
        while (offset < byteCount) {
            val readByteCount = inputStream.read(buffer, offset, byteCount - offset)
            if (readByteCount <= 0) {
                return null
            }
            offset += readByteCount
        }
        return buffer
    }

    private const val SOCKS5_VERSION = 5
    private const val SOCKS5_NO_AUTH_METHOD = 0
    private const val SOCKS5_CONNECT_COMMAND = 1
    private const val SOCKS5_IPV4_ADDRESS_TYPE = 1
    private const val SOCKS5_DOMAIN_ADDRESS_TYPE = 3
    private const val SOCKS5_IPV6_ADDRESS_TYPE = 4
    private const val IPV4_BYTE_COUNT = 4
    private const val IPV6_BYTE_COUNT = 16
    private val SOCKS5_SUCCESS_REPLY =
        byteArrayOf(0x05, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
    private val SOCKS5_FAILURE_REPLY =
        byteArrayOf(0x05, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
}
