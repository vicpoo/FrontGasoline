package org.vicpoo.gasolineproject.data.triprecording

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.vicpoo.gasolineproject.domain.common.AppClock
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.GeoMath
import org.vicpoo.gasolineproject.domain.common.GeoPoint
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.location.LocationFix
import org.vicpoo.gasolineproject.domain.location.LocationProvider
import org.vicpoo.gasolineproject.domain.location.TrackingServiceController
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.session.SessionReader
import org.vicpoo.gasolineproject.domain.sync.SyncRejection
import org.vicpoo.gasolineproject.data.triprecording.local.PauseEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TrackPointEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripEntity
import org.vicpoo.gasolineproject.data.triprecording.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.domain.triprecording.AssignedVehicle
import org.vicpoo.gasolineproject.domain.triprecording.CostCenter
import org.vicpoo.gasolineproject.domain.triprecording.RecordedTrip
import org.vicpoo.gasolineproject.domain.triprecording.SEND_WAIT_MS
import org.vicpoo.gasolineproject.domain.triprecording.SendOutcome
import org.vicpoo.gasolineproject.domain.triprecording.TrackingSession
import org.vicpoo.gasolineproject.domain.triprecording.TripPhase
import org.vicpoo.gasolineproject.domain.triprecording.TripRecordingSync
import org.vicpoo.gasolineproject.domain.triprecording.TripTracker
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Después de guardar "Registrar Viaje": sincroniza y le dice al usuario qué pasó.
 * - Enviado: la API ya lo tiene (o solo falta la foto, que se sube sola después).
 * - Guardado en el teléfono: sin red o el servidor no respondió; se enviará solo después.
 * - Rechazado: la API no lo aceptó (4xx, ej. kilometraje menor al registrado), con su mensaje.
 */
internal class FinishSendReporter(
    private val store: TripRecordingLocalStore,
    private val sync: TripRecordingSync,
) {
    suspend fun report(localId: String): SendOutcome {
        // Se espera como máximo unos segundos; con señal débil el envío sigue en segundo plano
        val result = sync.syncWithin(SEND_WAIT_MS)
        val trip = store.current().trips.firstOrNull { it.localId == localId }
        return when {
            trip == null -> SendOutcome.Sent // ya se envió todo y se borró del teléfono
            trip.closedOnServer -> SendOutcome.Sent // falta solo la foto: se sube sola después
            // Solo un rechazo DEFINITIVO (4xx: el dato está mal) regresa al usuario a corregir
            trip.syncErrorMessage != null && SyncRejection.isPermanent(trip.syncErrorCode) ->
                SendOutcome.Rejected(trip.syncErrorMessage)
            // Sin internet o sigue enviándose: quedó guardado y se envía solo
            result == null -> SendOutcome.SavedOffline
            result is AppResult.Failure && result.error == AppError.NoConnection -> SendOutcome.SavedOffline
            // Hay internet pero el servidor falló (5xx, respuesta ilegible...): también quedó guardado,
            // pero NO se le dice al usuario que no tiene internet
            trip.syncErrorMessage != null || result is AppResult.Failure ->
                SendOutcome.SavedServerError(trip.syncErrorMessage)
            // Todo se mandó menos el cierre (ej. sesión vencida): se termina en segundo plano
            else -> SendOutcome.SavedOffline
        }
    }
}
