package mobi.tarantino.proxykillswitcher.feature.killswitch.domain

import dev.zacsweers.metro.Inject

@Inject
class VerifyProxyEndpointUseCase
    constructor(
        private val proxyEndpointVerifier: ProxyEndpointVerifier,
    ) {
        suspend operator fun invoke(
            host: String,
            port: Int,
            protocol: ProxyProtocol,
        ): Boolean {
            return proxyEndpointVerifier.verify(host = host.trim(), port = port, protocol = protocol)
        }
    }
