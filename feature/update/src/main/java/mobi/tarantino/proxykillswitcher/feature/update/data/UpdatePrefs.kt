package mobi.tarantino.proxykillswitcher.feature.update.data

import android.content.Context
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mobi.tarantino.proxykillswitcher.core.common.prefs.Preferences
import mobi.tarantino.proxykillswitcher.core.di.AppScope

@SingleIn(AppScope::class)
@Inject
class UpdatePrefs
    constructor(
        context: Context,
    ) : Preferences(context, NAME) {
        var lastCheckAtMillis: Long by longProperty(LAST_CHECK_AT_MILLIS_KEY)

        companion object {
            private const val NAME = "update"
            private const val LAST_CHECK_AT_MILLIS_KEY = "last_check_at_millis"
        }
    }
