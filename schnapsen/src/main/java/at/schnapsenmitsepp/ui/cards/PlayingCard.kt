package at.schnapsenmitsepp.ui.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import at.schnapsenmitsepp.engine.Card
import at.schnapsenmitsepp.engine.Rank
import at.schnapsenmitsepp.ui.theme.SeppColors

/** Seitenverhältnis Höhe/Breite einer Karte. */
const val CARD_RATIO = 1.55f

/**
 * Eine gezeichnete doppeldeutsche Karte. Ganz ohne Bilddateien – alles Vektor, darum in jeder
 * Größe scharf.
 */
@Composable
fun PlayingCard(
    card: Card?,
    modifier: Modifier = Modifier,
    faceUp: Boolean = true,
    highlighted: Boolean = false,
    dimmed: Boolean = false,
    elevation: Float = 4f,
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier.shadow(elevation.dp, RoundedCornerShape(8))) {
        if (faceUp && card != null) drawCardFace(card, textMeasurer) else drawCardBack()
        val r = CornerRadius(size.width * 0.08f)
        if (highlighted) {
            drawRoundRect(SeppColors.Gold, cornerRadius = r, style = Stroke(width = size.width * 0.045f))
        }
        if (dimmed) drawRoundRect(Color.Black.copy(alpha = 0.38f), cornerRadius = r)
    }
}

fun DrawScope.drawCardFace(card: Card, textMeasurer: TextMeasurer) {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(w * 0.08f)
    val color = suitColor(card.suit)

    drawRoundRect(
        Brush.verticalGradient(listOf(SeppColors.Cream, SeppColors.CreamDark)),
        cornerRadius = corner,
    )
    drawRoundRect(SeppColors.Ink.copy(alpha = 0.45f), cornerRadius = corner, style = Stroke(width = w * 0.012f))
    val inset = w * 0.045f
    drawRoundRect(
        color.copy(alpha = 0.45f),
        topLeft = Offset(inset, inset),
        size = Size(w - 2 * inset, h - 2 * inset),
        cornerRadius = CornerRadius(w * 0.05f),
        style = Stroke(width = w * 0.01f),
    )

    drawCornerIndex(card, textMeasurer)
    rotate(180f) { drawCornerIndex(card, textMeasurer) }

    when (card.rank) {
        Rank.ASS -> drawAce(card)
        Rank.ZEHN -> drawTen(card)
        else -> drawFigure(card)
    }
}

private fun DrawScope.drawCornerIndex(card: Card, textMeasurer: TextMeasurer) {
    val w = size.width
    val style = TextStyle(
        color = SeppColors.Ink,
        fontSize = (w * 0.19f).toSp(),
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Serif,
    )
    val layout = textMeasurer.measure(card.rank.symbol, style)
    val left = w * 0.075f
    val top = w * 0.04f
    drawText(layout, topLeft = Offset(left, top))
    val cx = left + layout.size.width / 2f
    drawSuit(card.suit, Offset(cx, top + layout.size.height + w * 0.06f), w * 0.15f)
}

