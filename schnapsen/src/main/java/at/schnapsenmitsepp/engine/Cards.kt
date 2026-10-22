package at.schnapsenmitsepp.engine

/** Die vier Farben der doppeldeutschen Karten. */
enum class Suit(val displayName: String) {
    HERZ("Herz"),
    SCHELLE("Schell"),
    EICHEL("Eichel"),
    LAUB("Laub"),
}

/** Die fünf Kartenwerte beim Schnapsen, aufsteigend nach Stärke. [points] sind die Augen. */
enum class Rank(val points: Int, val symbol: String, val displayName: String) {
    UNTER(2, "U", "Unter"),
    OBER(3, "O", "Ober"),
    KOENIG(4, "K", "König"),
    ZEHN(10, "10", "Zehner"),
    ASS(11, "A", "Ass"),
}

data class Card(val suit: Suit, val rank: Rank) {
    val points: Int get() = rank.points
    override fun toString(): String = "${suit.displayName}-${rank.symbol}"

    companion object {
        /** Alle 20 Karten des Schnapsen-Blatts. */
        val DECK: List<Card> = Suit.entries.flatMap { s -> Rank.entries.map { r -> Card(s, r) } }
    }
}

enum class Seat {
    PLAYER, SEPP;

    val other: Seat get() = if (this == PLAYER) SEPP else PLAYER
}
