package mobi.tarantino.proxykillswitcher.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/**
 * Holds the three independent navigation surfaces (tab / fullscreen / sheet) so a feature never
 * touches a [NavHostController] directly. The sheet surface isn't a real back stack — a bottom
 * sheet is one screen deep in practice, so it's just "the currently shown [SheetRoute], if any."
 */
@Stable
class AppNavigator(
    val tabNavController: NavHostController,
    val fullscreenNavController: NavHostController,
) {
    var currentSheet by mutableStateOf<SheetRoute?>(null)
        private set

    fun navigate(route: TabRoute) {
        tabNavController.navigate(route)
    }

    fun navigate(route: FullscreenRoute) {
        fullscreenNavController.navigate(route)
    }

    fun back(): Boolean = fullscreenNavController.popBackStack()

    fun showSheet(route: SheetRoute) {
        currentSheet = route
    }

    fun dismissSheet() {
        currentSheet = null
    }
}

@Composable
fun rememberAppNavigator(
    tabNavController: NavHostController = rememberNavController(),
    fullscreenNavController: NavHostController = rememberNavController(),
): AppNavigator =
    remember(tabNavController, fullscreenNavController) {
        AppNavigator(tabNavController, fullscreenNavController)
    }

val LocalAppNavigator =
    compositionLocalOf<AppNavigator> {
        error("No AppNavigator provided in this composition")
    }
