package mobi.tarantino.proxykillswitcher.feature.update.domain

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Inject
class DownloadUpdateUseCase
    constructor(
        private val updateDownloader: UpdateDownloader,
        private val downloadedApkVerifier: DownloadedApkVerifier,
    ) {
        operator fun invoke(update: AvailableUpdate): Flow<UpdateDownload> {
            return updateDownloader
                .download(update)
                .map { download -> verify(download = download, expectedVersionCode = update.versionCode) }
                .catch { emit(UpdateDownload.Failed) }
        }

        private fun verify(
            download: UpdateDownload,
            expectedVersionCode: Int,
        ): UpdateDownload {
            return when (download) {
                is UpdateDownload.Ready -> {
                    val isTrusted =
                        downloadedApkVerifier.isTrusted(
                            apkFile = download.apkFile,
                            expectedVersionCode = expectedVersionCode,
                        )
                    if (isTrusted) download else UpdateDownload.Failed
                }

                is UpdateDownload.InProgress, UpdateDownload.Failed -> {
                    download
                }
            }
        }
    }
