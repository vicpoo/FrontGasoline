package org.vicpoo.gasolineproject.core.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okio.FileSystem
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.vicpoo.gasolineproject.core.common.AppScope
import org.vicpoo.gasolineproject.core.location.LocationFix
import org.vicpoo.gasolineproject.core.location.LocationProvider
import org.vicpoo.gasolineproject.core.location.TrackingServiceController
import org.vicpoo.gasolineproject.core.platform.AppDirectories
import org.vicpoo.gasolineproject.core.platform.ImageCompressor
import org.vicpoo.gasolineproject.core.push.PushTokenProvider
import org.vicpoo.gasolineproject.core.rbac.AccessGateViewModel
import org.vicpoo.gasolineproject.core.rbac.AccessRule
import org.vicpoo.gasolineproject.core.session.DATASTORE_FILE_NAME
import org.vicpoo.gasolineproject.core.session.LogoutListener
import org.vicpoo.gasolineproject.core.session.createDataStore
import org.vicpoo.gasolineproject.core.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.core.sync.PendingSyncTask
import org.vicpoo.gasolineproject.core.sync.SyncAllPendingUseCase
import org.vicpoo.gasolineproject.di.appModules
import org.vicpoo.gasolineproject.feature.auth.presentation.license.LicenseGateViewModel
import org.vicpoo.gasolineproject.feature.auth.presentation.license.LicenseViewModel
import org.vicpoo.gasolineproject.feature.auth.presentation.login.LoginViewModel
import org.vicpoo.gasolineproject.feature.profile.presentation.ProfileViewModel
import org.vicpoo.gasolineproject.feature.profile.presentation.editname.EditNameViewModel
import org.vicpoo.gasolineproject.feature.reservations.presentation.bell.ReservationsBellViewModel
import org.vicpoo.gasolineproject.feature.reservations.presentation.company.CompanyReservationsViewModel
import org.vicpoo.gasolineproject.feature.reservations.presentation.detail.ReservationDetailViewModel
import org.vicpoo.gasolineproject.feature.reservations.presentation.list.MyReservationsViewModel
import org.vicpoo.gasolineproject.feature.triprecording.presentation.active.ActiveTripViewModel
import org.vicpoo.gasolineproject.feature.triprecording.presentation.assigned.AssignedVehiclesViewModel
import org.vicpoo.gasolineproject.feature.triprecording.presentation.finish.FinishTripViewModel
import org.vicpoo.gasolineproject.feature.triprecording.presentation.fuel.FuelRecordFormArgs
import org.vicpoo.gasolineproject.feature.triprecording.presentation.fuel.FuelRecordFormViewModel
import org.vicpoo.gasolineproject.feature.triprecording.presentation.start.StartTripArgs
import org.vicpoo.gasolineproject.feature.triprecording.presentation.start.StartTripViewModel
import org.vicpoo.gasolineproject.feature.trips.presentation.detail.TripDetailArgs
import org.vicpoo.gasolineproject.feature.trips.presentation.detail.TripDetailViewModel
import org.vicpoo.gasolineproject.feature.trips.presentation.history.TripHistoryViewModel
import org.vicpoo.gasolineproject.feature.vehicles.presentation.detail.VehicleDetailArgs
import org.vicpoo.gasolineproject.feature.vehicles.presentation.detail.VehicleDetailViewModel
import org.vicpoo.gasolineproject.feature.vehicles.presentation.form.VehicleFormArgs
import org.vicpoo.gasolineproject.feature.vehicles.presentation.form.VehicleFormViewModel
import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehicleListArgs
import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehicleListViewModel
import org.vicpoo.gasolineproject.feature.vehicles.presentation.list.VehiclesMode
import org.vicpoo.gasolineproject.navigation.MainViewModel
import org.vicpoo.gasolineproject.navigation.StartViewModel
import org.vicpoo.gasolineproject.testing.FakeApiServer
import org.vicpoo.gasolineproject.testing.RecordingSyncScheduler
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Red de seguridad del refactor multimódulo: arma el grafo REAL de Koin (appModules) y pide
 * cada ViewModel. Si al mover un módulo se pierde una definición, esta prueba falla con el
 * nombre de la clase que falta.
 *
 * Solo se reemplaza lo que depende del teléfono (platformModule) y lo que haría trabajo real:
 * - AppScope: un scope que nunca avanza (nada corre en segundo plano).
 * - HttpClient: API falsa (ninguna petición sale a internet).
 * - Dispatchers.Main: dispatcher de pruebas que no avanza (los init de los ViewModels no corren).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KoinGraphTest {

    private val filesDir = Files.createTempDirectory("koin-graph").toString()

    private val testPlatformModule = module {
        single { AppDirectories(filesDir = filesDir) }
        single<FileSystem> { FileSystem.SYSTEM }
        single { createDataStore { "$filesDir/$DATASTORE_FILE_NAME" } }
        single<LocationProvider> { NoLocationProvider }
        single<ImageCompressor> { ImageCompressor { it } }
        single<TrackingServiceController> { NoTrackingService }
        single<BackgroundSyncScheduler> { RecordingSyncScheduler() }
        single<PushTokenProvider> { NoPushTokenProvider }
    }

    private val noRealWorkModule = module {
        single { AppScope(CoroutineScope(SupervisorJob() + StandardTestDispatcher())) }
        single<HttpClient> { FakeApiServer().client }
    }

    private fun testModules(): List<Module> =
        appModules.filterNot { it === platformModule } + testPlatformModule + noRealWorkModule

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun elGrafoCompletoSeArma_yCadaViewModelSePuedeCrear() {
        // koinApplication también crea los createdAtStart (BackgroundSyncCoordinator, GpsTripTracker, DeviceRegistrar)
        val app = koinApplication {
            allowOverride(true)
            modules(testModules())
        }
        try {
            val koin = app.koin
            koin.get<SyncAllPendingUseCase>()
            resolveAllViewModels(koin)
        } finally {
            app.close()
        }
    }

    @Test
    fun lasFeaturesSiguenRegistradasEnSincronizacionYCierreDeSesion() {
        val app = koinApplication {
            allowOverride(true)
            modules(testModules())
        }
        try {
            // vehicles, triprecording y push. Si cambia el número, revisa que no se haya perdido un `bind`.
            assertEquals(3, app.koin.getAll<PendingSyncTask>().size)
            // push, reservations, tripRecording, trips y vehicles.
            assertEquals(5, app.koin.getAll<LogoutListener>().size)
        } finally {
            app.close()
        }
    }

    private fun resolveAllViewModels(koin: Koin) {
        // core / navegación
        koin.get<AccessGateViewModel> { parametersOf(AccessRule.Everyone) }
        koin.get<StartViewModel>()
        koin.get<MainViewModel>()

        // auth
        koin.get<LoginViewModel>()
        koin.get<LicenseViewModel>()
        koin.get<LicenseGateViewModel>()

        // profile
        koin.get<ProfileViewModel>()
        koin.get<EditNameViewModel>()

        // vehicles
        VehiclesMode.entries.forEach { mode ->
            koin.get<VehicleListViewModel> { parametersOf(VehicleListArgs(mode)) }
            koin.get<VehicleDetailViewModel> { parametersOf(VehicleDetailArgs(vehicleId = "loc-1", mode = mode)) }
        }
        koin.get<VehicleFormViewModel> { parametersOf(VehicleFormArgs(vehicleId = null)) }

        // reservations
        koin.get<MyReservationsViewModel>()
        koin.get<ReservationsBellViewModel>()
        koin.get<CompanyReservationsViewModel>()
        koin.get<ReservationDetailViewModel> { parametersOf("res-1") }

        // trips
        koin.get<TripHistoryViewModel>()
        koin.get<TripDetailViewModel> { parametersOf(TripDetailArgs(tripId = "trip-1")) }

        // triprecording
        koin.get<AssignedVehiclesViewModel>()
        koin.get<ActiveTripViewModel>()
        koin.get<FinishTripViewModel>()
        koin.get<StartTripViewModel> { parametersOf(StartTripArgs(reservationId = "res-1")) }
        koin.get<FuelRecordFormViewModel> { parametersOf(FuelRecordFormArgs(recordId = null)) }
    }

    private object NoLocationProvider : LocationProvider {
        override fun hasPermission(): Boolean = false
        override fun isLocationEnabled(): Boolean = false
        override fun updates(intervalMillis: Long, minDistanceMeters: Float): Flow<LocationFix> = emptyFlow()
        override suspend fun lastKnown(): LocationFix? = null
    }

    private object NoTrackingService : TrackingServiceController {
        override fun start() = Unit
        override fun stop() = Unit
    }

    private object NoPushTokenProvider : PushTokenProvider {
        override val platform: String = "test"
        override suspend fun currentToken(): String? = null
        override suspend fun deleteToken() = Unit
    }
}
