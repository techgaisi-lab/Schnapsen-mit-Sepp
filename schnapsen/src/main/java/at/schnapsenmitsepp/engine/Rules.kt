package at.schnapsenmitsepp.engine

/**
 * Regeln für das österreichische Zweier-Schnapsen (20 Karten, 66 Augen).
 *
 * - Solange der Talon offen ist, besteht weder Farb- noch Stichzwang.
 * - Nach dem Zudrehen oder wenn der Talon aufgebraucht ist: Farbe bekennen, wenn möglich überstechen,
 *   sonst mit Trumpf stechen.
 * - 20 (Hochzeit) bzw. 40 (Trumpf-Hochzeit) darf nur ansagen, wer ausspielt. Die Punkte zählen erst
 *   mit dem ersten eigenen Stich.
 * - Trumpf-Unter tauschen und Zudrehen nur, wenn man ausspielt und noch mindestens 4 Karten im Talon sind.
 */
object Rules {

    private const val MIN_TALON_FOR_CLOSE_OR_EXCHANGE = 4

    fun marriageValue(state: HandState, suit: Suit): Int = if (suit == state.trumpSuit) 40 else 20

    fun trumpUnter(state: HandState): Card = Card(state.trumpSuit, Rank.UNTER)

    fun canExchangeTrump(state: HandState, seat: Seat): Boolean =
        isLeadingFresh(state, seat) &&
            !state.isClosed &&
            state.trumpCard != null &&
            state.talonSize >= MIN_TALON_FOR_CLOSE_OR_EXCHANGE &&
            trumpUnter(state) in state.hand(seat)

    fun canClose(state: HandState, seat: Seat): Boolean =
        isLeadingFresh(state, seat) &&
            !state.isClosed &&
            state.trumpCard != null &&
            state.talonSize >= MIN_TALON_FOR_CLOSE_OR_EXCHANGE

    /** Farben, in denen [seat] gerade 20/40 ansagen kann. */
    fun announceableSuits(state: HandState, seat: Seat): List<Suit> {
        if (!isLeadingFresh(state, seat)) return emptyList()
        val hand = state.hand(seat)
        return Suit.entries.filter { Card(it, Rank.KOENIG) in hand && Card(it, Rank.OBER) in hand }
    }

    fun canClaim(state: HandState, seat: Seat): Boolean =
        state.toAct == seat && state.leadCard == null

    /** Darf [seat] gerade mit "Aus!" richtig ausmelden (mind. 66 gezählte Augen)? */
    fun claimIsValid(state: HandState, seat: Seat): Boolean =
        canClaim(state, seat) && state.points(seat) >= HandState.WINNING_POINTS

    private fun isLeadingFresh(state: HandState, seat: Seat): Boolean =
        state.toAct == seat && state.leadCard == null && state.announcedSuit == null

    /** Alle Karten, die [seat] jetzt spielen darf. */
    fun legalCards(state: HandState, seat: Seat): List<Card> {
        if (state.toAct != seat) return emptyList()
        val hand = state.hand(seat)
        val lead = state.leadCard
        if (lead == null) {
            val announced = state.announcedSuit ?: return hand
            return hand.filter { it.suit == announced && (it.rank == Rank.KOENIG || it.rank == Rank.OBER) }
        }
        if (!state.strictRules) return hand
        val sameSuit = hand.filter { it.suit == lead.suit }
        if (sameSuit.isNotEmpty()) {
            val higher = sameSuit.filter { it.rank > lead.rank }
            return higher.ifEmpty { sameSuit }
        }
        if (lead.suit != state.trumpSuit) {
            val trumps = hand.filter { it.suit == state.trumpSuit }
            if (trumps.isNotEmpty()) return trumps
        }
        return hand
    }