private fun DrawScope.drawAce(card: Card) {
    val w = size.width
    val h = size.height
    val c = Offset(w / 2, h / 2)
    val color = suitColor(card.suit)
    // Ornament-Oval hinter dem großen Symbol
    drawOval(
        Brush.radialGradient(listOf(color.copy(alpha = 0.18f), Color.Transparent), center = c, radius = w * 0.45f),
        topLeft = Offset(w * 0.17f, h * 0.24f),
        size = Size(w * 0.66f, h * 0.52f),
    )
    drawOval(
        SeppColors.GoldDark,
        topLeft = Offset(w * 0.19f, h * 0.26f),
        size = Size(w * 0.62f, h * 0.48f),
        style = Stroke(width = w * 0.012f),
    )
    // kleine Ranken oben und unten
    for (sign in listOf(-1f, 1f)) {
        val y = c.y + sign * h * 0.3f
        drawLine(SeppColors.GoldDark, Offset(w * 0.36f, y), Offset(w * 0.64f, y), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
        drawCircle(SeppColors.GoldDark, radius = w * 0.022f, center = Offset(w * 0.5f, y))
        drawCircle(SeppColors.GoldDark, radius = w * 0.013f, center = Offset(w * 0.32f, y))
        drawCircle(SeppColors.GoldDark, radius = w * 0.013f, center = Offset(w * 0.68f, y))
    }
    drawSuit(card.suit, c, w * 0.56f)
}

private fun DrawScope.drawTen(card: Card) {
    val w = size.width
    val h = size.height
    val pip = w * 0.19f
    val top = h * 0.17f
    val span = h * 0.66f
    val positions = buildList {
        for (row in 0..3) {
            val y = top + span * row / 3f
            add(Offset(w * 0.33f, y))
            add(Offset(w * 0.67f, y))
        }
        add(Offset(w * 0.5f, top + span / 6f))
        add(Offset(w * 0.5f, top + span * 5f / 6f))
    }
    for (p in positions) {
        if (p.y > h / 2) rotate(180f, pivot = p) { drawSuit(card.suit, p, pip) } else drawSuit(card.suit, p, pip)
    }
}

/** Doppelköpfige Figur wie auf den echten Karten: obere Hälfte zeichnen, dann um 180° gedreht wiederholen. */
private fun DrawScope.drawFigure(card: Card) {
    val w = size.width
    val h = size.height
    val color = suitColor(card.suit)
    val left = w * 0.22f
    val right = w * 0.78f
    val top = h * 0.085f
    val bottom = h * 0.915f
    val panelW = right - left
    val panelCorner = CornerRadius(w * 0.04f)

    drawRoundRect(
        lerp(SeppColors.Cream, color, 0.13f),
        topLeft = Offset(left, top),
        size = Size(panelW, bottom - top),
        cornerRadius = panelCorner,
    )
    val half = (bottom - top) / 2f
    clipRect(left, top, right, top + half) { drawFigureHalf(card, left, top, panelW, half) }
    rotate(180f) {
        clipRect(left, top, right, top + half) { drawFigureHalf(card, left, top, panelW, half) }
    }
    drawLine(SeppColors.GoldDark, Offset(left, h / 2), Offset(right, h / 2), strokeWidth = w * 0.012f)
    drawRoundRect(
        SeppColors.GoldDark,
        topLeft = Offset(left, top),
        size = Size(panelW, bottom - top),
        cornerRadius = panelCorner,
        style = Stroke(width = w * 0.014f),
    )
}

private val Skin = Color(0xFFF2C6A0)
private val SkinShade = Color(0xFFD9A07A)

private fun DrawScope.drawFigureHalf(card: Card, left: Float, top: Float, pw: Float, half: Float) {
    val color = suitColor(card.suit)
    val dark = lerp(color, Color.Black, 0.35f)
    val cx = left + pw / 2
    val headY = top + half * 0.42f
    val headR = pw * 0.15f

    // Gewand
    val robe = Path().apply {
        moveTo(cx - pw * 0.2f, headY + headR * 1.05f)
        lineTo(cx + pw * 0.2f, headY + headR * 1.05f)
        lineTo(cx + pw * 0.46f, top + half + 2f)
        lineTo(cx - pw * 0.46f, top + half + 2f)
        close()
    }
    drawPath(robe, Brush.verticalGradient(listOf(color, dark), startY = headY, endY = top + half))
    drawPath(robe, dark, style = Stroke(width = pw * 0.02f))
    // Goldborte und Knöpfe
    drawLine(SeppColors.Gold, Offset(cx, headY + headR * 1.1f), Offset(cx, top + half), strokeWidth = pw * 0.045f)
    for (i in 1..3) drawCircle(SeppColors.Cream, radius = pw * 0.018f, center = Offset(cx, headY + headR * 1.1f + i * half * 0.11f))
    // Kragen
    drawOval(Color.White, topLeft = Offset(cx - pw * 0.2f, headY + headR * 0.8f), size = Size(pw * 0.4f, headR * 0.6f))
    drawOval(SeppColors.Ink.copy(alpha = 0.3f), topLeft = Offset(cx - pw * 0.2f, headY + headR * 0.8f), size = Size(pw * 0.4f, headR * 0.6f), style = Stroke(pw * 0.012f))

    // Kopf
    drawCircle(Skin, radius = headR, center = Offset(cx, headY))
    drawCircle(SkinShade, radius = headR, center = Offset(cx, headY), style = Stroke(pw * 0.015f))
    drawCircle(SeppColors.Ink, radius = pw * 0.018f, center = Offset(cx - headR * 0.38f, headY - headR * 0.1f))
    drawCircle(SeppColors.Ink, radius = pw * 0.018f, center = Offset(cx + headR * 0.38f, headY - headR * 0.1f))
    drawCircle(Color(0xFFE88B7A).copy(alpha = 0.6f), radius = pw * 0.03f, center = Offset(cx - headR * 0.55f, headY + headR * 0.3f))
    drawCircle(Color(0xFFE88B7A).copy(alpha = 0.6f), radius = pw * 0.03f, center = Offset(cx + headR * 0.55f, headY + headR * 0.3f))

    when (card.rank) {
        Rank.KOENIG -> {
            // Weißer Vollbart
            val beard = Path().apply {
                moveTo(cx - headR * 0.85f, headY + headR * 0.2f)
                quadraticTo(cx, headY + headR * 2.1f, cx + headR * 0.85f, headY + headR * 0.2f)
                quadraticTo(cx, headY + headR * 0.75f, cx - headR * 0.85f, headY + headR * 0.2f)
            }
            drawPath(beard, Color(0xFFF4F1EA))
            drawPath(beard, Color(0xFFB8B2A6), style = Stroke(pw * 0.012f))
            // Krone
            val crown = Path().apply {
                val base = headY - headR * 0.75f
                val tip = headY - headR * 1.9f
                moveTo(cx - headR * 0.95f, base)
                lineTo(cx - headR * 1.05f, tip)
                lineTo(cx - headR * 0.5f, base - headR * 0.55f)
                lineTo(cx, tip - headR * 0.15f)
                lineTo(cx + headR * 0.5f, base - headR * 0.55f)
                lineTo(cx + headR * 1.05f, tip)
                lineTo(cx + headR * 0.95f, base)
                close()
            }
            drawPath(crown, Brush.verticalGradient(listOf(Color(0xFFFFE9A0), SeppColors.Gold, SeppColors.GoldDark)))
            drawPath(crown, Color(0xFF7A5A1C), style = Stroke(pw * 0.015f))
            drawCircle(SeppColors.Herz, radius = pw * 0.025f, center = Offset(cx, headY - headR * 1.05f))
            drawCircle(Color(0xFF1565C0), radius = pw * 0.02f, center = Offset(cx - headR * 0.55f, headY - headR * 0.95f))
            drawCircle(Color(0xFF1565C0), radius = pw * 0.02f, center = Offset(cx + headR * 0.55f, headY - headR * 0.95f))
        }
        Rank.OBER -> {
            // Schnurrbart
            drawMustache(cx, headY + headR * 0.35f, headR, Color(0xFF5A3A1E))
            // Hut mit Feder
            drawOval(dark, topLeft = Offset(cx - headR * 1.45f, headY - headR * 0.95f), size = Size(headR * 2.9f, headR * 0.5f))
            drawRoundRect(
                dark,
                topLeft = Offset(cx - headR * 0.8f, headY - headR * 1.75f),
                size = Size(headR * 1.6f, headR * 0.95f),
                cornerRadius = CornerRadius(headR * 0.5f),
            )
            drawLine(SeppColors.Gold, Offset(cx - headR * 0.8f, headY - headR * 0.95f), Offset(cx + headR * 0.8f, headY - headR * 0.95f), strokeWidth = pw * 0.03f)
            val feather = Path().apply {
                moveTo(cx + headR * 0.6f, headY - headR * 1.2f)
                quadraticTo(cx + headR * 2.2f, headY - headR * 2.6f, cx + headR * 1.6f, headY - headR * 0.9f)
                quadraticTo(cx + headR * 1.6f, headY - headR * 1.8f, cx + headR * 0.6f, headY - headR * 1.2f)
            }
            drawPath(feather, Color.White)
            drawPath(feather, Color(0xFF9E9E9E), style = Stroke(pw * 0.01f))
        }
        else -> {
            // Unter: junger Bursch mit Kappe und Bommel
            drawArc(
                dark,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - headR * 1.05f, headY - headR * 1.25f),
                size = Size(headR * 2.1f, headR * 1.7f),
            )
            drawLine(SeppColors.Gold, Offset(cx - headR * 1.05f, headY - headR * 0.4f), Offset(cx + headR * 1.05f, headY - headR * 0.4f), strokeWidth = pw * 0.03f)
            drawCircle(SeppColors.Cream, radius = headR * 0.28f, center = Offset(cx, headY - headR * 1.3f))
            drawArc(
                SeppColors.Ink,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - headR * 0.35f, headY + headR * 0.1f),
                size = Size(headR * 0.7f, headR * 0.45f),
                style = Stroke(pw * 0.015f, cap = StrokeCap.Round),
            )
        }
    }

    // Farbsymbol, das die Figur in der Hand hält
    val emblemCenter = Offset(left + pw * 0.8f, top + half * 0.74f)
    drawCircle(SeppColors.Cream, radius = pw * 0.13f, center = emblemCenter)
    drawCircle(SeppColors.GoldDark, radius = pw * 0.13f, center = emblemCenter, style = Stroke(pw * 0.012f))
    drawSuit(card.suit, emblemCenter, pw * 0.2f)
}

