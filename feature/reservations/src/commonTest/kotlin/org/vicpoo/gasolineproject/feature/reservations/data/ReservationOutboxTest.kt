package org.vicpoo.gasolineproject.feature.reservations.data

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.core.common.AppClock
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.data.JsonFileCache
import org.vicpoo.gasolineproject.feature.reservations.api.RequestOutcome
import org.vicpoo.gasolineproject.feature.reservations.api.RequestedVehicle
import org.vicpoo.gasolineproject.feature.reservations.data.local.CompanyReservationsSnapshot
import org.vicpoo.gasolineproject.feature.reservations.data.local.MyReservationsSnapshot
import org.vicpoo.gasolineproject.feature.reservations.data.local.ReservationOutbox
import org.vicpoo.gasolineproject.feature.reservations.data.local.ReservationOutboxSnapshot
import org.vicpoo.gasolineproject.feature.reservations.data.remote.KtorReservationApi
import org.vicpoo.gasolineproject.feature.reservations.data.sync.ReservationOutboxSyncer
import org.vicpoo.gasolineproject.feature.reservations.domain.ReservationStatus
import org.vicpoo.gasolineproject.testing.FakeApiServer
import org.vicpoo.gasolineproject.testing.FakeSessionReader
import org.vicpoo.gasolineproject.testing.RecordingSyncScheduler
import org.vicpoo.gasolineproject.testing.TestStorage
import org.vicpoo.gasolineproject.testing.noConnection
import org.vicpoo.gasolineproject.testing.respondJson
import org.vicpoo.gasolineproject.testing.respondProblem
import org.vicpoo.gasolineproject.testing.testJson
import org.vicpoo.gasolineproject.testing.testSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** "Solicitar vehículo" sin conexión: se guarda en cola y se envía sola al volver la red. */
class ReservationOutboxTest {

    private val api = FakeApiServer()
    private val storage = TestStorage()
    private val scheduler = RecordingSyncScheduler()
    private val session = FakeSessionReader(testSession())

    private fun <T : Any> cache(name: String, serializer: kotlinx.serialization.KSerializer<T>, empty: T) =
        JsonFileCache(storage.fileSystem, storage.directories, name, serializer, testJson, empty)

    private val outbox = ReservationOutbox(cache("outbox.json", ReservationOutboxSnapshot.serializer(), ReservationOutboxSnapshot()))

    private val clock = object : AppClock {
        override fun nowMillis(): Long = 0
        override fun toLocalIso(epochMillis: Long): String = "2026-10-27T10:00:00-06:00"
    }

    private val repository = ReservationRepositoryImpl(
        api = KtorReservationApi(api.client),
        sessionReader = session,
        cache = cache("mine.json", MyReservationsSnapshot.serializer(), MyReservationsSnapshot()),
        companyCache = cache("company.json", CompanyReservationsSnapshot.serializer(), CompanyReservationsSnapshot()),
        outbox = outbox,
        clock = clock,
        scheduler = scheduler,
    )

    private var refreshesAfterSend = 0
    private val syncer = ReservationOutboxSyncer(
        outbox = outbox,
        api = KtorReservationApi(api.client),
        sessionReader = session,
        scheduler = scheduler,
        onSent = { refreshesAfterSend++ },
    )

    private val vocho = RequestedVehicle(serverId = "car-1", title = "Volkswagen Sedán", plates = "ABC-123")

    @Test
    fun sinRed_quedaEnCola_yApareceComoPorEnviar() = runTest {
        api.handler = { noConnection() }

        val result = repository.requestVehicle(vocho, "Visita a cliente")

        assertEquals(AppResult.Success(RequestOutcome.QUEUED), result)
        val mine = repository.observeMine().first()
        assertEquals(1, mine.size)
        assertEquals(ReservationStatus.NOT_SENT, mine.single().status)
        assertEquals("Volkswagen Sedán", mine.single().vehicleTitle)
        assertEquals("Visita a cliente", mine.single().requestReason)
        assertEquals(1, scheduler.whenOnlineCalls)
    }

    @Test
    fun sinRed_dosVecesElMismoCarro_soloQuedaUna() = runTest {
        api.handler = { noConnection() }

        repository.requestVehicle(vocho, "Primera")
        repository.requestVehicle(vocho, "Segunda")

        val pending = outbox.all()
        assertEquals(1, pending.size)
        assertEquals("Segunda", pending.single().reason)
    }

    @Test
    fun otroError_noSeGuardaEnCola() = runTest {
        api.handler = { respondProblem(HttpStatusCode.Conflict, "El carro no está disponible") }

        val result = repository.requestVehicle(vocho, "Visita")

        assertEquals(AppResult.Failure(AppError.Server(409, "El carro no está disponible")), result)
        assertTrue(outbox.all().isEmpty())
    }

    @Test
    fun alVolverLaRed_seEnvia_yRecargaMisSolicitudes() = runTest {
        api.handler = { noConnection() }
        repository.requestVehicle(vocho, "Visita")

        api.handler = { respondJson("{}", HttpStatusCode.Created) }
        val result = syncer.syncPending()

        assertEquals(AppResult.Success(Unit), result)
        assertTrue(outbox.all().isEmpty())
        assertEquals(1, refreshesAfterSend)
        assertEquals("POST solicitudes-reserva", api.requests.last())
    }

    @Test
    fun rechazo409_quedaComoNoSePudoEnviar_yNoSeReenviaSola() = runTest {
        api.handler = { noConnection() }
        repository.requestVehicle(vocho, "Visita")
        val requestsBefore = api.requests.size

        api.handler = { respondProblem(HttpStatusCode.Conflict, "Ya tienes una solicitud pendiente para este carro") }
        syncer.syncPending()
        syncer.syncPending()

        assertEquals(requestsBefore + 1, api.requests.size, "Un 4xx no se reenvía en automático")
        val mine = repository.observeMine().first().single()
        assertEquals(ReservationStatus.SEND_FAILED, mine.status)
        assertEquals("Ya tienes una solicitud pendiente para este carro", mine.resolutionReason)
        assertEquals(0, refreshesAfterSend)
    }

    @Test
    fun sinRedOtraVez_sigueEnCola_yPideReintentar() = runTest {
        api.handler = { noConnection() }
        repository.requestVehicle(vocho, "Visita")

        val result = syncer.syncPending()

        assertEquals(AppResult.Failure(AppError.NoConnection), result)
        assertEquals(1, outbox.all().size)
        assertEquals(2, scheduler.whenOnlineCalls)
    }
}