    /** Alle erlaubten Aktionen. Ein falsches "Aus!" ist zwar erlaubt, wird hier aber nicht angeboten. */
    fun legalActions(state: HandState, seat: Seat): List<Action> {
        if (state.toAct != seat) return emptyList()
        val actions = mutableListOf<Action>()
        if (claimIsValid(state, seat)) actions += Action.Claim
        if (canExchangeTrump(state, seat)) actions += Action.ExchangeTrump
        if (canClose(state, seat)) actions += Action.Close
        announceableSuits(state, seat).forEach { actions += Action.Announce(it) }
        legalCards(state, seat).forEach { actions += Action.Play(it) }
        return actions
    }

    fun trickWinner(trumpSuit: Suit, leader: Seat, lead: Card, follow: Card): Seat = when {
        follow.suit == lead.suit -> if (follow.rank > lead.rank) leader.other else leader
        follow.suit == trumpSuit -> leader.other
        else -> leader
    }

    fun isLegal(state: HandState, seat: Seat, action: Action): Boolean = when (action) {
        is Action.Play -> action.card in legalCards(state, seat)
        is Action.Announce -> action.suit in announceableSuits(state, seat)
        Action.ExchangeTrump -> canExchangeTrump(state, seat)
        Action.Close -> canClose(state, seat)
        Action.Claim -> canClaim(state, seat)
    }

    /**
     * Führt [action] für den Spieler am Zug aus. Wird der zweite Karte eines Stichs gespielt, bleibt
     * der Stich liegen ([HandState.trickComplete]) bis [collectTrick] aufgerufen wird – so kann die
     * Oberfläche den Stich kurz zeigen.
     */
    fun apply(state: HandState, action: Action): HandState {
        val seat = requireNotNull(state.toAct) { "Niemand ist am Zug" }
        require(isLegal(state, seat, action)) { "Unerlaubter Zug $action für $seat" }
        return when (action) {
            is Action.Play -> play(state, seat, action.card)
            is Action.Announce -> announce(state, seat, action.suit)
            Action.ExchangeTrump -> exchange(state, seat)
            Action.Close -> state.copy(
                closed = CloseSnapshot(seat, state.points(seat.other), state.tricks(seat.other)),
            )
            Action.Claim -> claim(state, seat)
        }
    }

    /** [apply] und – falls dadurch ein Stich fertig wurde – gleich einsammeln. Praktisch für die KI. */
    fun applyAndCollect(state: HandState, action: Action): HandState {
        val next = apply(state, action)
        return if (next.trickComplete) collectTrick(next) else next
    }

    private fun play(state: HandState, seat: Seat, card: Card): HandState {
        val hands = state.hands.replace(seat, state.hand(seat) - card)
        val revealed = state.revealed.replace(seat, state.revealed[seat.ordinal] - card)
        return if (state.leadCard == null) {
            state.copy(hands = hands, revealed = revealed, leadCard = card, announcedSuit = null)
        } else {
            state.copy(hands = hands, revealed = revealed, followCard = card)
        }
    }

    private fun announce(state: HandState, seat: Seat, suit: Suit): HandState {
        val value = marriageValue(state, suit)
        val i = seat.ordinal
        return state.copy(
            announcedSuit = suit,
            marriagePoints = state.marriagePoints.replaceAt(i, state.marriagePoints[i] + value),
            marriages = state.marriages + Marriage(seat, suit, value),
            revealed = state.revealed.replace(
                seat,
                state.revealed[i] + Card(suit, Rank.KOENIG) + Card(suit, Rank.OBER),
            ),
        )
    }

    private fun exchange(state: HandState, seat: Seat): HandState {
        val unter = trumpUnter(state)
        val oldTrump = state.trumpCard!!
        return state.copy(
            hands = state.hands.replace(seat, state.hand(seat) - unter + oldTrump),
            trumpCard = unter,
            revealed = state.revealed.replace(seat, state.revealed[seat.ordinal] + oldTrump),
        )
    }

