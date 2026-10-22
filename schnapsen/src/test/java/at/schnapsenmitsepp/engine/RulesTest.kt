package at.schnapsenmitsepp.engine

import at.schnapsenmitsepp.bot.Difficulty
import at.schnapsenmitsepp.bot.SeppBot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RulesTest {

    private fun c(suit: Suit, rank: Rank) = Card(suit, rank)

    /** Ein fixer Spielstand: Herz ist Trumpf, Spieler spielt aus. */
    private fun fixture(
        player: List<Card>,
        sepp: List<Card>,
        stock: List<Card> = emptyList(),
        trumpCard: Card? = null,
        leader: Seat = Seat.PLAYER,
    ) = HandState(
        dealer = leader.other,
        trumpSuit = Suit.HERZ,
        hands = listOf(player, sepp),
        stock = stock,
        trumpCard = trumpCard,
        leader = leader,
    )

    @Test
    fun dealGivesFiveCardsAndTenInTalon() {
        val s = HandState.deal(Seat.SEPP, Random(1))
        assertEquals(5, s.hand(Seat.PLAYER).size)
        assertEquals(5, s.hand(Seat.SEPP).size)
        assertEquals(10, s.talonSize)
        assertEquals(Seat.PLAYER, s.toAct)
        assertEquals(s.trumpCard!!.suit, s.trumpSuit)
        val all = s.hands.flatten() + s.stock + s.trumpCard!!
        assertEquals(20, all.toSet().size)
    }

    @Test
    fun noFollowSuitWhileTalonOpen() {
        val rest = Card.DECK - setOf(
            c(Suit.LAUB, Rank.ASS), c(Suit.LAUB, Rank.UNTER), c(Suit.EICHEL, Rank.UNTER),
        )
        var s = fixture(
            player = listOf(c(Suit.LAUB, Rank.ASS)),
            sepp = listOf(c(Suit.LAUB, Rank.UNTER), c(Suit.EICHEL, Rank.UNTER)),
            stock = rest.drop(1), trumpCard = rest.first(),
        )
        s = Rules.apply(s, Action.Play(c(Suit.LAUB, Rank.ASS)))
        assertEquals(2, Rules.legalCards(s, Seat.SEPP).size)
    }

    @Test
    fun strictRulesForceHeadingAndTrumping() {
        var s = fixture(
            player = listOf(c(Suit.LAUB, Rank.KOENIG)),
            sepp = listOf(c(Suit.LAUB, Rank.UNTER), c(Suit.LAUB, Rank.ASS), c(Suit.HERZ, Rank.UNTER)),
        )
        s = Rules.apply(s, Action.Play(c(Suit.LAUB, Rank.KOENIG)))
        assertEquals(listOf(c(Suit.LAUB, Rank.ASS)), Rules.legalCards(s, Seat.SEPP))

        var t = fixture(
            player = listOf(c(Suit.LAUB, Rank.KOENIG)),
            sepp = listOf(c(Suit.EICHEL, Rank.ASS), c(Suit.HERZ, Rank.UNTER)),
        )
        t = Rules.apply(t, Action.Play(c(Suit.LAUB, Rank.KOENIG)))
        assertEquals(listOf(c(Suit.HERZ, Rank.UNTER)), Rules.legalCards(t, Seat.SEPP))
    }

    @Test
    fun trumpBeatsHighCard() {
        assertEquals(Seat.SEPP, Rules.trickWinner(Suit.HERZ, Seat.PLAYER, c(Suit.LAUB, Rank.ASS), c(Suit.HERZ, Rank.UNTER)))
        assertEquals(Seat.PLAYER, Rules.trickWinner(Suit.HERZ, Seat.PLAYER, c(Suit.LAUB, Rank.UNTER), c(Suit.EICHEL, Rank.ASS)))
        assertEquals(Seat.SEPP, Rules.trickWinner(Suit.HERZ, Seat.PLAYER, c(Suit.LAUB, Rank.ZEHN), c(Suit.LAUB, Rank.ASS)))
    }

    @Test
    fun marriageCountsOnlyAfterFirstTrick() {
        var s = fixture(
            player = listOf(c(Suit.LAUB, Rank.KOENIG), c(Suit.LAUB, Rank.OBER), c(Suit.HERZ, Rank.ASS)),
            sepp = listOf(c(Suit.LAUB, Rank.ASS), c(Suit.EICHEL, Rank.UNTER), c(Suit.EICHEL, Rank.OBER)),
        )
        s = Rules.apply(s, Action.Announce(Suit.LAUB))
        assertEquals(0, s.points(Seat.PLAYER))
        assertEquals(20, s.pendingMarriagePoints(Seat.PLAYER))
        s = Rules.applyAndCollect(s, Action.Play(c(Suit.LAUB, Rank.OBER)))
        s = Rules.applyAndCollect(s, Action.Play(c(Suit.LAUB, Rank.ASS))) // Sepp sticht mit dem Ass
        assertEquals(0, s.points(Seat.PLAYER))
        assertEquals(14, s.points(Seat.SEPP))
        s = Rules.applyAndCollect(s, Action.Play(c(Suit.EICHEL, Rank.UNTER)))
        s = Rules.applyAndCollect(s, Action.Play(c(Suit.HERZ, Rank.ASS)))
        assertEquals(20 + 13, s.points(Seat.PLAYER))
    }

    @Test
    fun trumpMarriageIsForty() {
        val s = fixture(
            player = listOf(c(Suit.HERZ, Rank.KOENIG), c(Suit.HERZ, Rank.OBER)),
            sepp = listOf(c(Suit.LAUB, Rank.ASS), c(Suit.EICHEL, Rank.UNTER)),
        )
        assertEquals(40, Rules.apply(s, Action.Announce(Suit.HERZ)).marriagePoints[0])
    }

    @Test
    fun exchangeTrumpUnter() {
        val unter = c(Suit.HERZ, Rank.UNTER)
        val ass = c(Suit.HERZ, Rank.ASS)
        val player = listOf(unter, c(Suit.LAUB, Rank.KOENIG))
        val sepp = listOf(c(Suit.LAUB, Rank.UNTER), c(Suit.EICHEL, Rank.UNTER))
        val stock = (Card.DECK - player.toSet() - sepp.toSet() - ass).take(5)
        val s = fixture(player, sepp, stock = stock, trumpCard = ass)
        assertTrue(Rules.canExchangeTrump(s, Seat.PLAYER))
        val after = Rules.apply(s, Action.ExchangeTrump)
        assertEquals(unter, after.trumpCard)
        assertTrue(ass in after.hand(Seat.PLAYER))
        assertTrue(ass in after.revealed[Seat.PLAYER.ordinal])
    }

    @Test
    fun closerWinsWithOpponentStateAtClosing() {
        val player = listOf(c(Suit.HERZ, Rank.ASS), c(Suit.HERZ, Rank.ZEHN))
        val sepp = listOf(c(Suit.LAUB, Rank.UNTER), c(Suit.EICHEL, Rank.UNTER))
        val stock = (Card.DECK - player.toSet() - sepp.toSet()).take(4)
        var s = fixture(player, sepp, stock = stock.drop(1), trumpCard = stock.first())
            .copy(cardPoints = listOf(60, 0), trickCounts = listOf(3, 0))
        s = Rules.apply(s, Action.Close)
        assertTrue(s.strictRules)
        s = Rules.applyAndCollect(s, Action.Play(c(Suit.HERZ, Rank.ASS)))
        s = Rules.applyAndCollect(s, Action.Play(c(Suit.LAUB, Rank.UNTER)))
        assertEquals(1, s.hand(Seat.PLAYER).size) // nach dem Zudrehen wird nicht mehr nachgezogen
        s = Rules.apply(s, Action.Claim)
        assertEquals(Seat.PLAYER, s.result!!.winner)
        assertEquals(3, s.result!!.gamePoints) // Sepp hatte beim Zudrehen keinen Stich
    }

    @Test
    fun scoringTiers() {
        assertEquals(1, Rules.gamePointsFor(40, 3))
        assertEquals(2, Rules.gamePointsFor(20, 2))
        assertEquals(3, Rules.gamePointsFor(0, 0))
    }

    @Test
    fun wrongClaimLoses() {
        val s = fixture(
            player = listOf(c(Suit.LAUB, Rank.KOENIG)),
            sepp = listOf(c(Suit.LAUB, Rank.UNTER)),
        )
        val r = Rules.apply(s, Action.Claim).result!!
        assertEquals(Seat.SEPP, r.winner)
        assertEquals(HandEndReason.WRONG_CLAIM, r.reason)
    }

    @Test
    fun bummerlCountsDown() {
        var b = Bummerl()
        b = b.record(HandResult(Seat.PLAYER, 3, HandEndReason.CLAIMED, 70, 0))
        b = b.record(HandResult(Seat.PLAYER, 3, HandEndReason.CLAIMED, 70, 0))
        assertFalse(b.isFinished)
        b = b.record(HandResult(Seat.PLAYER, 2, HandEndReason.CLAIMED, 70, 10))
        assertEquals(Seat.PLAYER, b.winner)
        assertEquals(2, b.bummerlCount) // Sepp ist Schneider
        assertEquals(listOf(4 to 7, 1 to 7, 0 to 7), b.runningTotals())
    }

    @Test
    fun botsPlayCompleteGamesWithoutBreakingRules() {
        val random = Random(42)
        for (d in Difficulty.entries) {
            val a = SeppBot(d, random)
            val b = SeppBot(Difficulty.MITTEL, random)
            repeat(if (d == Difficulty.MEISTER) 6 else 30) {
                var s = HandState.deal(if (it % 2 == 0) Seat.SEPP else Seat.PLAYER, random)
                var guard = 0
                while (!s.isOver) {
                    val actor = s.toAct!!
                    val bot = if (actor == Seat.SEPP) a else b
                    s = Rules.applyAndCollect(s, bot.chooseAction(s, actor))
                    check(++guard < 200)
                }
                assertTrue(s.result!!.gamePoints in 1..3)
            }
        }
    }

    @Test
    fun strongerBotBeatsEasyBot() {
        val random = Random(3)
        val strong = SeppBot(Difficulty.SCHWER, random)
        val easy = SeppBot(Difficulty.LEICHT, random)
        var strongPoints = 0
        var easyPoints = 0
        repeat(80) {
            var s = HandState.deal(if (it % 2 == 0) Seat.SEPP else Seat.PLAYER, random)
            while (!s.isOver) {
                val actor = s.toAct!!
                s = Rules.applyAndCollect(s, (if (actor == Seat.SEPP) strong else easy).chooseAction(s, actor))
            }
            if (s.result!!.winner == Seat.SEPP) strongPoints += s.result!!.gamePoints else easyPoints += s.result!!.gamePoints
        }
        println("Schwer $strongPoints : Leicht $easyPoints")
        assertTrue(strongPoints > easyPoints)
    }
}
