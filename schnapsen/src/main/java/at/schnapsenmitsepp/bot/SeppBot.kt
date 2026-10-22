package at.schnapsenmitsepp.bot

import at.schnapsenmitsepp.engine.Action
import at.schnapsenmitsepp.engine.HandState
import at.schnapsenmitsepp.engine.Rank
import at.schnapsenmitsepp.engine.Rules
import at.schnapsenmitsepp.engine.Seat
import kotlin.random.Random

enum class Difficulty(val title: String, val subtitle: String) {
    LEICHT("Leicht", "Sepp nach dem dritten Spritzer"),
    MITTEL("Mittel", "Sepp beim Kirtag"),
    SCHWER("Schwer", "Sepp im Stammwirtshaus"),
    MEISTER("Meister", "Sepp, der Kaffeehaus-Profi"),
}

/** Der Computergegner. Spielt immer fair – er sieht nur, was ein Mensch am Tisch auch sehen würde. */
class SeppBot(
    val difficulty: Difficulty,
    private val random: Random = Random.Default,
) {
    private val search = Search(random)

    fun chooseAction(state: HandState, me: Seat): Action {
        require(state.toAct == me)
        return when (difficulty) {
            Difficulty.LEICHT -> easy(state, me)
            Difficulty.MITTEL -> Heuristic.choose(state, me)
            Difficulty.SCHWER -> searching(state, me, minSamples = 8, maxSamples = 30, budget = 400, fullSearch = false)
            Difficulty.MEISTER -> searching(state, me, minSamples = 16, maxSamples = 120, budget = 1100, fullSearch = true)
        }
    }

    private fun easy(state: HandState, me: Seat): Action {
        val legal = Rules.legalActions(state, me)
        if (state.announcedSuit != null || state.leadCard == null) {
            legal.firstOrNull { it is Action.Announce }?.let { return it }
        }
        if (random.nextInt(100) < 55) {
            val plays = legal.filterIsInstance<Action.Play>()
            return plays[random.nextInt(plays.size)]
        }
        return Heuristic.choose(state, me, allowClose = false)
    }

    private fun searching(
        state: HandState,
        me: Seat,
        minSamples: Int,
        maxSamples: Int,
        budget: Long,
        fullSearch: Boolean,
    ): Action {
        // Hochzeit: welche der beiden Karten spielt man aus?
        if (state.announcedSuit != null) {
            val options = Rules.legalActions(state, me).filterIsInstance<Action.Play>()
            return search.bestAction(state, me, options, minSamples, maxSamples, budget, exactLimit = 5)
        }
        if (Rules.canExchangeTrump(state, me)) return Action.ExchangeTrump

        if (state.strictRules) {
            return search.bestAction(state, me, candidates(state, me), minSamples, maxSamples, budget, exactLimit = 5)
        }
        // Talon offen: Schwer überlegt nur, ob Zudrehen sich auszahlt; Meister rechnet jeden Zug durch.
        if (!fullSearch) {
            val heuristic = Heuristic.choose(state, me, allowClose = false)
            if (Rules.canClose(state, me) && heuristic is Action.Play) {
                return search.bestAction(
                    state, me, listOf(heuristic, Action.Close), minSamples, maxSamples, budget, exactLimit = 3,
                )
            }
            return heuristic
        }
        return search.bestAction(state, me, candidates(state, me), minSamples, maxSamples, budget, exactLimit = 3)
    }

    private fun candidates(state: HandState, me: Seat): List<Action> {
        val legal = Rules.legalActions(state, me)
        val marriageSuits = legal.filterIsInstance<Action.Announce>().map { it.suit }.toSet()
        // Eine Hochzeit wird immer angesagt, König/Ober also nicht "stumm" ausspielen.
        return legal.filter { a ->
            !(a is Action.Play && state.leadCard == null && a.card.suit in marriageSuits &&
                (a.card.rank == Rank.KOENIG || a.card.rank == Rank.OBER))
        }
    }
}
