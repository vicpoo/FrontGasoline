package org.vicpoo.gasolineproject

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.ui.adaptive.ProvideWindowWidth
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.navigation.AppNavHost
import org.vicpoo.gasolineproject.navigation.StartViewModel

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
            val startRoute by startViewModel.startRoute.collectAsStateWithLifecycle()
            val isLoggedIn by startViewModel.isLoggedIn.collectAsStateWithLifecycle()

            when (val route = startRoute) {
                null -> LoadingView()
                else -> AppNavHost(startRoute = route, isLoggedIn = isLoggedIn)
            }
        }
    }
}
