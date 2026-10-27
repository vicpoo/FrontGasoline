package org.vicpoo.gasolineproject.feature.reservations.presentation.company

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.components.ErrorText
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.OfflineBanner
import org.vicpoo.gasolineproject.core.ui.components.ScreenHeader
import org.vicpoo.gasolineproject.core.ui.components.asText
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.reservations.presentation.common.ReservationCard
import org.vicpoo.gasolineproject.feature.reservations.resources.Res
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_all
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_empty_all
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_empty_pending
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_pending
import org.vicpoo.gasolineproject.feature.reservations.resources.company_requests_title
import org.vicpoo.gasolineproject.feature.reservations.resources.requests_offline

private const val AUTO_REFRESH_MS = 30_000L

@Composable
fun CompanyReservationsRoute(
    onRequestClick: (String) -> Unit,
    viewModel: CompanyReservationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Mientras la pestaña está en pantalla: se actualiza al entrar (cambiar de pestaña, regresar
    // del detalle o volver a la app) y cada 30 s, sin el indicador. El push la actualiza al instante.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                viewModel.refreshSilently()
                delay(AUTO_REFRESH_MS)
            }
        }
    }
    CompanyReservationsScreen(state = state, onEvent = viewModel::onEvent, onRequestClick = onRequestClick)
}

/** Pestaña llave: "Solicitud de {empleado}" con su foto; al tocarla se abre el detalle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyReservationsScreen(
    state: CompanyReservationsUiState,
    onEvent: (CompanyReservationsEvent) -> Unit,
    onRequestClick: (String) -> Unit,
) {
    val dimens = AppTheme.dimens
    Box(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = dimens.screenPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxSize().widthIn(max = dimens.maxContentWidthWide)) {
            ScreenHeader(title = stringResource(Res.string.company_requests_title))
            Spacer(Modifier.height(dimens.spaceS))

            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceS)) {
                FilterTab(
                    text = stringResource(Res.string.company_requests_pending, state.pendingCount),
                    selected = state.filter == CompanyReservationsFilter.PENDING,
                    onClick = { onEvent(CompanyReservationsEvent.FilterChanged(CompanyReservationsFilter.PENDING)) },
                )
                FilterTab(
                    text = stringResource(Res.string.company_requests_all),
                    selected = state.filter == CompanyReservationsFilter.ALL,
                    onClick = { onEvent(CompanyReservationsEvent.FilterChanged(CompanyReservationsFilter.ALL)) },
                )
            }

            if (state.isOffline) {
                Spacer(Modifier.height(dimens.spaceS))
                OfflineBanner(stringResource(Res.string.requests_offline))
            }
            state.error?.let {
                Spacer(Modifier.height(dimens.spaceS))
                ErrorText(it.asText())
            }
            Spacer(Modifier.height(dimens.spaceM))

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(CompanyReservationsEvent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.isInitialLoading -> LoadingView()
                    state.requests.isEmpty() -> EmptyView(
                        stringResource(
                            if (state.filter == CompanyReservationsFilter.PENDING) Res.string.company_requests_empty_pending
                            else Res.string.company_requests_empty_all,
                        ),
                    )
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = dimens.spaceXl),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceM),
                    ) {
                        items(state.requests, key = { it.id }) { request ->
                            ReservationCard(
                                request = request,
                                showRequester = true,
                                onClick = { onRequestClick(request.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTab(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AppTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = AppTheme.typography.caption) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.primary,
            selectedLabelColor = colors.onPrimary,
            containerColor = colors.background,
            labelColor = colors.text,
        ),
    )
}
