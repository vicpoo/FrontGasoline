package org.vicpoo.gasolineproject.ui.reports.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.ui.common.navigation.privilegedComposable
import org.vicpoo.gasolineproject.domain.rbac.AppAccess
import org.vicpoo.gasolineproject.ui.reports.ReportsRoute

/** Subgrafo de la pestaña "Reportes" (módulo de auditoría: auditoria.ver). */
@Serializable
data object ReportsGraph

sealed interface ReportsDestination {
    @Serializable
    data object Fleet : ReportsDestination
}

fun NavGraphBuilder.reportsGraph() {
    navigation<ReportsGraph>(startDestination = ReportsDestination.Fleet) {
        privilegedComposable<ReportsDestination.Fleet>(AppAccess.REPORTS) {
            ReportsRoute()
        }
    }
}
