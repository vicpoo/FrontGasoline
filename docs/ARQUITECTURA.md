# Arquitectura de la app (Etapa 2)

Un módulo de Gradle por capa y, dentro de cada capa, una carpeta (paquete) por feature.

```
domain/    Reglas del negocio: modelos, interfaces y casos de uso. Kotlin puro.
database/  Esquema de la base local (SQLDelight: .sq, migraciones .sqm). Solo lo usa data.
data/      Implementa domain: API (Ktor), sesión, base local cifrada, colas offline y sincronización.
platform/  Lo del sistema operativo: GPS, conectividad, push, WorkManager, cámara, archivos, permisos,
           base cifrada (SQLCipher), motor HTTP, tokens (Keystore / Keychain) y datos del teléfono.
ui/        Pantallas (Compose), ViewModels, estados / eventos / efectos, navegación y sistema de diseño.
testing/   Fakes para las pruebas (sesión, API falsa, archivos, dispatcher Main).
shared/    Arma la app: App.kt, navegación principal (AppNavHost) e initKoin. Framework de iOS.
androidApp/ Application, MainActivity y el Manifest.
```

Features: `welcome`, `auth`, `license`, `profile`, `vehicles`, `reservations`, `trips`, `triprecording`, `reports`.
Piezas compartidas: `domain/common`, `session`, `settings` (apariencia claro / oscuro), `rbac`, `sync`, `push`, `location`, `files`, `connectivity`;
`ui/common` (tema, componentes, íconos, mapa, RBAC en la UI).

## Quién puede usar a quién

```
ui ──► domain ◄── data
 │       ▲
 └──► platform
```

- `domain` no conoce a nadie (sin Ktor, DataStore, okio, Android ni Compose).
- `data` y `platform` solo conocen `domain` (`data` además usa `database`, el esquema generado).
- `ui` usa casos de uso de `domain` y los lanzadores de Compose de `platform` (cámara, permisos...). Nunca `data`.
  Los ViewModels también pueden leer cuatro interfaces informativas de `domain` sin reglas de negocio
  (`ConnectivityMonitor`, `LastRefreshStore`, `PermissionChecker`, `AppClock`); envolverlas en casos de uso
  sería código sin lógica (punto 3.2.6).
- Dependencias entre módulos con `implementation` salvo lo que de verdad es parte de la API pública: `data`
  no expone Ktor, DataStore ni okio y `ui` no expone `platform` (3.5.1, 3.5.2).
- `shared` junta todo. `ArchitectureTest` (shared, androidHostTest) revisa estas reglas en cada corrida de pruebas.

## Inyección de dependencias (Koin DSL por capa)

Cada capa declara sus módulos en su carpeta `di/` y los expone en una lista:

| Capa | Archivo | Lista |
|---|---|---|
| domain | `domain/di/<Feature>DomainModule.kt` | `domainModules` en `DomainModule.kt` (casos de uso, un archivo por feature) |
| data | `data/di/*DataModule.kt` | `dataModules` (repositorios, APIs, cachés, sincronizadores) |
| platform | `platform/di/PlatformModule*.kt` | `platformModule` (el único `expect val Module` del proyecto) |
| ui | `ui/di/UiModule.kt` | `uiModules` (ViewModels) |

`shared/di/KoinInit.kt` solo suma las listas. `KoinGraphTest` arma el grafo real y pide cada ViewModel.

### expect / actual (punto 2 del code review)

- Un solo `expect val platformModule: Module`. Todo lo nativo se registra ahí y las otras capas lo reciben por
  interfaz de `domain`: `FileSystem`, `HttpClientEngine` (OkHttp / Darwin), `TokenStore` (Keystore / Keychain),
  `ClientDeviceInfo`, `SqlDriverFactory`, `FileCipher`, GPS, conectividad, push, etc.
- `data` ya no tiene `androidMain` ni `iosMain`.
- Los tokens en Android: `KeystoreTokenStore` (platform) cifra y guarda el texto cifrado en `KeyValueStore`
  (interfaz de domain que data implementa con DataStore). Así platform no depende de DataStore; misma clave,
  llave y formato que antes, por eso actualizar la app no cierra la sesión.
