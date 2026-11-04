package mobi.tarantino.proxykillswitcher.core.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable

/**
 * Renders whichever [SheetRoute] [AppNavigator.currentSheet] holds, or nothing. [content]
 * dispatches on the concrete route type — every feature that shows a sheet adds a branch here
 * at the app composition root, since that's the one place that knows about every feature's routes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheetHost(
    navigator: AppNavigator,
    content: @Composable (SheetRoute) -> Unit,
) {
    val route = navigator.currentSheet ?: return
    ModalBottomSheet(
        onDismissRequest = navigator::dismissSheet,
        sheetState = rememberModalBottomSheetState(),
    ) {
        content(route)
    }
}
