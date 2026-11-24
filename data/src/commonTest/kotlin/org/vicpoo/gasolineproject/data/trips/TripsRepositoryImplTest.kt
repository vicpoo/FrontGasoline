package org.vicpoo.gasolineproject.data.trips

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppDates
import org.vicpoo.gasolineproject.testing.pagedList
import org.vicpoo.gasolineproject.data.trips.remote.KtorTripsApi
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.GeoPoint
import org.vicpoo.gasolineproject.domain.rbac.Privileges
import org.vicpoo.gasolineproject.testing.FakeApiServer
import org.vicpoo.gasolineproject.testing.FakePermissionChecker
import org.vicpoo.gasolineproject.testing.FakeSessionReader
import org.vicpoo.gasolineproject.testing.TestStorage
import org.vicpoo.gasolineproject.testing.noConnection
import org.vicpoo.gasolineproject.testing.respondJson
import org.vicpoo.gasolineproject.testing.testJson
import org.vicpoo.gasolineproject.testing.testSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Historial de viajes: solo cerrados, RBAC (propios / todos), paginación y detalles por bloques. */
class TripsRepositoryImplTest {

    private val api = FakeApiServer()
    private val storage = TestStorage()
    private val session = FakeSessionReader(testSession())
    private val permissions = FakePermissionChecker()

    private val repository = TripsRepositoryImpl(
        api = KtorTripsApi(api.client),
        trips = storage.pagedList("trips", TripEntity.serializer(), { it.id }, { AppDates.sortKey(it.startedAt) }),
        sessionReader = session,
        permissionChecker = permissions,
    )

    // ---------- JSON de la API ----------

    private fun tripJson(
        id: String,
        inicio: String = "2026-11-20T08:00:00",
        fin: String? = "2026-11-20T10:00:00",
        estado: String = "finalizado",
        operador: String? = "Juan Pérez",
        operadorId: String = "emp-1",
    ): String {
        val end = fin?.let { "\"$it\"" } ?: "null"
        val op = operador?.let { ""","operador":{"empleadoId":"$operadorId","nombreCompleto":"$it"}""" }.orEmpty()
        return """{"viajeId":"$id","empresaId":"company-1","carroId":"car-1","empleadoOperadorId":"$operadorId",""" +
            """"fechaInicio":"$inicio","fechaFin":$end,"distanciaRecorrida":42.5,"estado":"$estado","segundosEnPausa":120$op}"""
    }

    private fun list(vararg items: String) = items.joinToString(",", "[", "]")

    private fun MockRequestHandleScope.respondPage(body: String, total: Int): HttpResponseData = respond(
        body,
        HttpStatusCode.OK,
        headersOf(
            HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
            "X-Total-Count" to listOf(total.toString()),
        ),
    )

    private fun HttpRequestData.param(name: String): String? = url.parameters[name]
    private val HttpRequestData.path: String get() = url.encodedPath.removePrefix("/api/")

    private fun fuelJson(tripId: String) =
        """[{"cargaCombustibleId":"f-$tripId","viajeId":"$tripId","litros":20.0,"monto":500.0,"ubicacionLat":16.7,"ubicacionLng":-93.1}]"""

    private fun routeJson(tripId: String) =
        """{"viajeId":"$tripId","coordenadas":[{"latitud":16.75,"longitud":-93.12},{"latitud":16.76,"longitud":-93.13}]}"""

    /** Responde la lista de viajes con [trips] y los detalles de cualquier viaje. */
    private fun serve(trips: String, total: Int) {
        api.handler = { request ->
            when {
                request.path == "viajes" -> respondPage(trips, total)
                request.path == "cargas-combustible" -> respondJson(fuelJson(request.param("viajeId").orEmpty()))
                request.path.endsWith("/ruta-procesada/mapa") -> respondJson(routeJson(request.path.split("/")[1]))
                else -> error("Ruta inesperada: ${request.path}")
            }
        }
    }

    // ---------- Pruebas ----------

    @Test
    fun soloGuardaViajesCerrados_delMasReciente_alMasAntiguo() = runTest {
        serve(
            list(
                tripJson("viejo", inicio = "2026-11-01T08:00:00"),
                tripJson("en-curso", fin = null, estado = "enCurso"),
                tripJson("nuevo", inicio = "2026-11-21T08:00:00"),
                tripJson("interrumpido", inicio = "2026-11-10T08:00:00", estado = "interrumpido"),
            ),
            total = 4,
        )

        assertEquals(AppResult.Success(Unit), repository.refresh())

        assertEquals(listOf("nuevo", "interrumpido", "viejo"), repository.observeTrips().first().map { it.id })
    }

