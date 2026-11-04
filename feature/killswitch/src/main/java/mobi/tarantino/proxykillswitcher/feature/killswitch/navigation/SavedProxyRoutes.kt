package mobi.tarantino.proxykillswitcher.feature.killswitch.navigation

import kotlinx.serialization.Serializable
import mobi.tarantino.proxykillswitcher.core.navigation.FullscreenRoute

@Serializable
data object SavedProxyList : FullscreenRoute

@Serializable
data class SavedProxyEditor(
    val host: String? = null,
    val port: Int? = null,
    val protocol: String? = null,
) : FullscreenRoute
