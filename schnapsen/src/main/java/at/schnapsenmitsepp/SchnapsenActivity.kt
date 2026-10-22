package at.schnapsenmitsepp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/** Fertige Activity, die Host-Apps einfach starten können: `SchnapsenMitSepp.start(context)`. */
class SchnapsenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SchnapsenMitSeppGame(onExit = { finish() }) }
    }
}
