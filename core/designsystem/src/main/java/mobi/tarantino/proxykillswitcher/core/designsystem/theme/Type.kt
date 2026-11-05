package mobi.tarantino.proxykillswitcher.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography =
    Typography(
        bodyLarge =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontSize = 15.sp,
                lineHeight = 20.sp,
            ),
        titleLarge =
            TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 26.sp,
            ),
        headlineMedium =
            TextStyle(
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                lineHeight = 32.sp,
            ),
        labelSmall =
            TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp,
                lineHeight = 16.sp,
            ),
    )
