package at.schnapsenmitsepp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object SeppColors {
    val FeltLight = Color(0xFF2F8A55)
    val Felt = Color(0xFF1E6B3F)
    val FeltDark = Color(0xFF0E3B22)
    val Wood = Color(0xFF5A3A22)
    val WoodDark = Color(0xFF3B2414)
    val Gold = Color(0xFFE8C26A)
    val GoldDark = Color(0xFFB08A3C)
    val Cream = Color(0xFFFFF8E7)
    val CreamDark = Color(0xFFF1E3C2)
    val Ink = Color(0xFF2B1D12)
    val Paper = Color(0xFFFBF5E4)
    val PaperLine = Color(0xFF9DB7D6)
    val Margin = Color(0xFFD9534F)
    val Burgundy = Color(0xFF7A1F2B)
    val BurgundyDark = Color(0xFF4E1019)

    val Herz = Color(0xFFC62828)
    val Schelle = Color(0xFFD99A06)
    val Eichel = Color(0xFF7A5230)
    val Laub = Color(0xFF2E7D32)
}

private val Serif = FontFamily.Serif

private val SeppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 42.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 21.sp),
    titleMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontFamily = Serif, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Serif, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp),
)

/** Eigenes Theme, damit das Spiel in jeder Host-App gleich aussieht. */
@Composable
fun SeppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = SeppColors.Gold,
            onPrimary = SeppColors.Ink,
            secondary = SeppColors.Cream,
            background = SeppColors.FeltDark,
            surface = SeppColors.WoodDark,
            onSurface = SeppColors.Cream,
            onBackground = SeppColors.Cream,
        ),
        typography = SeppTypography,
        content = content,
    )
}
