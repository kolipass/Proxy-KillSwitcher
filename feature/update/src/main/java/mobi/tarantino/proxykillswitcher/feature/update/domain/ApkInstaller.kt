package mobi.tarantino.proxykillswitcher.feature.update.domain

import java.io.File

interface ApkInstaller {
    fun install(apkFile: File): InstallResult
}
