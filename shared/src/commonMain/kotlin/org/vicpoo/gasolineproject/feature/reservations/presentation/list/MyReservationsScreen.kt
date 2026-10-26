package org.vicpoo.gasolineproject.feature.reservations.presentation.list

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.requests_date
import gasolineproject.shared.generated.resources.requests_empty
import gasolineproject.shared.generated.resources.requests_offline
import gasolineproject.shared.generated.resources.requests_reason
import gasolineproject.shared.generated.resources.requests_status_approved
import gasolineproject.shared.generated.resources.requests_status_cancelled
import gasolineproject.shared.generated.resources.requests_status_finished
import gasolineproject.shared.generated.resources.requests_status_pending
import gasolineproject.shared.generated.resources.requests_status_rejected
import gasolineproject.shared.generated.resources.requests_status_unknown
import gasolineproject.shared.generated.resources.requests_title
import gasolineproject.shared.generated.resources.requests_unknown_vehicle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.ui.components.AppBackButton
import org.vicpoo.gasolineproject.core.ui.components.AppCard
import org.vicpoo.gasolineproject.core.ui.components.AppImage
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.OfflineBanner
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.components.TitleText
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.ReservationCard

private const val AUTO_REFRESH_MS = 30_000L

@Composable
fun MyReservationsRoute(
    onBack: () -> Unit,
    viewModel: MyReservationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Se actualiza al entrar y cada 30 s mientras está en pantalla (sin el indicador)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                viewModel.refreshSilently()
                delay(AUTO_REFRESH_MS)
            }
        }
    }
    MyReservationsScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

/** Campana: tarjeta por solicitud con foto, carro, placas, para qué se pidió y su estado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReservationsScreen(
    state: MyReservationsUiState,
    onEvent: (MyReservationsEvent) -> Unit,
    onBack: () -> Unit,
) {
    val colors = AppTheme.colors
    val dimens = AppTheme.dimens
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = dimens.screenPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxSize().widthIn(max = dimens.maxContentWidthWide)) {
            Spacer(Modifier.height(dimens.spaceM))
            // Flecha + "Solicitudes" en la misma línea (Figma)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppBackButton(onClick = onBack)
                Spacer(Modifier.width(dimens.spaceM))
                TitleText(stringResource(Res.string.requests_title))
            }

            if (state.isOffline) {
                Spacer(Modifier.height(dimens.spaceS))
                OfflineBanner(stringResource(Res.string.requests_offline))
            }
            state.error?.let {
                Spacer(Modifier.height(dimens.spaceS))
                ErrorText(it.asText())
            }
            Spacer(Modifier.height(dimens.spaceL))

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(MyReservationsEvent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.isInitialLoading -> LoadingView()
                    state.requests.isEmpty() -> EmptyView(stringResource(Res.string.requests_empty))
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = dimens.spaceXl),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
                    ) {
                        items(state.requests, key = { it.id }) { request -> ReservationCard(request) }
                    }
                }
            }
        }
    }
}