    /** Wertet den fertigen Stich aus, lässt nachziehen und beendet ggf. das Spiel. */
    fun collectTrick(state: HandState): HandState {
        val lead = requireNotNull(state.leadCard)
        val follow = requireNotNull(state.followCard)
        val winner = trickWinner(state.trumpSuit, state.leader, lead, follow)
        val w = winner.ordinal
        var hands = state.hands
        var stock = state.stock
        var trumpCard = state.trumpCard
        var revealed = state.revealed
        if (!state.isClosed) {
            // Gewinner zieht zuerst, dann der Verlierer.
            for (drawer in listOf(winner, winner.other)) {
                val card = when {
                    stock.isNotEmpty() -> stock.first().also { stock = stock.drop(1) }
                    trumpCard != null -> trumpCard.also {
                        trumpCard = null
                        revealed = revealed.replace(drawer, revealed[drawer.ordinal] + it)
                    }
                    else -> null
                }
                if (card != null) hands = hands.replace(drawer, hands[drawer.ordinal] + card)
            }
        }
        val next = state.copy(
            hands = hands,
            stock = stock,
            trumpCard = trumpCard,
            revealed = revealed,
            leader = winner,
            leadCard = null,
            followCard = null,
            announcedSuit = null,
            cardPoints = state.cardPoints.replaceAt(w, state.cardPoints[w] + lead.points + follow.points),
            trickCounts = state.trickCounts.replaceAt(w, state.trickCounts[w] + 1),
            won = state.won.replace(winner, state.won[w] + lead + follow),
            lastTrick = Trick(state.leader, lead, follow, winner),
        )
        return if (next.hands.all { it.isEmpty() }) finishWithoutClaim(next, winner) else next
    }

    private fun finishWithoutClaim(state: HandState, lastTrickWinner: Seat): HandState {
        val closed = state.closed
        if (closed != null) {
            // Wer zugedreht hat und nicht ausgemeldet hat, verliert.
            val winner = closed.closer.other
            return state.copy(
                result = HandResult(
                    winner = winner,
                    gamePoints = maxOf(closerPenalty(closed), normalGamePoints(state, closed.closer)),
                    reason = HandEndReason.CLOSER_FAILED,
                    winnerPoints = state.points(winner),
                    loserPoints = state.points(closed.closer),
                ),
            )
        }
        val loser = lastTrickWinner.other
        return state.copy(
            result = HandResult(
                winner = lastTrickWinner,
                gamePoints = normalGamePoints(state, loser),
                reason = HandEndReason.LAST_TRICK,
                winnerPoints = state.points(lastTrickWinner),
                loserPoints = state.points(loser),
            ),
        )
    }

    private fun claim(state: HandState, seat: Seat): HandState {
        val other = seat.other
        val closed = state.closed
        if (state.points(seat) < HandState.WINNING_POINTS) {
            val points = if (state.tricks(other) == 0) 3 else 2
            return state.copy(
                result = HandResult(other, points, HandEndReason.WRONG_CLAIM, state.points(other), state.points(seat)),
            )
        }
        val gamePoints = when {
            closed == null -> normalGamePoints(state, other)
            // Zudreher gewinnt: es zählt, wie der Gegner beim Zudrehen dastand.
            closed.closer == seat -> gamePointsFor(closed.opponentPoints, closed.opponentTricks)
            // Gegner des Zudrehers erreicht die 66 zuerst.
            else -> maxOf(closerPenalty(closed), normalGamePoints(state, other))
        }
        return state.copy(
            result = HandResult(seat, gamePoints, HandEndReason.CLAIMED, state.points(seat), state.points(other)),
        )
    }

    private fun closerPenalty(closed: CloseSnapshot): Int = if (closed.opponentTricks == 0) 3 else 2

    private fun normalGamePoints(state: HandState, loser: Seat): Int =
        gamePointsFor(state.points(loser), state.tricks(loser))

    /** 1 Punkt; 2 wenn der Verlierer "Schneider" ist (unter 33); 3 wenn er "schwarz" ist (kein Stich). */
    fun gamePointsFor(loserPoints: Int, loserTricks: Int): Int = when {
        loserTricks == 0 -> 3
        loserPoints < 33 -> 2
        else -> 1
    }
}

internal fun <T> List<T>.replace(seat: Seat, value: T): List<T> = replaceAt(seat.ordinal, value)

internal fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
    toMutableList().also { it[index] = value }
