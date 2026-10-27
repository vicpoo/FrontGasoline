package org.vicpoo.gasolineproject.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.core.ui.resources.Res
import org.vicpoo.gasolineproject.core.ui.resources.sync_error
import org.vicpoo.gasolineproject.core.ui.resources.sync_pending
import org.vicpoo.gasolineproject.core.ui.theme.AppTheme

/*
 * Píldoras de estado offline-first. Viven en core para que cualquier feature
 * (vehículos, viajes, recargas...) las use sin depender de otra feature.
 */

/** Píldora gris "Pendiente" (guardado en el teléfono, falta enviarlo). */
@Composable
fun PendingSyncChip(modifier: Modifier = Modifier) {
    StatusChip(
        text = stringResource(Res.string.sync_pending),
        containerColor = AppTheme.colors.labelBackground,
        contentColor = AppTheme.colors.onLabel,
        modifier = modifier,
    )
}

/** Píldora roja "Error al sincronizar" (la API rechazó el cambio). */
@Composable
fun SyncErrorChip(modifier: Modifier = Modifier) {
    StatusChip(
        text = stringResource(Res.string.sync_error),
        containerColor = AppTheme.colors.danger,
        contentColor = AppTheme.colors.onDanger,
        modifier = modifier,
    )
}
