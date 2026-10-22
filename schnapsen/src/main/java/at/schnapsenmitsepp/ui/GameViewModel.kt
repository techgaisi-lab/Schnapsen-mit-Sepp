package at.schnapsenmitsepp.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.schnapsenmitsepp.bot.Difficulty
import at.schnapsenmitsepp.bot.SeppBot
import at.schnapsenmitsepp.data.ScoreStore
import at.schnapsenmitsepp.data.Settings
import at.schnapsenmitsepp.data.Stats
import at.schnapsenmitsepp.engine.Action
import at.schnapsenmitsepp.engine.Bummerl
import at.schnapsenmitsepp.engine.HandEndReason
import at.schnapsenmitsepp.engine.HandState
import at.schnapsenmitsepp.engine.Rank
import at.schnapsenmitsepp.engine.Rules
import at.schnapsenmitsepp.engine.Seat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Steuert ein Bummerl gegen Sepp: Züge, Pausen für Animationen, Sprüche und die Liste. */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ScoreStore(app)

    var settings by mutableStateOf(store.loadSettings())
        private set
    var stats by mutableStateOf(store.loadStats())
        private set
    var bummerl by mutableStateOf(store.loadBummerl())
        private set

    var hand by mutableStateOf<HandState?>(null)
        private set

    /** Wohin der fertige Stich gerade eingesammelt wird (für die Animation). */
    var collectingTo by mutableStateOf<Seat?>(null)
        private set
    var seppSays by mutableStateOf<String?>(null)
        private set
    var seppMood by mutableStateOf(SeppMood.NORMAL)
        private set
    var seppThinking by mutableStateOf(false)
        private set
    var hint by mutableStateOf<String?>(null)
        private set

    /** Bummerl, das gerade abgeschlossen wurde – wird am Ergebnisbildschirm gezeigt. */
    var finishedBummerl by mutableStateOf<Bummerl?>(null)
        private set

    private var bot = SeppBot(settings.difficulty)
    private var loop: Job? = null

    fun updateSettings(new: Settings) {
        settings = new
        store.saveSettings(new)
        if (bot.difficulty != new.difficulty) bot = SeppBot(new.difficulty)
    }

    fun setDifficulty(d: Difficulty) = updateSettings(settings.copy(difficulty = d))

    /** Startet ein Spiel, wenn gerade keines läuft (z.B. beim Öffnen des Spieltischs). */
    fun ensureHand() {
        if (hand == null) nextHand()
    }

    fun nextHand() {
        loop?.cancel()
        if (bummerl.isFinished) {
            bummerl = Bummerl(nextDealer = bummerl.nextDealer)
            store.saveBummerl(bummerl)
        }
        finishedBummerl = null
        collectingTo = null
        seppMood = SeppMood.NORMAL
        val dealt = HandState.deal(bummerl.nextDealer)
        hand = dealt
        seppSays = if (dealt.dealer == Seat.SEPP) "I gib. Du kommst raus!" else "Du gibst – i spiel aus."
        hint = null
        advance()
    }

    /** Bricht das laufende Bummerl ab und beginnt ein neues. */
    fun newBummerl() {
        loop?.cancel()
        bummerl = Bummerl(nextDealer = bummerl.nextDealer)
        store.saveBummerl(bummerl)
        hand = null
        nextHand()
    }

    fun onPlayer(action: Action) {
        val s = hand ?: return
        if (loop?.isActive == true || s.toAct != Seat.PLAYER) return
        if (!Rules.isLegal(s, Seat.PLAYER, action)) {
            if (action is Action.Play && s.leadCard != null && s.strictRules) {
                hint = "Farb- und Stichzwang! Du musst bedienen, wenn möglich höher – sonst stechen."
            }
            return
        }
        hint = null
        when (action) {
            is Action.Announce -> seppSays = if (Rules.marriageValue(s, action.suit) == 40) "Vierzg! Respekt." else "A Zwanzger, eh scho wissen."
            Action.Close -> seppSays = "Du drahst zua? Na schau ma moi."
            Action.ExchangeTrump -> seppSays = "Aha, den Atout-Buam tauschst."
            else -> seppSays = null
        }
        hand = Rules.apply(s, action)
        advance()
    }

    private fun advance() {
        loop = viewModelScope.launch {
            while (true) {
                val s = hand ?: return@launch
                when {
                    s.isOver -> {
                        onHandOver(s)
                        return@launch
                    }
                    s.trickComplete -> {
                        delay(850)
                        val winner = Rules.trickWinner(s.trumpSuit, s.leader, s.leadCard!!, s.followCard!!)
                        collectingTo = winner
                        delay(380)
                        hand = Rules.collectTrick(s)
                        collectingTo = null
                    }
                    s.toAct == Seat.SEPP -> seppTurn(s)
                    else -> return@launch
                }
            }
        }
    }

    private suspend fun seppTurn(s: HandState) {
        seppThinking = true
        seppMood = SeppMood.THINKING
        val started = System.currentTimeMillis()
        val action = withContext(Dispatchers.Default) { bot.chooseAction(s, Seat.SEPP) }
        val minThink = if (s.leadCard == null) 750L else 550L
        delay((minThink - (System.currentTimeMillis() - started)).coerceAtLeast(0))
        seppThinking = false
        seppMood = SeppMood.NORMAL
        when (action) {
            is Action.Announce -> {
                seppSays = if (Rules.marriageValue(s, action.suit) == 40) "Vierzg!" else "Zwanzg in ${settings.cardStyle.suitName(action.suit)}!"
                seppMood = SeppMood.HAPPY
            }
            Action.Close -> {
                seppSays = "I drah zua!"
                seppMood = SeppMood.HAPPY
            }
            Action.ExchangeTrump -> seppSays = "Den Atout-Buam tausch i ma."
            is Action.Play -> if (s.leadCard != null) {
                val winner = Rules.trickWinner(s.trumpSuit, s.leader, s.leadCard!!, action.card)
                if (winner == Seat.SEPP && s.leadCard!!.rank >= Rank.ZEHN) seppSays = "Den nimm i ma!"
            }
        }
        hand = Rules.apply(s, action)
        if (action is Action.Announce || action is Action.Close || action is Action.ExchangeTrump) delay(900)
    }

    private fun onHandOver(s: HandState) {
        val r = s.result ?: return
        val updated = bummerl.record(r)
        bummerl = updated
        store.saveBummerl(updated)
        var st = stats.copy(
            handsWon = stats.handsWon + if (r.winner == Seat.PLAYER) 1 else 0,
            handsLost = stats.handsLost + if (r.winner == Seat.SEPP) 1 else 0,
        )
        seppMood = if (r.winner == Seat.SEPP) SeppMood.HAPPY else SeppMood.GRUMPY
        seppSays = when {
            r.reason == HandEndReason.CLOSER_FAILED && r.winner == Seat.SEPP -> "Zuadraht und ned g'schafft, gell?"
            r.winner == Seat.SEPP && r.gamePoints == 3 -> "Schwarz! Des woar nix."
            r.winner == Seat.SEPP && r.reason == HandEndReason.REACHED_66 -> "Und aus is'! ${r.winnerPoints} hab i."
            r.winner == Seat.SEPP -> "Des Spü g'hört mir."
            r.gamePoints == 3 -> "Schwarz?! Heast, des gibt's ned..."
            else -> "Glück g'habt, gell?"
        }
        if (updated.isFinished) {
            val loser = updated.winner!!.other
            st = if (loser == Seat.PLAYER) st.copy(bummerlPlayer = st.bummerlPlayer + updated.bummerlCount)
            else st.copy(bummerlSepp = st.bummerlSepp + updated.bummerlCount)
            finishedBummerl = updated
            seppSays = if (loser == Seat.PLAYER) "A Bummerl für di! Hehe." else "Jessas, a Bummerl für mi..."
        }
        stats = st
        store.saveStats(st)
    }

    fun resetStats() {
        stats = Stats()
        store.saveStats(stats)
    }

    fun dismissHint() {
        hint = null
    }
}