- Solo quedan `expect fun` para composables: `rememberCameraCapture`, `rememberDocumentPicker`,
  `rememberFileSharer`, `rememberLocationPermission`, `RequestNotificationPermission` (platform) y
  `LiveRouteMap` (ui). `ArchitectureTest.expectSoloParaElModuloDePlataformaYComposables` lo revisa.
- `testing` tiene `expect fun inMemorySqlDriver` (base en memoria para pruebas); no es código de la app.

### `@Serializable` en domain (punto 3.2.7)

Solo `AuthTokens` y `Session` lo llevan (por eso `domain` depende de `kotlinx-serialization`). Se deja así a
propósito: son exactamente lo que se guarda de la sesión, los guardan dos capas (data: `Session` en DataStore;
platform: `AuthTokens` en Keystore / Keychain) y un DTO por capa sería duplicar el mismo dato sin ganancia.
Los demás modelos de domain no se serializan: lo guardado en el teléfono usa `*Entity` de data.

## Agregar una feature nueva

1. `domain/<feature>/`: modelos, interfaz del repositorio y casos de uso → nuevo `domain/di/<Feature>DomainModule.kt` y agrégalo a `domainModules`.
2. `data/<feature>/`: API, caché y `RepositoryImpl` → nuevo `data/di/<Feature>DataModule.kt` y agrégalo a `dataModules`.
3. `ui/<feature>/`: por pantalla `XScreen.kt`, `XViewModel.kt`, `XUiState.kt`, `XEvent.kt` y `XEffect.kt` (si tiene efectos),
   más su `navigation/` → ViewModels en `UiModule.kt`.
4. Textos en `ui/src/commonMain/composeResources/values/strings.xml` (un solo `Res`: `org.vicpoo.gasolineproject.ui.resources`).
5. Engancha su grafo de navegación en `shared/navigation`.

## Seguridad de red

### URL de la API

La URL base de APIGasoline se define en un solo lugar: `buildConfigField("String", "API_BASE_URL", ...)` en
`androidApp/build.gradle.kts` (hoy `http://13.222.72.73/api/`, igual en debug y release).
`GasolineApplication` la pasa a `initKoin(apiBaseUrl = ...)` y `data` la recibe con el qualifier `API_BASE_URL`
(`data/di/CoreDataModule.kt`). Para apuntar a otro servidor (emulador `10.0.2.2`, IP de la PC, producción)
se cambia ese campo, o se define uno distinto dentro de cada `buildType`, y se agrega el host en
`res/xml/network_security_config.xml`.

### HTTP sin TLS (riesgo aceptado)

La API no tiene HTTPS: no hay dominio ni certificado disponibles. Por esa conexión viajan en claro:
credenciales del login, JWT y refresh token, fotos de licencia, recibos y odómetro, y la ruta GPS.
Cualquiera en la misma red (Wi-Fi pública, red del operador) puede leerlos o modificarlos.

Mitigaciones aplicadas:

- `network_security_config.xml` permite HTTP **solo** hacia `13.222.72.73`; cualquier otro host exige HTTPS.
  Se quitó `usesCleartextTraffic="true"`, que permitía HTTP hacia cualquier sitio.
- Los nombres de archivo que manda la API (`Content-Disposition` de los reportes) se limpian con
  `safeFileName` y se verifica que el archivo quede dentro de `cache/reports` (evita path traversal).
- Las fotos siempre se recodifican antes de subirse: no viajan los metadatos EXIF (ubicación).

Si en el futuro hay un dominio, Let's Encrypt da certificados gratis: al pasar la API a HTTPS basta con
cambiar `API_BASE_URL` y quitar el `domain-config` del archivo de seguridad de red.

### Firebase

`androidApp/google-services.json` no se versiona (está en `.gitignore`). Se descarga de la consola de
Firebase (proyecto `apigasoline`, app Android `org.vicpoo.gasolineproject`) y se coloca en `androidApp/`.
La estructura esperada está en `androidApp/google-services.json.example`. La API key que quedó en el
historial de git (commit `580514f`) debe estar restringida en Google Cloud Console al paquete y la huella
SHA-1 de la app, y solo a las APIs de Firebase que se usan.


