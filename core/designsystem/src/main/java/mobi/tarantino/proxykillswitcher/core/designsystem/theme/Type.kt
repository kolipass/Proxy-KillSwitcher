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
                fontSize = 18.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontSize = 15.sp,
                lineHeight = 20.sp,
            ),
        titleLarge =
            TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 30.sp,
            ),
        headlineMedium =
            TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                lineHeight = 34.sp,
            ),
        labelSmall =
            TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.6.sp,
                lineHeight = 16.sp,
            ),
    )
