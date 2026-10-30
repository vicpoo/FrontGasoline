package org.vicpoo.gasolineproject.feature.trips.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.vicpoo.gasolineproject.core.common.AppDates
import org.vicpoo.gasolineproject.core.common.formatDecimal
import org.vicpoo.gasolineproject.core.ui.components.AppBackButton
import org.vicpoo.gasolineproject.core.ui.components.EmptyView
import org.vicpoo.gasolineproject.core.ui.components.ImageViewerDialog
import org.vicpoo.gasolineproject.core.ui.components.LoadingView
import org.vicpoo.gasolineproject.core.ui.components.RouteMap
import org.vicpoo.gasolineproject.core.ui.components.SecondaryButton
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.trips.domain.FuelLoad
import org.vicpoo.gasolineproject.feature.trips.domain.Trip
import org.vicpoo.gasolineproject.feature.trips.presentation.common.OperatorHeader
import org.vicpoo.gasolineproject.feature.trips.resources.Res
import org.vicpoo.gasolineproject.feature.trips.resources.cd_close
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_amount
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_distance
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_end
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_final_mileage
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_liters
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_loads
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_no_loads
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_start
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_view_receipt
import org.vicpoo.gasolineproject.feature.trips.resources.trip_not_found

@Composable
fun TripDetailRoute(
    tripId: String,
    onBack: () -> Unit,
    viewModel: TripDetailViewModel = koinViewModel(key = "trip-$tripId") { parametersOf(TripDetailArgs(tripId)) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TripDetailScreen(
        state = state,
        onBack = onBack,
        onViewReceipt = viewModel::openReceipt,
        onCloseReceipt = viewModel::closeReceipt,
    )
}

/** Figma: mapa arriba, operador, fechas, distancia, kilometraje y cargas con "Ver recibo". */
@Composable
fun TripDetailScreen(
    state: TripDetailUiState,
    onBack: () -> Unit,
    onViewReceipt: (String?) -> Unit,
    onCloseReceipt: () -> Unit,
) {
    val trip = state.trip
    when {
        state.isLoading -> LoadingView()
        trip == null -> Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
            EmptyView(stringResource(Res.string.trip_not_found))
            AppBackButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(AppTheme.dimens.spaceM))
        }
        else -> TripDetailContent(trip, onBack, onViewReceipt)
    }

    if (state.openReceiptUrl != null) {
        ImageViewerDialog(
            model = state.openReceiptUrl,
            onDismiss = onCloseReceipt,
            closeDescription = stringResource(Res.string.cd_close),
        )
    }
}

/** Alto de la hoja "abajo": deja ver al operador y la fecha, y casi todo el mapa. */
private val SHEET_PEEK_HEIGHT = 112.dp

