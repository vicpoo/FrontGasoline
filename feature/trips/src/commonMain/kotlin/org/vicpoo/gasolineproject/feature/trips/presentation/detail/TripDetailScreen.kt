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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
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
import org.vicpoo.gasolineproject.core.ui.components.SecondaryText
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.trips.domain.FuelLoad
import org.vicpoo.gasolineproject.feature.trips.domain.Trip
import org.vicpoo.gasolineproject.feature.trips.presentation.common.OperatorHeader
import org.vicpoo.gasolineproject.feature.trips.resources.Res
import org.vicpoo.gasolineproject.feature.trips.resources.cd_close
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_amount_value
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_distance
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_end
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_final_mileage
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_liters_chip
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_loads
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_no_loads
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_start
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_view_receipt
import org.vicpoo.gasolineproject.feature.trips.resources.trip_not_found
import org.vicpoo.gasolineproject.feature.trips.presentation.common.fuelMarkers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.launch
import org.vicpoo.gasolineproject.feature.trips.resources.trip_detail_show_full_route

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
    val scope = rememberCoroutineScope()
    // Carga elegida en la hoja: el mapa se centra en su icono (null = toda la ruta)
    var focusedLoadId by rememberSaveable(trip.id) { mutableStateOf<String?>(null) }
    val focusedPoint = trip.fuelLoads.firstOrNull { it.id == focusedLoadId }?.location
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
                    focusedLoadId = focusedLoadId,
                    onLoadClick = { load ->
                        // Tocar la misma carga otra vez regresa a la ruta completa
                        focusedLoadId = if (focusedLoadId == load.id) null else load.id
                        // Se baja la hoja para que se vea el mapa
                        if (focusedLoadId != null) scope.launch { sheetState.partialExpand() }
                    },
                    modifier = Modifier.heightIn(max = maxSheetHeight),
                )
            },
        ) { _ ->
            // El mapa va detrás de la hoja (por eso no se usa el padding del scaffold)
            Box(Modifier.fillMaxSize()) {
                RouteMap(
                    points = trip.route,
                    animate = true,
                    // Icono de gasolina donde se hizo cada carga
                    markers = trip.fuelMarkers(),
                    // Al tocar una carga, el mapa se acerca a su icono
                    focusPoint = focusedPoint,
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
                // Con una carga enfocada: botón para volver a ver toda la ruta
                if (focusedPoint != null) {
                    Surface(
                        onClick = { focusedLoadId = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(dimens.spaceM)
                            .size(dimens.iconButtonSize),
                        shape = CircleShape,
                        color = colors.labelBackground,
                        contentColor = colors.onLabel,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = AppIcons.Route,
                                contentDescription = stringResource(Res.string.trip_detail_show_full_route),
                                modifier = Modifier.size(dimens.iconSize),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Contenido de la hoja: operador y fecha arriba (lo que se ve con la hoja abajo) y luego el resto. */
@Composable
private fun TripDetailSheet(
    trip: Trip,
    onViewReceipt: (String?) -> Unit,
    focusedLoadId: String?,
    onLoadClick: (FuelLoad) -> Unit,
    modifier: Modifier = Modifier,
) {
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

        Text(
            stringResource(Res.string.trip_detail_loads).uppercase(),
            style = AppTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(dimens.spaceS))
        if (trip.fuelLoads.isEmpty()) {
            SecondaryText(stringResource(Res.string.trip_detail_no_loads))
        } else {
            // De la más antigua a la más reciente
            trip.fuelLoads.sortedBy { AppDates.sortKey(it.date) }.forEach { load ->
                FuelLoadCard(
                    load = load,
                    selected = load.id == focusedLoadId,
                    onClick = { onLoadClick(load) },
                    onViewReceipt = { onViewReceipt(load.receiptUrl) },
                )
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

/**
 * Tarjeta de carga: arriba la fecha y hora con reloj, abajo el monto grande con un chip de litros
 * y a la derecha el botón de recibo (solo icono). Sin borde: se separa por contraste (colors.card).
 */
@Composable
private fun FuelLoadCard(load: FuelLoad, selected: Boolean, onClick: () -> Unit, onViewReceipt: () -> Unit) {
    val dimens = AppTheme.dimens
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(dimens.radius)
    // Tocar la tarjeta centra el mapa en la carga (solo si se sabe dónde fue).
    // La elegida se marca con un fondo azul tenue (sin borde, igual que las demás tarjetas).
    Surface(
        onClick = onClick,
        enabled = load.location != null,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = if (selected) colors.primary.copy(alpha = if (colors.isDark) 0.28f else 0.14f) else colors.card,
        contentColor = colors.text,
    ) {
        Row(Modifier.padding(dimens.spaceM), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val date = AppDates.shortDate(load.date)
                if (date.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.Clock,
                            contentDescription = null,
                            tint = colors.textSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "$date • ${AppDates.time12(load.date)}",
                            style = AppTheme.typography.caption,
                            color = colors.textSecondary.copy(alpha = 0.7f),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.trip_detail_amount_value, load.amount?.let(::formatDecimal) ?: "—"),
                        style = AppTheme.typography.title.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                        color = colors.primary,
                    )
                    Spacer(Modifier.width(dimens.spaceS))
                    // Chip de litros
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.text.copy(alpha = if (colors.isDark) 0.12f else 0.08f),
                    ) {
                        Text(
                            stringResource(Res.string.trip_detail_liters_chip, formatDecimal(load.liters)),
                            style = AppTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.text,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            // Botón de recibo: solo icono, azul, sin borde
            val enabled = load.receiptUrl != null
            Surface(
                onClick = onViewReceipt,
                enabled = enabled,
                shape = shape,
                color = colors.primary.copy(alpha = if (colors.isDark) 0.22f else 0.12f),
                border = null,
            ) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.FileDollar,
                        contentDescription = stringResource(Res.string.trip_detail_view_receipt),
                        tint = if (enabled) colors.primary else colors.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
