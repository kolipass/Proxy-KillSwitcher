package mobi.tarantino.proxykillswitcher.feature.update.data

interface GitHubReleaseLoader {
    suspend fun loadLatestRelease(): GitHubRelease
}
