package org.vicpoo.gasolineproject.feature.reservations.presentation.list

import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationRequest
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/** Filtros con conteo y agrupación por mes de "Mis solicitudes". */
class MyReservationsGroupingTest {

    private fun request(id: String, status: ReservationStatus, date: String?) = ReservationRequest(
        id = id,
        vehicleId = "car",
        vehicleTitle = "Nissan",
        plates = "ABC-123",
        photoUrl = null,
        status = status,
        requestedAt = date,
        resolvedAt = null,
        resolutionReason = null,
    )

    private val requests = listOf(
        request("a", ReservationStatus.APPROVED, "2026-10-26T10:00:00"),
        request("b", ReservationStatus.REJECTED, "2026-10-27T09:00:00"),
        request("c", ReservationStatus.CANCELLED, "2026-09-02T09:00:00"),
        request("d", ReservationStatus.PENDING, "2026-10-28T09:00:00"),
        request("e", ReservationStatus.NOT_SENT, null),
        request("f", ReservationStatus.FINISHED, "2026-08-15T09:00:00"),
    )

    @Test
    fun conteos_porFiltro() {
        val counts = MyReservationsGrouping.counts(requests)
        assertEquals(6, counts[MyReservationsFilter.ALL])
        assertEquals(2, counts[MyReservationsFilter.PENDING]) // pendiente + por enviar
        assertEquals(2, counts[MyReservationsFilter.APPROVED]) // aprobada + finalizada
        assertEquals(2, counts[MyReservationsFilter.REJECTED]) // denegada + cancelada
    }

    @Test
    fun agrupaPorMes_delMasRecienteAlMasAntiguo_ySinFechaAlFinal() {
        val groups = MyReservationsGrouping.group(requests, MyReservationsFilter.ALL)

        assertEquals(listOf("2026-10", "2026-09", "2026-08", "sin-fecha"), groups.map { it.key })
        assertEquals(listOf("d", "b", "a"), groups.first().requests.map { it.id })
        assertEquals(10, groups.first().month)
        assertEquals(2026, groups.first().year)
        assertEquals(null, groups.last().month)
    }

    @Test
    fun filtrar_soloDejaLosDelEstado() {
        val groups = MyReservationsGrouping.group(requests, MyReservationsFilter.REJECTED)
        assertEquals(listOf("b", "c"), groups.flatMap { it.requests }.map { it.id })
    }
}
