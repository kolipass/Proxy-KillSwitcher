package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.AppDimens
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.AppTheme
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.Hairline
import mobi.tarantino.proxykillswitcher.core.di.viewmodel.viewModel
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun SavedProxyEditorUiScreen(
    savedEndpoint: SavedProxyEndpoint?,
    onBack: () -> Unit,
) {
    val viewModel = viewModel<SavedProxyEditorViewModel>()
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(savedEndpoint) {
        viewModel.onIntent(SavedProxyEditorIntent.Initialize(savedEndpoint))
    }
    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            SavedProxyEditorSideEffect.Saved -> {
                onBack()
            }

            SavedProxyEditorSideEffect.Rejected -> {
                Toast.makeText(context, "Check the proxy before saving", Toast.LENGTH_SHORT).show()
            }
        }
    }

    SavedProxyEditorContent(
        uiState = viewModel.uiStateMapper.map(state),
        onBack = onBack,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun SavedProxyEditorContent(
    uiState: SavedProxyEditorUiState,
    onBack: () -> Unit,
    onIntent: (SavedProxyEditorIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.SpaceScreen, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceBlock),
    ) {
        SavedProxyEditorHeader(onBack = onBack)

        if (uiState.isFrozen) {
            SavedProxyEditorFrozenBanner()
        }

        LanScanBlock(
            lanScanUiState = uiState.lanScanUiState,
            onIntent = onIntent,
        )

        SavedProxyEditorFields(
            uiState = uiState,
            onIntent = onIntent,
        )

        SavedProxyEditorProtocolRow(protocolText = uiState.protocolText)

        SavedProxyEditorActionButtons(
            uiState = uiState,
            onIntent = onIntent,
        )

        if (uiState.checkStatusText.isNotEmpty()) {
            SavedProxyEditorStatusText(checkStatusText = uiState.checkStatusText)
        }
    }
}

@Composable
private fun SavedProxyEditorFrozenBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.RadiusS),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Text(
            text = "Turn off the VPN to edit proxies.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(AppDimens.SpaceInline),
        )
    }
}

@Composable
private fun SavedProxyEditorFields(
    uiState: SavedProxyEditorUiState,
    onIntent: (SavedProxyEditorIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceBlock)) {
        OutlinedTextField(
            value = uiState.hostText,
            onValueChange = { host -> onIntent(SavedProxyEditorIntent.HostChanged(host)) },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = AppDimens.FieldHeight),
            enabled = !uiState.isFrozen,
            label = { Text("Proxy host", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)) },
            shape = RoundedCornerShape(AppDimens.RadiusS),
            colors = savedProxyFieldColors(),
            singleLine = true,
        )

        OutlinedTextField(
            value = uiState.portText,
            onValueChange = { portText -> onIntent(SavedProxyEditorIntent.PortChanged(portText)) },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = AppDimens.FieldHeight),
            enabled = !uiState.isFrozen,
            label = { Text("Proxy port", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)) },
            shape = RoundedCornerShape(AppDimens.RadiusS),
            colors = savedProxyFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
    }
}

@Composable
private fun savedProxyFieldColors() =
    OutlinedTextFieldDefaults.colors(
        disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
        disabledTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
        disabledLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
    )

@Composable
private fun SavedProxyEditorProtocolRow(protocolText: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Protocol",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = protocolText.uppercase(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SavedProxyEditorActionButtons(
    uiState: SavedProxyEditorUiState,
    onIntent: (SavedProxyEditorIntent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceInline),
    ) {
        OutlinedButton(
            onClick = { onIntent(SavedProxyEditorIntent.CheckClick) },
            enabled = uiState.isCheckEnabled,
            modifier = Modifier.weight(1f).height(AppDimens.ButtonHeight),
            shape = RoundedCornerShape(AppDimens.RadiusS),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
                ),
        ) {
            if (uiState.isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppDimens.ProgressSize),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = "Check proxy",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Button(
            onClick = { onIntent(SavedProxyEditorIntent.SaveClick) },
            enabled = uiState.isSaveEnabled,
            modifier = Modifier.weight(1f).height(AppDimens.ButtonHeight),
            shape = RoundedCornerShape(AppDimens.RadiusS),
        ) {
            Text(
                text = "Save proxy",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun SavedProxyEditorStatusText(checkStatusText: String) {
    val statusColor =
        if (checkStatusText.contains("succeeded", ignoreCase = true)) {
            MaterialTheme.colorScheme.primary
        } else if (checkStatusText.contains("failed", ignoreCase = true)) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Text(
        text = checkStatusText,
        style = MaterialTheme.typography.bodyMedium,
        color = statusColor,
    )
}

@Composable
private fun SavedProxyEditorHeader(onBack: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceInline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceInline),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier =
                    Modifier
                        .size(24.dp)
                        .clickable(onClick = onBack),
            )
            Text(
                text = "Proxy editor",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        HorizontalDivider(
            thickness = AppDimens.Hairline,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SavedProxyEditorContentPreview() {
    AppTheme {
        SavedProxyEditorContent(
            uiState = SavedProxyEditorContentPreviewData.uiState,
            onBack = {},
            onIntent = {},
        )
    }
}

private object SavedProxyEditorContentPreviewData {
    val uiState =
        SavedProxyEditorUiState(
            hostText = "192.168.1.10",
            portText = "1080",
            protocolText = "http",
            isFrozen = false,
            isChecking = false,
            isCheckEnabled = true,
            isSaveEnabled = true,
            checkStatusText = "Proxy check succeeded",
            lanScanUiState = LanScanUiState.Hidden,
        )
}
