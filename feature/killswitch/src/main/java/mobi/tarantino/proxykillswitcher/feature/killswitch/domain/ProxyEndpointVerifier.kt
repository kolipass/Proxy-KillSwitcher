package mobi.tarantino.proxykillswitcher.feature.killswitch.domain

interface ProxyEndpointVerifier {
    suspend fun verify(
        host: String,
        port: Int,
        protocol: ProxyProtocol,
    ): Boolean
}
