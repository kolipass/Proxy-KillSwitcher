package mobi.tarantino.proxykillswitcher.feature.update.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.update.domain.ApkInstaller
import mobi.tarantino.proxykillswitcher.feature.update.domain.InstallResult
import java.io.File

@Inject
class IntentApkInstaller
    constructor(
        private val context: Context,
    ) : ApkInstaller {
        override fun install(apkFile: File): InstallResult {
            if (!context.packageManager.canRequestPackageInstalls()) {
                return InstallResult.UnknownSourcesRequired
            }
            val apkUri = FileProvider.getUriForFile(context, providerAuthority(), apkFile)
            val installIntent =
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(apkUri, APK_MIME_TYPE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            return try {
                context.startActivity(installIntent)
                InstallResult.InstallerLaunched
            } catch (activityNotFoundException: ActivityNotFoundException) {
                Log.w(TAG, "No activity can install the downloaded APK", activityNotFoundException)
                InstallResult.Failed
            }
        }

        private fun providerAuthority(): String {
            return "${context.packageName}$PROVIDER_AUTHORITY_SUFFIX"
        }

        companion object {
            private const val TAG = "IntentApkInstaller"
            private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
            private const val PROVIDER_AUTHORITY_SUFFIX = ".update.provider"
        }
    }
