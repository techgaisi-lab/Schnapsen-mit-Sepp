package at.schnapsenmitsepp.ui.cards

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import at.schnapsenmitsepp.engine.Suit
import at.schnapsenmitsepp.ui.theme.SeppColors

fun suitColor(suit: Suit): Color = when (suit) {
    Suit.HERZ -> SeppColors.Herz
    Suit.SCHELLE -> SeppColors.Schelle
    Suit.EICHEL -> SeppColors.Eichel
    Suit.LAUB -> SeppColors.Laub
}

/** Rechnet Einheitskoordinaten (-0.5..0.5) in Pixel um. */
internal class UnitSpace(val center: Offset, val size: Float) {
    fun x(v: Float) = center.x + v * size
    fun y(v: Float) = center.y + v * size
    fun o(x: Float, y: Float) = Offset(x(x), y(y))
    fun d(v: Float) = v * size
    fun Path.m(x: Float, y: Float) = moveTo(x(x), y(y))
    fun Path.l(x: Float, y: Float) = lineTo(x(x), y(y))
    fun Path.c(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
        cubicTo(x(x1), y(y1), x(x2), y(y2), x(x3), y(y3))
    fun Path.q(x1: Float, y1: Float, x2: Float, y2: Float) = quadraticTo(x(x1), y(y1), x(x2), y(y2))
}

/** Zeichnet ein Farbsymbol der doppeldeutschen Karten (Herz, Schelle, Eichel, Laub). */
fun DrawScope.drawSuit(suit: Suit, center: Offset, size: Float) {
    val u = UnitSpace(center, size)
    when (suit) {
        Suit.HERZ -> drawHerz(u)
        Suit.SCHELLE -> drawSchelle(u)
        Suit.EICHEL -> drawEichel(u)
        Suit.LAUB -> drawLaub(u)
    }
}

private fun DrawScope.drawHerz(u: UnitSpace) { with(u) {
    val p = Path().apply {
        m(0f, 0.45f)
        c(-0.62f, 0.04f, -0.56f, -0.46f, -0.25f, -0.46f)
        c(-0.1f, -0.46f, 0f, -0.34f, 0f, -0.22f)
        c(0f, -0.34f, 0.1f, -0.46f, 0.25f, -0.46f)
        c(0.56f, -0.46f, 0.62f, 0.04f, 0f, 0.45f)
        close()
    }
    drawPath(
        p,
        Brush.radialGradient(
            listOf(Color(0xFFFF6B6B), SeppColors.Herz, Color(0xFF8E1414)),
            center = o(-0.16f, -0.22f),
            radius = d(0.7f),
        ),
    )
    drawPath(p, Color(0xFF6D0F0F), style = Stroke(width = d(0.035f)))
    // Glanzlicht
    drawOval(Color.White.copy(alpha = 0.35f), topLeft = o(-0.36f, -0.36f), size = Size(d(0.16f), d(0.1f)))
}
}

private fun DrawScope.drawSchelle(u: UnitSpace) { with(u) {
    val outline = Color(0xFF6B4300)
    val body = Path().apply { addOval(androidx.compose.ui.geometry.Rect(o(0f, 0.04f), d(0.4f))) }
    drawPath(
        body,
        Brush.radialGradient(
            listOf(Color(0xFFFFE680), Color(0xFFE6A817), Color(0xFF9A6200)),
            center = o(-0.14f, -0.12f),
            radius = d(0.6f),
        ),
    )
    clipPath(body) {
        drawRect(Color(0xFF8A5A00).copy(alpha = 0.75f), topLeft = o(-0.5f, -0.1f), size = Size(d(1f), d(0.12f)))
        for (i in -1..1) drawCircle(Color(0xFFFFF1B0), radius = d(0.03f), center = o(i * 0.18f, -0.04f))
    }
    drawPath(body, outline, style = Stroke(width = d(0.035f)))
    // Schlitz unten
    drawRoundRect(
        outline,
        topLeft = o(-0.2f, 0.18f),
        size = Size(d(0.4f), d(0.08f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(d(0.04f)),
    )
    drawLine(outline, o(0f, 0.25f), o(0f, 0.42f), strokeWidth = d(0.04f), cap = StrokeCap.Round)
    drawCircle(Color(0xFF3B2400), radius = d(0.04f), center = o(0f, 0.22f))
    // Öse oben
    drawCircle(Color(0xFFE6A817), radius = d(0.075f), center = o(0f, -0.4f))
    drawCircle(outline, radius = d(0.075f), center = o(0f, -0.4f), style = Stroke(width = d(0.03f)))
    drawOval(Color.White.copy(alpha = 0.4f), topLeft = o(-0.26f, -0.26f), size = Size(d(0.14f), d(0.09f)))
}
}

private fun DrawScope.drawEichel(u: UnitSpace) { with(u) {
    val dark = Color(0xFF3E2512)
    // Stiel
    drawLine(dark, o(0f, -0.36f), o(0.05f, -0.5f), strokeWidth = d(0.07f), cap = StrokeCap.Round)
    // Nuss
    val nutTopLeft = o(-0.24f, -0.14f)
    val nutSize = Size(d(0.48f), d(0.62f))
    drawOval(
        Brush.radialGradient(
            listOf(Color(0xFFF3DC7A), Color(0xFFC99A2E), Color(0xFF7C5810)),
            center = o(-0.08f, 0.08f),
            radius = d(0.5f),
        ),
        topLeft = nutTopLeft,
        size = nutSize,
    )
    drawOval(dark, topLeft = nutTopLeft, size = nutSize, style = Stroke(width = d(0.03f)))
    drawCircle(dark, radius = d(0.025f), center = o(0f, 0.47f))
    drawOval(Color.White.copy(alpha = 0.35f), topLeft = o(-0.15f, 0.08f), size = Size(d(0.08f), d(0.18f)))
    // Hütchen mit Gittermuster
    val cap = Path().apply {
        m(-0.34f, 0.02f)
        q(-0.36f, -0.38f, 0f, -0.38f)
        q(0.36f, -0.38f, 0.34f, 0.02f)
        q(0f, 0.1f, -0.34f, 0.02f)
        close()
    }
    drawPath(
        cap,
        Brush.verticalGradient(listOf(Color(0xFF9C6B3A), Color(0xFF5A3A1C)), startY = y(-0.38f), endY = y(0.06f)),
    )
    clipPath(cap) {
        var t = -0.8f
        while (t < 0.8f) {
            drawLine(dark.copy(alpha = 0.55f), o(t, -0.45f), o(t + 0.5f, 0.1f), strokeWidth = d(0.018f))
            drawLine(dark.copy(alpha = 0.55f), o(t, 0.1f), o(t + 0.5f, -0.45f), strokeWidth = d(0.018f))
            t += 0.1f
        }
    }
    drawPath(cap, dark, style = Stroke(width = d(0.035f)))
}
}

private fun DrawScope.drawLaub(u: UnitSpace) { with(u) {
    val dark = Color(0xFF123F16)
    // Stiel
    drawLine(dark, o(0f, 0.16f), o(-0.05f, 0.48f), strokeWidth = d(0.07f), cap = StrokeCap.Round)
    val leaf = Path().apply {
        m(0f, -0.47f)
        c(0.18f, -0.28f, 0.5f, -0.12f, 0.43f, 0.1f)
        c(0.38f, 0.3f, 0.14f, 0.3f, 0f, 0.22f)
        c(-0.14f, 0.3f, -0.38f, 0.3f, -0.43f, 0.1f)
        c(-0.5f, -0.12f, -0.18f, -0.28f, 0f, -0.47f)
        close()
    }
    drawPath(
        leaf,
        Brush.radialGradient(
            listOf(Color(0xFF8BD18E), SeppColors.Laub, Color(0xFF14501A)),
            center = o(-0.12f, -0.15f),
            radius = d(0.65f),
        ),
    )
    // Blattadern
    val vein = Color(0xFFDDF5C8).copy(alpha = 0.8f)
    drawLine(vein, o(0f, -0.36f), o(0f, 0.2f), strokeWidth = d(0.025f), cap = StrokeCap.Round)
    for (y in listOf(-0.18f, -0.02f, 0.12f)) {
        drawLine(vein, o(0f, y + 0.05f), o(0.22f, y - 0.06f), strokeWidth = d(0.018f), cap = StrokeCap.Round)
        drawLine(vein, o(0f, y + 0.05f), o(-0.22f, y - 0.06f), strokeWidth = d(0.018f), cap = StrokeCap.Round)
    }
    drawPath(leaf, dark, style = Stroke(width = d(0.035f)))
}
}
