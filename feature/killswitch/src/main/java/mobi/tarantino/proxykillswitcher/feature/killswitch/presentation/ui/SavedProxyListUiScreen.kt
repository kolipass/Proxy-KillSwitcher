package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.AppDimens
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.AppTheme
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.Hairline
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
                SavedProxyAddFab(onAdd = { onIntent(SavedProxyListIntent.AddClick) })
            }
        },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(horizontal = AppDimens.SpaceScreen, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceBlock),
        ) {
            SavedProxyListHeader(onBack = onBack)

            if (uiState.isFrozen) {
                SavedProxyFrozenBanner()
            }

            if (uiState.endpoints.isEmpty()) {
                Text(
                    text = "No saved proxies",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SavedProxyItemsList(
                    endpoints = uiState.endpoints,
                    isFrozen = uiState.isFrozen,
                    onIntent = onIntent,
                )
            }
        }
    }
}

@Composable
private fun SavedProxyAddFab(onAdd: () -> Unit) {
    FloatingActionButton(
        onClick = onAdd,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text("+", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SavedProxyFrozenBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.RadiusS),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Text(
            text = "Turn off the VPN to change saved proxies.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(AppDimens.SpaceInline),
        )
    }
}

@Composable
private fun SavedProxyItemsList(
    endpoints: List<SavedProxyListUiState.Endpoint>,
    isFrozen: Boolean,
    onIntent: (SavedProxyListIntent) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(
            items = endpoints,
            key = { endpoint -> endpoint.addressText },
        ) { endpoint ->
            SavedProxyEndpointRow(
                endpoint = endpoint,
                isFrozen = isFrozen,
                onIntent = onIntent,
            )
            HorizontalDivider(
                thickness = AppDimens.Hairline,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
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
                .height(AppDimens.RowHeight)
                .clickable(enabled = !isFrozen) {
                    onIntent(SavedProxyListIntent.EditClick(endpoint.savedEndpoint))
                },
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceInline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = endpoint.isActive,
            enabled = !isFrozen && !endpoint.isSelecting,
            onClick = { onIntent(SavedProxyListIntent.SelectClick(endpoint.savedEndpoint)) },
        )
        SavedProxyEndpointAddress(
            addressText = endpoint.addressText,
            modifier = Modifier.weight(1f),
        )
        if (endpoint.isSelecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(AppDimens.ProgressSize),
                strokeWidth = 2.dp,
            )
        }
        Text(
            text = "✕",
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (isFrozen) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                } else {
                    MaterialTheme.colorScheme.error
                },
            modifier =
                Modifier
                    .clickable(enabled = !isFrozen) {
                        onIntent(SavedProxyListIntent.DeleteClick(endpoint.savedEndpoint))
                    }.padding(8.dp),
        )
    }
}

@Composable
private fun SavedProxyEndpointAddress(
    addressText: String,
    modifier: Modifier = Modifier,
) {
    val (address, protocol) =
        remember(addressText) {
            if (addressText.contains("://")) {
                val parts = addressText.split("://", limit = 2)
                parts.getOrElse(1) { "" } to parts[0].uppercase()
            } else {
                addressText to null
            }
        }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = address,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (protocol != null) {
            Text(
                text = protocol,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun SavedProxyListHeader(onBack: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceInline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceInline),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "‹",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier =
                    Modifier
                        .clickable(onClick = onBack)
                        .padding(horizontal = 8.dp),
            )
            Text(
                text = "Saved proxies",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        HorizontalDivider(
            thickness = AppDimens.Hairline,
            color = Hairline,
        )
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
    val uiState =
        SavedProxyListUiState(
            endpoints =
                listOf(
                    SavedProxyListUiState.Endpoint(
                        savedEndpoint =
                            SavedProxyEndpoint(
                                host = "192.168.1.10",
                                port = 1080,
                                protocol = ProxyProtocol.Http,
                            ),
                        addressText = "http://192.168.1.10:1080",
                        isActive = true,
                        isSelecting = false,
                    ),
                    SavedProxyListUiState.Endpoint(
                        savedEndpoint =
                            SavedProxyEndpoint(
                                host = "10.0.0.1",
                                port = 8080,
                                protocol = ProxyProtocol.Http,
                            ),
                        addressText = "http://10.0.0.1:8080",
                        isActive = false,
                        isSelecting = false,
                    ),
                ),
            isFrozen = false,
        )
}
