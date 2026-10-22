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
import org.vicpoo.gasolineproject.core.ui.adaptive.ProvideWindowWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import org.vicpoo.gasolineproject.core.startup.AppStartup
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
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

    AppTheme {
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
            if (status == null || !minTimeDone) {
                Box(Modifier.fillMaxSize().background(AppTheme.colors.background))
            } else {
                LaunchedEffect(Unit) { AppStartup.markReady() }
                AppNavHost(startStatus = status, isLoggedIn = isLoggedIn)
            }
        }
    }
}
