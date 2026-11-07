package mobi.tarantino.proxykillswitcher.feature.update.domain

import java.io.File

interface DownloadedApkVerifier {
    fun isTrusted(
        apkFile: File,
        expectedVersionCode: Int,
    ): Boolean
}
