package org.vicpoo.gasolineproject

import android.app.UiModeManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.vicpoo.gasolineproject.domain.push.PushEvents
import org.vicpoo.gasolineproject.domain.settings.ObserveThemeModeUseCase
import org.vicpoo.gasolineproject.domain.settings.ThemeMode
import org.vicpoo.gasolineproject.platform.push.PushNotifications
import org.vicpoo.gasolineproject.startup.AppStartup

class MainActivity : ComponentActivity() {

    private val pushEvents: PushEvents by inject()
    private val observeThemeMode: ObserveThemeModeUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Logo al abrir la app (antes de super.onCreate). Se queda hasta que App() ya sabe a qué
        // pantalla entrar: así solo se ve este logo (no uno grande y luego otro chico)
        installSplashScreen().setKeepOnScreenCondition { !AppStartup.isReady.value }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Se abrió tocando una notificación push (al girar la pantalla no se vuelve a abrir)
        if (savedInstanceState == null) openPushTarget(intent)

        // Apariencia elegida en Perfil: barras del sistema y (Android 12+) el splash y el fondo de la ventana
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                observeThemeMode().collect(::applyThemeMode)
            }
        }

        setContent {
            App()
        }
    }

    private fun applyThemeMode(mode: ThemeMode) {
        // Íconos de la barra de estado y de navegación: oscuros sobre claro, claros sobre oscuro
        val transparent = Color.TRANSPARENT
        val barStyle = when (mode) {
            ThemeMode.LIGHT -> SystemBarStyle.light(transparent, transparent)
            ThemeMode.DARK -> SystemBarStyle.dark(transparent)
            ThemeMode.SYSTEM -> SystemBarStyle.auto(transparent, transparent)
        }
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)

        // Android 12+: el sistema recuerda el modo de la app, así el splash también lo sigue.
        // La Activity NO se recrea (configChanges="uiMode" en el Manifest): Compose hace la transición suave.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mode != appliedNightMode) {
            appliedNightMode = mode
            val nightMode = when (mode) {
                ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
                ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            }
            getSystemService(UiModeManager::class.java)?.setApplicationNightMode(nightMode)
        }
    }

    private companion object {
        /** Último modo mandado al sistema en este proceso (evita recrear la Activity en bucle). */
        var appliedNightMode: ThemeMode? = null
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
