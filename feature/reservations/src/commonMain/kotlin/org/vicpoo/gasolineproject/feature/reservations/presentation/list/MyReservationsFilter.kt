package org.vicpoo.gasolineproject.feature.reservations.presentation.list

import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationRequest
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationStatus

/** Filtros de "Mis solicitudes" (chips con su conteo). */
enum class MyReservationsFilter(val statuses: Set<ReservationStatus>?) {
    /** null = sin filtro. */
    ALL(null),

    /** Aún sin respuesta (incluye las que esperan enviarse o fallaron al enviarse). */
    PENDING(setOf(ReservationStatus.PENDING, ReservationStatus.NOT_SENT, ReservationStatus.SEND_FAILED)),
    APPROVED(setOf(ReservationStatus.APPROVED, ReservationStatus.FINISHED)),
    REJECTED(setOf(ReservationStatus.REJECTED, ReservationStatus.CANCELLED)),
    ;

    fun matches(request: ReservationRequest): Boolean = statuses == null || request.status in statuses
}

/** Grupo de solicitudes de un mes ("OCTUBRE 2026"). [year]/[month] = null si no tienen fecha. */
data class ReservationMonthGroup(
    val key: String,
    val year: Int?,
    val month: Int?,
    val requests: List<ReservationRequest>,
)

/** Lógica pura de la lista (se prueba sin UI). */
object MyReservationsGrouping {

    private val YEAR_MONTH = Regex("""^(\d{4})-(\d{2})""")

    fun counts(requests: List<ReservationRequest>): Map<MyReservationsFilter, Int> =
        MyReservationsFilter.entries.associateWith { filter -> requests.count(filter::matches) }

    /** Filtra y agrupa por mes, de la más reciente a la más antigua (las sin fecha al final). */
    fun group(requests: List<ReservationRequest>, filter: MyReservationsFilter): List<ReservationMonthGroup> =
        requests
            .filter(filter::matches)
            .sortedByDescending { it.requestedAt.orEmpty() }
            .groupBy { request ->
                YEAR_MONTH.find(request.requestedAt.orEmpty())?.value ?: NO_DATE
            }
            .map { (key, items) ->
                val match = YEAR_MONTH.find(key)
                ReservationMonthGroup(
                    key = key,
                    year = match?.groupValues?.get(1)?.toInt(),
                    month = match?.groupValues?.get(2)?.toInt(),
                    requests = items,
                )
            }

    private const val NO_DATE = "sin-fecha"
}
