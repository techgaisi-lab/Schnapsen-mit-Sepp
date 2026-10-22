package at.schnapsenmitsepp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.schnapsenmitsepp.engine.Action
import at.schnapsenmitsepp.engine.Card
import at.schnapsenmitsepp.engine.HandEndReason
import at.schnapsenmitsepp.engine.HandState
import at.schnapsenmitsepp.engine.Rank
import at.schnapsenmitsepp.engine.Rules
import at.schnapsenmitsepp.engine.Seat
import at.schnapsenmitsepp.engine.Suit
import at.schnapsenmitsepp.engine.Trick
import at.schnapsenmitsepp.ui.cards.CARD_RATIO
import at.schnapsenmitsepp.ui.cards.CardStyle
import at.schnapsenmitsepp.ui.cards.LocalCardStyle
import at.schnapsenmitsepp.ui.cards.PlayingCard
import at.schnapsenmitsepp.ui.cards.drawSuit
import at.schnapsenmitsepp.ui.theme.SeppColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset

@Composable
internal fun GameScreen(vm: GameViewModel, onBack: () -> Unit, onShowList: () -> Unit) {
    LaunchedEffect(Unit) { vm.ensureHand() }
    val state = vm.hand ?: return
    val cardStyle = LocalCardStyle.current
    var showLastTrick by remember { mutableStateOf(false) }
    // Nur die eigenen Augen werden gezeigt – und nur, wenn "Augen mitzählen" eingeschaltet ist.
    val ownPoints = if (vm.settings.showPoints) state.points(Seat.PLAYER) else null

    BoxWithConstraints(Modifier.fillMaxSize().feltBackground()) {
        val landscape = maxWidth > maxHeight * 1.15f
        val handCardW = if (landscape) {
            minOf(maxHeight * 0.3f / CARD_RATIO, (maxWidth - 410.dp) / 4.6f)
        } else {
            minOf(maxWidth / 4.4f, maxHeight * 0.19f / CARD_RATIO)
        }
        val tableCardW = handCardW * if (landscape) 0.85f else 0.92f
        val seppCardW = handCardW * if (landscape) 0.5f else 0.6f
        val travel = maxHeight * 0.38f

        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                // Seitenleiste links: Sepp, Liste, Atout, Augen
                Column(
                    Modifier
                        .width(190.dp)
                        .fillMaxHeight()
                        .background(SeppColors.WoodDark.copy(alpha = 0.85f))
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SeppHeader(vm, onBack)
                    MiniScore(vm, Modifier.fillMaxWidth().clickable(onClick = onShowList))
                    TrumpBadge(state.trumpSuit, cardStyle, Modifier)
                }
                // Der Tisch
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    SeppHand(state, seppCardW)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        Talon(state, tableCardW, Modifier.align(Alignment.CenterStart).padding(start = 18.dp))
                        TrickArea(state, vm.collectingTo, tableCardW, travel, Modifier.align(Alignment.Center).offset(x = tableCardW * 0.45f))
                        TrickPile(state.tricks(Seat.SEPP), "Sepps Stiche", null, tableCardW * 0.55f, Modifier.align(Alignment.TopEnd).padding(12.dp))
                        TrickPile(state.tricks(Seat.PLAYER), "Deine Stiche", ownPoints, tableCardW * 0.55f, Modifier.align(Alignment.BottomEnd).padding(12.dp))
                    }
                    PlayerHand(state, handCardW) { card -> vm.onPlayer(Action.Play(card)) }
                }
                // Aktionen rechts
                Column(
                    Modifier.width(200.dp).fillMaxHeight().padding(vertical = 10.dp, horizontal = 6.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StatusLine(state, vm)
                    Spacer(Modifier.height(10.dp))
                    ActionBar(state, vm, cardStyle, vertical = true, onShowLastTrick = { showLastTrick = true })
                }
            }
        } else Column(Modifier.fillMaxSize()) {
            TopBar(vm, onBack, onShowList)
            SeppHand(state, seppCardW)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Talon(state, tableCardW, Modifier.align(Alignment.CenterStart).padding(start = 18.dp))
                TrickArea(state, vm.collectingTo, tableCardW, travel, Modifier.align(Alignment.Center).offset(x = tableCardW * 0.45f))
                TrumpBadge(state.trumpSuit, cardStyle, Modifier.align(Alignment.TopEnd).padding(10.dp))
                TrickPile(state.tricks(Seat.SEPP), "Sepps Stiche", null, tableCardW * 0.55f, Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 6.dp))
                TrickPile(state.tricks(Seat.PLAYER), "Deine Stiche", ownPoints, tableCardW * 0.55f, Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 6.dp))
            }
            StatusLine(state, vm)
            ActionBar(state, vm, cardStyle, onShowLastTrick = { showLastTrick = true })
            PlayerHand(state, handCardW) { card -> vm.onPlayer(Action.Play(card)) }
        }

        state.lastTrick?.takeIf { showLastTrick }?.let { trick ->
            LastTrickDialog(trick, tableCardW, onDismiss = { showLastTrick = false })
        }

        if (state.isOver) {
            ResultOverlay(vm, state, onShowList)
        }
    }
}

