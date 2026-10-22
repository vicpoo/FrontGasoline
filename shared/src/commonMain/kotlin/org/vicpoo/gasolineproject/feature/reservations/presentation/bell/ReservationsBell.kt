package org.vicpoo.gasolineproject.feature.reservations.presentation.bell

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import gasolineproject.shared.generated.resources.Res
import gasolineproject.shared.generated.resources.cd_notifications
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.core.rbac.AppAccess
import org.vicpoo.gasolineproject.core.rbac.RequireAccess
import org.vicpoo.gasolineproject.core.ui.icons.AppIcons
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme
import org.vicpoo.gasolineproject.feature.reservations.domain.ObserveUnseenReservationUpdatesUseCase
import org.vicpoo.gasolineproject.feature.reservations.domain.RefreshUnseenReservationUpdatesUseCase

/** Número del badge: respuestas a mis solicitudes que no he visto (sin red, el último guardado). */
class ReservationsBellViewModel(
    observeUnseen: ObserveUnseenReservationUpdatesUseCase,
    private val refreshUnseen: RefreshUnseenReservationUpdatesUseCase,
) : ViewModel() {

    val unseen: StateFlow<Int> = observeUnseen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { refreshUnseen() }
    }
}

/**
 * Campana de la parte superior derecha de Inicio: abre "Mis solicitudes" (solo estado, no se
 * acepta ni rechaza nada ahí). Sin solicitud.ver_propias no se muestra.
 */
@Composable
fun ReservationsBell(onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Quién ve la campana: los que pueden ver el estado de sus solicitudes
    RequireAccess(AppAccess.MY_RESERVATIONS) {
        BellButton(onClick = onClick, modifier = modifier)
    }
}

@Composable
private fun BellButton(
    onClick: () -> Unit,
    modifier: Modifier,
    viewModel: ReservationsBellViewModel = koinViewModel(),
) {
    val unseen by viewModel.unseen.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    IconButton(onClick = onClick, modifier = modifier) {
        BadgedBox(
            badge = {
                if (unseen > 0) {
                    Badge(containerColor = colors.danger, contentColor = colors.onDanger) {
                        Text(if (unseen > 9) "9+" else unseen.toString())
                    }
                }
            },
        ) {
            Icon(
                imageVector = AppIcons.Bell,
                contentDescription = stringResource(Res.string.cd_notifications, unseen),
                tint = colors.title,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
