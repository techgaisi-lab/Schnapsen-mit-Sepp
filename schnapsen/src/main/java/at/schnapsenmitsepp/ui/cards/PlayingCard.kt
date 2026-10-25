package at.schnapsenmitsepp.ui.cards

import android.content.res.Resources
import android.graphics.Bitmap
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

private val CardShape = RoundedCornerShape(8)

/**
 * Eine Spielkarte aus dem eigenen französischen Blatt (Bilder in res/drawable-nodpi, 600 x 930 px).
 * Die Bilder haben abgerundete, durchsichtige Ecken mit 8 % Radius – passend zum Schatten.
 *
 * Sparsam für alte Tablets: Bei [elevation] 0 wird kein Schatten und keine eigene Grafik-Ebene
 * angelegt; das spart viel Rechenzeit bei Stapeln, Stichen und Sepps verdeckten Karten.
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

    val boxModifier = if (elevation > 0f) modifier.shadow(elevation.dp, CardShape) else modifier
    Box(boxModifier) {
        Image(
            bitmap = bitmap,
            contentDescription = description,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.Low,
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
 * Jedes Kartenbild wird nur einmal geladen und dann überall wiederverwendet (Hand, Tisch, Stapel,
 * Menü). Auf Geräten, auf denen die Karten kleiner als ~340 Pixel breit angezeigt werden, wird nur
 * jedes zweite Pixel geladen (300 x 465 statt 600 x 930) – das braucht nur ein Viertel des Speichers
 * (alle 21 Karten zusammen ca. 12 MB statt 47 MB) und ist schneller zu zeichnen.
 */
object CardBitmaps {
    private val cache = HashMap<Int, ImageBitmap>()

    private val ALL: List<Int> by lazy {
        Card.DECK.map { cardImageRes(it) } + R.drawable.sms_rueckseite
    }

    fun get(resources: Resources, resId: Int): ImageBitmap = synchronized(cache) {
        cache.getOrPut(resId) { decode(resources, resId) }
    }

    /** Lädt alle Karten vorab – im Hintergrund aufrufen, damit beim ersten Austeilen nichts hakt. */
    fun preload(resources: Resources) {
        for (id in ALL) get(resources, id)
    }

    private fun decode(resources: Resources, resId: Int): ImageBitmap {
        val dm = resources.displayMetrics
        // Größte Karte am Bildschirm: ca. 40 % der kürzeren Seite hoch → Breite = Höhe / 1,55
        val largestCardPx = minOf(dm.widthPixels, dm.heightPixels) * 0.40f / CARD_RATIO
        val options = BitmapFactory.Options().apply {
            inSampleSize = if (largestCardPx <= 340f) 2 else 1
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = BitmapFactory.decodeResource(resources, resId, options)
        bmp.prepareToDraw() // schon jetzt an die Grafikkarte übergeben
        return bmp.asImageBitmap()
    }
}
