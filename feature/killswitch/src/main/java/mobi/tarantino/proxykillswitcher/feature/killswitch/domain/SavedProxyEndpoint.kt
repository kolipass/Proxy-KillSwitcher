package mobi.tarantino.proxykillswitcher.feature.killswitch.domain

data class SavedProxyEndpoint(
    val host: String,
    val port: Int,
    val protocol: ProxyProtocol,
)
