package mobi.tarantino.proxykillswitcher.feature.killswitch.data

import android.content.Context
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mobi.tarantino.proxykillswitcher.core.common.prefs.Preferences
import mobi.tarantino.proxykillswitcher.core.di.AppScope

@SingleIn(AppScope::class)
@Inject
class KillSwitchPrefs
    constructor(
        context: Context,
    ) : Preferences(context, NAME) {
        var host: String by stringProperty(HOST_KEY)
        var port: Int by intProperty(PORT_KEY, DEFAULT_PORT)
        var protocol: String by stringProperty(PROTOCOL_KEY, DEFAULT_PROTOCOL)
        var desiredEnabled: Boolean by booleanProperty(DESIRED_ENABLED_KEY)
        var savedEndpoints: String by stringProperty(SAVED_ENDPOINTS_KEY)

        companion object {
            const val DEFAULT_PORT = 1080
            const val DEFAULT_PROTOCOL = "http"
            private const val NAME = "kill_switch"
            private const val HOST_KEY = "host"
            private const val PORT_KEY = "port"
            private const val PROTOCOL_KEY = "protocol"
            private const val DESIRED_ENABLED_KEY = "desired_enabled"
            private const val SAVED_ENDPOINTS_KEY = "saved_endpoints"
        }
    }
