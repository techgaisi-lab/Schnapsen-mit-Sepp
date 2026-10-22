package at.schnapsenmitsepp.engine

/** Ein Eintrag auf der Liste: wer hat ein Spiel mit wie vielen Punkten gewonnen. */
data class ListEntry(val winner: Seat, val gamePoints: Int, val reason: HandEndReason)

/**
 * Ein Bummerl wird auf der Liste "heruntergeschrieben": Jeder beginnt bei 7, gewonnene Spielpunkte
 * werden abgezogen. Wer zuerst auf 0 ist, gewinnt – der andere bekommt ein Bummerl.
 * Hat der Verlierer noch gar nichts abgeschrieben (steht noch auf 7), ist er "Schneider" und
 * bekommt zwei Bummerl.
 */
data class Bummerl(
    val entries: List<ListEntry> = emptyList(),
    /** Wer im nächsten Spiel gibt. */
    val nextDealer: Seat = Seat.SEPP,
) {
    fun remaining(seat: Seat): Int =
        (START - entries.filter { it.winner == seat }.sumOf { it.gamePoints }).coerceAtLeast(0)

    /** Zwischenstände nach jedem Spiel, wie sie auf der Liste stehen. */
    fun runningTotals(): List<Pair<Int, Int>> {
        var player = START
        var sepp = START
        return entries.map {
            if (it.winner == Seat.PLAYER) player = (player - it.gamePoints).coerceAtLeast(0)
            else sepp = (sepp - it.gamePoints).coerceAtLeast(0)
            player to sepp
        }
    }

    val winner: Seat? get() = Seat.entries.firstOrNull { remaining(it) == 0 }
    val isFinished: Boolean get() = winner != null

    /** Wie viele Bummerl der Verlierer bekommt (2 bei Schneider). */
    val bummerlCount: Int
        get() {
            val w = winner ?: return 0
            return if (remaining(w.other) == START) 2 else 1
        }

    fun record(result: HandResult): Bummerl = copy(
        entries = entries + ListEntry(result.winner, result.gamePoints, result.reason),
        nextDealer = nextDealer.other,
    )

    companion object {
        const val START = 7
    }
}
