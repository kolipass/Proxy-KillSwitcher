package mobi.tarantino.proxykillswitcher

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
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import mobi.tarantino.proxykillswitcher.core.designsystem.theme.AppTheme
import mobi.tarantino.proxykillswitcher.core.di.viewmodel.LocalViewModelFactoryProvider
import mobi.tarantino.proxykillswitcher.core.navigation.AppBottomSheetHost
import mobi.tarantino.proxykillswitcher.core.navigation.FullscreenNavHost
import mobi.tarantino.proxykillswitcher.core.navigation.LocalAppNavigator
import mobi.tarantino.proxykillswitcher.core.navigation.rememberAppNavigator
import mobi.tarantino.proxykillswitcher.feature.killswitch.navigation.KillSwitchHome
import mobi.tarantino.proxykillswitcher.feature.killswitch.navigation.killSwitchFullscreenGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ProxyKillSwitcherApp
        app.graph.appInitializer.start(scope = lifecycleScope)

        setContent {
            val navigator = rememberAppNavigator()
            CompositionLocalProvider(
                LocalViewModelFactoryProvider provides app.graph.viewModelFactoryProvider,
                LocalAppNavigator provides navigator,
            ) {
                AppTheme {
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                    ) {
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
    }
}
