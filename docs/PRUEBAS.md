# Pruebas

## Qué cubre cada módulo

| Módulo | Dónde | Qué se prueba | Cómo corre |
|---|---|---|---|
| `domain` | `commonTest` | Casos de uso, validadores, reintentos (`RunPendingSyncUseCase`), bitácora | JVM: `:domain:testAndroidHostTest` |
| `data` | `commonTest` | Repositorios contra una API falsa (MockEngine) y la base en memoria: sesión y renovación de tokens, viajes y GPS (`GpsTripTracker`), sincronizadores, cola de solicitudes, vehículos y conflictos, paginación por llave, migración de archivos viejos (`LegacyDataMigrationTest`) | JVM: `:data:testAndroidHostTest` |
| `platform` | `commonTest` / `androidHostTest` | Nombres de archivo seguros, temporales de cámara y reportes | JVM: `:platform:testAndroidHostTest` |
| `platform` | `androidDeviceTest` | Lo que necesita Android de verdad: SQLCipher (`AndroidSqlDriverFactoryTest`), cifrado de fotos con el Keystore (`KeystoreFileCipherTest`), compresión de fotos y EXIF (`AndroidImageCompressorTest`) | Emulador / celular: `:platform:connectedAndroidDeviceTest` |
| `ui` | `commonTest` | Cada ViewModel: estados, eventos, efectos y errores | JVM: `:ui:testAndroidHostTest` |
| `shared` | `androidHostTest` | `KoinGraphTest` (se arma el grafo real y se pide cada ViewModel registrado) y `ArchitectureTest` (capas, `expect` solo en platform/ui, un estado por archivo) | JVM: `:shared:testAndroidHostTest` |
| `database` | — | Que las migraciones `.sqm` lleguen al esquema actual | `:database:verifyCommonMainAppDatabaseMigration` |

Todo junto (lo que conviene correr antes de cada commit):

```powershell
.\gradlew :database:verifyCommonMainAppDatabaseMigration :domain:testAndroidHostTest :data:testAndroidHostTest :platform:testAndroidHostTest :ui:testAndroidHostTest :shared:testAndroidHostTest :androidApp:assembleDebug --continue
```

Los reportes HTML quedan en `<módulo>/build/reports/tests/testAndroidHostTest/index.html`.

## El módulo `:testing`

Solo se usa desde `commonTest`: `commonTest.dependencies { implementation(project(":testing")) }`.

| Pieza | Para qué |
|---|---|
| `FakeApiServer` (`TestNetwork.kt`) | API falsa con `MockEngine` de Ktor. `handler = { request -> respondJson(...) }`; guarda cada petición en `requests` ("METHOD ruta") para revisar qué se llamó. `noConnection()` simula sin red |
| `TestStorage` | Sistema de archivos en memoria (`FakeFileSystem`), `AppDirectories`, base SQLite en memoria (`database`) y un cifrado de prueba (`fileCipher`) |
| `testLocalDatabase()`, `pagedList(...)`, `TestReservationStorage` | Base y listas paginadas en memoria, como las arma Koin |
| `TestFileCipher` | "Cifra" con el encabezado real: permite revisar que en disco no queda la foto en claro; `failDecrypt` simula llave perdida |
| `MainDispatcherTest` | Clase base para pruebas de ViewModel: pone `Dispatchers.Main` de pruebas |
| `FakeSessionReader`, `FakePermissionChecker`, `FakeConnectivityMonitor`, `FakeAppClock`, `FakeAppLogger`, `FakeClientDeviceInfo`, `RecordingSyncScheduler` | Sesión, privilegios, red, reloj, bitácora y planificador controlados por la prueba |
| `TripRecordingFakes.kt`, `ReservationFakes.kt`, `VehicleTripProfileFakes.kt` | Repositorios falsos y constructores de datos (`testRecordedTrip`, `testReservation`, `testVehicle`...) por feature |

## Cómo probar un ViewModel nuevo

```kotlin
class MiPantallaViewModelTest : MainDispatcherTest() {

    private val repo = FakeMiRepositorio()            // de :testing, o uno nuevo ahí mismo
    private fun viewModel() = MiPantallaViewModel(ObserveAlgoUseCase(repo), GuardarAlgoUseCase(repo))

    @Test
    fun alGuardar_muestraElResultado() = runTest {
        val vm = viewModel()
        // StateFlow con stateIn(WhileSubscribed): hay que suscribirse para que corra
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }

        vm.onEvent(MiPantallaEvent.Guardar)
        runCurrent()

        assertEquals(..., vm.state.value.algo)
    }
}
```

Reglas que ya nos costaron una prueba colgada:

- Con bucles infinitos (`while (isActive) delay(...)`, como el reloj del viaje en curso) usa `runCurrent()`, nunca
  `advanceUntilIdle()`; y limpia el `ViewModelStore` antes de que termine `runTest` (ver `ActiveTripViewModelTest`).
- Los casos de uso se construyen con fakes, no se mockean.
- Agrega el ViewModel a `UiModule.kt`: `KoinGraphTest` falla si hay un ViewModel registrado que no se pide.

## Pruebas manuales

### Android 7.0 a 9 (API 24–28) — punto 3.5.4

La revisión estática no encontró APIs nuevas sin protección, pero nunca se probó en un teléfono viejo.
Crea en Android Studio un emulador **API 24** y otro **API 28** (imagen "Google APIs" o "Google Play" para
push) y en cada uno:

- [ ] Instalar e iniciar sesión; cerrar la app y volver a abrirla: sigue la sesión (token cifrado con Keystore).
- [ ] Abrir vehículos, historial y reservas sin red: se ven los datos guardados (base SQLCipher).
- [ ] Iniciar un viaje, aceptar ubicación, apagar la pantalla 2–3 minutos: la notificación del viaje sigue y la
      ruta avanza (en el emulador: Extended controls → Location → ruta o puntos).
- [ ] Pausar, reanudar, agregar una recarga con foto del recibo (cámara y galería), terminar con foto del odómetro.
- [ ] Hacer lo anterior sin red, luego encender la red: todo se envía solo ("Envíos pendientes" queda vacío).
- [ ] Descargar un reporte PDF y uno Excel y compartirlos.
- [ ] Recibir un push con la app cerrada y con la app abierta (en API 24–28 no se pide permiso de notificaciones).
- [ ] Cerrar sesión con un viaje abierto: queda en pausa; al volver a entrar con la misma cuenta se retoma.

### Actualizar desde una versión anterior (migración de datos)

- [ ] Instalar la versión anterior (archivos JSON sin cifrar), iniciar un viaje con puntos y una recarga con foto,
      **sin terminarlo**, y sin desinstalar instalar esta versión: el viaje sigue en curso, con su ruta y la foto.
- [ ] Con la sesión abierta en la versión anterior, actualizar: no pide iniciar sesión otra vez.

### Release con R8 — punto 3.1.6

Con `keystore.properties` configurado (ver README), `.\gradlew :androidApp:assembleRelease`, instalar el APK y:

- [ ] Login, refresco de sesión (dejar la app abierta más de lo que dura el token) y cerrar sesión.
- [ ] Viaje completo con GPS, recarga con foto y "Registrar viaje".
- [ ] Sincronización en segundo plano (cerrar la app con algo pendiente y encender la red): WorkManager crea el worker.
- [ ] Push con la app cerrada.
- [ ] Abrir la base cifrada (SQLCipher por JNI): cualquier lista sin red.
- [ ] Revisar logcat por `ClassNotFoundException`, `NoSuchMethodError` o `SerializationException`: si aparece,
      falta una regla en `proguard-rules.pro`.
