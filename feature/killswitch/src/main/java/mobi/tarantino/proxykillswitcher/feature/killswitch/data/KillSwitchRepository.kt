package mobi.tarantino.proxykillswitcher.feature.killswitch.data

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mobi.tarantino.proxykillswitcher.core.di.AppScope
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyProtocol
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint

@SingleIn(AppScope::class)
@Inject
class KillSwitchRepository
    constructor(
        private val killSwitchPrefs: KillSwitchPrefs,
    ) {
        private val mutableSettingsFlow = MutableStateFlow(readSettings())

        val settingsFlow: StateFlow<KillSwitchSettings> = mutableSettingsFlow.asStateFlow()

        val host: String
            get() = killSwitchPrefs.host

        val port: Int
            get() = killSwitchPrefs.port

        val protocol: ProxyProtocol
            get() = ProxyProtocol.fromStored(killSwitchPrefs.protocol)

        val desiredEnabled: Boolean
            get() = killSwitchPrefs.desiredEnabled

        fun setHost(host: String) {
            killSwitchPrefs.host = host
            publishEndpoint()
        }

        fun setPort(port: Int) {
            killSwitchPrefs.port = port
            publishEndpoint()
        }

        fun setProtocol(protocol: ProxyProtocol) {
            killSwitchPrefs.protocol = protocol.storedValue
            publishEndpoint()
        }

        fun setDesiredEnabled(desiredEnabled: Boolean) {
            killSwitchPrefs.desiredEnabled = desiredEnabled
        }

        fun saveEndpoint(
            savedEndpoint: SavedProxyEndpoint,
            replacedEndpoint: SavedProxyEndpoint? = null,
        ) {
            val normalizedEndpoint = savedEndpoint.copy(host = savedEndpoint.host.trim())
            val updatedSavedEndpoints =
                settingsFlow.value.savedEndpoints
                    .filterNot { existingEndpoint -> existingEndpoint == replacedEndpoint }
                    .plus(normalizedEndpoint)
                    .distinct()
            killSwitchPrefs.savedEndpoints = SavedProxyEndpointCodec.encode(updatedSavedEndpoints)
            killSwitchPrefs.host = normalizedEndpoint.host
            killSwitchPrefs.port = normalizedEndpoint.port
            killSwitchPrefs.protocol = normalizedEndpoint.protocol.storedValue
            mutableSettingsFlow.value =
                KillSwitchSettings(
                    host = normalizedEndpoint.host,
                    port = normalizedEndpoint.port,
                    protocol = normalizedEndpoint.protocol,
                    savedEndpoints = updatedSavedEndpoints,
                )
        }

        fun deleteEndpoint(savedEndpoint: SavedProxyEndpoint) {
            val updatedSavedEndpoints =
                settingsFlow.value.savedEndpoints
                    .filterNot { endpoint -> endpoint == savedEndpoint }
            killSwitchPrefs.savedEndpoints = SavedProxyEndpointCodec.encode(updatedSavedEndpoints)
            val activeEndpoint = settingsFlow.value.activeEndpoint
            if (activeEndpoint == savedEndpoint) {
                killSwitchPrefs.host = ""
            }
            mutableSettingsFlow.value =
                readSettings(savedEndpoints = updatedSavedEndpoints)
        }

        private fun publishEndpoint() {
            mutableSettingsFlow.update { settings ->
                settings.copy(
                    host = killSwitchPrefs.host,
                    port = killSwitchPrefs.port,
                    protocol = protocol,
                )
            }
        }

        private fun readSettings(savedEndpoints: List<SavedProxyEndpoint> = readSavedEndpoints()): KillSwitchSettings {
            return KillSwitchSettings(
                host = killSwitchPrefs.host,
                port = killSwitchPrefs.port,
                protocol = ProxyProtocol.fromStored(killSwitchPrefs.protocol),
                savedEndpoints = savedEndpoints,
            )
        }

        private fun readSavedEndpoints(): List<SavedProxyEndpoint> {
            val storedEndpoints = killSwitchPrefs.savedEndpoints
            val decodedEndpoints = SavedProxyEndpointCodec.decode(storedEndpoints)
            if (decodedEndpoints.isNotEmpty()) {
                return decodedEndpoints
            }
            if (storedEndpoints.isNotBlank()) {
                return emptyList()
            }
            val persistedPort = killSwitchPrefs.port
            val persistedHost = killSwitchPrefs.host.trim()
            if (!ProxyEndpoint.isValid(host = persistedHost, port = persistedPort)) {
                return emptyList()
            }
            val seededEndpoints =
                listOf(
                    SavedProxyEndpoint(
                        host = persistedHost,
                        port = persistedPort,
                        protocol = ProxyProtocol.fromStored(killSwitchPrefs.protocol),
                    ),
                )
            killSwitchPrefs.savedEndpoints = SavedProxyEndpointCodec.encode(seededEndpoints)
            return seededEndpoints
        }
    }

data class KillSwitchSettings(
    val host: String,
    val port: Int,
    val protocol: ProxyProtocol,
    val savedEndpoints: List<SavedProxyEndpoint> = emptyList(),
) {
    val activeEndpoint: SavedProxyEndpoint? =
        if (ProxyEndpoint.isValid(host = host, port = port)) {
            SavedProxyEndpoint(host = host, port = port, protocol = protocol)
        } else {
            null
        }
}