## Bitácora (logging)

- `AppLogger` (interfaz en `domain/common`) con niveles DEBUG, INFO, WARN y ERROR. La implementa `platform`
  (`AndroidAppLogger` → Logcat con etiqueta `Gasoline/<tag>`; `IosAppLogger` → NSLog) y se registra en `platformModule`.
- Nivel mínimo según el build (`AppBuildInfo`, que registra `initKoin` con `BuildConfig.DEBUG`): en debug todo;
  en release solo WARN y ERROR.
- Las clases creadas con Koin reciben el `AppLogger` por constructor. El código que no se crea con Koin
  (`safeApiCall`, `AuthRepositoryImpl`, `AuthTokenManager`) usa `AppLog`, que `initKoin` instala al arrancar.
- Ktor solo instala su plugin de Logging en debug, escribe por el `AppLogger` y nivel INFO (método, URL y código).
- `AppScope` tiene un `CoroutineExceptionHandler`: una excepción no capturada en segundo plano queda como ERROR.
- Si una feature no puede borrar sus datos al cerrar sesión se reintenta una vez; si sigue fallando queda como ERROR.
- **Nunca** registrar tokens, el encabezado `Authorization`, cuerpos de login, coordenadas GPS ni fotos.

## Respaldo de Android

`android:allowBackup="false"` y `res/xml/data_extraction_rules.xml` (Android 12+) dejan fuera del respaldo en la nube
y de la transferencia entre teléfonos todo lo que la app guarda (placas, rutas GPS, fotos, cola sin enviar y sesión).
Al cambiar de teléfono el usuario vuelve a iniciar sesión.

## Almacenamiento en el teléfono

Todo es almacenamiento interno de la app (privado, sin permisos de almacenamiento externo).

| Dato | Dónde | Se borra |
|---|---|---|
| Base local `gasoline.db` (SQLCipher): viajes en curso y sin enviar, ruta GPS, cola de solicitudes, vehículos, reservas, historial, reportes, carros asignados, centros de costo, bitácora de envíos | `databases/` | Al cerrar sesión (cada feature con su `LogoutListener`); los viajes sin enviar se quedan para su dueño |
| Fotos pendientes de subir (recibos, odómetro, vehículos), cifradas AES-256-GCM | `filesDir/trip_recording/receipts/`, `filesDir/vehicles/photos/` | Al enviarse; al cerrar sesión |
| Llave de la base (cifrada con el Keystore) | `noBackupFilesDir/local_db.key` | Nunca (si se pierde, la base se recrea) |
| DataStore (sesión, preferencias) | `filesDir` | Al cerrar sesión |
| Foto de la cámara antes de reducirla | `cacheDir/camera/` | Al leerla; al abrir la app si tiene más de 1 h; al cerrar sesión |
| Reporte PDF / Excel compartido | `cacheDir/reports/` (FileProvider) | Al abrir la app si tiene más de 1 h; al cerrar sesión |
| Fotos descargadas (vehículos, operadores, perfil) y teselas del mapa | `cacheDir/image_cache/` (Coil, máx. 100 MB) + memoria | Al cerrar sesión (`ImageCache`) |
| Tokens | DataStore cifrado con Android Keystore | Al cerrar sesión |

- `AndroidTemporaryFiles` (platform) limpia `camera/` y `reports/`. Al abrir la app solo borra lo de más de una
  hora: si Android cerró la app mientras el usuario estaba en la cámara, la foto recién tomada se lee al regresar,
  y la app con la que se compartió un reporte puede seguir leyéndolo.
- `ImageCache` (shared) crea el `ImageLoader` de Coil con la caché de disco explícita y la limpia al cerrar sesión.
- Si la sesión termina sola (refresh token vencido) los datos NO se borran, para que el mismo usuario recupere lo
  pendiente; si entra otra cuenta se borran antes (ver `LocalDataOwner.kt`).

### Cifrado de los datos en el teléfono (punto 4.5) — hecho en la fase 6

**Decisión:** cifrar al migrar a SQLDelight (fase 6). Ya está hecho en Android; iOS queda como TODO.

