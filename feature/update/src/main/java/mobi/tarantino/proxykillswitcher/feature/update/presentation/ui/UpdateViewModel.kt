package mobi.tarantino.proxykillswitcher.feature.update.presentation.ui

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.update.domain.AvailableUpdate
import mobi.tarantino.proxykillswitcher.feature.update.domain.CheckForUpdateUseCase
import mobi.tarantino.proxykillswitcher.feature.update.domain.DownloadUpdateUseCase
import mobi.tarantino.proxykillswitcher.feature.update.domain.InstallResult
import mobi.tarantino.proxykillswitcher.feature.update.domain.InstallUpdateUseCase
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateCheck
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateDownload
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateInstallMode
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import org.orbitmvi.orbit.syntax.Syntax

@Stable
@Inject
class UpdateViewModel
    constructor(
        val uiStateMapper: UpdateUiStateMapper,
        private val checkForUpdateUseCase: CheckForUpdateUseCase,
        private val downloadUpdateUseCase: DownloadUpdateUseCase,
        private val installUpdateUseCase: InstallUpdateUseCase,
    ) : ViewModel(),
        ContainerHost<UpdateState, UpdateSideEffect> {
        override val container: Container<UpdateState, UpdateSideEffect> =
            viewModelScope.container(initialState = UpdateState()) {
                loadUpdate(force = false)
            }

        fun onIntent(intent: UpdateIntent) {
            when (intent) {
                UpdateIntent.InstallClick -> installUpdate()
                UpdateIntent.PostponeClick -> postponeUpdate()
                UpdateIntent.RetryClick -> loadUpdate(force = true)
                UpdateIntent.OpenReleasePageClick -> openReleasePage()
            }
        }

        private fun loadUpdate(force: Boolean) {
            intent {
                if (force) {
                    reduce {
                        state.copy(
                            update = null,
                            phase = UpdatePhase.Idle,
                            downloadedApk = null,
                            downloadedBytes = 0L,
                            totalBytes = 0L,
                            isPostponed = false,
                        )
                    }
                }
                when (val updateCheck = checkForUpdateUseCase(force = force)) {
                    is UpdateCheck.Available -> {
                        reduce {
                            state.copy(
                                update = updateCheck.update,
                                installMode = updateCheck.installMode,
                                phase = initialPhaseFor(installMode = updateCheck.installMode),
                                totalBytes = updateCheck.update.apkSizeBytes,
                            )
                        }
                        if (updateCheck.installMode == UpdateInstallMode.SelfInstall) {
                            downloadUpdate(updateCheck.update)
                        }
                    }

                    UpdateCheck.Failed, UpdateCheck.NoUpdate, UpdateCheck.Throttled -> {
                        Unit
                    }
                }
            }
        }

        private suspend fun Syntax<UpdateState, UpdateSideEffect>.downloadUpdate(update: AvailableUpdate) {
            downloadUpdateUseCase(update).collect { download ->
                when (download) {
                    is UpdateDownload.InProgress -> {
                        reduce {
                            state.copy(
                                downloadedBytes = download.downloadedBytes,
                                totalBytes = download.totalBytes,
                            )
                        }
                    }

                    is UpdateDownload.Ready -> {
                        reduce {
                            state.copy(
                                phase = UpdatePhase.Ready,
                                downloadedApk = download.apkFile,
                            )
                        }
                    }

                    UpdateDownload.Failed -> {
                        reduce {
                            state.copy(phase = UpdatePhase.Failed)
                        }
                    }
                }
            }
        }

        private fun installUpdate() {
            intent {
                val apkFile = state.downloadedApk ?: return@intent
                reduce { state.copy(phase = UpdatePhase.Installing) }
                when (installUpdateUseCase(apkFile)) {
                    InstallResult.InstallerLaunched -> {
                        Unit
                    }

                    InstallResult.UnknownSourcesRequired -> {
                        reduce { state.copy(phase = UpdatePhase.Ready) }
                        postSideEffect(UpdateSideEffect.OpenUnknownSourcesSettings)
                    }

                    InstallResult.Failed -> {
                        reduce { state.copy(phase = UpdatePhase.Failed) }
                    }
                }
            }
        }

        private fun postponeUpdate() {
            intent {
                reduce { state.copy(isPostponed = true) }
            }
        }

        private fun openReleasePage() {
            intent {
                val releaseUrl = state.update?.releaseUrl ?: return@intent
                postSideEffect(UpdateSideEffect.OpenReleasePage(releaseUrl))
            }
        }

        private fun initialPhaseFor(installMode: UpdateInstallMode): UpdatePhase {
            return if (installMode == UpdateInstallMode.SelfInstall) {
                UpdatePhase.Downloading
            } else {
                UpdatePhase.Ready
            }
        }
    }

sealed interface UpdateIntent {
    data object InstallClick : UpdateIntent

    data object PostponeClick : UpdateIntent

    data object RetryClick : UpdateIntent

    data object OpenReleasePageClick : UpdateIntent
}

sealed interface UpdateSideEffect {
    data object OpenUnknownSourcesSettings : UpdateSideEffect

    data class OpenReleasePage(
        val releaseUrl: String,
    ) : UpdateSideEffect
}
