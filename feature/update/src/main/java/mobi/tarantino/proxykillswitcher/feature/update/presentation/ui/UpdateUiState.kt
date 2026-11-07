package mobi.tarantino.proxykillswitcher.feature.update.presentation.ui

import androidx.compose.runtime.Immutable

@Immutable
data class UpdateUiState(
    val isVisible: Boolean,
    val titleText: String,
    val statusText: String,
    val isProgressVisible: Boolean,
    val progressFraction: Float,
    val primaryActionText: String,
    val isPrimaryActionVisible: Boolean,
    val primaryActionKind: UpdatePrimaryActionKind,
    val isPostponeVisible: Boolean,
    val isRetryVisible: Boolean,
)

enum class UpdatePrimaryActionKind {
    Install,
    OpenReleasePage,
}
