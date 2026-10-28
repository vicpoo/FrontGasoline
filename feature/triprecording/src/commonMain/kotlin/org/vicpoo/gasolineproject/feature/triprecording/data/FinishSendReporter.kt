package org.vicpoo.gasolineproject.feature.triprecording.data

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
import org.vicpoo.gasolineproject.core.common.AppClock
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.GeoMath
import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.core.common.AppScope
import org.vicpoo.gasolineproject.core.location.LocationFix
import org.vicpoo.gasolineproject.core.location.LocationProvider
import org.vicpoo.gasolineproject.core.location.TrackingServiceController
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.core.session.SessionReader
import org.vicpoo.gasolineproject.feature.triprecording.data.local.PauseEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TrackPointEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TripEntity
import org.vicpoo.gasolineproject.feature.triprecording.data.local.TripRecordingLocalStore
import org.vicpoo.gasolineproject.feature.triprecording.domain.AssignedVehicle
import org.vicpoo.gasolineproject.feature.triprecording.domain.CostCenter
import org.vicpoo.gasolineproject.feature.triprecording.domain.RecordedTrip
import org.vicpoo.gasolineproject.feature.triprecording.domain.SendOutcome
import org.vicpoo.gasolineproject.feature.triprecording.domain.TrackingSession
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripPhase
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripRecordingSync
import org.vicpoo.gasolineproject.feature.triprecording.domain.TripTracker
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Después de guardar "Registrar Viaje": sincroniza y le dice al usuario qué pasó.
 * - Enviado: la API ya lo tiene (o solo falta la foto, que se sube sola después).
 * - Guardado sin red: se enviará solo cuando vuelva la conexión.
 * - Rechazado: la API no lo aceptó (ej. kilometraje menor al registrado), con su mensaje.
 */
internal class FinishSendReporter(
    private val store: TripRecordingLocalStore,
    private val sync: TripRecordingSync,
) {
    suspend fun report(localId: String): SendOutcome {
        val result = sync.sync()
        val trip = store.current().trips.firstOrNull { it.localId == localId }
        return when {
            trip == null -> SendOutcome.Sent // ya se envió todo y se borró del teléfono
            trip.syncErrorMessage != null -> SendOutcome.Rejected(trip.syncErrorMessage)
            trip.closedOnServer -> SendOutcome.Sent // falta solo la foto: se sube sola después
            result is AppResult.Failure && result.error != AppError.NoConnection -> SendOutcome.Rejected(null)
            else -> SendOutcome.SavedOffline
        }
    }
}
