package mobi.tarantino.proxykillswitcher.feature.killswitch.domain

object ProxyEndpoint {
    val VALID_PORT_RANGE = 1..65535

    fun isValid(
        host: String,
        port: Int?,
    ): Boolean {
        return host.isNotBlank() && port != null && port in VALID_PORT_RANGE
    }
}
