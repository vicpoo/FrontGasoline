package org.vicpoo.gasolineproject

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.ui.common.adaptive.ProvideWindowWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import org.vicpoo.gasolineproject.startup.AppStartup
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.common.theme.ThemeViewModel
import org.vicpoo.gasolineproject.ui.common.theme.isDarkTheme
import org.vicpoo.gasolineproject.navigation.AppNavHost
import org.vicpoo.gasolineproject.navigation.StartViewModel

/** Tiempo mínimo que se ve el logo al abrir la app. */
private const val SPLASH_MIN_MILLIS = 800L

/** Raíz de la app (Koin ya debe estar iniciado: ver GasolineApplication / MainViewController). */
@Composable
fun App() {
    // Cargador de imágenes (fotos de vehículos desde MinIO o desde el teléfono)
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }

    // Apariencia: siempre arranca igual que el teléfono. El botón de Perfil la cambia mientras la app está abierta.
    val themeViewModel = koinViewModel<ThemeViewModel>()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

    // Si el teléfono cambia de claro a oscuro (o al revés) la app lo vuelve a seguir, aunque se haya usado el botón
    val systemDark = isSystemInDarkTheme()
    var lastSystemDark by remember { mutableStateOf(systemDark) }
    LaunchedEffect(systemDark) {
        if (systemDark != lastSystemDark) {
            lastSystemDark = systemDark
            themeViewModel.followSystem()
        }
    }

    AppTheme(darkTheme = themeMode.isDarkTheme()) {
        ProvideWindowWidth {
            val startViewModel = koinViewModel<StartViewModel>()
            val startStatus by startViewModel.startStatus.collectAsStateWithLifecycle()
            val isLoggedIn by startViewModel.isLoggedIn.collectAsStateWithLifecycle()

            // Logo de inicio (como Airbnb): es el del sistema (Android). Se queda en pantalla
            // al menos un momento y mientras se decide a qué pantalla entrar; aquí solo se
            // pinta el fondo para que no aparezca un segundo logo.
            // rememberSaveable: al girar la pantalla no se vuelve a esperar
            var minTimeDone by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                if (minTimeDone) return@LaunchedEffect
                delay(SPLASH_MIN_MILLIS)
                minTimeDone = true
            }

            val status = startStatus
            // También espera la apariencia guardada para no pintar un instante con el modo equivocado
            if (status == null || !minTimeDone || themeMode == null) {
                Box(Modifier.fillMaxSize().background(AppTheme.colors.background))
            } else {
                LaunchedEffect(Unit) { AppStartup.markReady() }
                // Fondo del tema detrás de TODAS las pantallas: lo que no pinta su propio fondo
                // ya no deja ver el fondo de la ventana (que se quedaba con el color anterior)
                Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
                    AppNavHost(startStatus = status, isLoggedIn = isLoggedIn)
                }
            }
        }
    }
}
