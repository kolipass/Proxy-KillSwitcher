package mobi.tarantino.proxykillswitcher.feature.update.domain

import java.io.File

sealed interface UpdateDownload {
    data class InProgress(
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : UpdateDownload

    data class Ready(
        val apkFile: File,
    ) : UpdateDownload

    data object Failed : UpdateDownload
}
