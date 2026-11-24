package org.vicpoo.gasolineproject.ui.reports

import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.domain.common.AppDates
import org.vicpoo.gasolineproject.platform.files.SharedFile
import org.vicpoo.gasolineproject.platform.files.rememberFileSharer
import org.vicpoo.gasolineproject.ui.common.adaptive.LocalWindowWidth
import org.vicpoo.gasolineproject.ui.common.adaptive.WindowWidth
import org.vicpoo.gasolineproject.ui.common.components.EmptyView
import org.vicpoo.gasolineproject.ui.common.components.LoadingView
import org.vicpoo.gasolineproject.ui.common.components.ScreenHeader
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.domain.reports.FleetReport
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.reports_empty
import org.vicpoo.gasolineproject.ui.resources.reports_generated_at
import org.vicpoo.gasolineproject.ui.resources.reports_range
import org.vicpoo.gasolineproject.ui.resources.reports_range_all_time
import org.vicpoo.gasolineproject.ui.resources.reports_title

/** Id del selector para "Toda la flota". */
internal const val ALL_VEHICLES = "__all__"

@Composable
fun ReportsRoute(viewModel: ReportsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sharer = rememberFileSharer()

    // Al volver a la pestaña (o a la app) se actualizan los números sin mostrar el indicador
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshSilently()
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReportsEffect.Share -> sharer.share(SharedFile(effect.file.name, effect.file.mimeType, effect.file.bytes))
            }
        }
    }
    ReportsScreen(state = state, onEvent = viewModel::onEvent)
}

/**
 * Reportes de la flota: filtros (periodo y vehículo), resumen general, detalle por vehículo y
 * descarga en PDF o Excel (los genera la API; aquí solo se comparte el archivo).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(state: ReportsUiState, onEvent: (ReportsEvent) -> Unit) {
    val dimens = AppTheme.dimens
    val wide = LocalWindowWidth.current != WindowWidth.Compact
    Box(
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.background).statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = dimens.maxContentWidthWide)
                .padding(horizontal = dimens.screenPadding),
        ) {
            ScreenHeader(
                title = stringResource(Res.string.reports_title),
                subtitle = state.report?.let { subtitleOf(it) },
            )
            Spacer(Modifier.height(dimens.spaceS))

            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(ReportsEvent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = dimens.spaceL),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceM),
                ) {
                    item(key = "filters") { Filters(state, onEvent) }

                    if (state.isOffline || state.error != null || state.showingOtherFilters) {
                        item(key = "notices") { Notices(state) }
                    }

                    val report = state.report
                    when {
                        state.isInitialLoading -> item(key = "loading") { LoadingView(Modifier.fillMaxWidth().height(240.dp)) }
                        report == null -> item(key = "empty") { EmptyView(stringResource(Res.string.reports_empty)) }
                        else -> {
                            item(key = "summary") { SummarySection(report.summary, columns = if (wide) 4 else 2) }
                            item(key = "downloads") { Downloads(state, onEvent, sideBySide = wide) }
                            item(key = "generated") {
                                Text(
                                    stringResource(
                                        Res.string.reports_generated_at,
                                        AppDates.shortDate(report.generatedAt),
                                        AppDates.time12(report.generatedAt),
                                    ),
                                    style = AppTheme.typography.caption,
                                    color = AppTheme.colors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun subtitleOf(report: FleetReport): String {
    val range = if (report.from == null || report.to == null) {
        stringResource(Res.string.reports_range_all_time)
    } else {
        stringResource(Res.string.reports_range, AppDates.shortDate(report.from), AppDates.shortDate(report.to))
    }
    return listOf(report.companyName, range).filter { it.isNotBlank() }.joinToString(" · ")
}
