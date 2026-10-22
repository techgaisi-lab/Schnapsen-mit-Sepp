package at.schnapsenmitsepp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
    Column(
        Modifier
            .fillMaxSize()
            .feltBackground()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (onExit != null) {
            Text(
                "‹ Zurück",
                color = SeppColors.Cream,
                modifier = Modifier.align(Alignment.Start).clickable(onClick = onExit).padding(6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        // Vier Asse als Fächer
        Box(Modifier.height(150.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            val suits = listOf(Suit.EICHEL, Suit.LAUB, Suit.HERZ, Suit.SCHELLE)
            suits.forEachIndexed { i, suit ->
                val rel = i - 1.5f
                PlayingCard(
                    Card(suit, Rank.ASS),
                    Modifier
                        .offset(x = (rel * 46).dp, y = (kotlin.math.abs(rel) * 8).dp)
                        .graphicsLayer {
                            rotationZ = rel * 12f
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1.2f)
                        }
                        .width(84.dp)
                        .aspectRatio(1f / CARD_RATIO),
                    elevation = 8f,
                )
            }
        }
        Spacer(Modifier.height(34.dp))
        Text(
            "Schnapsen",
            style = MaterialTheme.typography.displayLarge,
            color = SeppColors.Gold,
        )
        Text("mit Sepp", style = MaterialTheme.typography.headlineMedium, color = SeppColors.Cream)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            SeppAvatar(SeppMood.HAPPY, Modifier.size(76.dp))
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .background(SeppColors.Cream, RoundedCornerShape(topStart = 2.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("Servus! Spü ma a Bummerl?", color = SeppColors.Ink)
            }
        }
        Spacer(Modifier.height(18.dp))

        Text("Wie stark soll Sepp spielen?", color = SeppColors.Cream, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.widthIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "★".repeat(d.ordinal + 1).padEnd(4, '☆'),
                        color = SeppColors.Gold,
                        fontSize = 16.sp,
                        modifier = Modifier.width(72.dp),
                    )
                    Column {
                        Text(d.title, color = SeppColors.Cream, fontWeight = FontWeight.Bold)
                        Text(d.subtitle, color = SeppColors.Cream.copy(alpha = 0.75f), fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        val running = vm.bummerl.entries.isNotEmpty() && !vm.bummerl.isFinished || vm.hand?.isOver == false
        GoldButton(if (running) "Weiterspielen" else "Bummerl starten", Modifier.widthIn(min = 240.dp), onClick = onPlay)
        Spacer(Modifier.height(10.dp))
        if (running) {
            GoldButton("Neues Bummerl", Modifier.widthIn(min = 240.dp), secondary = true, onClick = onNewBummerl)
            Spacer(Modifier.height(10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoldButton("Liste", secondary = true, onClick = onList)
            GoldButton("Regeln", secondary = true, onClick = onRules)
        }
        Spacer(Modifier.height(20.dp))

        Column(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            SettingRow("Automatisch ausmelden", "Bei 66 Augen sagt das Spiel für dich \"Aus!\"", vm.settings.autoClaim) {
                vm.updateSettings(vm.settings.copy(autoClaim = it))
            }
            SettingRow("Augen mitzählen", "Zeigt die Augen beider Spieler an", vm.settings.showPoints) {
                vm.updateSettings(vm.settings.copy(showPoints = it))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = SeppColors.Cream, fontWeight = FontWeight.Bold)
            Text(subtitle, color = SeppColors.Cream.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = SeppColors.Gold, checkedThumbColor = SeppColors.Ink),
        )
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
        Text("‹ Zurück", color = SeppColors.Cream, modifier = Modifier.clickable(onClick = onBack).padding(6.dp))
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
                Text(title, style = MaterialTheme.typography.titleMedium, color = SeppColors.Burgundy)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = SeppColors.Ink)
            }
        }
    }
}

private val RULES = listOf(
    "Die Karten" to "Gespielt wird mit 20 doppeldeutschen Karten in den Farben Herz, Schell, Eichel und Laub. " +
        "Werte: Ass 11, Zehner 10, König 4, Ober 3, Unter 2 Augen.",
    "Ziel" to "Wer zuerst 66 Augen hat und \"Aus!\" sagt, gewinnt das Spiel. Die Augen kommen aus den Stichen und aus angesagten Hochzeiten.",
    "Geben" to "Jeder bekommt 5 Karten (3 + 2). Dazwischen wird die Atoutkarte (Trumpf) aufgedeckt und quer unter den Talon gelegt. " +
        "Die Vorhand spielt aus. Nach jedem Stich zieht zuerst der Gewinner, dann der Verlierer eine Karte vom Talon.",
    "Solange der Talon offen ist" to "Es gibt keinen Farb- oder Stichzwang – du darfst jede Karte zugeben.",
    "Farb- und Stichzwang" to "Ist der Talon aufgebraucht oder zugedreht, musst du Farbe bekennen und wenn möglich höher stechen. " +
        "Hast du die Farbe nicht, musst du mit Atout stechen.",
    "20 und 40" to "Hast du König und Ober einer Farbe, kannst du beim Ausspielen 20 ansagen – in der Atoutfarbe 40. " +
        "Eine der beiden Karten musst du dann ausspielen. Die Punkte zählen erst, wenn du mindestens einen Stich hast.",
    "Atout tauschen" to "Wer den Atout-Unter hat, darf ihn beim Ausspielen gegen die aufgedeckte Atoutkarte tauschen, " +
        "solange der Talon offen ist und noch mindestens 4 Karten darin liegen.",
    "Zudrehen" to "Beim Ausspielen kannst du den Talon zudrehen (mind. 4 Karten im Talon). Ab dann wird nicht mehr nachgezogen und es gilt Farb- und Stichzwang. " +
        "Wer zudreht, muss 66 erreichen – sonst bekommt der Gegner 2 Punkte, bzw. 3, wenn er beim Zudrehen noch keinen Stich hatte. " +
        "Gewinnt der Zudreher, zählt für die Wertung, wie der Gegner beim Zudrehen dagestanden ist.",
    "Wertung" to "1 Punkt: Der Verlierer hat mindestens 33 Augen.\n2 Punkte (Schneider): Der Verlierer hat weniger als 33 Augen.\n" +
        "3 Punkte (Schwarz): Der Verlierer hat keinen einzigen Stich.\n" +
        "Sagt niemand \"Aus!\", gewinnt der letzte Stich. Wer falsch ausmeldet, verliert 2 Punkte (3, wenn der Gegner noch keinen Stich hat).",
    "Bummerl und Liste" to "Auf der Liste beginnt jeder bei 7 und schreibt seine gewonnenen Punkte herunter. Wer zuerst auf 0 ist, gewinnt – " +
        "der andere bekommt ein Bummerl. Steht der Verlierer noch auf 7, ist er Schneider und bekommt zwei Bummerl.",
)
