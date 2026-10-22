package at.schnapsenmitsepp

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import at.schnapsenmitsepp.ui.GameScreen
import at.schnapsenmitsepp.ui.GameViewModel
import at.schnapsenmitsepp.ui.ListScreen
import at.schnapsenmitsepp.ui.MenuScreen
import at.schnapsenmitsepp.ui.RulesScreen
import at.schnapsenmitsepp.ui.feltBackground
import at.schnapsenmitsepp.ui.theme.SeppTheme

/**
 * Öffentlicher Einstieg für Host-Apps.
 *
 * - Als eigene Activity: `SchnapsenMitSepp.start(context)`
 * - Als Composable (z.B. in einer NavHost-Route): `SchnapsenMitSeppGame(onExit = { navController.popBackStack() })`
 */
object SchnapsenMitSepp {
    fun intent(context: Context): Intent = Intent(context, SchnapsenActivity::class.java)

    fun start(context: Context) {
        val i = intent(context)
        if (context !is android.app.Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }
}

private enum class Screen { MENU, GAME, LIST, RULES }

/**
 * Das komplette Spiel inkl. Menü, Liste und Regeln. Bringt sein eigenes Theme mit.
 *
 * @param onExit wird aufgerufen, wenn im Menü "Zurück" gedrückt wird. `null` = kein Zurück-Knopf
 *   (z.B. wenn das Spiel die ganze App ist).
 */
@Composable
fun SchnapsenMitSeppGame(modifier: Modifier = Modifier, onExit: (() -> Unit)? = null) {
    val vm: GameViewModel = viewModel()
    var screen by rememberSaveable { mutableStateOf(Screen.MENU) }

    BackHandler(enabled = screen != Screen.MENU || onExit != null) {
        when (screen) {
            Screen.MENU -> onExit?.invoke()
            Screen.LIST, Screen.RULES -> screen = if (vm.hand != null && vm.hand?.isOver == false) Screen.GAME else Screen.MENU
            Screen.GAME -> screen = Screen.MENU
        }
    }

    // Für Tablets und ältere Spieler: alles größer. Auf Tablets wird die ganze Oberfläche gezoomt,
    // die Schrift überall zusätzlich vergrößert – die Systemeinstellung für Schriftgröße bleibt wirksam.
    val base = LocalDensity.current
    val tablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val zoomed = Density(
        density = base.density * if (tablet) 1.3f else 1f,
        fontScale = base.fontScale * if (tablet) 1.15f else 1.2f,
    )

    SeppTheme {
        CompositionLocalProvider(LocalDensity provides zoomed) {
        Box(modifier.fillMaxSize().feltBackground().windowInsetsPadding(WindowInsets.safeDrawing)) {
            when (screen) {
                Screen.MENU -> MenuScreen(
                    vm = vm,
                    onPlay = { screen = Screen.GAME },
                    onNewBummerl = {
                        vm.newBummerl()
                        screen = Screen.GAME
                    },
                    onList = { screen = Screen.LIST },
                    onRules = { screen = Screen.RULES },
                    onExit = onExit,
                )
                Screen.GAME -> GameScreen(vm, onBack = { screen = Screen.MENU }, onShowList = { screen = Screen.LIST })
                Screen.LIST -> ListScreen(vm, onBack = { screen = if (vm.hand != null) Screen.GAME else Screen.MENU })
                Screen.RULES -> RulesScreen(onBack = { screen = Screen.MENU })
            }
        }
        }
    }
}
