package mobi.tarantino.proxykillswitcher.feature.killswitch.domain

import dev.zacsweers.metro.Inject

@Inject
class SelectSavedProxyEndpointUseCase
    constructor(
        private val verifyProxyEndpointUseCase: VerifyProxyEndpointUseCase,
        private val saveProxyEndpointUseCase: SaveProxyEndpointUseCase,
    ) {
        suspend operator fun invoke(savedEndpoint: SavedProxyEndpoint): Boolean {
            val isVerified =
                verifyProxyEndpointUseCase(
                    host = savedEndpoint.host,
                    port = savedEndpoint.port,
                    protocol = savedEndpoint.protocol,
                )
            if (!isVerified) {
                return false
            }
            return saveProxyEndpointUseCase(
                host = savedEndpoint.host,
                port = savedEndpoint.port,
                protocol = savedEndpoint.protocol,
            )
        }
    }
