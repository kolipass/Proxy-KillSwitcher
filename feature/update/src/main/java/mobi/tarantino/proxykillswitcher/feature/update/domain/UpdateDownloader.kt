package mobi.tarantino.proxykillswitcher.feature.update.domain

import kotlinx.coroutines.flow.Flow

interface UpdateDownloader {
    fun download(update: AvailableUpdate): Flow<UpdateDownload>
}
