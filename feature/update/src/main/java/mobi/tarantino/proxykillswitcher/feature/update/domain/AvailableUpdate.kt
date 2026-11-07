package mobi.tarantino.proxykillswitcher.feature.update.domain

data class AvailableUpdate(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseUrl: String,
    val apkSizeBytes: Long,
)
