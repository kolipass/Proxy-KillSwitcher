package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.AppTheme
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onBack) {
                Text("Back")
            }
            Text("Proxy editor")
        }
        if (uiState.isFrozen) {
            Text("Turn off the VPN to edit proxies.")
        }
        OutlinedTextField(
            value = uiState.hostText,
            onValueChange = { host -> onIntent(SavedProxyEditorIntent.HostChanged(host)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isFrozen,
            label = { Text("Proxy host") },
            singleLine = true,
        )
        OutlinedTextField(
            value = uiState.portText,
            onValueChange = { portText -> onIntent(SavedProxyEditorIntent.PortChanged(portText)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isFrozen,
            label = { Text("Proxy port") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        Text(text = "Protocol: ${uiState.protocolText}")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onIntent(SavedProxyEditorIntent.CheckClick) },
                enabled = uiState.isCheckEnabled,
            ) {
                if (uiState.isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Check proxy")
                }
            }
            Button(
                onClick = { onIntent(SavedProxyEditorIntent.SaveClick) },
                enabled = uiState.isSaveEnabled,
            ) {
                Text("Save proxy")
            }
        }
        if (uiState.checkStatusText.isNotEmpty()) {
            Text(uiState.checkStatusText)
        }
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
        )
}
