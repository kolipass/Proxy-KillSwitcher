package mobi.tarantino.proxykillswitcher.feature.update.presentation.ui

import mobi.tarantino.proxykillswitcher.feature.update.domain.AvailableUpdate
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateInstallMode
import java.io.File

data class UpdateState(
    val update: AvailableUpdate? = null,
    val phase: UpdatePhase = UpdatePhase.Idle,
    val installMode: UpdateInstallMode = UpdateInstallMode.SelfInstall,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadedApk: File? = null,
    val isPostponed: Boolean = false,
)

sealed interface UpdatePhase {
    data object Idle : UpdatePhase

    data object Downloading : UpdatePhase

    data object Ready : UpdatePhase

    data object Installing : UpdatePhase

    data object Failed : UpdatePhase
}
