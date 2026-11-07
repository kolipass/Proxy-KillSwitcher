package mobi.tarantino.proxykillswitcher.feature.update

import mobi.tarantino.proxykillswitcher.feature.update.domain.AvailableUpdate
import mobi.tarantino.proxykillswitcher.feature.update.domain.LatestRelease
import mobi.tarantino.proxykillswitcher.feature.update.presentation.ui.UpdatePhase
import mobi.tarantino.proxykillswitcher.feature.update.presentation.ui.UpdateState
import java.io.File

object UpdateGenerator {
    const val INSTALLED_VERSION_CODE = 100

    val LATEST_RELEASE =
        LatestRelease(
            tagName = "v1.2.3",
            releaseUrl = "https://github.com/kolipass/Proxy-KillSwitcher/releases/tag/v1.2.3",
            apkUrl = "https://github.com/kolipass/Proxy-KillSwitcher/releases/download/v1.2.3/app.apk",
            apkSizeBytes = 1_024L,
        )

    val AVAILABLE_UPDATE =
        AvailableUpdate(
            versionCode = 10203,
            versionName = "1.2.3",
            apkUrl = LATEST_RELEASE.apkUrl,
            releaseUrl = LATEST_RELEASE.releaseUrl,
            apkSizeBytes = LATEST_RELEASE.apkSizeBytes,
        )

    val APK_FILE: File = File("update.apk")

    val READY_STATE =
        UpdateState(
            update = AVAILABLE_UPDATE,
            phase = UpdatePhase.Ready,
            downloadedBytes = AVAILABLE_UPDATE.apkSizeBytes,
            totalBytes = AVAILABLE_UPDATE.apkSizeBytes,
            downloadedApk = APK_FILE,
        )
}
