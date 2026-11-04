package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.AppTheme
import mobi.tarantino.proxykillswitcher.core.di.viewmodel.viewModel
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyProtocol
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun SavedProxyListUiScreen(
    onBack: () -> Unit,
    onOpenEditor: (SavedProxyEndpoint?) -> Unit,
) {
    val viewModel = viewModel<SavedProxyListViewModel>()
    val state by viewModel.collectAsState()
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is SavedProxyListSideEffect.OpenEditor -> {
                onOpenEditor(sideEffect.savedEndpoint)
            }

            SavedProxyListSideEffect.SelectionFailed -> {
                Toast.makeText(context, "Proxy check failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    SavedProxyListContent(
        uiState = viewModel.uiStateMapper.map(state),
        onBack = onBack,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun SavedProxyListContent(
    uiState: SavedProxyListUiState,
    onBack: () -> Unit,
    onIntent: (SavedProxyListIntent) -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            if (!uiState.isFrozen) {
                FloatingActionButton(
                    onClick = { onIntent(SavedProxyListIntent.AddClick) },
                ) {
                    Text("+")
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onBack) {
                    Text("Back")
                }
                Text("Saved proxies")
            }
            if (uiState.isFrozen) {
                Text("Turn off the VPN to change saved proxies.")
            }
            if (uiState.endpoints.isEmpty()) {
                Text("No saved proxies")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(
                        items = uiState.endpoints,
                        key = { endpoint -> endpoint.addressText },
                    ) { endpoint ->
                        SavedProxyEndpointRow(
                            endpoint = endpoint,
                            isFrozen = uiState.isFrozen,
                            onIntent = onIntent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedProxyEndpointRow(
    endpoint: SavedProxyListUiState.Endpoint,
    isFrozen: Boolean,
    onIntent: (SavedProxyListIntent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = !isFrozen) { onIntent(SavedProxyListIntent.EditClick(endpoint.savedEndpoint)) }
                .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = endpoint.isActive,
            enabled = !isFrozen && !endpoint.isSelecting,
            onClick = { onIntent(SavedProxyListIntent.SelectClick(endpoint.savedEndpoint)) },
        )
        Text(
            text = endpoint.addressText,
            modifier = Modifier.weight(1f),
        )
        if (endpoint.isSelecting) {
            CircularProgressIndicator()
        }
        Button(
            enabled = !isFrozen,
            onClick = { onIntent(SavedProxyListIntent.DeleteClick(endpoint.savedEndpoint)) },
        ) {
            Text("Delete")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SavedProxyListContentPreview() {
    AppTheme {
        SavedProxyListContent(
            uiState = SavedProxyListContentPreviewData.uiState,
            onBack = {},
            onIntent = {},
        )
    }
}

private object SavedProxyListContentPreviewData {
    private val savedEndpoint =
        SavedProxyEndpoint(
            host = "192.168.1.10",
            port = 1080,
            protocol = ProxyProtocol.Http,
        )
    val uiState =
        SavedProxyListUiState(
            endpoints =
                listOf(
                    SavedProxyListUiState.Endpoint(
                        savedEndpoint = savedEndpoint,
                        addressText = "http://192.168.1.10:1080",
                        isActive = true,
                        isSelecting = false,
                    ),
                ),
            isFrozen = false,
        )
}
