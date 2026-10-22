package org.vicpoo.gasolineproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.vicpoo.gasolineproject.core.startup.AppStartup

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Logo al abrir la app (antes de super.onCreate). Se queda hasta que App() ya sabe a qué
        // pantalla entrar: así solo se ve este logo (no uno grande y luego otro chico)
        installSplashScreen().setKeepOnScreenCondition { !AppStartup.isReady.value }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}
