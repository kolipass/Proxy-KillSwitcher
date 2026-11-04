package mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui

import android.app.Activity
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
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
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.ProxyVpnService
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun KillSwitchUiScreen(onOpenSavedProxyList: () -> Unit) {
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(text = "Proxy kill switch")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = uiState.connectionStatusText)
            if (uiState.isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Switch(
                    checked = uiState.isEnabled,
                    enabled = uiState.isToggleEnabled,
                    onCheckedChange = { _ -> onIntent(KillSwitchIntent.ToggleClick) },
                )
            }
        }
        Text(text = "Active proxy")
        Text(text = uiState.activeEndpointText)
        Button(
            onClick = { onIntent(KillSwitchIntent.ManageProxiesClick) },
        ) {
            Text("Manage proxies")
        }
        if (!uiState.isToggleEnabled) {
            Text("Save a valid endpoint before connecting.")
        }
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
