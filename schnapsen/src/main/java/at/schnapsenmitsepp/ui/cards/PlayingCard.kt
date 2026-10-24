package at.schnapsenmitsepp.ui.cards

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import at.schnapsenmitsepp.R
import at.schnapsenmitsepp.engine.Card
import at.schnapsenmitsepp.engine.Rank
import at.schnapsenmitsepp.engine.Suit
import at.schnapsenmitsepp.ui.theme.SeppColors

/** Seitenverhältnis Höhe/Breite einer Karte. */
const val CARD_RATIO = 1.55f

/**
 * Eine Spielkarte aus dem eigenen französischen Blatt (Bilder in res/drawable-nodpi, 600 x 930 px).
 * Die Bilder haben abgerundete, durchsichtige Ecken mit 8 % Radius – passend zum Schatten.
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
    val resources = LocalContext.current.resources
    val showFace = faceUp && card != null
    val resId = if (showFace) cardImageRes(card!!) else R.drawable.sms_rueckseite
    val bitmap = remember(resId) { CardBitmaps.get(resources, resId) }
    val description = if (showFace) {
        "${CardStyle.FRANZOESISCH.suitName(card!!.suit)} ${CardStyle.FRANZOESISCH.rankName(card.rank)}"
    } else {
        "Verdeckte Karte"
    }

    Box(modifier.shadow(elevation.dp, RoundedCornerShape(8), clip = true)) {
        Image(
            bitmap = bitmap,
            contentDescription = description,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.High,
        )
        if (highlighted || dimmed) {
            Canvas(Modifier.fillMaxSize()) {
                val r = CornerRadius(size.width * 0.08f)
                if (highlighted) {
                    drawRoundRect(SeppColors.Gold, cornerRadius = r, style = Stroke(width = size.width * 0.045f))
                }
                if (dimmed) drawRoundRect(Color.Black.copy(alpha = 0.38f), cornerRadius = r)
            }
        }
    }
}

/** Ordnet jeder Karte ihr Bild zu. Engine-Farben: Herz = Herz, Schelle = Karo, Eichel = Kreuz, Laub = Pik. */
fun cardImageRes(card: Card): Int = when (card.suit) {
    Suit.HERZ -> when (card.rank) {
        Rank.ASS -> R.drawable.sms_herz_ass
        Rank.ZEHN -> R.drawable.sms_herz_zehner
        Rank.KOENIG -> R.drawable.sms_herz_koenig
        Rank.OBER -> R.drawable.sms_herz_dame
        Rank.UNTER -> R.drawable.sms_herz_bube
    }
    Suit.SCHELLE -> when (card.rank) {
        Rank.ASS -> R.drawable.sms_karo_ass
        Rank.ZEHN -> R.drawable.sms_karo_zehner
        Rank.KOENIG -> R.drawable.sms_karo_koenig
        Rank.OBER -> R.drawable.sms_karo_dame
        Rank.UNTER -> R.drawable.sms_karo_bube
    }
    Suit.EICHEL -> when (card.rank) {
        Rank.ASS -> R.drawable.sms_kreuz_ass
        Rank.ZEHN -> R.drawable.sms_kreuz_zehner
        Rank.KOENIG -> R.drawable.sms_kreuz_koenig
        Rank.OBER -> R.drawable.sms_kreuz_dame
        Rank.UNTER -> R.drawable.sms_kreuz_bube
    }
    Suit.LAUB -> when (card.rank) {
        Rank.ASS -> R.drawable.sms_pik_ass
        Rank.ZEHN -> R.drawable.sms_pik_zehner
        Rank.KOENIG -> R.drawable.sms_pik_koenig
        Rank.OBER -> R.drawable.sms_pik_dame
        Rank.UNTER -> R.drawable.sms_pik_bube
    }
}

/**
 * Jedes Kartenbild wird nur einmal geladen und dann für alle Stellen im Spiel wiederverwendet
 * (Hand, Tisch, Talon, Menü) – spart Speicher und verhindert Ruckeln.
 */
private object CardBitmaps {
    private val cache = HashMap<Int, ImageBitmap>()

    fun get(resources: Resources, resId: Int): ImageBitmap =
        cache.getOrPut(resId) { BitmapFactory.decodeResource(resources, resId).asImageBitmap() }
}
