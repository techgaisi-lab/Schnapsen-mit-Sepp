package at.schnapsenmitsepp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import at.schnapsenmitsepp.ui.theme.SeppColors
import kotlin.random.Random

enum class SeppMood { NORMAL, HAPPY, GRUMPY, THINKING }

/** Sepp: Tiroler Hut mit Gamsbart, mächtiger Schnauzer, rote Backen. */
@Composable
fun SeppAvatar(mood: SeppMood, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        fun o(x: Float, y: Float) = Offset(c.x + x * s, c.y + y * s)

        drawCircle(
            Brush.radialGradient(listOf(SeppColors.Gold, SeppColors.GoldDark), center = o(-0.1f, -0.1f), radius = s * 0.6f),
            radius = s * 0.5f,
            center = c,
        )
        drawCircle(SeppColors.Cream, radius = s * 0.47f, center = c)
        drawCircle(Color(0xFFDDEBD5), radius = s * 0.45f, center = c)

        // Trachtenjanker mit grünem Kragen
        val jacket = Path().apply {
            moveTo(c.x - s * 0.36f, c.y + s * 0.45f)
            quadraticTo(c.x - s * 0.3f, c.y + s * 0.2f, c.x, c.y + s * 0.2f)
            quadraticTo(c.x + s * 0.3f, c.y + s * 0.2f, c.x + s * 0.36f, c.y + s * 0.45f)
            close()
        }
        drawPath(jacket, Color(0xFF6D6D5F))
        drawLine(SeppColors.Laub, o(-0.12f, 0.22f), o(0f, 0.4f), strokeWidth = s * 0.05f, cap = StrokeCap.Round)
        drawLine(SeppColors.Laub, o(0.12f, 0.22f), o(0f, 0.4f), strokeWidth = s * 0.05f, cap = StrokeCap.Round)

        // Gesicht
        val faceColor = Color(0xFFF0BE96)
        drawOval(faceColor, topLeft = o(-0.22f, -0.2f), size = Size(s * 0.44f, s * 0.48f))
        drawCircle(faceColor, radius = s * 0.05f, center = o(-0.22f, 0.04f))
        drawCircle(faceColor, radius = s * 0.05f, center = o(0.22f, 0.04f))
        drawCircle(Color(0xFFE5776A).copy(alpha = 0.55f), radius = s * 0.055f, center = o(-0.13f, 0.1f))
        drawCircle(Color(0xFFE5776A).copy(alpha = 0.55f), radius = s * 0.055f, center = o(0.13f, 0.1f))
        // Nase
        drawOval(Color(0xFFE09A7A), topLeft = o(-0.045f, -0.01f), size = Size(s * 0.09f, s * 0.11f))

        // Augen
        val eyeY = -0.04f
        if (mood == SeppMood.HAPPY) {
            for (x in listOf(-0.09f, 0.09f)) {
                drawArc(SeppColors.Ink, 200f, 140f, false, topLeft = o(x - 0.04f, eyeY - 0.02f), size = Size(s * 0.08f, s * 0.06f), style = Stroke(s * 0.018f, cap = StrokeCap.Round))
            }
        } else {
            for (x in listOf(-0.09f, 0.09f)) {
                drawCircle(Color.White, radius = s * 0.032f, center = o(x, eyeY))
                val lookUp = if (mood == SeppMood.THINKING) -0.012f else 0f
                drawCircle(SeppColors.Ink, radius = s * 0.017f, center = o(x + if (mood == SeppMood.THINKING) 0.01f else 0f, eyeY + lookUp))
            }
        }
        // Augenbrauen
        val browTilt = when (mood) {
            SeppMood.GRUMPY -> 0.03f
            SeppMood.THINKING -> -0.02f
            else -> 0f
        }
        drawLine(Color(0xFF8C8C8C), o(-0.14f, -0.1f - browTilt), o(-0.04f, -0.1f + browTilt), strokeWidth = s * 0.025f, cap = StrokeCap.Round)
        drawLine(Color(0xFF8C8C8C), o(0.04f, -0.1f + browTilt), o(0.14f, -0.1f - browTilt), strokeWidth = s * 0.025f, cap = StrokeCap.Round)

        // Mund
        when (mood) {
            SeppMood.HAPPY -> drawArc(Color(0xFF7A2E22), 0f, 180f, true, topLeft = o(-0.07f, 0.13f), size = Size(s * 0.14f, s * 0.1f))
            SeppMood.GRUMPY -> drawArc(Color(0xFF7A2E22), 200f, 140f, false, topLeft = o(-0.06f, 0.17f), size = Size(s * 0.12f, s * 0.06f), style = Stroke(s * 0.02f, cap = StrokeCap.Round))
            else -> drawLine(Color(0xFF7A2E22), o(-0.04f, 0.18f), o(0.04f, 0.18f), strokeWidth = s * 0.02f, cap = StrokeCap.Round)
        }
        // Mächtiger Schnauzer
        val mustache = Path().apply {
            moveTo(c.x, c.y + s * 0.09f)
            quadraticTo(c.x - s * 0.12f, c.y + s * 0.05f, c.x - s * 0.22f, c.y + s * 0.13f)
            quadraticTo(c.x - s * 0.25f, c.y + s * 0.06f, c.x - s * 0.28f, c.y + s * 0.07f)
            quadraticTo(c.x - s * 0.2f, c.y + s * 0.2f, c.x, c.y + s * 0.14f)
            quadraticTo(c.x + s * 0.2f, c.y + s * 0.2f, c.x + s * 0.28f, c.y + s * 0.07f)
            quadraticTo(c.x + s * 0.25f, c.y + s * 0.06f, c.x + s * 0.22f, c.y + s * 0.13f)
            quadraticTo(c.x + s * 0.12f, c.y + s * 0.05f, c.x, c.y + s * 0.09f)
            close()
        }
        drawPath(mustache, Color(0xFF9A9A9A))
        drawPath(mustache, Color(0xFF6E6E6E), style = Stroke(s * 0.008f))

        // Tiroler Hut
        drawOval(Color(0xFF2F4A2A), topLeft = o(-0.33f, -0.24f), size = Size(s * 0.66f, s * 0.1f))
        val crown = Path().apply {
            moveTo(c.x - s * 0.2f, c.y - s * 0.2f)
            quadraticTo(c.x - s * 0.2f, c.y - s * 0.42f, c.x, c.y - s * 0.4f)
            quadraticTo(c.x + s * 0.2f, c.y - s * 0.42f, c.x + s * 0.2f, c.y - s * 0.2f)
            close()
        }
        drawPath(crown, Brush.verticalGradient(listOf(Color(0xFF4F7A45), Color(0xFF2F4A2A)), startY = c.y - s * 0.42f, endY = c.y - s * 0.2f))
        drawLine(Color(0xFF7A2E22), o(-0.2f, -0.23f), o(0.2f, -0.23f), strokeWidth = s * 0.035f)
        // Gamsbart
        for (i in 0..6) {
            val a = -0.05f + i * 0.02f
            drawLine(Color(0xFF3B3B3B), o(0.17f, -0.25f), o(0.22f + a, -0.48f + kotlin.math.abs(a) * 0.5f), strokeWidth = s * 0.012f, cap = StrokeCap.Round)
        }
        drawCircle(SeppColors.Gold, radius = s * 0.025f, center = o(0.17f, -0.25f))
    }
}

