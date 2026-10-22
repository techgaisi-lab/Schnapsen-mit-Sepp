package at.schnapsenmitsepp.data

import android.content.Context
import androidx.core.content.edit
import at.schnapsenmitsepp.bot.Difficulty
import at.schnapsenmitsepp.engine.Bummerl
import at.schnapsenmitsepp.engine.HandEndReason
import at.schnapsenmitsepp.engine.ListEntry
import at.schnapsenmitsepp.engine.Seat

data class Settings(
    val difficulty: Difficulty = Difficulty.MITTEL,
    /** Bei 66 automatisch "Aus!" sagen. */
    val autoClaim: Boolean = true,
    /** Augen mitzählen und anzeigen. */
    val showPoints: Boolean = true,
)

data class Stats(
    /** Bummerl, die der Spieler bekommen (= verloren) hat. */
    val bummerlPlayer: Int = 0,
    /** Bummerl, die Sepp bekommen hat. */
    val bummerlSepp: Int = 0,
    val handsWon: Int = 0,
    val handsLost: Int = 0,
)

/** Speichert Liste, Bummerl-Stand und Einstellungen in den SharedPreferences. */
class ScoreStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sms_schnapsen_mit_sepp", Context.MODE_PRIVATE)

    fun loadSettings() = Settings(
        difficulty = runCatching { Difficulty.valueOf(prefs.getString("difficulty", null)!!) }.getOrDefault(Difficulty.MITTEL),
        autoClaim = prefs.getBoolean("autoClaim", true),
        showPoints = prefs.getBoolean("showPoints", true),
    )

    fun saveSettings(s: Settings) = prefs.edit {
        putString("difficulty", s.difficulty.name)
        putBoolean("autoClaim", s.autoClaim)
        putBoolean("showPoints", s.showPoints)
    }

    fun loadStats() = Stats(
        bummerlPlayer = prefs.getInt("bummerlPlayer", 0),
        bummerlSepp = prefs.getInt("bummerlSepp", 0),
        handsWon = prefs.getInt("handsWon", 0),
        handsLost = prefs.getInt("handsLost", 0),
    )

    fun saveStats(s: Stats) = prefs.edit {
        putInt("bummerlPlayer", s.bummerlPlayer)
        putInt("bummerlSepp", s.bummerlSepp)
        putInt("handsWon", s.handsWon)
        putInt("handsLost", s.handsLost)
    }

    fun loadBummerl(): Bummerl {
        val entries = prefs.getString("entries", "").orEmpty()
            .split(';')
            .filter { it.isNotBlank() }
            .mapNotNull { e ->
                val parts = e.split(':')
                runCatching { ListEntry(Seat.valueOf(parts[0]), parts[1].toInt(), HandEndReason.valueOf(parts[2])) }.getOrNull()
            }
        val dealer = runCatching { Seat.valueOf(prefs.getString("nextDealer", null)!!) }.getOrDefault(Seat.SEPP)
        return Bummerl(entries, dealer)
    }

    fun saveBummerl(b: Bummerl) = prefs.edit {
        putString("entries", b.entries.joinToString(";") { "${it.winner.name}:${it.gamePoints}:${it.reason.name}" })
        putString("nextDealer", b.nextDealer.name)
    }
}
