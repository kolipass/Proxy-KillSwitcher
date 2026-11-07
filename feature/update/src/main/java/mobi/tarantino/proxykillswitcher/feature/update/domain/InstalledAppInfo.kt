package mobi.tarantino.proxykillswitcher.feature.update.domain

interface InstalledAppInfo {
    val versionCode: Int
    val versionName: String
    val isDebuggable: Boolean
}
