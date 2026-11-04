package mobi.tarantino.proxykillswitcher.feature.killswitch.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import mobi.tarantino.proxykillswitcher.core.navigation.AppNavigator
import mobi.tarantino.proxykillswitcher.core.navigation.LocalAppNavigator
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.ProxyProtocol
import mobi.tarantino.proxykillswitcher.feature.killswitch.domain.SavedProxyEndpoint
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.KillSwitchUiScreen
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyEditorUiScreen
import mobi.tarantino.proxykillswitcher.feature.killswitch.presentation.ui.SavedProxyListUiScreen

fun NavGraphBuilder.killSwitchFullscreenGraph() {
    composable<KillSwitchHome> {
        val navigator = LocalAppNavigator.current
        KillSwitchUiScreen(
            onOpenSavedProxyList = { navigator.navigate(SavedProxyList) },
            onOpenAddProxy = { navigator.openEditor(null) },
        )
    }
    composable<SavedProxyList> {
        val navigator = LocalAppNavigator.current
        SavedProxyListUiScreen(
            onBack = { navigator.back() },
            onOpenEditor = { savedEndpoint -> navigator.openEditor(savedEndpoint) },
        )
    }
    composable<SavedProxyEditor> { backStackEntry ->
        val navigator = LocalAppNavigator.current
        val route = backStackEntry.toRoute<SavedProxyEditor>()
        SavedProxyEditorUiScreen(
            savedEndpoint = route.toSavedEndpoint(),
            onBack = { navigator.back() },
        )
    }
}

private fun AppNavigator.openEditor(savedEndpoint: SavedProxyEndpoint?) {
    navigate(
        SavedProxyEditor(
            host = savedEndpoint?.host,
            port = savedEndpoint?.port,
            protocol = savedEndpoint?.protocol?.storedValue,
        ),
    )
}

private fun SavedProxyEditor.toSavedEndpoint(): SavedProxyEndpoint? {
    val routeHost = host ?: return null
    val routePort = port ?: return null
    val routeProtocol = protocol ?: return null
    return SavedProxyEndpoint(
        host = routeHost,
        port = routePort,
        protocol = ProxyProtocol.fromStored(routeProtocol),
    )
}
