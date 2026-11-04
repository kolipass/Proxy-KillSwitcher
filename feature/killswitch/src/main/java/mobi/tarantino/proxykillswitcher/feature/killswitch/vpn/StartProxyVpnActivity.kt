package mobi.tarantino.proxykillswitcher.feature.killswitch.vpn

import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class StartProxyVpnActivity : ComponentActivity() {
    private val vpnPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                ProxyVpnService.start(this)
            }
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            return
        }
        val vpnPermissionIntent = VpnService.prepare(this)
        if (vpnPermissionIntent == null) {
            ProxyVpnService.start(this)
            finish()
        } else {
            vpnPermissionLauncher.launch(vpnPermissionIntent)
        }
    }
}
