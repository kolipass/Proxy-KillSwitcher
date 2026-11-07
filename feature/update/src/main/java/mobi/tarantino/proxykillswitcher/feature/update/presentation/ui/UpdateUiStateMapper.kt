package mobi.tarantino.proxykillswitcher.feature.update.presentation.ui

import androidx.compose.runtime.Stable
import dev.zacsweers.metro.Inject
import mobi.tarantino.proxykillswitcher.feature.update.domain.UpdateInstallMode

@Stable
@Inject
class UpdateUiStateMapper {
    fun map(state: UpdateState): UpdateUiState {
        val update = state.update ?: return HIDDEN_UI_STATE
        return UpdateUiState(
            isVisible = !state.isPostponed,
            titleText = "Update ${update.versionName}",
            statusText = mapStatusText(state = state),
            isProgressVisible = state.phase == UpdatePhase.Downloading,
            progressFraction = mapProgressFraction(state = state),
            primaryActionText = mapPrimaryActionText(state = state),
            isPrimaryActionVisible = state.phase == UpdatePhase.Ready,
            primaryActionKind =
                if (state.installMode == UpdateInstallMode.SelfInstall) {
                    UpdatePrimaryActionKind.Install
                } else {
                    UpdatePrimaryActionKind.OpenReleasePage
                },
            isPostponeVisible =
                state.phase == UpdatePhase.Downloading ||
                    state.phase == UpdatePhase.Ready ||
                    state.phase == UpdatePhase.Failed,
            isRetryVisible = state.phase == UpdatePhase.Failed,
        )
    }

    private fun mapStatusText(state: UpdateState): String {
        return when (state.phase) {
            UpdatePhase.Idle -> ""
            UpdatePhase.Downloading -> mapDownloadingText(state = state)
            UpdatePhase.Ready -> mapReadyText(installMode = state.installMode)
            UpdatePhase.Installing -> INSTALLING_STATUS_TEXT
            UpdatePhase.Failed -> FAILED_STATUS_TEXT
        }
    }

    private fun mapDownloadingText(state: UpdateState): String {
        val percent = (mapProgressFraction(state = state) * PERCENT_BASE).toInt()
        return if (state.totalBytes > 0L) {
            "Downloading $percent%"
        } else {
            DOWNLOADING_STATUS_TEXT
        }
    }

    private fun mapReadyText(installMode: UpdateInstallMode): String {
        return when (installMode) {
            UpdateInstallMode.SelfInstall -> SELF_INSTALL_READY_STATUS_TEXT
            UpdateInstallMode.OpenReleasePage -> OPEN_RELEASE_PAGE_READY_STATUS_TEXT
        }
    }

    private fun mapPrimaryActionText(state: UpdateState): String {
        return when (state.installMode) {
            UpdateInstallMode.SelfInstall -> INSTALL_ACTION_TEXT
            UpdateInstallMode.OpenReleasePage -> OPEN_RELEASE_PAGE_ACTION_TEXT
        }
    }

    private fun mapProgressFraction(state: UpdateState): Float {
        if (state.totalBytes <= 0L) {
            return MIN_PROGRESS_FRACTION
        }
        return (state.downloadedBytes.toFloat() / state.totalBytes.toFloat()).coerceIn(
            minimumValue = MIN_PROGRESS_FRACTION,
            maximumValue = MAX_PROGRESS_FRACTION,
        )
    }

    companion object {
        private const val PERCENT_BASE = 100
        private const val MIN_PROGRESS_FRACTION = 0f
        private const val MAX_PROGRESS_FRACTION = 1f
        private const val DOWNLOADING_STATUS_TEXT = "Downloading…"
        private const val INSTALLING_STATUS_TEXT = "Opening the installer…"
        private const val FAILED_STATUS_TEXT = "Update failed. Check your connection and try again."
        private const val SELF_INSTALL_READY_STATUS_TEXT =
            "Ready to install. The VPN will reconnect after the update."
        private const val OPEN_RELEASE_PAGE_READY_STATUS_TEXT =
            "This build cannot install updates itself. Open the release page to install it manually."
        private const val INSTALL_ACTION_TEXT = "Install"
        private const val OPEN_RELEASE_PAGE_ACTION_TEXT = "Open on GitHub"

        private val HIDDEN_UI_STATE =
            UpdateUiState(
                isVisible = false,
                titleText = "",
                statusText = "",
                isProgressVisible = false,
                progressFraction = MIN_PROGRESS_FRACTION,
                primaryActionText = "",
                isPrimaryActionVisible = false,
                primaryActionKind = UpdatePrimaryActionKind.Install,
                isPostponeVisible = false,
                isRetryVisible = false,
            )
    }
}
