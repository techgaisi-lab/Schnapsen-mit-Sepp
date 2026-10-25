package at.schnapsenmitsepp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.schnapsenmitsepp.bot.Difficulty
import at.schnapsenmitsepp.engine.Bummerl
import at.schnapsenmitsepp.engine.Card
import at.schnapsenmitsepp.engine.Rank
import at.schnapsenmitsepp.engine.Seat
import at.schnapsenmitsepp.engine.Suit
import at.schnapsenmitsepp.ui.cards.CARD_RATIO
import at.schnapsenmitsepp.ui.cards.PlayingCard
import at.schnapsenmitsepp.ui.theme.SeppColors

@Composable
internal fun MenuScreen(
    vm: GameViewModel,
    onPlay: () -> Unit,
    onNewBummerl: () -> Unit,
    onList: () -> Unit,
    onRules: () -> Unit,
    onExit: (() -> Unit)?,
) {
    BoxWithConstraints(Modifier.fillMaxSize().feltBackground()) {
        val compact = maxHeight < 560.dp
        if (maxWidth > maxHeight * 1.15f) {
            // Querformat: links Titel und Sepp, rechts Auswahl und Knöpfe
            Row(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    MenuHeader(onExit, compact = compact)
                    Spacer(Modifier.height(14.dp))
                    MenuButtons(vm, onPlay, onNewBummerl, onList, onRules, compact = compact)
                }
                Spacer(Modifier.width(20.dp))
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DifficultyPicker(vm)
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MenuHeader(onExit)
                Spacer(Modifier.height(18.dp))
                DifficultyPicker(vm)
                Spacer(Modifier.height(20.dp))
                MenuButtons(vm, onPlay, onNewBummerl, onList, onRules)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ColumnScope.MenuHeader(onExit: (() -> Unit)?, compact: Boolean = false) {
    if (onExit != null) {
        Text(
            "‹ Zurück",
            color = SeppColors.Cream,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.Start).clickable(onClick = onExit).padding(6.dp),
        )
    }
    Spacer(Modifier.height(8.dp))
    // Vier Asse als Fächer
    val cardW = if (compact) 60.dp else 84.dp
    Box(Modifier.height(cardW * 1.8f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        val suits = listOf(Suit.EICHEL, Suit.LAUB, Suit.HERZ, Suit.SCHELLE)
        suits.forEachIndexed { i, suit ->
            val rel = i - 1.5f
            PlayingCard(
                Card(suit, Rank.ASS),
                Modifier
                    .offset(x = cardW * 0.55f * rel, y = (kotlin.math.abs(rel) * 8).dp)
                    .graphicsLayer {
                        rotationZ = rel * 12f
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1.2f)
                    }
                    .width(cardW)
                    .aspectRatio(1f / CARD_RATIO),
                elevation = 8f,
            )
        }
    }
    Spacer(Modifier.height(if (compact) 20.dp else 34.dp))
    Text(
        "Schnapsen",
        style = if (compact) MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp) else MaterialTheme.typography.displayLarge,
        color = SeppColors.Gold,
    )
    Text(
        "mit Sepp",
        style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
        color = SeppColors.Cream,
    )
    if (compact) return
    Spacer(Modifier.height(12.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        SeppAvatar(SeppMood.HAPPY, Modifier.size(76.dp))
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .background(SeppColors.Cream, RoundedCornerShape(topStart = 2.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text("Servus! Spü ma a Bummerl?", color = SeppColors.Ink, fontSize = 19.sp)
        }
    }
}

@Composable
private fun ColumnScope.DifficultyPicker(vm: GameViewModel) {
    Text("Wie stark soll Sepp spielen?", color = SeppColors.Cream, style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp))
    Spacer(Modifier.height(10.dp))
    Column(Modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Difficulty.entries.forEach { d ->
            val selected = vm.settings.difficulty == d
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (selected) SeppColors.Gold.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.22f),
                        RoundedCornerShape(12.dp),
                    )
                    .border(if (selected) 2.dp else 1.dp, if (selected) SeppColors.Gold else SeppColors.GoldDark.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { vm.setDifficulty(d) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "★".repeat(d.ordinal + 1).padEnd(4, '☆'),
                    color = SeppColors.Gold,
                    fontSize = 20.sp,
                    modifier = Modifier.width(92.dp),
                )
                Column {
                    Text(d.title, color = SeppColors.Cream, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text(d.subtitle, color = SeppColors.Cream.copy(alpha = 0.8f), fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.MenuButtons(
    vm: GameViewModel,
    onPlay: () -> Unit,
    onNewBummerl: () -> Unit,
    onList: () -> Unit,
    onRules: () -> Unit,
    compact: Boolean = false,
) {
    val running = vm.bummerl.entries.isNotEmpty() && !vm.bummerl.isFinished || vm.hand?.isOver == false
    GoldButton(if (running) "Weiterspielen" else "Bummerl starten", Modifier.widthIn(min = 240.dp), onClick = onPlay)
    Spacer(Modifier.height(10.dp))
    if (compact) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (running) GoldButton("Neues Bummerl", small = true, secondary = true, onClick = onNewBummerl)
            GoldButton("Liste", small = true, secondary = true, onClick = onList)
            GoldButton("Regeln", small = true, secondary = true, onClick = onRules)
        }
        return
    }
    if (running) {
        GoldButton("Neues Bummerl", Modifier.widthIn(min = 240.dp), secondary = true, onClick = onNewBummerl)
        Spacer(Modifier.height(10.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GoldButton("Liste", secondary = true, onClick = onList)
        GoldButton("Regeln", secondary = true, onClick = onRules)
    }
}

private val Handwriting = TextStyle(fontFamily = FontFamily.Cursive, color = Color(0xFF1C2E6B))

/** Die Liste wie am Wirtshaustisch: kariertes Papier, runtergeschriebene Punkte, Bummerl als Kreise. */
@Composable
internal fun ListScreen(vm: GameViewModel, onBack: () -> Unit) {
    val b: Bummerl = vm.bummerl
    val stats = vm.stats
    Column(Modifier.fillMaxSize().feltBackground().padding(16.dp)) {
        Text(
            "‹ Zurück",
            color = SeppColors.Cream,
            fontSize = 20.sp,
            modifier = Modifier.clickable(onClick = onBack).padding(6.dp),
        )
        Spacer(Modifier.height(6.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SeppColors.Paper, RoundedCornerShape(6.dp))
                .verticalScroll(rememberScrollState()),
        ) {
            Box(Modifier.fillMaxWidth()) {
                PaperLines(Modifier.matchParentSize())
                Column(Modifier.padding(start = 40.dp, end = 16.dp, top = 12.dp, bottom = 24.dp)) {
                    Text("Liste", style = Handwriting.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("Du", style = Handwriting.copy(fontSize = 26.sp), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("Sepp", style = Handwriting.copy(fontSize = 26.sp), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                    // Bummerl-Strichliste
                    Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            BummerlDots(minOf(stats.bummerlPlayer, 8), 18.dp, Color(0xFF1C2E6B), SeppColors.Paper)
                        }
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            BummerlDots(minOf(stats.bummerlSepp, 8), 18.dp, Color(0xFF1C2E6B), SeppColors.Paper)
                        }
                    }
                    Text(
                        "Bummerl: ${stats.bummerlPlayer} : ${stats.bummerlSepp}",
                        style = Handwriting.copy(fontSize = 18.sp),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    val totals = listOf(Bummerl.START to Bummerl.START) + b.runningTotals()
                    // Wie auf der echten Liste: nur neue Stände anschreiben, alte durchstreichen.
                    val changedP = totals.indices.filter { it == 0 || totals[it].first != totals[it - 1].first }
                    val changedS = totals.indices.filter { it == 0 || totals[it].second != totals[it - 1].second }
                    totals.forEachIndexed { i, (p, s) ->
                        Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
                            ScoreCell(p.takeIf { i in changedP }, crossed = i != changedP.last(), modifier = Modifier.weight(1f))
                            ScoreCell(s.takeIf { i in changedS }, crossed = i != changedS.last(), modifier = Modifier.weight(1f))
                        }
                    }
                    if (b.isFinished) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (b.winner == Seat.PLAYER) "Bummerl für Sepp!" else "Bummerl für dich!",
                            style = Handwriting.copy(fontSize = 24.sp, color = SeppColors.Margin),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Spiele gewonnen: ${stats.handsWon}   verloren: ${stats.handsLost}",
                        style = Handwriting.copy(fontSize = 17.sp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Statistik löschen",
                        style = Handwriting.copy(fontSize = 15.sp, color = SeppColors.Margin, textDecoration = TextDecoration.Underline),
                        modifier = Modifier.clickable { vm.resetStats() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreCell(value: Int?, crossed: Boolean, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (value != null) {
            Text(
                value.toString(),
                style = Handwriting.copy(
                    fontSize = 26.sp,
                    textDecoration = if (crossed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (crossed) Color(0xFF1C2E6B).copy(alpha = 0.55f) else Color(0xFF1C2E6B),
                ),
            )
        }
    }
}

@Composable
private fun PaperLines(modifier: Modifier) {
    Canvas(modifier) {
        val step = 34.dp.toPx()
        var y = step * 2.2f
        while (y < size.height) {
            drawLine(SeppColors.PaperLine.copy(alpha = 0.6f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            y += step
        }
        val x = 30.dp.toPx()
        drawLine(SeppColors.Margin.copy(alpha = 0.7f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
        drawLine(SeppColors.PaperLine, Offset(size.width / 2 + x / 2, step * 2.2f), Offset(size.width / 2 + x / 2, size.height), strokeWidth = 1.dp.toPx())
    }
}

@Composable
internal fun RulesScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().feltBackground().padding(16.dp)) {
        Text("‹ Zurück", color = SeppColors.Cream, fontSize = 20.sp, modifier = Modifier.clickable(onClick = onBack).padding(6.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SeppColors.Paper, RoundedCornerShape(6.dp))
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
        ) {
            Text("Spielregeln", style = MaterialTheme.typography.headlineMedium, color = SeppColors.Ink)
            RULES.forEach { (title, body) ->
                Spacer(Modifier.height(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp), color = SeppColors.Burgundy)
                Text(body, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp, lineHeight = 25.sp), color = SeppColors.Ink)
            }
        }
    }
}

private val RULES = listOf(
    "Die Karten" to "Gespielt wird mit 20 französischen Karten: Ass (11 Punkte), Zehner (10), König (4), Dame (3) und Bube (2) " +
        "in den Farben Herz, Karo, Pik und Kreuz. Zusammen sind das 120 Punkte.",
    "Ziel" to "Wer zuerst 66 Punkte aus Stichen und Ansagen hat, gewinnt das Spiel – es ist dann sofort automatisch aus. " +
        "Wer zuerst 7 Spielpunkte hat, gewinnt das Bummerl.",
    "Geben" to "Die Spieler geben abwechselnd. Jeder bekommt 5 Karten, eine Karte wird aufgedeckt und bestimmt die Atoutfarbe (Trumpf). " +
        "Der Stapel liegt quer darüber, die Atoutkarte zählt als unterste Karte des Stapels. Wer nicht gegeben hat, spielt aus.",
    "Stechen" to "Ein Stich geht an die höhere Karte derselben Farbe oder an Atout. Bei verschiedenen Farben ohne Atout gewinnt die ausgespielte Karte. " +
        "Wer den Stich macht, hebt zuerst vom Stapel ab, dann der Gegner, und spielt zum nächsten Stich aus.",
    "Solange der Stapel offen ist" to "Es gibt weder Farb- noch Stichzwang – du darfst jede Karte zugeben.",
    "Farb- und Stichzwang" to "Ist der Stapel aufgebraucht oder zugedreht, musst du: mit einer höheren Karte der angespielten Farbe stechen; " +
        "geht das nicht, eine niedrigere Karte der Farbe zugeben; hast du die Farbe nicht, mit Atout stechen; sonst beliebig abwerfen. " +
        "Farbzwang geht vor Stichzwang – mit Atout stechen, obwohl du die Farbe hast, ist verboten.",
    "20 und 40" to "Spielst du König oder Dame aus und hast den Partner derselben Farbe in der Hand, wird automatisch angesagt: " +
        "20 Punkte, in der Atoutfarbe 40. Die Punkte werden sofort gutgeschrieben.",
    "Atout tauschen" to "Wer den Atout-Buben hat, darf ihn beim Ausspielen gegen die offene Atoutkarte unter dem Stapel tauschen – " +
        "solange der Stapel offen ist.",
    "Zudrehen" to "Wenn du ausspielst, tippst du einfach auf den Stapel, um zuzudrehen. Dann wird nicht mehr abgehoben, es gilt Farb- und Stichzwang " +
        "und Atout tauschen geht nicht mehr. Die Punkte des Gegners werden eingefroren: Hatte er beim Zudrehen keinen Stich, bekommst du 3 Spielpunkte, " +
        "bei 32 oder weniger Punkten 2, sonst 1. Schaffst du die 66 nicht, bekommt der Gegner die Spielpunkte, die du bekommen hättest – mindestens 2.",
    "Spielende und Wertung" to "Sobald jemand 66 Punkte hat, ist das Spiel automatisch vorbei. Der Gewinner schreibt ab:\n" +
        "3 Spielpunkte: der Gegner hat keinen Stich.\n2 Spielpunkte: der Gegner hat 32 Punkte oder weniger.\n1 Spielpunkt: der Gegner hat 33 oder mehr.",
    "Letzter Stich" to "Hat niemand 66 Punkte, wenn alle Karten gespielt sind, gewinnt der letzte Stich das Spiel. " +
        "Wurde zugedreht, gilt das nicht – dann hat der Zudreher verloren.",
    "Bummerl und Liste" to "Wer zuerst 7 Spielpunkte hat, gewinnt; der Verlierer hat das Bummerl. Auf der Liste wird traditionell von 7 heruntergeschrieben. " +
        "Hat der Verlierer gar keinen Spielpunkt gemacht, bekommt er zwei Bummerl.",
)
