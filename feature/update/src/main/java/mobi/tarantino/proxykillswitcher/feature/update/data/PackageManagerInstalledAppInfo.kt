package mobi.tarantino.proxykillswitcher.feature.update.data

import android.content.Context
import android.os.Build
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import mobi.tarantino.proxykillswitcher.core.di.AppScope
import mobi.tarantino.proxykillswitcher.feature.update.domain.InstalledAppInfo

@SingleIn(AppScope::class)
@Inject
class PackageManagerInstalledAppInfo
    constructor(
        context: Context,
    ) : InstalledAppInfo {
        private val packageInfo = context.packageManager.getPackageInfo(context.packageName, NO_FLAGS)

        override val versionCode: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }

        override val versionName: String = packageInfo.versionName.orEmpty()

        override val isDebuggable: Boolean =
            (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        companion object {
            private const val NO_FLAGS = 0
        }
    }
