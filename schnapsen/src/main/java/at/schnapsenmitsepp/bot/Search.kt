package at.schnapsenmitsepp.bot

import at.schnapsenmitsepp.engine.Action
import at.schnapsenmitsepp.engine.HandState
import at.schnapsenmitsepp.engine.Rank
import at.schnapsenmitsepp.engine.Rules
import at.schnapsenmitsepp.engine.Seat
import kotlin.random.Random

/**
 * Suche für die starken Stufen:
 * - Monte-Carlo über "mögliche Welten" (Determinisierung), weil Sepp die Karten des Gegners nicht kennt.
 * - In jeder Welt: heuristisches Ausspielen solange der Talon offen ist, danach exaktes Alpha-Beta.
 */
internal class Search(private val random: Random) {

    /** Bewertet jede Aktion an der Wurzel und gibt die beste zurück. */
    fun bestAction(
        state: HandState,
        me: Seat,
        candidates: List<Action>,
        minSamples: Int,
        maxSamples: Int,
        timeBudgetMs: Long,
        exactLimit: Int,
    ): Action {
        if (candidates.size == 1) return candidates.first()
        val totals = DoubleArray(candidates.size)
        val perfect = Knowledge.isPerfectInformation(state)
        val samples = if (perfect) 1 else maxSamples
        val deadline = System.currentTimeMillis() + timeBudgetMs
        var done = 0
        while (done < samples) {
            if (done >= minSamples && System.currentTimeMillis() > deadline) break
            val world = Knowledge.determinize(state, me, random)
            candidates.forEachIndexed { i, action ->
                totals[i] += evaluate(Rules.applyAndCollect(world, action), me, exactLimit)
            }
            done++
        }
        return candidates[totals.indices.maxBy { totals[it] }]
    }

    /** Wert eines Zustands aus Sicht von [me]: Spielpunkte (+ gewonnen / − verloren), plus kleiner Augen-Bonus. */
    fun evaluate(start: HandState, me: Seat, exactLimit: Int): Double {
        var s = start
        while (!s.isOver) {
            val actor = s.toAct!!
            // Exakt rechnen erst, wenn der Baum klein genug ist – sonst wird's am Handy zu langsam.
            if (s.strictRules && s.hand(actor).size <= exactLimit) return alphaBeta(s, me, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
            s = Rules.applyAndCollect(s, Heuristic.choose(s, actor))
        }
        return terminalValue(s, me)
    }

    private fun terminalValue(s: HandState, me: Seat): Double {
        val r = s.result!!
        val sign = if (r.winner == me) 1.0 else -1.0
        return sign * r.gamePoints + (s.points(me) - s.points(me.other)) / 1000.0
    }

    /** Exakte Suche bei voller Information (nach Zudrehen bzw. bei leerem Talon in der jeweiligen Welt). */
    fun alphaBeta(s: HandState, me: Seat, alphaIn: Double, betaIn: Double): Double {
        if (s.isOver) return terminalValue(s, me)
        val actor = s.toAct!!
        val moves = searchMoves(s, actor)
        var alpha = alphaIn
        var beta = betaIn
        if (actor == me) {
            var best = Double.NEGATIVE_INFINITY
            for (m in moves) {
                val v = alphaBeta(Rules.applyAndCollect(s, m), me, alpha, beta)
                if (v > best) best = v
                if (best > alpha) alpha = best
                if (alpha >= beta) break
            }
            return best
        } else {
            var best = Double.POSITIVE_INFINITY
            for (m in moves) {
                val v = alphaBeta(Rules.applyAndCollect(s, m), me, alpha, beta)
                if (v < best) best = v
                if (best < beta) beta = best
                if (alpha >= beta) break
            }
            return best
        }
    }

    /** Zugauswahl im Suchbaum: Ausmelden wenn möglich, Hochzeiten immer ansagen, Karten nach Wert sortiert. */
    private fun searchMoves(s: HandState, actor: Seat): List<Action> {
        if (Rules.claimIsValid(s, actor)) return listOf(Action.Claim)
        val moves = ArrayList<Action>(8)
        if (s.leadCard == null && s.announcedSuit == null) {
            val marriageSuits = Rules.announceableSuits(s, actor)
            marriageSuits.forEach { moves += Action.Announce(it) }
            Rules.legalCards(s, actor)
                .filter { c ->
                    // König/Ober einer Hochzeit spielt man nicht ohne Ansage.
                    !(c.suit in marriageSuits && (c.rank == Rank.KOENIG || c.rank == Rank.OBER))
                }
                .sortedByDescending { it.points }
                .forEach { moves += Action.Play(it) }
        } else {
            Rules.legalCards(s, actor).sortedByDescending { it.points }.forEach { moves += Action.Play(it) }
        }
        return moves
    }
}
