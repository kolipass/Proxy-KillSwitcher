package mobi.tarantino.proxykillswitcher.feature.update.presentation.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.AppDimens
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.AppTheme
import mobi.tarantino.proxykillswitcher.core.di.viewmodel.viewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun UpdateBlock(modifier: Modifier = Modifier) {
    val viewModel = viewModel<UpdateViewModel>()
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            UpdateSideEffect.OpenUnknownSourcesSettings -> context.openUnknownSourcesSettings()
            is UpdateSideEffect.OpenReleasePage -> context.openReleasePage(releaseUrl = sideEffect.releaseUrl)
        }
    }

    UpdateBlockContent(
        uiState = viewModel.uiStateMapper.map(state),
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
private fun UpdateBlockContent(
    uiState: UpdateUiState,
    onIntent: (UpdateIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!uiState.isVisible) {
        return
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.RadiusS),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDimens.SpaceBlock),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceInline),
        ) {
            UpdateBlockTexts(uiState = uiState)
            if (uiState.isProgressVisible) {
                LinearProgressIndicator(
                    progress = { uiState.progressFraction },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            UpdateBlockActions(uiState = uiState, onIntent = onIntent)
        }
    }
}

@Composable
private fun UpdateBlockTexts(uiState: UpdateUiState) {
    Text(
        text = uiState.titleText,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = uiState.statusText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun UpdateBlockActions(
    uiState: UpdateUiState,
    onIntent: (UpdateIntent) -> Unit,
) {
    if (!uiState.isPrimaryActionVisible && !uiState.isPostponeVisible && !uiState.isRetryVisible) {
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceItem),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (uiState.isPrimaryActionVisible) {
            Button(onClick = { onIntent(uiState.primaryActionKind.toIntent()) }) {
                Text(text = uiState.primaryActionText)
            }
        }
        if (uiState.isRetryVisible) {
            TextButton(onClick = { onIntent(UpdateIntent.RetryClick) }) {
                Text(text = "Retry")
            }
        }
        if (uiState.isPostponeVisible) {
            TextButton(onClick = { onIntent(UpdateIntent.PostponeClick) }) {
                Text(text = "Later")
            }
        }
    }
}

private fun UpdatePrimaryActionKind.toIntent(): UpdateIntent {
    return when (this) {
        UpdatePrimaryActionKind.Install -> UpdateIntent.InstallClick
        UpdatePrimaryActionKind.OpenReleasePage -> UpdateIntent.OpenReleasePageClick
    }
}

private fun Context.openUnknownSourcesSettings() {
    val settingsIntent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivitySafely(intent = settingsIntent)
}

private fun Context.openReleasePage(releaseUrl: String) {
    val browserIntent =
        Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivitySafely(intent = browserIntent)
}

private fun Context.startActivitySafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (activityNotFoundException: ActivityNotFoundException) {
        Log.w(LOG_TAG, "No activity handles $intent", activityNotFoundException)
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateBlockContentPreview() {
    AppTheme {
        UpdateBlockContent(
            uiState = UpdateBlockContentPreviewData.uiState,
            onIntent = {},
        )
    }
}

private object UpdateBlockContentPreviewData {
    val uiState =
        UpdateUiState(
            isVisible = true,
            titleText = "Update 1.2.3",
            statusText = "Downloading 42%",
            isProgressVisible = true,
            progressFraction = 0.42f,
            primaryActionText = "Install",
            isPrimaryActionVisible = false,
            primaryActionKind = UpdatePrimaryActionKind.Install,
            isPostponeVisible = true,
            isRetryVisible = false,
        )
}

private const val LOG_TAG = "UpdateBlock"