    @Test
    fun sinVerTodos_soloPideLosViajesPropios() = runTest {
        var operator: String? = "no-se-pidio"
        api.handler = { request ->
            operator = request.param("empleadoOperadorId")
            assertEquals("company-1", request.param("empresaId"))
            respondPage("[]", total = 0)
        }

        repository.refresh()

        assertEquals("emp-1", operator)
    }

    @Test
    fun conVerTodos_pideLosDeTodaLaEmpresa() = runTest {
        permissions.privileges.value = setOf(Privileges.VIAJE_VER_TODOS)
        var operator: String? = "no-se-pidio"
        api.handler = { request ->
            operator = request.param("empleadoOperadorId")
            respondPage("[]", total = 0)
        }

        repository.refresh()

        assertNull(operator)
    }

    @Test
    fun viajePropioSinOperador_usaElNombreDeLaSesion() = runTest {
        serve(list(tripJson("t1", operador = null)), total = 1)

        repository.refresh()

        assertEquals("test@polarsoft.mx", repository.observeTrips().first().single().operatorName)
    }

    @Test
    fun siLaPrimeraPaginaSoloTieneViajesEnCurso_pideLaSiguiente() = runTest {
        api.handler = { request ->
            when (request.param("pagina")) {
                "1" -> respondPage(list(*Array(10) { tripJson("c$it", fin = null, estado = "enCurso") }), total = 20)
                else -> respondPage(list(tripJson("cerrado")), total = 20)
            }
        }

        repository.refresh()

        assertEquals(listOf("cerrado"), repository.observeTrips().first().map { it.id })
        assertEquals(2, api.requests.size)
    }

    @Test
    fun alBajar_agregaLaSiguientePagina_sinRepetir() = runTest {
        api.handler = { request ->
            when (request.param("pagina")) {
                "1" -> respondPage(list(*Array(10) { tripJson("a$it") }), total = 12)
                else -> respondPage(list(tripJson("a9"), tripJson("b1"), tripJson("b2")), total = 12)
            }
        }
        repository.refresh()
        assertTrue(repository.observeHasMore().first())

        repository.loadMore()

        assertEquals(12, repository.observeTrips().first().size)
        assertFalse(repository.observeHasMore().first())
    }

    @Test
    fun sinRed_regresaSinConexion_yConservaLoGuardado() = runTest {
        serve(list(tripJson("t1")), total = 1)
        repository.refresh()

        api.handler = { noConnection() }

        assertEquals(AppResult.Failure(AppError.NoConnection), repository.refresh())
        assertEquals(listOf("t1"), repository.observeTrips().first().map { it.id })
    }

    @Test
    fun sinSesion_noPideNada() = runTest {
        session.state.value = null

        assertEquals(AppResult.Success(Unit), repository.refresh())
        assertTrue(api.requests.isEmpty())
    }

    @Test
    fun detalles_soloDeLosViajesPedidos_yNoSeRepiten() = runTest {
        serve(list(tripJson("t1"), tripJson("t2")), total = 2)
        repository.refresh()

        repository.loadDetails(listOf("t1"))
        repository.loadDetails(listOf("t1"))

        val t1 = repository.observeTrip("t1").first()
        assertEquals(1, t1?.fuelLoads?.size)
        assertEquals(20.0, t1?.fuelLoads?.single()?.liters)
        assertEquals(GeoPoint(16.7, -93.1), t1?.fuelLoads?.single()?.location)
        assertEquals(2, t1?.route?.size)
        assertTrue(repository.observeTrip("t2").first()?.fuelLoads.orEmpty().isEmpty())
        assertEquals(1, api.requests.count { it == "GET cargas-combustible" }, "El segundo llamado no repite")
    }

    @Test
    fun detallesSinRed_avisaSinConexion_yQuedaLoGuardado() = runTest {
        serve(list(tripJson("t1")), total = 1)
        repository.refresh()
        repository.loadDetails(listOf("t1"))

        // Nueva actualización: los detalles se vuelven a pedir, pero ya no hay red
        repository.refresh()
        api.handler = { noConnection() }
        val result = repository.loadDetails(listOf("t1"))

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
        val t1 = repository.observeTrip("t1").first()
        assertEquals(1, t1?.fuelLoads?.size)
        assertEquals(2, t1?.route?.size)
    }

    @Test
    fun alRecargar_seConservanLasCargasYLaRutaGuardadas() = runTest {
        serve(list(tripJson("t1")), total = 1)
        repository.refresh()
        repository.loadDetails(listOf("t1"))

        repository.refresh()

        val t1 = repository.observeTrip("t1").first()
        assertEquals(1, t1?.fuelLoads?.size)
        assertEquals(2, t1?.route?.size)
    }
}
