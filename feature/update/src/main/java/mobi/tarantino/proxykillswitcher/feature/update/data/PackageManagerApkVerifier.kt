package mobi.tarantino.proxykillswitcher.feature.update.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.update.domain.DownloadedApkVerifier
import java.io.File

@Inject
class PackageManagerApkVerifier
    constructor(
        private val context: Context,
    ) : DownloadedApkVerifier {
        override fun isTrusted(
            apkFile: File,
            expectedVersionCode: Int,
        ): Boolean {
            val packageManager = context.packageManager
            val archivedInfo =
                packageManager.getPackageArchiveInfo(apkFile.path, signingFlags()) ?: return false
            if (archivedInfo.packageName != context.packageName) {
                return false
            }
            if (archivedInfo.versionCodeCompat() != expectedVersionCode) {
                return false
            }
            val archivedCertificate = archivedInfo.certificateBytes() ?: return false
            val installedCertificate =
                packageManager
                    .getPackageInfo(context.packageName, signingFlags())
                    .certificateBytes() ?: return false
            return archivedCertificate.contentEquals(installedCertificate)
        }

        private fun signingFlags(): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
        }

        private fun PackageInfo.certificateBytes(): ByteArray? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                signatures?.firstOrNull()?.toByteArray()
            }
        }

        private fun PackageInfo.versionCodeCompat(): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                versionCode
            }
        }
    }
