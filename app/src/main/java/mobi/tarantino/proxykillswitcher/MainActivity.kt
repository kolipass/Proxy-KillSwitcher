package mobi.tarantino.proxykillswitcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hasRoute
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.AppTheme
import mobi.tarantino.proxykillswitcher.core.di.viewmodel.LocalViewModelFactoryProvider
import mobi.tarantino.proxykillswitcher.core.navigation.AppBottomSheetHost
import mobi.tarantino.proxykillswitcher.core.navigation.FullscreenNavHost
import mobi.tarantino.proxykillswitcher.core.navigation.LocalAppNavigator
import mobi.tarantino.proxykillswitcher.core.navigation.rememberAppNavigator
import mobi.tarantino.proxykillswitcher.feature.killswitch.navigation.KillSwitchHome
import mobi.tarantino.proxykillswitcher.feature.killswitch.navigation.SavedProxyEditor
import mobi.tarantino.proxykillswitcher.feature.killswitch.navigation.killSwitchFullscreenGraph
import mobi.tarantino.proxykillswitcher.feature.killswitch.vpn.KillSwitchTileService

class MainActivity : ComponentActivity() {
    private var pendingIntentToHandle by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingIntentToHandle = intent
        enableEdgeToEdge()
        val app = application as ProxyKillSwitcherApp
        app.graph.appInitializer.start(scope = lifecycleScope)

        setContent {
            val navigator = rememberAppNavigator()
            LaunchedEffect(pendingIntentToHandle) {
                val receivedIntent = pendingIntentToHandle ?: return@LaunchedEffect
                if (receivedIntent.getBooleanExtra(KillSwitchTileService.EXTRA_OPEN_ADD_PROXY, false)) {
                    pendingIntentToHandle = null
                    intent.removeExtra(KillSwitchTileService.EXTRA_OPEN_ADD_PROXY)
                    if (navigator.fullscreenNavController.currentDestination?.hasRoute<SavedProxyEditor>() != true) {
                        navigator.navigate(SavedProxyEditor())
                    }
                }
            }

            CompositionLocalProvider(
                LocalViewModelFactoryProvider provides app.graph.viewModelFactoryProvider,
                LocalAppNavigator provides navigator,
            ) {
                AppTheme {
                    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                        FullscreenNavHost(
                            navController = navigator.fullscreenNavController,
                            startDestination = KillSwitchHome,
                        ) {
                            killSwitchFullscreenGraph()
                        }
                    }
                    AppBottomSheetHost(navigator) { }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntentToHandle = intent
    }
}
