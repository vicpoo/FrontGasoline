package org.vicpoo.gasolineproject.data.reservations

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import org.vicpoo.gasolineproject.data.local.JsonFileCache
import org.vicpoo.gasolineproject.data.reservations.local.CompanyReservationsSnapshot
import org.vicpoo.gasolineproject.data.reservations.local.MyReservationsSnapshot
import org.vicpoo.gasolineproject.data.reservations.local.ReservationOutbox
import org.vicpoo.gasolineproject.data.reservations.local.ReservationOutboxSnapshot
import org.vicpoo.gasolineproject.data.reservations.remote.KtorReservationApi
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.reservations.ReservationStatus
import org.vicpoo.gasolineproject.testing.FakeApiServer
import org.vicpoo.gasolineproject.testing.FakeAppClock
import org.vicpoo.gasolineproject.testing.FakeSessionReader
import org.vicpoo.gasolineproject.testing.RecordingSyncScheduler
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

/**
 * Repositorio de solicitudes contra la API falsa: copia del teléfono, paginación de 10 en 10,
 * pendientes de la empresa, aceptar / rechazar y la campana. (El envío sin red está en ReservationOutboxTest.)
 */
class ReservationRepositoryImplTest {

    private val api = FakeApiServer()
    private val storage = TestStorage()
    private val session = FakeSessionReader(testSession())

    private fun <T : Any> cache(name: String, serializer: KSerializer<T>, empty: T) =
        JsonFileCache(storage.fileSystem, storage.directories, name, serializer, testJson, empty)

    private val mineCache = cache("mine.json", MyReservationsSnapshot.serializer(), MyReservationsSnapshot())
    private val companyCache = cache("company.json", CompanyReservationsSnapshot.serializer(), CompanyReservationsSnapshot())
    private val outbox = ReservationOutbox(cache("outbox.json", ReservationOutboxSnapshot.serializer(), ReservationOutboxSnapshot()))

    private val repository = ReservationRepositoryImpl(
        api = KtorReservationApi(api.client),
        sessionReader = session,
        cache = mineCache,
        companyCache = companyCache,
        outbox = outbox,
        clock = FakeAppClock(),
        scheduler = RecordingSyncScheduler(),
    )

    /** Cuerpos JSON que se mandaron (POST), en orden. */
    private val sentBodies = mutableListOf<String>()

    // ---------- JSON de la API ----------

    private fun requestJson(
        id: String,
        estado: String = "pendiente",
        fecha: String = "2026-11-20T09:00:00",
        withCar: Boolean = true,
        carroId: String = "car-$id",
    ): String {
        val car = if (withCar) {
            ""","carro":{"carroId":"$carroId","marca":"Nissan","modelo":"Versa","placas":"ABC-123"}"""
        } else {
            ""
        }
        return """{"solicitudReservaId":"$id","carroId":"$carroId","empleadoSolicitanteId":"emp-1","motivoSolicitud":"Visita",""" +
            """"estado":"$estado","fechaSolicitud":"$fecha","solicitante":{"empleadoId":"emp-1","nombreCompleto":"Juan Pérez"}$car}"""
    }

    private fun list(vararg items: String) = items.joinToString(",", "[", "]")

    /** Página con el total en X-Total-Count (como la API). */
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

    private fun HttpRequestData.recordBody() {
        (body as? TextContent)?.let { sentBodies += it.text }
    }

    // ---------- Mis solicitudes ----------

    @Test
    fun refrescarMias_guardaLaCopia_ordenadaDeLaMasReciente() = runTest {
        api.handler = { request ->
            assertEquals("emp-1", request.param("empleadoSolicitanteId"))
            assertEquals("1", request.param("pagina"))
            assertEquals("10", request.param("tamanoPagina"))
            respondPage(
                list(requestJson("a", "aprobada", "2026-11-01T09:00:00"), requestJson("b", "pendiente", "2026-11-10T09:00:00")),
                total = 2,
            )
        }

        assertEquals(AppResult.Success(Unit), repository.refreshMine())

        val mine = repository.observeMine().first()
        assertEquals(listOf("b", "a"), mine.map { it.id })
        assertEquals(ReservationStatus.APPROVED, mine.last().status)
        assertEquals("Nissan Versa", mine.first().vehicleTitle)
        assertEquals("Juan Pérez", mine.first().requesterName)
        assertFalse(repository.observeMineHasMore().first())
    }

