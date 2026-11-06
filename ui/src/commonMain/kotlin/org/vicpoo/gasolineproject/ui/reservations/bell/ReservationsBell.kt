package org.vicpoo.gasolineproject.ui.reservations.bell

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.vicpoo.gasolineproject.domain.rbac.AppAccess
import org.vicpoo.gasolineproject.ui.common.rbac.RequireAccess
import org.vicpoo.gasolineproject.ui.common.icons.AppIcons
import org.vicpoo.gasolineproject.ui.common.theme.AppTheme
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.cd_notifications

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
