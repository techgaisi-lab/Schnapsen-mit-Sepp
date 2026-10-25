package at.schnapsenmitsepp.bot

import at.schnapsenmitsepp.engine.Action
import at.schnapsenmitsepp.engine.Card
import at.schnapsenmitsepp.engine.HandState
import at.schnapsenmitsepp.engine.Rank
import at.schnapsenmitsepp.engine.Rules
import at.schnapsenmitsepp.engine.Seat

/**
 * Regelbasierte Spielweise, wie sie ein ordentlicher Wirtshausspieler hat. Wird direkt für "Mittel"
 * verwendet und von den stärkeren Stufen als Ausspielpolitik in Simulationen.
 */
internal object Heuristic {

    fun choose(state: HandState, me: Seat, allowClose: Boolean = true): Action {
        val lead = state.leadCard
        return if (lead == null) chooseAsLeader(state, me, allowClose) else Action.Play(chooseFollow(state, me, lead))
    }

    private fun chooseAsLeader(state: HandState, me: Seat, allowClose: Boolean): Action {
        state.announcedSuit?.let { suit ->
            val ober = Card(suit, Rank.OBER)
            return Action.Play(if (ober in state.hand(me)) ober else Card(suit, Rank.KOENIG))
        }

        if (Rules.canExchangeTrump(state, me)) return Action.ExchangeTrump

        val marriages = Rules.announceableSuits(state, me)
        if (marriages.isNotEmpty()) {
            return Action.Announce(marriages.maxBy { Rules.marriageValue(state, it) })
        }

        if (allowClose && Rules.canClose(state, me) && shouldClose(state, me)) return Action.Close

        return Action.Play(chooseLead(state, me))
    }

    /** Grobe Abschätzung: Reichen eigene Augen plus sichere Stiche für 66? */
    fun shouldClose(state: HandState, me: Seat): Boolean {
        val hand = state.hand(me)
        val possible = Knowledge.possibleOpponentCards(state, me)
        val trumps = hand.filter { it.suit == state.trumpSuit }
        var projected = state.cardPoints[me.ordinal] + state.marriagePoints[me.ordinal]
        var sureTricks = 0
        for (card in hand) {
            if (!isMaster(card, possible)) continue
            if (card.suit == state.trumpSuit) {
                projected += card.points + 5
                sureTricks++
            } else if (trumps.size >= 2) {
                projected += (card.points + 5) * 2 / 3
                sureTricks++
            }
        }
        if (state.tricks(me) == 0 && sureTricks == 0) return false
        return projected >= HandState.WINNING_POINTS + 2
    }

    /** Kann der Gegner diese Karte in ihrer Farbe überhaupt noch übertrumpfen? */
    fun isMaster(card: Card, possibleOpponent: Collection<Card>): Boolean =
        possibleOpponent.none { it.suit == card.suit && it.rank > card.rank }

    private fun marriagePiece(state: HandState, me: Seat, card: Card): Boolean {
        if (card.rank != Rank.KOENIG && card.rank != Rank.OBER) return false
        val partner = Card(card.suit, if (card.rank == Rank.KOENIG) Rank.OBER else Rank.KOENIG)
        if (partner in state.hand(me)) return true
        // Partner könnte noch aus dem Talon kommen.
        return !state.strictRules && partner in Knowledge.unseen(state, me)
    }

    private fun chooseLead(state: HandState, me: Seat): Card {
        val hand = state.hand(me)
        val trump = state.trumpSuit
        val possible = Knowledge.possibleOpponentCards(state, me)
        if (state.strictRules) {
            val oppHasTrump = possible.any { it.suit == trump }
            // Zuerst sichere Trümpfe, um die Trümpfe des Gegners zu ziehen, dann sichere Farben.
            hand.filter { it.suit == trump && isMaster(it, possible) }.maxByOrNull { it.points }?.let { return it }
            if (!oppHasTrump) {
                hand.filter { isMaster(it, possible) }.maxByOrNull { it.points }?.let { return it }
            }
            hand.filter { it.suit != trump && isMaster(it, possible) && possible.any { p -> p.suit == it.suit } }
                .maxByOrNull { it.points }?.let { return it }
            return hand.minBy { leadCost(state, me, it) }
        }
        return hand.minBy { leadCost(state, me, it) }
    }

    private fun leadCost(state: HandState, me: Seat, card: Card): Int {
        var cost = card.points
        if (card.suit == state.trumpSuit) cost += 12 + card.points
        if (marriagePiece(state, me, card)) cost += 9
        return cost
    }

    private fun chooseFollow(state: HandState, me: Seat, lead: Card): Card {
        val legal = Rules.legalCards(state, me)
        val trump = state.trumpSuit
        val winners = legal.filter { Rules.trickWinner(trump, me.other, lead, it) == me }
        val losers = legal - winners.toSet()
        val myPoints = state.cardPoints[me.ordinal] + state.marriagePoints[me.ordinal]

        // Wenn dieser Stich die 66 bringt: nimm ihn mit der billigsten Karte.
        winners.filter { myPoints + lead.points + it.points >= HandState.WINNING_POINTS }
            .minByOrNull { keepValue(state, me, it) }?.let { return it }

        if (state.strictRules) {
            winners.minByOrNull { keepValue(state, me, it) }?.let { return it }
            return losers.minBy { keepValue(state, me, it) }
        }

        // Offener Talon: mit höherer Karte gleicher Farbe übernehmen, wenn es sich lohnt.
        val sameSuitWinners = winners.filter { it.suit == lead.suit && lead.suit != trump }
        sameSuitWinners.filter { !marriagePiece(state, me, it) }.minByOrNull { it.points }?.let { return it }

        // Ass oder Zehner abstechen.
        if (lead.points >= 10 && lead.suit != trump) {
            winners.filter { it.suit == trump }.minByOrNull { keepValue(state, me, it) }?.let { return it }
        }

        val discard = losers.ifEmpty { legal }.minBy { keepValue(state, me, it) }
        // Lieber billig stechen als ein Ass oder einen Zehner herschenken.
        if (discard.points >= 10) {
            winners.minByOrNull { keepValue(state, me, it) }?.let { return it }
        }
        return discard
    }

    /** Wie ungern man eine Karte hergibt. */
    private fun keepValue(state: HandState, me: Seat, card: Card): Int {
        var value = card.points
        if (card.suit == state.trumpSuit) value += 8 + card.points / 2
        if (marriagePiece(state, me, card)) value += 10
        return value
    }
}
