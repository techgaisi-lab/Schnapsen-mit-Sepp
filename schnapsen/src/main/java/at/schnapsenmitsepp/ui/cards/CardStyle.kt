package at.schnapsenmitsepp.ui.cards

import androidx.compose.runtime.staticCompositionLocalOf
import at.schnapsenmitsepp.engine.Rank
import at.schnapsenmitsepp.engine.Suit

/**
 * Wie die Karten aussehen und heißen. Die Engine kennt nur Farbe und Rang; Herz = Herz,
 * Schell = Karo, Eichel = Kreuz, Laub = Pik bzw. Ober = Dame, Unter = Bube.
 */
enum class CardStyle(val title: String) {
    FRANZOESISCH("Französisch"),
    DOPPELDEUTSCH("Doppeldeutsch");

    fun suitName(suit: Suit): String = when (this) {
        FRANZOESISCH -> when (suit) {
            Suit.HERZ -> "Herz"
            Suit.SCHELLE -> "Karo"
            Suit.EICHEL -> "Kreuz"
            Suit.LAUB -> "Pik"
        }
        DOPPELDEUTSCH -> suit.displayName
    }

    fun rankSymbol(rank: Rank): String = when (this) {
        FRANZOESISCH -> when (rank) {
            Rank.OBER -> "D"
            Rank.UNTER -> "B"
            else -> rank.symbol
        }
        DOPPELDEUTSCH -> rank.symbol
    }

    fun rankName(rank: Rank): String = when (this) {
        FRANZOESISCH -> when (rank) {
            Rank.OBER -> "Dame"
            Rank.UNTER -> "Bube"
            else -> rank.displayName
        }
        DOPPELDEUTSCH -> rank.displayName
    }
}

val LocalCardStyle = staticCompositionLocalOf { CardStyle.FRANZOESISCH }
