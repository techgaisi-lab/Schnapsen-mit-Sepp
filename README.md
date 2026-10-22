# Schnapsen mit Sepp

Zweier-Schnapsen nach österreichischen Wirtshausregeln (angelehnt an „Schnapsen mit Karl“)
gegen den Computergegner **Sepp** – mit französischem Blatt (Herz, Karo, Pik, Kreuz), Bummerl, Liste,
20/40, Atout tauschen und Zudrehen. Große Schrift und Bedienelemente, gedacht für Tablets.

Das Projekt ist so aufgebaut, dass es **als eigene App** läuft und **in eine andere Android-App
eingebunden** werden kann.

## Aufbau

| Modul | Inhalt |
|---|---|
| `:schnapsen` | Android-Library: Spiel-Engine, Bot, komplette Compose-Oberfläche, Activity |
| `:app` | Schlanke eigenständige App, die nur `SchnapsenMitSeppGame()` anzeigt |

```
schnapsen/src/main/java/at/schnapsenmitsepp/
├── engine/   Regeln (Rules), Spielzustand (HandState), Bummerl/Liste – reines Kotlin, ohne Android
├── bot/      Sepp: Heuristik, Monte-Carlo-Suche, Alpha-Beta für das Endspiel
├── ui/       Compose: Menü, Spieltisch, Liste, Regeln, gezeichnete Karten & Sepp
├── data/     Speicherung von Liste, Statistik und Einstellungen (SharedPreferences)
├── SchnapsenMitSepp.kt   öffentliche API
└── SchnapsenActivity.kt  fertige Activity für Host-Apps
```

Alle Grafiken (französische Karten, Kartenrücken mit Edelweiß, Sepp, Filztisch) sind als Vektor-Zeichnungen in
Compose umgesetzt – keine Bilddateien, in jeder Auflösung scharf.

## Schwierigkeitsstufen

| Stufe | Spielweise |
|---|---|
| Leicht – *Sepp nach dem dritten Spritzer* | spielt oft zufällig, dreht nie zu |
| Mittel – *Sepp beim Kirtag* | solide Wirtshaus-Regeln (Heuristik) |
| Schwer – *Sepp im Stammwirtshaus* | merkt sich Karten, rechnet das Endspiel und das Zudrehen durch |
| Meister – *Sepp, der Kaffeehaus-Profi* | simuliert jeden Zug über viele mögliche Kartenverteilungen |

Sepp spielt **fair**: er sieht nur, was auch ein Mensch am Tisch sieht (eigene Karten, gespielte
Karten, Atout, gezeigte Hochzeiten).

## Regeln (Kurzfassung)

- 20 Karten: Ass 11 · Zehner 10 · König 4 · Dame/Ober 3 · Bube/Unter 2 (zusammen 120 Augen).
- Talon offen: kein Farb-/Stichzwang. Talon zu oder leer: höher stechen, sonst Farbe zugeben,
  sonst mit Atout stechen, sonst abwerfen (Farbzwang geht vor Stichzwang).
- 20/40 werden automatisch angesagt, wenn man König oder Dame mit Partner ausspielt; die Augen zählen **sofort**.
- Wer **66 Augen** hat, muss **selbst „Aus!“ melden** (beim Ausspielen oder gleich nach einer Ansage).
  Falsch ausgemeldet = verloren (Gegner bekommt mindestens 2 Punkte), vergessen = es wird weitergespielt.
- Atout-Bube tauschen und Zudrehen beim Ausspielen, solange der Talon offen ist.
- Wertung beim Ausmelden: 3 Punkte (Gegner ohne Stich), 2 (Gegner ≤ 32 Augen), 1 (Gegner ≥ 33).
- Zudrehen: Die Augen des Gegners werden eingefroren. Schafft der Zudreher die 66 nicht, bekommt der
  Gegner die Punkte, die der Zudreher bekommen hätte – mindestens 2.
- Meldet niemand aus, gewinnt der letzte Stich (nicht nach dem Zudrehen).
- Bummerl: Wer zuerst 7 Spielpunkte hat, gewinnt. Auf der Liste wird von 7 heruntergeschrieben;
  hat der Verlierer keinen Punkt, bekommt er zwei Bummerl.
- Den letzten Stich darf man sich ansehen.

## Als eigene App bauen

In Android Studio öffnen und die Run-Konfiguration `app` starten, oder:

```bash
./gradlew :app:assembleDebug
```

## In eine andere App einbinden

### 1. Modul ins Projekt holen

Am einfachsten als Git-Submodule im Host-Projekt:

```bash
git submodule add https://github.com/techgaisi-lab/Schnapsen-mit-Sepp.git schnapsen-mit-sepp
```

In der `settings.gradle.kts` der Host-App:

```kotlin
include(":schnapsen")
project(":schnapsen").projectDir = file("schnapsen-mit-sepp/schnapsen")
```

In der `build.gradle.kts` des App-Moduls:

```kotlin
dependencies {
    implementation(project(":schnapsen"))
}
```

Die Host-App braucht im Version-Catalog die Einträge, die `schnapsen/build.gradle.kts` verwendet
(`android-library`-Plugin, `kotlin-compose`, Compose-BOM, `activity-compose`,
`lifecycle-viewmodel-compose`, `core-ktx`, `junit`) – einfach aus `gradle/libs.versions.toml`
übernehmen, sie entsprechen deinen anderen Projekten (AGP 9.4.1, Kotlin 2.4.21, Compose-BOM 2026.09.00).

### 2. Spiel starten

**Variante A – eigene Activity (ein Einzeiler):**

```kotlin
import at.schnapsenmitsepp.SchnapsenMitSepp

Button(onClick = { SchnapsenMitSepp.start(context) }) { Text("Schnapsen mit Sepp") }
```

**Variante B – als Composable in deiner Navigation:**

```kotlin
import at.schnapsenmitsepp.SchnapsenMitSeppGame

composable("schnapsen") {
    SchnapsenMitSeppGame(onExit = { navController.popBackStack() })
}
```

Das Spiel bringt sein eigenes Theme mit und sieht in jeder Host-App gleich aus. Alle Ressourcen der
Library haben das Präfix `sms_`, damit es keine Namenskonflikte gibt.

## Tests

```bash
./gradlew :schnapsen:testDebugUnitTest
```

Die Tests prüfen Regeln (Farbzwang, Stechen, 20/40, Ausmelden, Tauschen, Zudrehen, Wertung, Bummerl) und lassen
alle Bot-Stufen komplette Spiele gegeneinander spielen.
