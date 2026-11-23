package org.vicpoo.gasolineproject.di

import androidx.lifecycle.ViewModel
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
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.common.AppLogger
import org.vicpoo.gasolineproject.domain.common.AppScope
import org.vicpoo.gasolineproject.domain.common.SilentAppLogger
import org.vicpoo.gasolineproject.domain.location.LocationFix
import org.vicpoo.gasolineproject.domain.location.LocationProvider
import org.vicpoo.gasolineproject.domain.location.TrackingServiceController
import org.vicpoo.gasolineproject.domain.connectivity.ConnectivityMonitor
import org.vicpoo.gasolineproject.domain.files.AppDirectories
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.push.PushTokenProvider
import org.vicpoo.gasolineproject.ui.common.rbac.AccessGateViewModel
import org.vicpoo.gasolineproject.ui.common.theme.ThemeViewModel
import org.vicpoo.gasolineproject.domain.rbac.AccessRule
import org.vicpoo.gasolineproject.domain.session.InMemoryTokenStore
import org.vicpoo.gasolineproject.domain.session.TokenStore
import org.vicpoo.gasolineproject.domain.session.LogoutListener
import org.vicpoo.gasolineproject.domain.sync.BackgroundSyncScheduler
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask
import org.vicpoo.gasolineproject.domain.sync.SyncAllPendingUseCase
import org.vicpoo.gasolineproject.data.di.apiConfigModule
import org.vicpoo.gasolineproject.data.di.dataPlatformModule
import org.vicpoo.gasolineproject.platform.di.platformModule
import org.vicpoo.gasolineproject.ui.license.LicenseGateViewModel
import org.vicpoo.gasolineproject.ui.license.LicenseViewModel
import org.vicpoo.gasolineproject.ui.auth.LoginViewModel
import org.vicpoo.gasolineproject.ui.profile.ProfileViewModel
import org.vicpoo.gasolineproject.ui.profile.editname.EditNameViewModel
import org.vicpoo.gasolineproject.ui.reports.ReportsViewModel
import org.vicpoo.gasolineproject.ui.reservations.bell.ReservationsBellViewModel
import org.vicpoo.gasolineproject.ui.reservations.assignmentdetail.AssignmentDetailViewModel
import org.vicpoo.gasolineproject.ui.reservations.assignments.AssignmentsViewModel
import org.vicpoo.gasolineproject.ui.reservations.company.CompanyReservationsViewModel
import org.vicpoo.gasolineproject.ui.reservations.detail.ReservationDetailViewModel
import org.vicpoo.gasolineproject.ui.reservations.list.MyReservationsViewModel
import org.vicpoo.gasolineproject.ui.triprecording.active.ActiveTripViewModel
import org.vicpoo.gasolineproject.ui.triprecording.assigned.AssignedVehiclesViewModel
import org.vicpoo.gasolineproject.ui.triprecording.finish.FinishTripViewModel
import org.vicpoo.gasolineproject.ui.triprecording.fuel.FuelRecordFormArgs
import org.vicpoo.gasolineproject.ui.triprecording.fuel.FuelRecordFormViewModel
import org.vicpoo.gasolineproject.ui.triprecording.start.StartTripArgs
import org.vicpoo.gasolineproject.ui.triprecording.start.StartTripViewModel
import org.vicpoo.gasolineproject.ui.triprecording.syncstatus.SyncStatusViewModel
import org.vicpoo.gasolineproject.ui.trips.detail.TripDetailArgs
import org.vicpoo.gasolineproject.ui.trips.detail.TripDetailViewModel
import org.vicpoo.gasolineproject.ui.trips.history.TripHistoryViewModel
import org.vicpoo.gasolineproject.ui.vehicles.detail.VehicleDetailArgs
import org.vicpoo.gasolineproject.ui.vehicles.detail.VehicleDetailViewModel
import org.vicpoo.gasolineproject.ui.vehicles.form.VehicleFormArgs
import org.vicpoo.gasolineproject.ui.vehicles.form.VehicleFormViewModel
import org.vicpoo.gasolineproject.ui.vehicles.list.VehicleListArgs
import org.vicpoo.gasolineproject.ui.vehicles.list.VehicleListViewModel
import org.vicpoo.gasolineproject.ui.vehicles.list.VehiclesMode
import org.vicpoo.gasolineproject.navigation.MainViewModel
import org.vicpoo.gasolineproject.navigation.StartViewModel
import org.vicpoo.gasolineproject.testing.FakeApiServer
import org.vicpoo.gasolineproject.testing.FakeConnectivityMonitor
import org.vicpoo.gasolineproject.testing.RecordingSyncScheduler
import org.vicpoo.gasolineproject.testing.TEST_API_BASE_URL
import java.nio.file.Files
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Red de seguridad del refactor multimódulo: arma el grafo REAL de Koin (appModules) y pide
 * cada ViewModel. Si al mover un módulo se pierde una definición, esta prueba falla con el
 * nombre de la clase que falta.
 *
 * Solo se reemplaza lo que depende del teléfono (platformModule y dataPlatformModule) y lo que haría trabajo real:
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
        single<ConnectivityMonitor> { FakeConnectivityMonitor() }
        single<TokenStore> { InMemoryTokenStore() }
        single<LocationProvider> { NoLocationProvider }
        single<ImageCompressor> { ImageCompressor { it } }
        single<TrackingServiceController> { NoTrackingService }
        single<BackgroundSyncScheduler> { RecordingSyncScheduler() }
        single<PushTokenProvider> { NoPushTokenProvider }
        single<AppLogger> { SilentAppLogger }
    }

    private val noRealWorkModule = module {
        single { AppScope(CoroutineScope(SupervisorJob() + StandardTestDispatcher())) }
        single<HttpClient> { FakeApiServer().client }
    }

    private fun testModules(): List<Module> =
        appModules.filterNot { it === platformModule || it === dataPlatformModule } +
            testPlatformModule + noRealWorkModule + apiConfigModule(TEST_API_BASE_URL)

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
            // vehicles, triprecording, push y la cola de solicitudes de reservas.
            // Si cambia el número, revisa que no se haya perdido un `bind`.
            assertEquals(4, app.koin.getAll<PendingSyncTask>().size)
            // lastRefresh, push, reports, reservations, tripRecording, trips, vehicles e imageCache.
            // (temporaryFiles es de platformModule, que esta prueba reemplaza)
            assertEquals(8, app.koin.getAll<LogoutListener>().size)
        } finally {
            app.close()
        }
    }

    /**
     * Red de seguridad de esta misma prueba: si se registra un ViewModel en Koin y se olvida pedirlo
     * en [resolveAllViewModels], nadie validaría que se puede crear. Aquí se comparan los registrados
     * contra los pedidos y la prueba dice cuál falta.
     */
    @OptIn(KoinInternalApi::class)
    @Test
    fun todosLosViewModelsRegistrados_sePidenEnEstaPrueba() {
        val app = koinApplication {
            allowOverride(true)
            modules(testModules())
        }
        try {
            resolveAllViewModels(app.koin)
            val registered = app.koin.instanceRegistry.instances.values
                .map { it.beanDefinition.primaryType }
                .filter { ViewModel::class.java.isAssignableFrom(it.java) }
                .toSet()

            val missing = (registered - resolved).mapNotNull { it.simpleName }.sorted()
            assertTrue(
                missing.isEmpty(),
                "ViewModels registrados en Koin que KoinGraphTest no pide: $missing. Agrégalos a resolveAllViewModels().",
            )
        } finally {
            app.close()
        }
    }

    /** ViewModels que esta prueba ya pidió (para detectar los que se registraron y nadie pidió). */
    private val resolved = mutableSetOf<KClass<*>>()

    /** Pide [T] a Koin (con sus parámetros) y lo anota en [resolved]. */
    private inline fun <reified T : Any> Koin.track(noinline parameters: ParametersDefinition? = null) {
        get<T>(parameters = parameters)
        resolved += T::class
    }

    /** Si agregas un ViewModel en UiModule.kt (o en navigationModule), pídelo aquí con sus parámetros. */
    private fun resolveAllViewModels(koin: Koin) {
        // core / navegación
        koin.track<AccessGateViewModel> { parametersOf(AccessRule.Everyone) }
        koin.track<StartViewModel>()
        koin.track<ThemeViewModel>()
        koin.track<MainViewModel>()

        // auth
        koin.track<LoginViewModel>()
        koin.track<LicenseViewModel>()
        koin.track<LicenseGateViewModel>()

        // profile
        koin.track<ProfileViewModel>()
        koin.track<EditNameViewModel>()

        // vehicles
        VehiclesMode.entries.forEach { mode ->
            koin.track<VehicleListViewModel> { parametersOf(VehicleListArgs(mode)) }
            koin.track<VehicleDetailViewModel> { parametersOf(VehicleDetailArgs(vehicleId = "loc-1", mode = mode)) }
        }
        koin.track<VehicleFormViewModel> { parametersOf(VehicleFormArgs(vehicleId = null)) }

        // reservations
        koin.track<MyReservationsViewModel>()
        koin.track<ReservationsBellViewModel>()
        koin.track<CompanyReservationsViewModel>()
        koin.track<AssignmentsViewModel>()
        koin.track<AssignmentDetailViewModel> { parametersOf("r:res-1") }
        koin.track<ReservationDetailViewModel> { parametersOf("res-1") }

        // trips
        koin.track<TripHistoryViewModel>()
        koin.track<TripDetailViewModel> { parametersOf(TripDetailArgs(tripId = "trip-1")) }

        // triprecording
        koin.track<AssignedVehiclesViewModel>()
        koin.track<ActiveTripViewModel>()
        koin.track<FinishTripViewModel>()
        koin.track<StartTripViewModel> { parametersOf(StartTripArgs(reservationId = "res-1")) }
        koin.track<FuelRecordFormViewModel> { parametersOf(FuelRecordFormArgs(recordId = null)) }
        koin.track<SyncStatusViewModel>()

        // reports
        koin.track<ReportsViewModel>()
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
