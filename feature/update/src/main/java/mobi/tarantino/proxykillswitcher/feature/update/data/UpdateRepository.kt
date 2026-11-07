package mobi.tarantino.proxykillswitcher.feature.update.data

import android.util.Log
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.SerializationException
import mobi.tarantino.proxykillswitcher.core.di.AppScope
import mobi.tarantino.proxykillswitcher.feature.update.domain.LatestRelease
import java.io.IOException

@SingleIn(AppScope::class)
@Inject
class UpdateRepository
    constructor(
        private val gitHubReleaseLoader: GitHubReleaseLoader,
        private val updatePrefs: UpdatePrefs,
    ) {
        suspend fun latestRelease(
            nowMillis: Long,
            force: Boolean,
        ): LatestReleaseResult {
            if (!force && isRecentlyChecked(nowMillis = nowMillis)) {
                return LatestReleaseResult.Throttled
            }
            val gitHubRelease =
                try {
                    gitHubReleaseLoader.loadLatestRelease()
                } catch (ioException: IOException) {
                    Log.w(TAG, "Could not load the latest GitHub release", ioException)
                    return LatestReleaseResult.Failed
                } catch (serializationException: SerializationException) {
                    Log.w(TAG, "Could not parse the latest GitHub release", serializationException)
                    return LatestReleaseResult.Failed
                }
            val latestRelease = gitHubRelease.toLatestReleaseOrNull() ?: return LatestReleaseResult.Failed
            updatePrefs.lastCheckAtMillis = nowMillis
            return LatestReleaseResult.Loaded(latestRelease)
        }

        private fun isRecentlyChecked(nowMillis: Long): Boolean {
            val lastCheckAtMillis = updatePrefs.lastCheckAtMillis
            return lastCheckAtMillis > 0L && nowMillis - lastCheckAtMillis < CHECK_INTERVAL_MILLIS
        }

        private fun GitHubRelease.toLatestReleaseOrNull(): LatestRelease? {
            val apkAsset = assets.firstOrNull { asset -> asset.name.endsWith(APK_EXTENSION, ignoreCase = true) }
            return LatestRelease(
                tagName = tagName,
                releaseUrl = releaseUrl,
                apkUrl = apkAsset?.downloadUrl ?: return null,
                apkSizeBytes = apkAsset.size,
            )
        }

        companion object {
            private const val TAG = "UpdateRepository"
            private const val APK_EXTENSION = ".apk"
            private const val CHECK_INTERVAL_MILLIS = 6L * 60L * 60L * 1000L
        }
    }

sealed interface LatestReleaseResult {
    data class Loaded(
        val release: LatestRelease,
    ) : LatestReleaseResult

    data object Throttled : LatestReleaseResult

    data object Failed : LatestReleaseResult
}
