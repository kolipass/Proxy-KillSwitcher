package mobi.tarantino.proxykillswitcher.feature.update.domain

import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.update.data.LatestReleaseResult
import mobi.tarantino.proxykillswitcher.feature.update.data.UpdateRepository

@Inject
class CheckForUpdateUseCase
    constructor(
        private val updateRepository: UpdateRepository,
        private val installedAppInfo: InstalledAppInfo,
        private val updateClock: UpdateClock,
    ) {
        suspend operator fun invoke(force: Boolean = false): UpdateCheck {
            val latestReleaseResult =
                updateRepository.latestRelease(
                    nowMillis = updateClock.nowMillis(),
                    force = force,
                )
            val latestRelease =
                when (latestReleaseResult) {
                    LatestReleaseResult.Throttled -> return UpdateCheck.Throttled
                    LatestReleaseResult.Failed -> return UpdateCheck.Failed
                    is LatestReleaseResult.Loaded -> latestReleaseResult.release
                }
            val releaseVersion = ReleaseTag.parse(latestRelease.tagName) ?: return UpdateCheck.NoUpdate
            if (releaseVersion.versionCode <= installedAppInfo.versionCode) {
                return UpdateCheck.NoUpdate
            }
            return UpdateCheck.Available(
                update =
                    AvailableUpdate(
                        versionCode = releaseVersion.versionCode,
                        versionName = releaseVersion.versionName,
                        apkUrl = latestRelease.apkUrl,
                        releaseUrl = latestRelease.releaseUrl,
                        apkSizeBytes = latestRelease.apkSizeBytes,
                    ),
                installMode =
                    if (installedAppInfo.isDebuggable) {
                        UpdateInstallMode.OpenReleasePage
                    } else {
                        UpdateInstallMode.SelfInstall
                    },
            )
        }
    }
