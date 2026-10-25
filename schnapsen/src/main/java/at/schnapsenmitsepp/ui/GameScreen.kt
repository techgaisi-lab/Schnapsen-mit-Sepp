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
import androidx.compose.ui.unit.IntOffset
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
    var confirmClose by remember { mutableStateOf(false) }
    // Die eigenen Punkte werden immer unter dem eigenen Stoß angezeigt.
    val ownPoints = state.points(Seat.PLAYER)
    val canClose = Rules.canClose(state, Seat.PLAYER) && !vm.seppThinking
    val onStackTap: () -> Unit = {
        if (canClose) confirmClose = true else vm.explainClose()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight * 1.15f
        // Im Querformat überlappen die eigenen Karten etwas mehr – dafür sind sie deutlich größer.
        val handSpread = if (landscape) 0.64f else 0.78f
        val handCardW = if (landscape) {
            minOf(maxHeight * 0.40f / CARD_RATIO, (maxWidth - 470.dp) / (1f + 4f * handSpread + 0.15f))
        } else {
            minOf(maxWidth / 4.15f, maxHeight * 0.22f / CARD_RATIO)
        }
        val tableCardW = handCardW * if (landscape) 0.72f else 0.92f
        val seppCardW = handCardW * if (landscape) 0.4f else 0.58f
        val travel = maxHeight * 0.38f

        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                // Seitenleiste links: Sepp, Liste, Atout
                Column(
                    Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .background(SeppColors.WoodDark.copy(alpha = 0.85f))
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SeppHeader(vm, onBack)
                    MiniScore(vm, Modifier.fillMaxWidth().clickable(onClick = onShowList))
                    TrumpBadge(state.trumpSuit, cardStyle, Modifier.fillMaxWidth())
                }
                // Der Tisch – oben eine eigene Zeile: links Sepps Stiche, daneben Sepps Karten.
                // So kann die Beschriftung nie über den Stapel darunter rutschen.
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Box(Modifier.width(160.dp).padding(start = 10.dp, top = 4.dp), contentAlignment = Alignment.TopCenter) {
                            TrickPile(state.tricks(Seat.SEPP), "Sepps Stiche", null, tableCardW * 0.45f, Modifier)
                        }
                        Box(Modifier.weight(1f)) { SeppHand(state, seppCardW) }
                        // gleich breit wie links, damit Sepps Karten mittig über dem Tisch bleiben
                        Spacer(Modifier.width(160.dp))
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        Stack(state, canClose, tableCardW, onStackTap, Modifier.align(Alignment.CenterStart).padding(start = 18.dp))
                        TrickArea(state, vm.collectingTo, tableCardW, travel, Modifier.align(Alignment.Center).offset(x = tableCardW * 0.45f))
                    }
                    PlayerHand(state, handCardW, handSpread) { card -> vm.onPlayer(Action.Play(card)) }
                }
                // Rechts: oben wer dran ist, darunter die Knöpfe, unten rechts der eigene Stichstoß mit den Punkten
                Column(
                    Modifier.width(220.dp).fillMaxHeight().padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StatusLine(state, vm)
                    Spacer(Modifier.height(12.dp))
                    ActionBar(state, vm, cardStyle, vertical = true, onShowLastTrick = { showLastTrick = true })
                    Spacer(Modifier.weight(1f))
                    TrickPile(state.tricks(Seat.PLAYER), "Deine Stiche", ownPoints, tableCardW * 0.6f, Modifier)
                }
            }
        } else Column(Modifier.fillMaxSize()) {
            TopBar(vm, onBack, onShowList)
            SeppHand(state, seppCardW)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Stack(state, canClose, tableCardW, onStackTap, Modifier.align(Alignment.CenterStart).padding(start = 18.dp))
                TrickArea(state, vm.collectingTo, tableCardW, travel, Modifier.align(Alignment.Center).offset(x = tableCardW * 0.45f))
                TrumpBadge(state.trumpSuit, cardStyle, Modifier.align(Alignment.TopEnd).padding(8.dp), large = false)
                TrickPile(state.tricks(Seat.SEPP), "Sepps Stiche", null, tableCardW * 0.55f, Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 6.dp))
                TrickPile(state.tricks(Seat.PLAYER), "Deine Stiche", ownPoints, tableCardW * 0.55f, Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 6.dp))
            }
            StatusLine(state, vm)
            ActionBar(state, vm, cardStyle, onShowLastTrick = { showLastTrick = true })
            PlayerHand(state, handCardW, handSpread) { card -> vm.onPlayer(Action.Play(card)) }
        }

        state.lastTrick?.takeIf { showLastTrick }?.let { trick ->
            LastTrickDialog(trick, tableCardW, onDismiss = { showLastTrick = false })
        }

        if (confirmClose) {
            ConfirmCloseDialog(
                onYes = {
                    confirmClose = false
                    vm.onPlayer(Action.Close)
                },
                onNo = { confirmClose = false },
            )
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
        SeppAvatar(if (vm.seppThinking) SeppMood.THINKING else vm.seppMood, Modifier.size(56.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sepp", color = SeppColors.Gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
    val says = if (vm.seppThinking) "Hmm, wos moch i jetzt…" else vm.seppSays
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                if (says != null) SeppColors.Cream else Color.Transparent,
                RoundedCornerShape(topStart = 2.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(says.orEmpty(), color = SeppColors.Ink, fontSize = 16.sp, maxLines = 2, lineHeight = 19.sp)
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
        SeppAvatar(if (vm.seppThinking) SeppMood.THINKING else vm.seppMood, Modifier.size(60.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Sepp", color = SeppColors.Gold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            val says = if (vm.seppThinking) "Hmm, wos moch i jetzt…" else vm.seppSays
            AnimatedVisibility(visible = says != null, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .background(SeppColors.Cream, RoundedCornerShape(topStart = 2.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(says.orEmpty(), color = SeppColors.Ink, fontSize = 17.sp, maxLines = 2)
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
        Text("Liste", color = SeppColors.Ink.copy(alpha = 0.7f), fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Du ", color = SeppColors.Ink, fontSize = 16.sp)
            Text("${b.remaining(Seat.PLAYER)}", color = SeppColors.Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("  ·  ", color = SeppColors.Ink, fontSize = 16.sp)
            Text("${b.remaining(Seat.SEPP)}", color = SeppColors.Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(" Sepp", color = SeppColors.Ink, fontSize = 16.sp)
        }
    }
}

@Composable
private fun SeppHand(state: HandState, cardW: Dp) {
    val cards = state.hand(Seat.SEPP)
    val revealed = state.revealed[Seat.SEPP.ordinal]
    Box(Modifier.fillMaxWidth().height(cardW * CARD_RATIO + 10.dp), contentAlignment = Alignment.TopCenter) {
        val n = cards.size
        cards.forEachIndexed { i, card ->
            key(card) {
                val rel = i - (n - 1) / 2f
                // Animierter Wert wird nur beim Zeichnen gelesen → keine Neuberechnung des Layouts pro Bild
                val x = animateDpAsState(cardW * 0.55f * rel, label = "seppX")
                PlayingCard(
                    card = card,
                    faceUp = card in revealed,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .graphicsLayer {
                            translationX = x.value.toPx()
                            translationY = (abs(rel) * 3).dp.toPx()
                            rotationZ = -rel * 5f
                        }
                        .width(cardW)
                        .aspectRatio(1f / CARD_RATIO),
                    elevation = 0f,
                )
            }
        }
    }
}

/**
 * Der Stapel (verdeckte Karten mit der Atoutkarte quer darunter). Antippen = zudrehen, solange das
 * erlaubt ist – dann ist die oberste Karte golden umrandet.
 */
@Composable
private fun Stack(state: HandState, canClose: Boolean, cardW: Dp, onTap: () -> Unit, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.Start) {
        Box(
            Modifier
                .width(cardW * 1.6f)
                .height(cardW * CARD_RATIO * 1.1f)
                .clickable(
                    enabled = state.talonSize > 0 && !state.isClosed,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            val trump = state.trumpCard
            if (trump != null) {
                // Atout liegt quer unter dem Stapel
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
                    highlighted = canClose && i == layers - 1,
                    modifier = Modifier
                        .offset(x = (i * 1.5f).dp, y = (-i * 1.5f).dp)
                        .width(cardW)
                        .aspectRatio(1f / CARD_RATIO),
                    elevation = if (i == layers - 1) 6f else 0f,
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
        }
        val label = when {
            state.isClosed -> "zugedreht"
            state.talonSize == 0 -> "Stapel leer"
            else -> "${state.talonSize} im Stapel"
        }
        Text(label, color = SeppColors.Cream, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        if (canClose) {
            Text("Antippen = zudrehen", color = SeppColors.Gold, fontSize = 15.sp)
        }
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
                    // nur die oberste Karte wirft einen Schatten
                    elevation = if (i == minOf(tricks, 6) - 1) 2f else 0f,
                )
            }
        }
        Text(
            "$label: $tricks",
            color = SeppColors.Cream,
            fontSize = 16.sp,
        )
        if (points != null) {
            Text(
                "$points Punkte",
                color = SeppColors.Gold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TrickArea(state: HandState, collectingTo: Seat?, cardW: Dp, travel: Dp, modifier: Modifier) {
    Box(modifier.width(cardW * 2.2f).height(cardW * CARD_RATIO * 1.15f), contentAlignment = Alignment.Center) {
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

/** Gut sichtbares Kasterl: welche Farbe Atout (Trumpf) ist – großes Symbol auf hellem Grund. */
@Composable
private fun TrumpBadge(suit: Suit, cardStyle: CardStyle, modifier: Modifier, large: Boolean = true) {
    Column(
        modifier
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .background(SeppColors.Cream, RoundedCornerShape(14.dp))
            .border(3.dp, SeppColors.Gold, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ATOUT", color = SeppColors.Burgundy, fontSize = if (large) 17.sp else 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Canvas(Modifier.padding(vertical = 4.dp).size(if (large) 56.dp else 40.dp)) {
            drawSuit(suit, Offset(size.width / 2, size.height / 2), size.minDimension, cardStyle)
        }
        Text(cardStyle.suitName(suit), color = SeppColors.Ink, fontSize = if (large) 22.sp else 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** Sicherheitsfrage vor dem Zudrehen – das lässt sich nicht rückgängig machen. */
@Composable
private fun ConfirmCloseDialog(onYes: () -> Unit, onNo: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onNo),
        contentAlignment = Alignment.Center,
    ) {
        WoodPanel(Modifier.padding(24.dp).widthIn(max = 460.dp)) {
            Text("Stapel zudrehen?", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = SeppColors.Gold)
            Spacer(Modifier.height(10.dp))
            Text(
                "Dann wird nicht mehr abgehoben und es gilt Farb- und Stichzwang. " +
                    "Schaffst du die 66 Punkte nicht, bekommt Sepp mindestens 2 Spielpunkte.",
                color = SeppColors.Cream,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GoldButton("Nein", secondary = true, onClick = onNo)
                GoldButton("Ja, zudrehen", onClick = onYes)
            }
        }
    }
}

@Composable
private fun StatusLine(state: HandState, vm: GameViewModel) {
    val text = vm.hint ?: when {
        state.isOver -> ""
        vm.seppThinking -> "Sepp überlegt…"
        state.toAct == Seat.PLAYER && state.announcedSuit != null -> "Spiel König oder ${LocalCardStyle.current.rankName(Rank.OBER)} aus"
        state.toAct == Seat.PLAYER && state.leadCard == null -> "Du spielst aus"
        state.toAct == Seat.PLAYER && state.strictRules -> "Gib zu – Farb- und Stichzwang!"
        state.toAct == Seat.PLAYER -> "Gib zu"
        else -> ""
    }
    Text(
        text,
        color = if (vm.hint != null) SeppColors.Gold else SeppColors.Cream,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
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
                .height(58.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) { buttons() }
    }
}

@Composable
private fun PlayerHand(state: HandState, cardW: Dp, spread: Float, onTap: (Card) -> Unit) {
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
                // Animierte Werte werden nur beim Zeichnen gelesen → kein Neu-Layout bei jedem Bild
                val x = animateDpAsState(cardW * spread * rel, tween(260), label = "x")
                val lift = animateDpAsState(if (isLegal) (-14).dp else 0.dp, tween(200), label = "lift")
                val curve = (abs(rel) * abs(rel) * 3).dp
                val deal = remember(card) { Animatable(1f) }
                LaunchedEffect(card) { deal.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
                PlayingCard(
                    card = card,
                    highlighted = isLegal && state.strictRules && state.leadCard != null,
                    dimmed = myTurn && !isLegal,
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = rel * 5f
                            translationX = x.value.toPx() - deal.value * size.width * 1.4f
                            translationY = (lift.value + curve).toPx() - deal.value * size.height * 2.2f
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
                        fontSize = 13.sp,
                        modifier = Modifier.offset {
                            IntOffset(x.value.roundToPx(), (lift.value - cardW * CARD_RATIO - 2.dp).roundToPx())
                        },
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
                        Text(if (seat == Seat.PLAYER) "Du" else "Sepp", color = SeppColors.Cream, fontSize = 18.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (trick.winner == Seat.PLAYER) "Den Stich hast du gemacht." else "Den Stich hat Sepp gemacht.",
                color = SeppColors.Cream,
                fontSize = 18.sp,
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
        HandEndReason.REACHED_66 -> if (playerWon) "Du hast ${r.winnerPoints} Punkte erreicht." else "Sepp hat ${r.winnerPoints} Punkte erreicht."
        HandEndReason.LAST_TRICK -> if (playerWon) "Keiner hatte 66 – dein letzter Stich entscheidet." else "Keiner hatte 66 – Sepp hat den letzten Stich."
        HandEndReason.CLOSER_FAILED -> if (loser == Seat.PLAYER) "Du hast zugedreht, aber keine 66 geschafft." else "Sepp hat zugedreht und keine 66 geschafft."
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(visible = true, enter = scaleIn() + fadeIn()) {
            WoodPanel(Modifier.padding(24.dp).widthIn(max = 460.dp)) {
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
                        fontSize = 20.sp,
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
                Text(reasonText, color = SeppColors.Cream, textAlign = TextAlign.Center, fontSize = 19.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Punkte: Du ${state.points(Seat.PLAYER)} · Sepp ${state.points(Seat.SEPP)}",
                    color = SeppColors.Cream,
                    fontSize = 19.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "${if (playerWon) "Du schreibst" else "Sepp schreibt"} ${r.gamePoints} ${if (r.gamePoints == 1) "Spielpunkt" else "Spielpunkte"} ab ($tier)",
                    color = SeppColors.Gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Liste: Du ${vm.bummerl.remaining(Seat.PLAYER)} · Sepp ${vm.bummerl.remaining(Seat.SEPP)}",
                    color = SeppColors.Cream,
                    fontSize = 18.sp,
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
            .padding(horizontal = if (small) 18.dp else 26.dp, vertical = if (small) 10.dp else 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (secondary) SeppColors.Gold else SeppColors.Ink,
            fontWeight = FontWeight.Bold,
            fontSize = if (small) 17.sp else 20.sp,
        )
    }
}
