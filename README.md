# Schnapsen mit Sepp

Zweier-Schnapsen nach österreichischen Regeln gegen den Computergegner **Sepp** – mit doppeldeutschen
Karten (Herz, Schell, Eichel, Laub), Bummerl, Liste, 20/40, Atout tauschen und Zudrehen.

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

Alle Grafiken (Karten, Kartenrücken mit Edelweiß, Sepp, Filztisch) sind als Vektor-Zeichnungen in
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

- 20 Karten, Ass 11 · Zehner 10 · König 4 · Ober 3 · Unter 2. Wer zuerst 66 hat und „Aus!“ sagt, gewinnt.
- Talon offen: kein Farb-/Stichzwang. Talon zu oder leer: Farbe bekennen, überstechen, sonst Atout.
- 20/40 beim Ausspielen ansagen; zählt erst ab dem ersten eigenen Stich.
- Atout-Unter tauschen und Zudrehen beim Ausspielen, solange mindestens 4 Karten im Talon liegen.
- Wertung: 1 Punkt, 2 (Schneider, Gegner < 33), 3 (Schwarz, Gegner ohne Stich).
  Beim Zudrehen zählt der Stand des Gegners zum Zeitpunkt des Zudrehens; schafft der Zudreher die 66
  nicht, bekommt der Gegner 2 bzw. 3 Punkte.
- Liste: Jeder startet bei 7 und schreibt herunter. Wer auf 0 ist, gewinnt das Bummerl; ist der
  Verlierer noch auf 7, ist er Schneider und bekommt zwei Bummerl.

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

Die Tests prüfen Regeln (Farbzwang, Stechen, 20/40, Tauschen, Zudrehen, Wertung, Bummerl) und lassen
alle Bot-Stufen komplette Spiele gegeneinander spielen.
