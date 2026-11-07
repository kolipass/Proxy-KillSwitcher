package mobi.tarantino.proxykillswitcher.feature.update.domain

import dev.zacsweers.metro.Inject
import java.io.File

@Inject
class InstallUpdateUseCase
    constructor(
        private val apkInstaller: ApkInstaller,
    ) {
        operator fun invoke(apkFile: File): InstallResult {
            return apkInstaller.install(apkFile)
        }
    }