    @Test
    fun conMasDeDiez_hayMas_yAlBajarPideLaPagina2_yLasAgrega() = runTest {
        api.handler = { request ->
            when (request.param("pagina")) {
                "1" -> respondPage(list(*Array(10) { requestJson("p1-$it") }), total = 15)
                else -> respondPage(list(*Array(5) { requestJson("p2-$it") }), total = 15)
            }
        }
        repository.refreshMine()
        assertTrue(repository.observeMineHasMore().first())

        repository.loadMoreMine()

        assertEquals(15, repository.observeMine().first().size)
        assertEquals(2, api.requests.size, "Página 1 y página 2")
        assertFalse(repository.observeMineHasMore().first())
    }

    @Test
    fun alRecargarDespuesDeBajar_pideTodoLoQueYaSeVeiaEnUnaSolaPeticion() = runTest {
        val sizes = mutableListOf<String?>()
        api.handler = { request ->
            sizes += request.param("tamanoPagina")
            when (request.param("pagina")) {
                "1" -> respondPage(list(*Array(10) { requestJson("x-$it") }), total = 30)
                else -> respondPage(list(*Array(10) { requestJson("y-$it") }), total = 30)
            }
        }
        repository.refreshMine()
        repository.loadMoreMine()

        repository.refreshMine()

        assertEquals(listOf<String?>("10", "10", "20"), sizes)
    }

    @Test
    fun siLaApiNoMandaElCarro_loPideAparte_unaVezPorCarro() = runTest {
        api.handler = { request ->
            when {
                request.path.startsWith("carros/") ->
                    respondJson("""{"carroId":"car-x","marca":"Chevrolet","modelo":"Aveo","placas":"XYZ-987"}""")
                else -> respondPage(
                    list(requestJson("a", withCar = false, carroId = "car-x"), requestJson("b", withCar = false, carroId = "car-x")),
                    total = 2,
                )
            }
        }

        repository.refreshMine()

        assertEquals(1, api.requests.count { it == "GET carros/car-x" })
        assertTrue(repository.observeMine().first().all { it.vehicleTitle == "Chevrolet Aveo" })
    }

    @Test
    fun sinRed_regresaSinConexion_yConservaLaCopia() = runTest {
        api.handler = { respondPage(list(requestJson("a")), total = 1) }
        repository.refreshMine()

        api.handler = { noConnection() }
        val result = repository.refreshMine()

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
        assertEquals(listOf("a"), repository.observeMine().first().map { it.id })
    }

    @Test
    fun sinEmpleado_noPuedePedirSusSolicitudes() = runTest {
        session.state.value = testSession(employeeId = null)

        assertEquals(AppResult.Failure(AppError.NoCompany), repository.refreshMine())
        assertTrue(api.requests.isEmpty())
    }

    // ---------- Solicitudes de la empresa ----------

    @Test
    fun refrescarEmpresa_juntaLaPaginaConTodasLasPendientes_sinRepetir() = runTest {
        api.handler = { request ->
            if (request.param("estado") == "Pendiente") {
                respondJson(list(requestJson("vieja-pendiente", fecha = "2026-01-01T09:00:00"), requestJson("r1")))
            } else {
                respondPage(list(requestJson("r1"), requestJson("r2", "aprobada")), total = 2)
            }
        }

        repository.refreshCompany()

        val ids = repository.observeCompany().first().map { it.id }
        assertEquals(setOf("r1", "r2", "vieja-pendiente"), ids.toSet())
        assertEquals(3, ids.size)
    }

    @Test
    fun siFallanLasPendientes_noSeGuardaNada() = runTest {
        api.handler = { request ->
            if (request.param("estado") == "Pendiente") noConnection() else respondPage(list(requestJson("r1")), total = 1)
        }

        val result = repository.refreshCompany()

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
        assertTrue(repository.observeCompany().first().isEmpty())
    }

    @Test
    fun elDetalleSeAbreConLoGuardado_deLaEmpresaODeLasMias() = runTest {
        api.handler = { respondPage(list(requestJson("mia")), total = 1) }
        repository.refreshMine()

        assertEquals("mia", repository.observeCompanyRequest("mia").first()?.id)
        assertNull(repository.observeCompanyRequest("otra").first())
    }

