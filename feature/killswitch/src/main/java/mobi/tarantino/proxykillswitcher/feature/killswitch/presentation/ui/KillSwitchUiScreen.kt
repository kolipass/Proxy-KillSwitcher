package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import android.app.Activity
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.AppDimens
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.AppTheme
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.Hairline
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.OnPrimaryMuted
import mobi.tarantino.proxykillswitcher.core.di.viewmodel.viewModel
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.ProxyVpnService
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun KillSwitchUiScreen(
    onOpenSavedProxyList: () -> Unit,
    onOpenAddProxy: () -> Unit,
) {
    val viewModel = viewModel<KillSwitchViewModel>()
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    val vpnPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                ProxyVpnService.start(context)
            } else {
                viewModel.onIntent(KillSwitchIntent.VpnPermissionDenied)
            }
        }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            KillSwitchSideEffect.PrepareVpn -> {
                val vpnPermissionIntent = VpnService.prepare(context)
                if (vpnPermissionIntent == null) {
                    ProxyVpnService.start(context)
                } else {
                    vpnPermissionLauncher.launch(vpnPermissionIntent)
                }
            }

            KillSwitchSideEffect.StopVpn -> {
                ProxyVpnService.stop(context)
            }

            KillSwitchSideEffect.ProxySaved -> {
                Toast.makeText(context, "Proxy endpoint saved", Toast.LENGTH_SHORT).show()
            }

            KillSwitchSideEffect.ProxyRejected -> {
                Toast.makeText(context, "Change the endpoint and check it before saving", Toast.LENGTH_SHORT).show()
            }

            KillSwitchSideEffect.ConnectionFailed -> {
                Toast.makeText(context, "Could not connect through the proxy", Toast.LENGTH_LONG).show()
            }

            KillSwitchSideEffect.OpenSavedProxyList -> {
                onOpenSavedProxyList()
            }

            KillSwitchSideEffect.OpenAddProxy -> {
                onOpenAddProxy()
            }
        }
    }

    KillSwitchContent(
        uiState = viewModel.uiStateMapper.map(state),
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun KillSwitchContent(
    uiState: KillSwitchUiState,
    onIntent: (KillSwitchIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.SpaceScreen, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceBlock),
    ) {
        KillSwitchHeader()
        KillSwitchStateBlock(
            uiState = uiState,
            onIntent = onIntent,
        )
        ManageProxiesButton(
            onClick = { onIntent(KillSwitchIntent.ManageProxiesClick) },
        )
        if (!uiState.isToggleEnabled) {
            Text(
                text = "Save a valid endpoint before connecting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun KillSwitchHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceInline),
    ) {
        Text(
            text = "Proxy kill switch",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        HorizontalDivider(
            thickness = AppDimens.Hairline,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun KillSwitchStateBlock(
    uiState: KillSwitchUiState,
    onIntent: (KillSwitchIntent) -> Unit,
) {
    val containerColor =
        if (uiState.isEnabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.RadiusS),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDimens.SpaceBlock),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KillSwitchStateHeader(
                uiState = uiState,
                onIntent = onIntent,
            )
            KillSwitchEndpointReadout(
                activeEndpointText = uiState.activeEndpointText,
                isEnabled = uiState.isEnabled,
            )
        }
    }
}

@Composable
private fun KillSwitchStateHeader(
    uiState: KillSwitchUiState,
    onIntent: (KillSwitchIntent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = uiState.connectionStatusText,
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (uiState.isEnabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        if (uiState.isConnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(AppDimens.ProgressSize),
                strokeWidth = 2.dp,
                color =
                    if (uiState.isEnabled) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            )
        } else {
            Switch(
                checked = uiState.isEnabled,
                enabled = uiState.isToggleEnabled,
                onCheckedChange = { onIntent(KillSwitchIntent.ToggleClick) },
                modifier = Modifier.height(32.dp),
                colors =
                    if (uiState.isEnabled) {
                        SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                            checkedBorderColor = Color.Transparent,
                        )
                    } else {
                        SwitchDefaults.colors()
                    },
            )
        }
    }
}

@Composable
private fun KillSwitchEndpointReadout(
    activeEndpointText: String,
    isEnabled: Boolean,
) {
    val (displayAddress, protocolTag) =
        remember(activeEndpointText) {
            if (activeEndpointText.contains("://")) {
                val parts = activeEndpointText.split("://", limit = 2)
                parts.getOrElse(1) { "" } to parts[0].uppercase()
            } else {
                activeEndpointText to null
            }
        }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Active proxy",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color =
                if (isEnabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        Text(
            text = displayAddress,
            style =
                if (protocolTag != null) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
            color =
                if (isEnabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
        )
        if (protocolTag != null) {
            Text(
                text = protocolTag,
                style = MaterialTheme.typography.labelSmall,
                color =
                    if (isEnabled) {
                        OnPrimaryMuted
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

@Composable
private fun ManageProxiesButton(onClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimens.RadiusS))
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(AppDimens.RowHeight)
                    .padding(start = AppDimens.SpaceItem),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Manage proxies",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(AppDimens.IconSize),
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
private fun KillSwitchContentPreview() {
    AppTheme {
        KillSwitchContent(
            uiState = KillSwitchContentPreviewData.uiState,
            onIntent = {},
        )
    }
}

private object KillSwitchContentPreviewData {
    val uiState =
        KillSwitchUiState(
            activeEndpointText = "http://192.168.1.10:1080",
            hostText = "192.168.1.10",
            portText = "1080",
            isEnabled = false,
            isConnecting = false,
            isToggleEnabled = true,
            isSaveEnabled = false,
            isCheckEnabled = true,
            isChecking = false,
            checkStatusText = "",
            connectionStatusText = "Direct connection",
        )
}