**Base local (`:database` + `LocalDatabase` en data + `SqlDriverFactory` en platform)**

- Esquema en el módulo `:database` (punto 1.2): `tables/*.sq` = esquema actual (tablas con sufijo `Entity`),
  `queries/*.sq` = consultas con nombre (generan `AppMetaQueries`, `ListItemQueries`...), `migrations/N.sqm` =
  de la versión N a la N+1 (nunca se edita uno publicado), `databases/N.db` = foto de cada versión para
  `verifyCommonMainAppDatabaseMigration`. Versión actual: 3. Es un módulo aparte con target `jvm()` porque el
  plugin de SQLDelight no funciona con `com.android.kotlin.multiplatform.library` (por eso no está en `data`,
  como pedía el 1.1).
- Un `.sqm` solo "ve" lo que crearon los `.sqm` anteriores: si cambia una tabla creada en la versión 1 (en un
  `.sq`), el `.sqm` empieza con `CREATE TABLE IF NOT EXISTS` de esa tabla (ver `2.sqm`).
- Android: toda la base cifrada con **SQLCipher**. La contraseña son 32 bytes al azar, guardados cifrados con
  una llave del Android Keystore en `noBackupFilesDir/local_db.key`, y se pasa como llave cruda (`x'…'`) para
  no gastar 256 000 vueltas de PBKDF2 al abrir.
- Si la llave del Keystore se pierde o la base no abre con ella: la base se borra, se crea vacía y queda un
  ERROR en la bitácora. Lo descargable vuelve de la API; lo que no se había enviado se pierde (caso muy raro:
  ni el Keystore ni la base se respaldan).
- iOS: SQLite sin cifrar (TODO: SQLCipher + Keychain).

**Qué guarda**

| Tabla | Antes | Notas |
|---|---|---|
| `CacheDocumentEntity` | `cache/<nombre>.json` (`JsonFileCache`) | Un documento JSON por caché (`LocalDocumentCache`): viajes y recargas pendientes, cola de solicitudes, badge de la campana, reportes, asignados, centros de costo, bitácora. Son valores que se leen y guardan completos |
| `TrackPointEntity` | `trip_recording/points/<viaje>.jsonl` | Una fila por punto; los lotes para la API se piden por llave (`seq >= N`), sin OFFSET |
| `VehicleEntity` | `vehicles/vehicles.json` | Filtro de borrados pendientes y conteo de pendientes en SQL; el orden sigue en Kotlin (acentos) |
| `ListItemEntity` + `ListStateEntity` | `trips.json`, `my_reservations.json`, `company_reservations.json` | Historial de viajes y solicitudes de reserva, una fila por elemento, paginadas por llave (abajo) |
| `AppMetaEntity` | — | Marcas internas (ej. migración de archivos terminada) |

**Paginación por llave (punto 1.6).** Historial de viajes, mis solicitudes y las de la empresa son
`LocalPagedList` (data) sobre `ListItemEntity`, ordenadas por `(sort_key, item_id)` de la más reciente a la más
vieja. `KeySetPaginationManager` recuerda la llave de la última fila que se ve:

- Lo que se ve = desde la más reciente hasta esa llave (`upToKey`). Al abrir, solo la primera página (10).
- Bajar: primero la página siguiente de lo GUARDADO por llave (`pageAfter`: llave menor que la última que se
  ve, `LIMIT 10`, sin OFFSET); solo si ya se ve todo lo guardado se pide la página siguiente a la API
  (`ListStateEntity` guarda cuántas páginas se pidieron y si la API tiene más).
- Recargar reemplaza las filas con lo que manda la API sin recortar lo que ya se veía.
- `observeHasMore` = hay guardadas que aún no se ven, o la API tiene más.

Reportes no es una lista que se recorra: es el último resultado completo y se queda como documento.

**Fotos pendientes:** `FileCipher` (domain) → `KeystoreFileCipher` (platform): AES-256-GCM con otra llave del
Keystore; archivo = `GPF1` + IV + datos + etiqueta. Se descifran al subirlas y al mostrarlas: la UI usa
`LocalImage(ruta)` y `LocalImageFetcher` (shared) las descifra en memoria para Coil. Una foto que ya no se puede
descifrar cuenta como faltante, igual que un archivo borrado.

