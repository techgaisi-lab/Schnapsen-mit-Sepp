package at.schnapsenmitsepp.engine

import kotlin.random.Random

/** Ein Zug eines Spielers. */
sealed interface Action {
    /** Eine Karte ausspielen bzw. zugeben. */
    data class Play(val card: Card) : Action

    /** 20 bzw. 40 ansagen (König + Ober einer Farbe). Danach muss König oder Ober dieser Farbe ausgespielt werden. */
    data class Announce(val suit: Suit) : Action

    /** Trumpf-Unter gegen die aufgedeckte Atoutkarte tauschen. */
    data object ExchangeTrump : Action

    /** Zudrehen: Der Stapel wird geschlossen, ab jetzt gilt Farb- und Stichzwang. */
    data object Close : Action
}

data class Trick(val leader: Seat, val lead: Card, val follow: Card, val winner: Seat)

data class Marriage(val seat: Seat, val suit: Suit, val points: Int)

enum class HandEndReason {
    /** Ein Spieler hat 66 Punkte erreicht. */
    REACHED_66,

    /** Niemand hat 66 erreicht, der letzte Stich entscheidet. */
    LAST_TRICK,

    /** Wer zugedreht hat, hat die 66 nicht erreicht. */
    CLOSER_FAILED,
}

data class HandResult(
    val winner: Seat,
    val gamePoints: Int,
    val reason: HandEndReason,
    val winnerPoints: Int,
    val loserPoints: Int,
)

/** Merkt sich beim Zudrehen, wie der Gegner des Zudrehers dastand – danach richtet sich die Wertung. */
data class CloseSnapshot(val closer: Seat, val opponentPoints: Int, val opponentTricks: Int)

/**
 * Zustand eines einzelnen Spiels (eine Hand, bis jemand 66 hat oder alle Karten gespielt sind).
 * Unveränderlich – jeder Zug liefert über [Rules.apply] einen neuen Zustand.
 */
data class HandState(
    val dealer: Seat,
    val trumpSuit: Suit,
    val hands: List<List<Card>>,
    /** Verdeckte Karten im Stapel, Index 0 wird als nächstes abgehoben. */
    val stock: List<Card>,
    /** Aufgedeckte Atoutkarte unter dem Stapel (wird als letzte Karte abgehoben). */
    val trumpCard: Card?,
    val closed: CloseSnapshot? = null,
    val leader: Seat,
    val leadCard: Card? = null,
    val followCard: Card? = null,
    /** Farbe der in diesem Zug angesagten Hochzeit (20/40). */
    val announcedSuit: Suit? = null,
    val cardPoints: List<Int> = listOf(0, 0),
    val marriagePoints: List<Int> = listOf(0, 0),
    val trickCounts: List<Int> = listOf(0, 0),
    val won: List<List<Card>> = listOf(emptyList(), emptyList()),
    val marriages: List<Marriage> = emptyList(),
    /** Karten in der Hand eines Spielers, die der Gegner kennt (gezeigte Hochzeit, getauschte Atoutkarte...). */
    val revealed: List<Set<Card>> = listOf(emptySet(), emptySet()),
    val lastTrick: Trick? = null,
    val result: HandResult? = null,
) {
    fun hand(seat: Seat): List<Card> = hands[seat.ordinal]
    fun tricks(seat: Seat): Int = trickCounts[seat.ordinal]
    fun wonCards(seat: Seat): List<Card> = won[seat.ordinal]

    /** Punkte: Stiche plus Ansagen (20/40 werden sofort gutgeschrieben). */
    fun points(seat: Seat): Int = cardPoints[seat.ordinal] + marriagePoints[seat.ordinal]

    val talonSize: Int get() = stock.size + if (trumpCard != null) 1 else 0
    val isClosed: Boolean get() = closed != null

    /** Farb- und Stichzwang gilt, sobald zugedreht wurde oder der Stapel aufgebraucht ist. */
    val strictRules: Boolean get() = isClosed || talonSize == 0

    val isOver: Boolean get() = result != null
    val trickComplete: Boolean get() = followCard != null

    /** Wer gerade am Zug ist; `null` wenn das Spiel vorbei ist oder ein fertiger Stich eingesammelt werden muss. */
    val toAct: Seat?
        get() = when {
            result != null -> null
            followCard != null -> null
            leadCard == null -> leader
            else -> leader.other
        }

    companion object {
        const val WINNING_POINTS = 66

        /**
         * Teilt aus: 3 Karten an jeden, Atout aufdecken, dann 2 an jeden. Die Atoutkarte liegt quer
         * unter dem Talon und zählt als unterste Talonkarte. Wer nicht gegeben hat, spielt aus.
         */
        fun deal(dealer: Seat, random: Random = Random.Default): HandState {
            val deck = Card.DECK.shuffled(random)
            val forehand = dealer.other
            val hands = arrayOf(mutableListOf<Card>(), mutableListOf())
            hands[forehand.ordinal] += deck.subList(0, 3)
            hands[dealer.ordinal] += deck.subList(3, 6)
            val trump = deck[6]
            hands[forehand.ordinal] += deck.subList(7, 9)
            hands[dealer.ordinal] += deck.subList(9, 11)
            return HandState(
                dealer = dealer,
                trumpSuit = trump.suit,
                hands = hands.map { it.toList() },
                stock = deck.subList(11, 20).toList(),
                trumpCard = trump,
                leader = forehand,
            )
        }
    }
}
