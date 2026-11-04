package mobi.tarantino.proxykillswitcher.feature.killswitch.data

import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyProtocol
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint

internal object SavedProxyEndpointCodec {
    fun encode(savedEndpoints: List<SavedProxyEndpoint>): String {
        return savedEndpoints.joinToString(separator = "\n", transform = ::encodeEndpoint)
    }

    fun decode(storedEndpoints: String): List<SavedProxyEndpoint> {
        return storedEndpoints
            .lineSequence()
            .mapNotNull(::decodeEndpoint)
            .distinct()
            .toList()
    }

    private fun encodeEndpoint(savedEndpoint: SavedProxyEndpoint): String {
        return "${savedEndpoint.host}|${savedEndpoint.port}|${savedEndpoint.protocol.storedValue}"
    }

    private fun decodeEndpoint(storedEndpoint: String): SavedProxyEndpoint? {
        val fields = storedEndpoint.split(FIELD_SEPARATOR)
        if (fields.size != FIELD_COUNT) {
            return null
        }
        val host = fields[HOST_INDEX].trim()
        val port = fields[PORT_INDEX].toIntOrNull()
        val protocol = ProxyProtocol.entries.firstOrNull { protocol -> protocol.storedValue == fields[PROTOCOL_INDEX] }
        if (!ProxyEndpoint.isValid(host = host, port = port) || port == null || protocol == null) {
            return null
        }
        return SavedProxyEndpoint(host = host, port = port, protocol = protocol)
    }

    private const val FIELD_SEPARATOR = "|"
    private const val FIELD_COUNT = 3
    private const val HOST_INDEX = 0
    private const val PORT_INDEX = 1
    private const val PROTOCOL_INDEX = 2
}
