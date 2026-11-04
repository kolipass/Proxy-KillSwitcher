package mobi.tarantino.proxykillswitcher.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun TabNavHost(
    navController: NavHostController,
    startDestination: TabRoute,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier, builder = builder)
}

@Composable
fun FullscreenNavHost(
    navController: NavHostController,
    startDestination: FullscreenRoute,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier, builder = builder)
}
