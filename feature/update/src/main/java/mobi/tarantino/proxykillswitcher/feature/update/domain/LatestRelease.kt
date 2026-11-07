package mobi.tarantino.proxykillswitcher.feature.update.domain

data class LatestRelease(
    val tagName: String,
    val releaseUrl: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
)