**Migración de lo que ya estaba en los teléfonos (`LegacyDataMigration`, al abrir la app)**

1. Cada caché JSON se guarda en `CacheDocumentEntity` y después se borra el archivo (si ya existía el documento, no se pisa).
2. Los puntos `.jsonl` de cada viaje pasan a `TrackPointEntity` solo si ese viaje aún no tiene puntos (sin duplicados).
3. `vehicles.json` pasa a `VehicleEntity` solo si la tabla está vacía.
   El historial y las solicitudes de la empresa NO se pasan: sus archivos y documentos viejos se borran
   (`OBSOLETE_DOCUMENTS`) porque son copia de la API y se vuelven a descargar al entrar.
4. Las fotos sin cifrar se cifran en su lugar (mismo nombre, escritura atómica), así los viajes las siguen encontrando.
5. Al terminar se anota en `AppMetaEntity`. Si algo falla se reintenta en el siguiente arranque; además cada pieza
   se migra sola la primera vez que se usa. Prueba principal: `LegacyDataMigrationTest` (un chofer actualiza
   con un viaje sin enviar y no pierde viaje, recargas, ruta ni foto).

## Sincronización: reintentos (punto 3.3.1)

| Qué pasó | Qué se hace |
|---|---|
| Sin red, o el JWT venció sin red para renovarlo | Se detiene la vuelta; WorkManager reintenta con backoff exponencial cuando hay red |
| 5xx, 408 o 429 | El registro queda pendiente; la vuelta se vuelve a programar con backoff (30 s, 1, 2, 4, 8 min) |
| 5xx / 429 con `Retry-After` | Se programa otra vuelta después de ese tiempo (entre 30 s y 1 h) |
| 4xx (400, 403, 404, 409, 422...) | Rechazo definitivo: se guarda el mensaje y no se reenvía solo (botón "Reintentar") |
| Sesión terminada (refresh rechazado) | No se reintenta hasta que vuelva a entrar |

- `safeApiCall` anota los errores temporales en `TemporarySyncFailures` (va en el contexto de la corrutina);
  `RunPendingSyncUseCase` decide el resultado y `PendingSyncWorker` lo aplica. Ver `domain/sync/SyncRetry.kt`.
- Máximo 5 intentos seguidos del Worker; después lo toma la vuelta periódica (cada 15 min).
- **No** se repite la misma petición en el momento: casi todos los envíos son POST que crean algo. Si el servidor
  falló después de guardarlo, repetirlo de inmediato podría duplicarlo; la siguiente vuelta llega con espera.
- iOS: se reintenta al volver a primer plano o al reconectar (`IosSyncScheduler`).

## Conflictos entre celulares (punto 3.3.2)

La web no edita vehículos, pero dos celulares sí pueden editar el mismo carro (uno o los dos sin red).
**Regla: gana el cambio más reciente**, no el último que sincroniza.

- Al crear o editar, el teléfono guarda la hora del cambio (`VehicleEntity.editedAt`, hora local con zona) y la
  manda en `fechaEdicion` (POST / PUT `/api/carros`).
- La API guarda la hora del último cambio aplicado de cada carro. Si llega un cambio con `fechaEdicion` más vieja,
  NO lo aplica y responde `409` con `codigo: "CAMBIO_MAS_RECIENTE"`. Sin `fechaEdicion` (versiones viejas de la
  app) se aplica como antes.
- La app, ante ese 409: descarta su cambio (y su foto pendiente), marca el vehículo como sincronizado con el aviso
  "Otro celular guardó un cambio más reciente…" y la siguiente actualización trae la versión del servidor.
- Depende de que la hora de los celulares sea correcta (la toman de la red del operador). Con horas muy
  desfasadas podría ganar el cambio equivocado.
- Si un celular borra el carro y otro lo edita: el PUT recibe 404 y queda como error (sin cambios de lógica).

**Pendiente en la API** (APIGasoline): columna `FechaEdicion` en Carro, comparar en el PUT y responder el 409.
Mientras no esté, la API ignora `fechaEdicion` y sigue ganando el último que sincroniza.