    @Test
    fun detalle_traeLaLicenciaDelSolicitanteYElCarro() = runTest {
        api.handler = {
            respondJson(
                """{"solicitudReservaId":"r1","carroId":"car-1","estado":"pendiente","motivoSolicitud":"Visita",""" +
                    """"solicitante":{"empleadoId":"emp-1","nombreCompleto":"Juan Pérez","tieneLicencia":true,"fotoLicenciaUrl":"http://x/lic.jpg"},""" +
                    """"carro":{"carroId":"car-1","marca":"Nissan","modelo":"Versa","placas":"ABC-123","anio":2022,"kilometrajeActual":15000.0,"estado":"disponible"}}""",
            )
        }

        val result = repository.getDetail("r1")

        val detail = when (result) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> error("No debía fallar: ${result.error}")
        }
        assertTrue(detail.requesterHasLicense)
        assertEquals("http://x/lic.jpg", detail.requesterLicenseUrl)
        assertEquals(2022, detail.vehicleYear)
        assertEquals("Nissan Versa", detail.request.vehicleTitle)
        assertEquals(ReservationStatus.PENDING, detail.request.status)
    }

    @Test
    fun aceptar_mandaQuienConfirma_yVuelveALeerLaLista() = runTest {
        api.handler = { request ->
            request.recordBody()
            when {
                request.method == HttpMethod.Post -> respondJson("{}")
                request.param("estado") == "Pendiente" -> respondJson("[]")
                else -> respondPage(list(requestJson("r1", "aprobada")), total = 1)
            }
        }

        val result = repository.approve("r1")

        assertEquals(AppResult.Success(Unit), result)
        assertEquals("POST solicitudes-reserva/r1/aprobar", api.requests.first())
        assertTrue(sentBodies.single().contains("\"empleadoConfirmadorId\":\"emp-1\""))
        assertEquals(ReservationStatus.APPROVED, repository.observeCompany().first().single().status)
    }

    @Test
    fun rechazar_mandaElMotivo() = runTest {
        api.handler = { request ->
            request.recordBody()
            when {
                request.method == HttpMethod.Post -> respondJson("{}")
                request.param("estado") == "Pendiente" -> respondJson("[]")
                else -> respondPage("[]", total = 0)
            }
        }

        repository.reject("r1", "No hay carros")

        assertEquals("POST solicitudes-reserva/r1/rechazar", api.requests.first())
        assertTrue(sentBodies.single().contains("\"motivo\":\"No hay carros\""))
    }

    @Test
    fun siAceptarFalla_noRecargaLaLista() = runTest {
        api.handler = { noConnection() }

        val result = repository.approve("r1")

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
        assertEquals(1, api.requests.size)
    }

    @Test
    fun aceptarSinEmpleado_noLlamaALaApi() = runTest {
        session.state.value = testSession(employeeId = null)

        assertEquals(AppResult.Failure(AppError.NoCompany), repository.approve("r1"))
        assertTrue(api.requests.isEmpty())
    }

    // ---------- Campana ----------

    private val notifications =
        """[{"notificacionId":"n1","tipo":"resultadoSolicitud","leida":false},""" +
            """{"notificacionId":"n2","tipo":"resultadoSolicitud","leida":true},""" +
            """{"notificacionId":"n3","tipo":"resultadoSolicitud","leida":false}]"""

    @Test
    fun campana_cuentaLasRespuestasSinLeer() = runTest {
        api.handler = { respondJson(notifications) }

        repository.refreshUnseenUpdates()

        assertEquals(2, repository.observeUnseenUpdates().first())
    }

    @Test
    fun abrirLaCampana_marcaSoloLasNoLeidas_yElNumeroVuelveACero() = runTest {
        api.handler = { request -> if (request.method == HttpMethod.Post) respondJson("{}") else respondJson(notifications) }
        repository.refreshUnseenUpdates()

        repository.markUpdatesSeen()

        val marked = api.requests.filter { it.startsWith("POST") }.toSet()
        assertEquals(setOf("POST notificaciones/n1/marcar-leida", "POST notificaciones/n3/marcar-leida"), marked)
        assertEquals(0, repository.observeUnseenUpdates().first())
    }

    @Test
    fun cerrarSesion_borraTodo() = runTest {
        api.handler = { respondPage(list(requestJson("a")), total = 1) }
        repository.refreshMine()

        repository.clear()

        assertTrue(repository.observeMine().first().isEmpty())
        assertTrue(repository.observeCompany().first().isEmpty())
    }
}
