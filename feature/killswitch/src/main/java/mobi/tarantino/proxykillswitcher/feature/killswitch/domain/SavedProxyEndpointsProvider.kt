package mobi.tarantino.proxykillswitcher.feature.killswitch.domain

interface SavedProxyEndpointsProvider {
    fun current(): List<SavedProxyEndpoint>
}
