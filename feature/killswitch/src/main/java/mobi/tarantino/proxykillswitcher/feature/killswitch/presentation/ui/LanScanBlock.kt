package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.AppDimens
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.DiscoveredProxy

@Composable
fun LanScanBlock(
    lanScanUiState: LanScanUiState,
    onIntent: (SavedProxyEditorIntent) -> Unit,
) {
    if (lanScanUiState == LanScanUiState.Hidden) {
        return
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.RadiusS),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDimens.SpaceInline),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceInline),
        ) {
            when (lanScanUiState) {
                LanScanUiState.Hidden -> {
                    Unit
                }

                LanScanUiState.NotOnWifi -> {
                    Text(
                        text = "Connect to Wi-Fi to discover proxies in your local network.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                LanScanUiState.Scanning -> {
                    Text(
                        text = "Searching for proxies in the local network…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceInline),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(AppDimens.ProgressSize),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "Scanning…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is LanScanUiState.Completed -> {
                    LanScanResults(
                        lanScanUiState = lanScanUiState,
                        onIntent = onIntent,
                    )
                }
            }
        }
    }
}

@Composable
private fun LanScanResults(
    lanScanUiState: LanScanUiState.Completed,
    onIntent: (SavedProxyEditorIntent) -> Unit,
) {
    val isScanning = lanScanUiState.progressText.startsWith("Scanning")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${lanScanUiState.progressText} · ${lanScanUiState.elapsedText}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!isScanning) {
            Text(
                text = "Rescan",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onIntent(SavedProxyEditorIntent.RescanClick) },
            )
        }
    }
    if (isScanning) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    if (lanScanUiState.found.isEmpty() && !isScanning) {
        Text(
            text = "No proxies found in the local network.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    lanScanUiState.found.forEach { discoveredProxy ->
        LanScanResultRow(
            discoveredProxy = discoveredProxy,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun LanScanResultRow(
    discoveredProxy: DiscoveredProxy,
    onIntent: (SavedProxyEditorIntent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimens.RadiusS))
                .clickable(enabled = !discoveredProxy.isAlreadySaved) {
                    onIntent(SavedProxyEditorIntent.DiscoveredProxyClick(discoveredProxy))
                }.defaultMinSize(minHeight = AppDimens.FieldHeight)
                .padding(vertical = AppDimens.SpaceInline),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${discoveredProxy.host}:${discoveredProxy.port}",
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (discoveredProxy.isAlreadySaved) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
        )
        Text(
            text =
                if (discoveredProxy.isAlreadySaved) {
                    "Already added"
                } else {
                    discoveredProxy.protocol.storedValue.uppercase()
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
