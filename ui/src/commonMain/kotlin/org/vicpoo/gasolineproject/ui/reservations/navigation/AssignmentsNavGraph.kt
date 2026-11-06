package org.vicpoo.gasolineproject.ui.reservations.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.vicpoo.gasolineproject.ui.common.navigation.privilegedComposable
import org.vicpoo.gasolineproject.domain.rbac.AppAccess
import org.vicpoo.gasolineproject.ui.common.adaptive.ListDetailLayout
import org.vicpoo.gasolineproject.ui.common.adaptive.showsListAndDetail
import org.vicpoo.gasolineproject.ui.reservations.assignmentdetail.AssignmentDetailRoute
import org.vicpoo.gasolineproject.ui.reservations.assignments.AssignmentsRoute
import org.vicpoo.gasolineproject.ui.resources.Res
import org.vicpoo.gasolineproject.ui.resources.assignments_select_hint

/** Subgrafo de la pestaña Asignaciones: carros vinculados a un empleado y su detalle (con "Desvincular"). */
@Serializable
data object AssignmentsGraph

sealed interface AssignmentsDestination {
    @Serializable
    data object List : AssignmentsDestination

    /** Detalle: [key] = "r:<reserva>" (un carro) o "e:<empleado>" (todos sus carros). */
    @Serializable
    data class Detail(val key: String) : AssignmentsDestination
}

/** Igual que Solicitudes: en el teléfono el detalle es otra pantalla; en tablet horizontal, lado a lado. */
fun NavGraphBuilder.assignmentsGraph(navController: NavHostController) {
    navigation<AssignmentsGraph>(startDestination = AssignmentsDestination.List) {
        privilegedComposable<AssignmentsDestination.List>(AppAccess.ASSIGNMENTS) {
            var selected by rememberSaveable { mutableStateOf<String?>(null) }
            val twoPanes = showsListAndDetail
            ListDetailLayout(
                selectedId = selected,
                emptyDetailText = stringResource(Res.string.assignments_select_hint),
                detailAccess = AppAccess.ASSIGNMENTS,
                onOpenSinglePane = { key ->
                    selected = null
                    navController.navigate(AssignmentsDestination.Detail(key))
                },
                onCloseDetail = { selected = null },
                list = {
                    AssignmentsRoute(
                        onAssignmentClick = { key ->
                            if (twoPanes) selected = key.value else navController.navigate(AssignmentsDestination.Detail(key.value))
                        },
                    )
                },
                // Al desvincular el último carro, el detalle llama onBack: se limpia el panel
                detail = { key -> AssignmentDetailRoute(key = key, onBack = { selected = null }) },
            )
        }

        privilegedComposable<AssignmentsDestination.Detail>(
            access = AppAccess.ASSIGNMENTS,
            onDenied = { navController.popBackStack() },
        ) { entry ->
            val route = entry.toRoute<AssignmentsDestination.Detail>()
            AssignmentDetailRoute(key = route.key, onBack = { navController.popBackStack() })
        }
    }
}