/** Sepp mit Sprechblase – für die Seitenleiste im Querformat. */
@Composable
private fun SeppHeader(vm: GameViewModel, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "‹",
            color = SeppColors.Cream,
            fontSize = 30.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 6.dp),
        )
        SeppAvatar(if (vm.seppThinking) SeppMood.THINKING else vm.seppMood, Modifier.size(48.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sepp", color = SeppColors.Gold, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
    val says = if (vm.seppThinking) "Hmm, wos moch i jetzt…" else vm.seppSays
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                if (says != null) SeppColors.Cream else Color.Transparent,
                RoundedCornerShape(topStart = 2.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(says.orEmpty(), color = SeppColors.Ink, fontSize = 13.sp, maxLines = 2, lineHeight = 16.sp)
    }
}

@Composable
private fun TopBar(vm: GameViewModel, onBack: () -> Unit, onShowList: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(SeppColors.WoodDark.copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "‹",
            color = SeppColors.Cream,
            fontSize = 34.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 8.dp),
        )
        SeppAvatar(if (vm.seppThinking) SeppMood.THINKING else vm.seppMood, Modifier.size(54.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Sepp", color = SeppColors.Gold, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            val says = if (vm.seppThinking) "Hmm, wos moch i jetzt…" else vm.seppSays
            AnimatedVisibility(visible = says != null, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .background(SeppColors.Cream, RoundedCornerShape(topStart = 2.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(says.orEmpty(), color = SeppColors.Ink, fontSize = 14.sp, maxLines = 2)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        MiniScore(vm, Modifier.clickable(onClick = onShowList))
    }
}

@Composable
private fun MiniScore(vm: GameViewModel, modifier: Modifier) {
    val b = vm.bummerl
    Column(
        modifier
            .background(SeppColors.Paper, RoundedCornerShape(8.dp))
            .border(1.dp, SeppColors.GoldDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Liste", color = SeppColors.Ink.copy(alpha = 0.6f), fontSize = 11.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Du ", color = SeppColors.Ink, fontSize = 13.sp)
            Text("${b.remaining(Seat.PLAYER)}", color = SeppColors.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("  ·  ", color = SeppColors.Ink, fontSize = 13.sp)
            Text("${b.remaining(Seat.SEPP)}", color = SeppColors.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(" Sepp", color = SeppColors.Ink, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SeppHand(state: HandState, cardW: Dp) {
    val cards = state.hand(Seat.SEPP)
    val revealed = state.revealed[Seat.SEPP.ordinal]
    Box(Modifier.fillMaxWidth().height(cardW * CARD_RATIO + 14.dp), contentAlignment = Alignment.TopCenter) {
        val n = cards.size
        cards.forEachIndexed { i, card ->
            key(card) {
                val rel = i - (n - 1) / 2f
                val x by animateDpAsState(cardW * 0.55f * rel, label = "seppX")
                PlayingCard(
                    card = card,
                    faceUp = card in revealed,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .offset(x = x, y = (abs(rel) * 3).dp)
                        .graphicsLayer { rotationZ = -rel * 5f }
                        .width(cardW)
                        .aspectRatio(1f / CARD_RATIO),
                    elevation = 3f,
                )
            }
        }
    }
}

@Composable
private fun Talon(state: HandState, cardW: Dp, modifier: Modifier) {
    Box(modifier.width(cardW * 1.6f).height(cardW * CARD_RATIO * 1.1f), contentAlignment = Alignment.CenterStart) {
        val trump = state.trumpCard
        if (trump != null) {
            // Atout liegt quer unter dem Talon
            PlayingCard(
                card = trump,
                faceUp = !state.isClosed,
                modifier = Modifier
                    .offset(x = cardW * 0.42f)
                    .graphicsLayer { rotationZ = 90f }
                    .width(cardW)
                    .aspectRatio(1f / CARD_RATIO),
            )
        }
        val layers = minOf(state.stock.size, 5)
        for (i in 0 until layers) {
            PlayingCard(
                card = null,
                faceUp = false,
                modifier = Modifier
                    .offset(x = (i * 1.5f).dp, y = (-i * 1.5f).dp)
                    .width(cardW)
                    .aspectRatio(1f / CARD_RATIO),
                elevation = if (i == layers - 1) 6f else 1f,
            )
        }
        if (state.isClosed && trump != null) {
            // Zugedreht: die Atoutkarte liegt verdeckt oben drauf
            PlayingCard(
                card = null,
                faceUp = false,
                modifier = Modifier
                    .offset(x = cardW * 0.2f, y = 4.dp)
                    .graphicsLayer { rotationZ = 75f }
                    .width(cardW)
                    .aspectRatio(1f / CARD_RATIO),
                elevation = 8f,
            )
        }
        val label = when {
            state.isClosed -> "zugedreht"
            state.talonSize == 0 -> "Talon leer"
            else -> "${state.talonSize} im Talon"
        }
        Text(
            label,
            color = SeppColors.Cream.copy(alpha = 0.85f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomStart).offset(y = 18.dp),
        )
    }
}

/**
 * Die gewonnenen Stiche als kleiner, verdeckter Stoß beim jeweiligen Spieler – wie am Wirtshaustisch
 * kreuzweise übereinander gelegt. [points] nur beim eigenen Stoß (Stiche plus Ansagen).
 */
@Composable
private fun TrickPile(tricks: Int, label: String, points: Int?, cardW: Dp, modifier: Modifier) {
    if (tricks == 0 && (points ?: 0) == 0) return
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(cardW * CARD_RATIO * 1.1f), contentAlignment = Alignment.Center) {
            for (i in 0 until minOf(tricks, 6)) {
                PlayingCard(
                    card = null,
                    faceUp = false,
                    modifier = Modifier
                        .offset(x = (i * 1.5f).dp, y = (-i * 2f).dp)
                        .graphicsLayer { rotationZ = if (i % 2 == 0) 82f else 98f }
                        .width(cardW)
                        .aspectRatio(1f / CARD_RATIO),
                    elevation = 2f,
                )
            }
        }
        Text(
            "$label: $tricks",
            color = SeppColors.Cream,
            fontSize = 13.sp,
        )
        if (points != null) {
            Text(
                "$points Augen",
                color = SeppColors.Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TrickArea(state: HandState, collectingTo: Seat?, cardW: Dp, travel: Dp, modifier: Modifier) {
    Box(modifier.width(cardW * 2.2f).height(cardW * CARD_RATIO * 1.3f), contentAlignment = Alignment.Center) {
        state.leadCard?.let {
            TrickCard(it, state.leader, collectingTo, -cardW * 0.3f, (-8).dp, -7f, cardW, travel)
        }
        state.followCard?.let {
            TrickCard(it, state.leader.other, collectingTo, cardW * 0.3f, 8.dp, 8f, cardW, travel)
        }
    }
}

@Composable
private fun TrickCard(
    card: Card,
    from: Seat,
    collectingTo: Seat?,
    baseX: Dp,
    baseY: Dp,
    rotation: Float,
    cardW: Dp,
    travel: Dp,
) {
    val travelPx = with(LocalDensity.current) { travel.toPx() }
    val y = remember(card) { Animatable(if (from == Seat.PLAYER) travelPx else -travelPx) }
    val alpha = remember(card) { Animatable(1f) }
    val scale = remember(card) { Animatable(1.08f) }
    LaunchedEffect(card) {
        launch { scale.animateTo(1f, tween(380)) }
        y.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(collectingTo) {
        if (collectingTo != null) {
            launch { alpha.animateTo(0f, tween(360)) }
            y.animateTo(if (collectingTo == Seat.PLAYER) travelPx else -travelPx, tween(360))
        }
    }
    PlayingCard(
        card = card,
        modifier = Modifier
            .offset(x = baseX, y = baseY)
            .graphicsLayer {
                translationY = y.value
                rotationZ = rotation
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
            }
            .width(cardW)
            .aspectRatio(1f / CARD_RATIO),
        elevation = 8f,
    )
}

@Composable
private fun TrumpBadge(suit: Suit, cardStyle: CardStyle, modifier: Modifier) {
    Column(
        modifier
            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Atout", color = SeppColors.Gold, fontSize = 11.sp)
        Canvas(Modifier.size(30.dp)) { drawSuit(suit, Offset(size.width / 2, size.height / 2), size.minDimension, cardStyle) }
        Text(cardStyle.suitName(suit), color = SeppColors.Cream, fontSize = 12.sp)
    }
}

@Composable
private fun StatusLine(state: HandState, vm: GameViewModel) {
    val text = vm.hint ?: when {
        state.isOver -> ""
        vm.seppThinking -> "Sepp überlegt…"
        state.toAct == Seat.PLAYER && state.announcedSuit != null -> "Du hast 66 – sag \"Aus!\" oder spiel König/${LocalCardStyle.current.rankName(Rank.OBER)} aus"
        state.toAct == Seat.PLAYER && state.leadCard == null -> "Du spielst aus"
        state.toAct == Seat.PLAYER && state.strictRules -> "Gib zu – Farb- und Stichzwang!"
        state.toAct == Seat.PLAYER -> "Gib zu"
        else -> ""
    }
    Text(
        text,
        color = if (vm.hint != null) SeppColors.Gold else SeppColors.Cream,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { vm.dismissHint() },
    )
}

@Composable
private fun ActionBar(
    state: HandState,
    vm: GameViewModel,
    cardStyle: CardStyle,
    vertical: Boolean = false,
    onShowLastTrick: () -> Unit,
) {
    val me = Seat.PLAYER
    val chips = buildList {
        if (Rules.canExchangeTrump(state, me)) add("Atout tauschen" to Action.ExchangeTrump)
        if (Rules.canClose(state, me)) add("Zudrehen" to Action.Close)
        if (Rules.canClaim(state, me)) add("Aus! (66)" to Action.Claim)
    }
    val buttons: @Composable () -> Unit = {
        chips.forEach { (label, action) -> GoldButton(label, small = true) { vm.onPlayer(action) } }
        if (state.lastTrick != null && !state.isOver) {
            GoldButton("Letzter Stich", small = true, secondary = true, onClick = onShowLastTrick)
        }
    }
    if (vertical) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            buttons()
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) { buttons() }
    }
}

@Composable
private fun PlayerHand(state: HandState, cardW: Dp, onTap: (Card) -> Unit) {
    val trump = state.trumpSuit
    val suitOrder = listOf(trump) + Suit.entries.filter { it != trump }
    val cards = state.hand(Seat.PLAYER).sortedWith(
        compareBy<Card> { suitOrder.indexOf(it.suit) }.thenByDescending { it.rank.ordinal },
    )
    val myTurn = state.toAct == Seat.PLAYER
    val legal = if (myTurn) Rules.legalCards(state, Seat.PLAYER).toSet() else emptySet()
    val revealedToSepp = state.revealed[Seat.PLAYER.ordinal]

    Box(
        Modifier
            .fillMaxWidth()
            .height(cardW * CARD_RATIO + 34.dp)
            .padding(bottom = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val n = cards.size
        cards.forEachIndexed { i, card ->
            key(card) {
                val rel = i - (n - 1) / 2f
                val isLegal = card in legal
                val x by animateDpAsState(cardW * 0.78f * rel, tween(260), label = "x")
                val lift by animateDpAsState(if (isLegal) (-14).dp else 0.dp, tween(200), label = "lift")
                val deal = remember(card) { Animatable(1f) }
                LaunchedEffect(card) { deal.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
                PlayingCard(
                    card = card,
                    highlighted = isLegal && state.strictRules && state.leadCard != null,
                    dimmed = myTurn && !isLegal,
                    modifier = Modifier
                        .offset(x = x, y = lift + (abs(rel) * abs(rel) * 3).dp)
                        .graphicsLayer {
                            rotationZ = rel * 5f
                            translationX = -deal.value * size.width * 1.4f
                            translationY = -deal.value * size.height * 2.2f
                            alpha = 1f - deal.value * 0.6f
                        }
                        .width(cardW)
                        .aspectRatio(1f / CARD_RATIO)
                        .clickable(enabled = myTurn) { onTap(card) },
                    elevation = 6f,
                )
                if (card in revealedToSepp) {
                    Text(
                        "gezeigt",
                        color = SeppColors.Gold,
                        fontSize = 10.sp,
                        modifier = Modifier.offset(x = x, y = lift - cardW * CARD_RATIO - 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LastTrickDialog(trick: Trick, cardW: Dp, onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        WoodPanel(Modifier.padding(24.dp)) {
            Text("Letzter Stich", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = SeppColors.Gold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(trick.leader to trick.lead, trick.leader.other to trick.follow).forEach { (seat, card) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PlayingCard(card, Modifier.width(cardW).aspectRatio(1f / CARD_RATIO))
                        Spacer(Modifier.height(4.dp))
                        Text(if (seat == Seat.PLAYER) "Du" else "Sepp", color = SeppColors.Cream)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (trick.winner == Seat.PLAYER) "Den Stich hast du gemacht." else "Den Stich hat Sepp gemacht.",
                color = SeppColors.Cream,
            )
        }
    }
}

@Composable
private fun ResultOverlay(vm: GameViewModel, state: HandState, onShowList: () -> Unit) {
    val r = state.result ?: return
    val finished = vm.finishedBummerl
    val playerWon = r.winner == Seat.PLAYER
    val loser = r.winner.other
    val tier = when (r.gamePoints) {
        3 -> "Schwarz"
        2 -> "Schneider"
        else -> "einfach"
    }
    val reasonText = when (r.reason) {
        HandEndReason.REACHED_66 -> if (playerWon) "Du hast mit ${r.winnerPoints} Augen ausgemeldet." else "Sepp hat mit ${r.winnerPoints} Augen ausgemeldet."
        HandEndReason.WRONG_CLAIM -> if (loser == Seat.PLAYER) "Falsch ausgemeldet – du hattest erst ${r.loserPoints} Augen." else "Sepp hat sich verzählt (${r.loserPoints} Augen)."
        HandEndReason.LAST_TRICK -> if (playerWon) "Keiner hatte 66 – dein letzter Stich entscheidet." else "Keiner hatte 66 – Sepp hat den letzten Stich."
        HandEndReason.CLOSER_FAILED -> if (loser == Seat.PLAYER) "Du hast zugedreht, aber keine 66 geschafft." else "Sepp hat zugedreht und keine 66 geschafft."
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(visible = true, enter = scaleIn() + fadeIn()) {
            WoodPanel(Modifier.padding(24.dp).widthIn(max = 380.dp)) {
                if (finished != null) {
                    val bummerlLoser = finished.winner!!.other
                    Text("Bummerl!", style = androidx.compose.material3.MaterialTheme.typography.displayLarge, color = SeppColors.Gold)
                    Spacer(Modifier.height(8.dp))
                    BummerlDots(finished.bummerlCount, 26.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (bummerlLoser == Seat.SEPP) "Sepp kriegt ${if (finished.bummerlCount == 2) "zwei Bummerl (Schneider)!" else "ein Bummerl!"}"
                        else "Du kriegst ${if (finished.bummerlCount == 2) "zwei Bummerl (Schneider)" else "ein Bummerl"}.",
                        color = SeppColors.Cream,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        if (playerWon) "Gewonnen!" else "Sepp gewinnt",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        color = SeppColors.Gold,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(reasonText, color = SeppColors.Cream, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Augen ${state.points(Seat.PLAYER)} : ${state.points(Seat.SEPP)}",
                    color = SeppColors.Cream.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "${if (playerWon) "Du schreibst" else "Sepp schreibt"} ${r.gamePoints} ${if (r.gamePoints == 1) "Punkt" else "Punkte"} ab ($tier)",
                    color = SeppColors.Gold,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Liste: Du ${vm.bummerl.remaining(Seat.PLAYER)} · Sepp ${vm.bummerl.remaining(Seat.SEPP)}",
                    color = SeppColors.Cream,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GoldButton("Liste", secondary = true, onClick = onShowList)
                    GoldButton(if (finished != null) "Neues Bummerl" else "Weiter", onClick = vm::nextHand)
                }
            }
        }
    }
}

/** Ein Bummerl wird auf der Liste traditionell als ausgemalter Kreis gezeichnet. */
@Composable
internal fun BummerlDots(count: Int, size: Dp, color: Color = SeppColors.Ink, ring: Color = SeppColors.Cream) {
    Row(horizontalArrangement = Arrangement.spacedBy(size / 4)) {
        repeat(count) {
            Canvas(Modifier.size(size)) {
                drawCircle(ring, radius = this.size.minDimension / 2)
                drawCircle(color, radius = this.size.minDimension / 2 * 0.82f)
            }
        }
    }
}

@Composable
internal fun WoodPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .shadow(16.dp, RoundedCornerShape(18.dp))
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(listOf(SeppColors.Wood, SeppColors.WoodDark)),
                RoundedCornerShape(18.dp),
            )
            .border(2.dp, SeppColors.GoldDark, RoundedCornerShape(18.dp))
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
internal fun GoldButton(
    label: String,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    secondary: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(if (small) 18.dp else 24.dp)
    Box(
        modifier
            .shadow(if (secondary) 0.dp else 4.dp, shape)
            .background(
                if (secondary) {
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                } else {
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFF6DA8E), SeppColors.Gold, SeppColors.GoldDark))
                },
                shape,
            )
            .border(1.5.dp, SeppColors.Gold, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = if (small) 14.dp else 22.dp, vertical = if (small) 7.dp else 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (secondary) SeppColors.Gold else SeppColors.Ink,
            fontWeight = FontWeight.Bold,
            fontSize = if (small) 14.sp else 16.sp,
        )
    }
}