/**
 * Estilo Google Maps: el mapa ocupa toda la pantalla y los detalles van en una hoja encima.
 * La hoja abre arriba (se ven los datos); se arrastra hacia abajo para ver el mapa completo y
 * la ruta se recentra en lo que queda visible mientras se arrastra.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDetailContent(trip: Trip, onBack: () -> Unit, onViewReceipt: (String?) -> Unit) {
    val dimens = AppTheme.dimens
    val colors = AppTheme.colors
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded),
    )
    val sheetState = scaffoldState.bottomSheetState
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background)) {
        val maxSheetHeight = maxHeight * 0.6f
        val screenHeightPx = constraints.maxHeight.toFloat()
        val fitInsetPx = with(LocalDensity.current) { maxSheetHeight.toPx() }

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = SHEET_PEEK_HEIGHT + navigationBarHeight,
            sheetMaxWidth = dimens.maxContentWidthWide,
            sheetShape = RoundedCornerShape(topStart = dimens.spaceL, topEnd = dimens.spaceL),
            sheetContainerColor = colors.background,
            sheetShadowElevation = 8.dp,
            sheetDragHandle = { BottomSheetDefaults.DragHandle(color = colors.labelBackground) },
            containerColor = colors.background,
            sheetContent = {
                TripDetailSheet(
                    trip = trip,
                    onViewReceipt = onViewReceipt,
                    modifier = Modifier.heightIn(max = maxSheetHeight),
                )
            },
        ) { _ ->
            // El mapa va detrás de la hoja (por eso no se usa el padding del scaffold)
            Box(Modifier.fillMaxSize()) {
                RouteMap(
                    points = trip.route,
                    animate = true,
                    // El zoom se elige para que la ruta quepa arriba de la hoja abierta...
                    fitBottomInset = maxSheetHeight,
                    // ...y al bajar la hoja la ruta se recorre al centro de lo que se ve
                    contentOffsetY = {
                        val sheetTop = runCatching { sheetState.requireOffset() }.getOrNull() ?: (screenHeightPx - fitInsetPx)
                        val visibleHeight = sheetTop.coerceIn(0f, screenHeightPx)
                        val fittedHeight = screenHeightPx - fitInsetPx
                        (visibleHeight - fittedHeight) / 2f
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                AppBackButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(dimens.spaceM))
            }
        }
    }
}

/** Contenido de la hoja: operador y fecha arriba (lo que se ve con la hoja abajo) y luego el resto. */
@Composable
private fun TripDetailSheet(trip: Trip, onViewReceipt: (String?) -> Unit, modifier: Modifier = Modifier) {
    val dimens = AppTheme.dimens
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = dimens.screenPadding, end = dimens.screenPadding, bottom = dimens.spaceL),
    ) {
        OperatorHeader(
            name = trip.operatorName,
            photoUrl = trip.operatorPhotoUrl,
            avatarSize = 48.dp,
            textStyle = AppTheme.typography.subtitle,
        )
        Spacer(Modifier.height(dimens.spaceS))
        Text(
            AppDates.longDate(trip.startedAt),
            style = AppTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.title,
        )
        Spacer(Modifier.height(dimens.spaceM))

        DateTimeRow(stringResource(Res.string.trip_detail_start), trip.startedAt)
        DateTimeRow(stringResource(Res.string.trip_detail_end), trip.endedAt)
        Spacer(Modifier.height(dimens.spaceL))

        SecondaryText(
            stringResource(Res.string.trip_detail_distance, trip.distanceKm?.let(::formatDecimal) ?: "—"),
        )
        SecondaryText(
            stringResource(Res.string.trip_detail_final_mileage, trip.finalMileageKm?.let(::formatDecimal) ?: "—"),
        )
        Spacer(Modifier.height(dimens.spaceL))

        Text(stringResource(Res.string.trip_detail_loads), style = AppTheme.typography.body, color = colors.title)
        Spacer(Modifier.height(dimens.spaceS))
        if (trip.fuelLoads.isEmpty()) {
            SecondaryText(stringResource(Res.string.trip_detail_no_loads))
        } else {
            trip.fuelLoads.forEach { load ->
                FuelLoadCard(load, onViewReceipt = { onViewReceipt(load.receiptUrl) })
                Spacer(Modifier.height(dimens.spaceS))
            }
        }
    }
}

@Composable
private fun DateTimeRow(label: String, iso: String?) {
    Row(Modifier.fillMaxWidth()) {
        SecondaryText("$label : ${AppDates.shortDate(iso).ifEmpty { "—" }}", modifier = Modifier.weight(1f))
        SecondaryText(AppDates.time12(iso))
    }
}

/** Tarjeta azul de carga (Figma): monto, litros y botón "Ver recibo" (botón de consulta = azul marino). */
@Composable
private fun FuelLoadCard(load: FuelLoad, onViewReceipt: () -> Unit) {
    val dimens = AppTheme.dimens
    val colors = AppTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimens.radius),
        color = colors.primary,
        contentColor = colors.onPrimary,
    ) {
        Row(Modifier.padding(dimens.spaceM), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.trip_detail_amount, load.amount?.let(::formatDecimal) ?: "—"),
                    style = AppTheme.typography.caption,
                    color = colors.onPrimary,
                )
                Text(
                    stringResource(Res.string.trip_detail_liters, formatDecimal(load.liters)),
                    style = AppTheme.typography.caption,
                    color = colors.onPrimary,
                )
            }
            SecondaryButton(
                text = stringResource(Res.string.trip_detail_view_receipt),
                onClick = onViewReceipt,
                enabled = load.receiptUrl != null,
            )
        }
    }
}
