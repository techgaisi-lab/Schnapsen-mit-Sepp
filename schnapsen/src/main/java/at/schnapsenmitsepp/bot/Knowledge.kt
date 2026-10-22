package at.schnapsenmitsepp.bot

import at.schnapsenmitsepp.engine.Card
import at.schnapsenmitsepp.engine.HandState
import at.schnapsenmitsepp.engine.Seat
import kotlin.random.Random

/**
 * Was ein Spieler ehrlich wissen kann: eigene Hand, gespielte Karten, die Atoutkarte und Karten,
 * die der Gegner gezeigt hat. Der Bot schaut nie in die Hand des Gegners oder in den Talon.
 */
internal object Knowledge {

    /** Karten, deren Ort [me] nicht kennt (gegnerische Hand oder verdeckter Talon). */
    fun unseen(state: HandState, me: Seat): List<Card> {
        val known = HashSet<Card>(20)
        known += state.hand(me)
        known += state.wonCards(Seat.PLAYER)
        known += state.wonCards(Seat.SEPP)
        state.leadCard?.let { known += it }
        state.followCard?.let { known += it }
        state.trumpCard?.let { known += it }
        known += state.revealed[me.other.ordinal]
        return Card.DECK.filter { it !in known }
    }

    /** Alle Karten, die der Gegner von [me] in der Hand haben könnte. */
    fun possibleOpponentCards(state: HandState, me: Seat): List<Card> =
        unseen(state, me) + state.revealed[me.other.ordinal].filter { it in state.hand(me.other) }

    /** Ist der Rest des Spiels für [me] vollständig bekannt? (Talon leer → Gegnerhand = ungesehene Karten.) */
    fun isPerfectInformation(state: HandState): Boolean = state.stock.isEmpty()

    /**
     * Erzeugt eine mögliche "Welt": Die ungesehenen Karten werden zufällig auf gegnerische Hand und
     * Talon verteilt. Gezeigte Karten bleiben beim Gegner.
     */
    fun determinize(state: HandState, me: Seat, random: Random): HandState {
        val opp = me.other
        val known = state.revealed[opp.ordinal].filter { it in state.hand(opp) }
        val pool = unseen(state, me).shuffled(random)
        val need = state.hand(opp).size - known.size
        val oppHand = known + pool.take(need)
        val stock = pool.drop(need)
        check(stock.size == state.stock.size) { "Inkonsistente Determinisierung" }
        return state.copy(hands = state.hands.toMutableList().also { it[opp.ordinal] = oppHand }, stock = stock)
    }
}
