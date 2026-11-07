package mobi.tarantino.proxykillswitcher.feature.update.data

import android.content.Context
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import mobi.tarantino.proxykillswitcher.feature.update.domain.AvailableUpdate
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateDownload
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateDownloader
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@Inject
class HttpUpdateDownloader
    constructor(
        private val context: Context,
    ) : UpdateDownloader {
        override fun download(update: AvailableUpdate): Flow<UpdateDownload> {
            return flow {
                val updatesDirectory = File(context.filesDir, UPDATES_DIRECTORY_NAME).apply { mkdirs() }
                deleteStaleUpdates(updatesDirectory)
                val apkFile = File(updatesDirectory, apkFileName(update.versionCode))
                downloadInto(update = update, apkFile = apkFile) { downloadedBytes, totalBytes ->
                    emit(
                        UpdateDownload.InProgress(
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes,
                        ),
                    )
                }
                emit(UpdateDownload.Ready(apkFile))
            }.flowOn(Dispatchers.IO)
        }

        private suspend fun downloadInto(
            update: AvailableUpdate,
            apkFile: File,
            onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit,
        ) {
            val connection = URL(update.apkUrl).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = HTTP_METHOD_GET
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.setRequestProperty(USER_AGENT_HEADER, USER_AGENT_VALUE)
                val responseCode = connection.responseCode
                if (responseCode != HTTP_OK) {
                    throw IOException("APK download answered with $responseCode")
                }
                val totalBytes = connection.contentLengthLong.takeIf { it > 0L } ?: update.apkSizeBytes
                connection.inputStream.use { inputStream ->
                    apkFile.outputStream().use { outputStream ->
                        copyWithProgress(
                            inputStream = inputStream,
                            outputStream = outputStream,
                            totalBytes = totalBytes,
                            onProgress = onProgress,
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        }

        private suspend fun copyWithProgress(
            inputStream: java.io.InputStream,
            outputStream: java.io.OutputStream,
            totalBytes: Long,
            onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit,
        ) {
            val buffer = ByteArray(BUFFER_SIZE_BYTES)
            var downloadedBytes = 0L
            var reportedBytes = 0L
            while (true) {
                val readCount = inputStream.read(buffer)
                if (readCount == -1) {
                    break
                }
                outputStream.write(buffer, 0, readCount)
                downloadedBytes += readCount
                if (downloadedBytes - reportedBytes >= PROGRESS_STEP_BYTES) {
                    reportedBytes = downloadedBytes
                    onProgress(downloadedBytes, totalBytes)
                }
            }
            onProgress(downloadedBytes, totalBytes)
        }

        private fun deleteStaleUpdates(updatesDirectory: File) {
            updatesDirectory.listFiles()?.forEach { staleFile -> staleFile.delete() }
        }

        private fun apkFileName(versionCode: Int): String {
            return "update-$versionCode.apk"
        }

        companion object {
            private const val UPDATES_DIRECTORY_NAME = "updates"
            private const val HTTP_METHOD_GET = "GET"
            private const val HTTP_OK = 200
            private const val CONNECT_TIMEOUT_MILLIS = 15_000
            private const val READ_TIMEOUT_MILLIS = 30_000
            private const val BUFFER_SIZE_BYTES = 8 * 1024
            private const val PROGRESS_STEP_BYTES = 256L * 1024L
            private const val USER_AGENT_HEADER = "User-Agent"
            private const val USER_AGENT_VALUE = "proxy-killswitcher-android"
        }
    }
