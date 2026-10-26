package org.vicpoo.gasolineproject

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.koin.android.ext.android.inject
import org.vicpoo.gasolineproject.core.push.PushEvents
import org.vicpoo.gasolineproject.core.push.PushNotifications
import org.vicpoo.gasolineproject.core.startup.AppStartup

class MainActivity : ComponentActivity() {

    private val pushEvents: PushEvents by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Logo al abrir la app (antes de super.onCreate). Se queda hasta que App() ya sabe a qué
        // pantalla entrar: así solo se ve este logo (no uno grande y luego otro chico)
        installSplashScreen().setKeepOnScreenCondition { !AppStartup.isReady.value }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Se abrió tocando una notificación push (al girar la pantalla no se vuelve a abrir)
        if (savedInstanceState == null) openPushTarget(intent)

        setContent {
            App()
        }
    }

    /** La app ya estaba abierta (launchMode singleTop) y se tocó una notificación. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openPushTarget(intent)
    }

    private fun openPushTarget(intent: Intent?) {
        PushNotifications.targetFrom(intent)?.let(pushEvents::open)
    }
}