private fun DrawScope.drawMustache(cx: Float, y: Float, headR: Float, color: Color) {
    val m = Path().apply {
        moveTo(cx, y - headR * 0.05f)
        quadraticTo(cx - headR * 0.5f, y - headR * 0.3f, cx - headR * 0.95f, y + headR * 0.05f)
        quadraticTo(cx - headR * 0.5f, y + headR * 0.2f, cx, y + headR * 0.12f)
        quadraticTo(cx + headR * 0.5f, y + headR * 0.2f, cx + headR * 0.95f, y + headR * 0.05f)
        quadraticTo(cx + headR * 0.5f, y - headR * 0.3f, cx, y - headR * 0.05f)
        close()
    }
    drawPath(m, color)
}

/** Kartenrücken: Bordeaux mit Goldgitter und einem Edelweiß in der Mitte. */
fun DrawScope.drawCardBack() {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(w * 0.08f)
    drawRoundRect(
        Brush.linearGradient(listOf(SeppColors.Burgundy, SeppColors.BurgundyDark), Offset.Zero, Offset(w, h)),
        cornerRadius = corner,
    )
    drawRoundRect(SeppColors.Cream, cornerRadius = corner, style = Stroke(width = w * 0.05f))
    val inset = w * 0.09f
    clipRect(inset, inset, w - inset, h - inset) {
        val step = w * 0.11f
        var t = -h
        while (t < w + h) {
            drawLine(SeppColors.Gold.copy(alpha = 0.32f), Offset(t, 0f), Offset(t + h, h), strokeWidth = w * 0.012f)
            drawLine(SeppColors.Gold.copy(alpha = 0.32f), Offset(t, h), Offset(t + h, 0f), strokeWidth = w * 0.012f)
            t += step
        }
    }
    drawRoundRect(
        SeppColors.Gold,
        topLeft = Offset(inset, inset),
        size = Size(w - 2 * inset, h - 2 * inset),
        cornerRadius = CornerRadius(w * 0.04f),
        style = Stroke(width = w * 0.015f),
    )
    val c = Offset(w / 2, h / 2)
    drawOval(SeppColors.BurgundyDark, topLeft = Offset(c.x - w * 0.27f, c.y - w * 0.33f), size = Size(w * 0.54f, w * 0.66f))
    drawOval(SeppColors.Gold, topLeft = Offset(c.x - w * 0.27f, c.y - w * 0.33f), size = Size(w * 0.54f, w * 0.66f), style = Stroke(w * 0.015f))
    drawEdelweiss(c, w * 0.42f)
}

fun DrawScope.drawEdelweiss(center: Offset, size: Float) {
    val petals = 9
    for (i in 0 until petals) {
        rotate(i * 360f / petals, pivot = center) {
            val p = Path().apply {
                moveTo(center.x, center.y - size * 0.08f)
                quadraticTo(center.x - size * 0.11f, center.y - size * 0.3f, center.x, center.y - size * 0.48f)
                quadraticTo(center.x + size * 0.11f, center.y - size * 0.3f, center.x, center.y - size * 0.08f)
            }
            drawPath(p, Color(0xFFF7F4EC))
            drawPath(p, Color(0xFFBDB6A6), style = Stroke(size * 0.015f))
        }
    }
    for ((dx, dy) in listOf(0f to 0f, -0.07f to -0.05f, 0.07f to -0.05f, -0.06f to 0.06f, 0.06f to 0.06f, 0f to -0.09f, 0f to 0.09f)) {
        drawCircle(Color(0xFFE8D98A), radius = size * 0.05f, center = Offset(center.x + dx * size, center.y + dy * size))
        drawCircle(Color(0xFFA8964A), radius = size * 0.05f, center = Offset(center.x + dx * size, center.y + dy * size), style = Stroke(size * 0.01f))
    }
}
