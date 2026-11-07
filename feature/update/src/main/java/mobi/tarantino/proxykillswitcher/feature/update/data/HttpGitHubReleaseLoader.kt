package mobi.tarantino.proxykillswitcher.feature.update.data

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@Inject
class HttpGitHubReleaseLoader : GitHubReleaseLoader {
    override suspend fun loadLatestRelease(): GitHubRelease {
        return withContext(Dispatchers.IO) {
            val connection = URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = HTTP_METHOD_GET
                connection.connectTimeout = TIMEOUT_MILLIS
                connection.readTimeout = TIMEOUT_MILLIS
                connection.setRequestProperty(ACCEPT_HEADER, ACCEPT_VALUE)
                connection.setRequestProperty(API_VERSION_HEADER, API_VERSION_VALUE)
                connection.setRequestProperty(USER_AGENT_HEADER, USER_AGENT_VALUE)
                val responseCode = connection.responseCode
                if (responseCode != HTTP_OK) {
                    throw IOException("GitHub releases request answered with $responseCode")
                }
                val responseBody =
                    connection.inputStream.bufferedReader().use { reader -> reader.readText() }
                GITHUB_RELEASE_JSON.decodeFromString<GitHubRelease>(responseBody)
            } finally {
                connection.disconnect()
            }
        }
    }

    companion object {
        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/kolipass/Proxy-KillSwitcher/releases/latest"
        private const val HTTP_METHOD_GET = "GET"
        private const val HTTP_OK = 200
        private const val TIMEOUT_MILLIS = 15_000
        private const val ACCEPT_HEADER = "Accept"
        private const val ACCEPT_VALUE = "application/vnd.github+json"
        private const val API_VERSION_HEADER = "X-GitHub-Api-Version"
        private const val API_VERSION_VALUE = "2022-11-28"
        private const val USER_AGENT_HEADER = "User-Agent"
        private const val USER_AGENT_VALUE = "proxy-killswitcher-android"

        private val GITHUB_RELEASE_JSON = Json { ignoreUnknownKeys = true }
    }
}
