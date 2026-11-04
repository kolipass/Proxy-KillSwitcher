package mobi.tarantino.proxykillswitcher.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors =
    lightColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        background = Background,
        onBackground = OnBackground,
        error = Error,
    )

private val DarkColors =
    darkColorScheme(
        primary = PrimaryDark,
        onPrimary = OnPrimaryDark,
        background = BackgroundDark,
        onBackground = OnBackgroundDark,
        error = ErrorDark,
    )

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