/** Grüner Filz mit leichter Struktur und dunklem Rand, wie am Kartentisch im Wirtshaus. */
/**
 * Grüner Filz mit leichter Struktur. Wird nur EINMAL (pro Bildschirmgröße) in ein Bild mit halber
 * Auflösung gezeichnet und danach nur noch als Bild ausgegeben – vorher wurden bei jeder Animation
 * 900 Punkte neu gezeichnet, das hat auf alten Tablets stark geruckelt.
 */
fun Modifier.feltBackground(): Modifier = this
    .background(SeppColors.FeltDark)
    .drawWithCache {
        val w = (size.width / 2f).toInt().coerceAtLeast(1)
        val h = (size.height / 2f).toInt().coerceAtLeast(1)
        val felt = ImageBitmap(w, h)
        CanvasDrawScope().draw(this, layoutDirection, androidx.compose.ui.graphics.Canvas(felt), Size(w.toFloat(), h.toFloat())) {
            drawRect(
                Brush.radialGradient(
                    listOf(SeppColors.FeltLight, SeppColors.Felt, SeppColors.FeltDark),
                    center = Offset(this.size.width / 2, this.size.height * 0.45f),
                    radius = maxOf(this.size.width, this.size.height) * 0.75f,
                ),
            )
            val random = Random(7)
            repeat(900) {
                val p = Offset(random.nextFloat() * this.size.width, random.nextFloat() * this.size.height)
                drawCircle(
                    if (random.nextBoolean()) Color.White.copy(alpha = 0.035f) else Color.Black.copy(alpha = 0.05f),
                    radius = (1.2f + random.nextFloat() * 1.5f) / 2f,
                    center = p,
                )
            }
        }
        onDrawBehind {
            drawImage(
                felt,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.Low,
            )
        }
    }

@Composable
fun FeltTable(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.feltBackground()) { content() }
}
