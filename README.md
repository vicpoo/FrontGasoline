# App de Combustible (GasolineProject)

App móvil para el control de combustible y vehículos de una empresa: los choferes registran sus viajes
con GPS, sus recargas de gasolina (con foto del recibo) y el kilometraje final; los administradores
gestionan la flotilla, aprueban solicitudes de vehículos y descargan reportes. Funciona sin conexión:
todo se guarda en el teléfono (cifrado) y se envía solo cuando hay red.

Consume la API **APIGasoline** (.NET, repositorio aparte). Android es la plataforma principal; iOS
compila pero varias funciones son de prueba (ver [Estado de iOS](#estado-de-ios)).

- Arquitectura, decisiones y reglas: [`docs/ARQUITECTURA.md`](docs/ARQUITECTURA.md)
- Pruebas (automáticas y manuales): [`docs/PRUEBAS.md`](docs/PRUEBAS.md)

---

## Módulos

```
androidApp/  Application, MainActivity, Manifest, firma y R8.
shared/      Arma la app: App.kt, navegación principal, initKoin, ImageLoader. Framework de iOS.
ui/          Pantallas (Compose), ViewModels, navegación y sistema de diseño.
domain/      Modelos, interfaces y casos de uso. Kotlin puro.
data/        API (Ktor), sesión, base local, colas offline y sincronización.
database/    Esquema SQLDelight (.sq, migraciones .sqm). Solo lo usa data.
platform/    Sistema operativo: GPS, push, WorkManager, cámara, archivos, SQLCipher, Keystore.
testing/     Fakes para las pruebas (API falsa, base en memoria, sesión, reloj...).
build-logic/ Plugins de convención de Gradle (gasoline.kmp.library, gasoline.kmp.compose).
iosApp/      Proyecto de Xcode.
```

Dirección de dependencias (la revisa `ArchitectureTest` en cada corrida de pruebas):

```
ui ──► domain ◄── data ──► database
 │       ▲
 └──► platform
```

`domain` no conoce a nadie; `data` y `platform` solo conocen `domain`; `ui` nunca usa `data`;
`shared` junta todo con Koin.

## Stack

| | Versión |
|---|---|
| Kotlin / Kotlin Multiplatform | 2.2.20 |
| Android Gradle Plugin / Gradle | 8.13.1 / 8.14.3 |
| Compose Multiplatform / Material3 | 1.9.3 / 1.9.0 |
| Koin | 4.1.1 |
| Ktor (OkHttp en Android, Darwin en iOS) | 3.3.1 |
| SQLDelight / SQLCipher para Android | 2.4.0 / 4.9.0 |
| Coil | 3.3.0 |
| WorkManager | 2.10.2 |
| Firebase Cloud Messaging (BoM) | 33.7.0 |
| minSdk / targetSdk / compileSdk | 24 / 36 / 36 |

Todas las versiones están en `gradle/libs.versions.toml`.

## Requisitos

- **JDK 21** para correr Gradle (el que trae Android Studio sirve). Si no lo encuentra, Gradle lo descarga
  solo (`gradle/gradle-daemon-jvm.properties`). El código compila para JVM 11.
- **Android Studio** (o el Android SDK con la plataforma 36) y un emulador o celular con Android 7.0+.
- **`local.properties`** en la raíz con la ruta del SDK (Android Studio lo crea solo):
  ```properties
  sdk.dir=C\:\\Users\\<tu-usuario>\\AppData\\Local\\Android\\Sdk
  ```
- **`androidApp/google-services.json`** de Firebase (ver [Firebase](#firebase)). Sin él no compila.
- Para iOS: macOS con Xcode.

## Comandos

En Windows usa `.\gradlew`; en macOS / Linux, `./gradlew`.

```powershell
# App de debug (APK en androidApp/build/outputs/apk/debug/)
.\gradlew :androidApp:assembleDebug

# Todas las pruebas en la JVM (rápidas, sin emulador)
.\gradlew :domain:testAndroidHostTest :data:testAndroidHostTest :platform:testAndroidHostTest :ui:testAndroidHostTest :shared:testAndroidHostTest --continue

# Pruebas en emulador o celular conectado (SQLCipher, Keystore, fotos con EXIF)
.\gradlew :platform:connectedAndroidDeviceTest

# Base de datos: después de cambiar el esquema (ver docs/ARQUITECTURA.md)
.\gradlew :database:generateCommonMainAppDatabaseSchema
.\gradlew :database:verifyCommonMainAppDatabaseMigration

# Release firmado (ver "Build de release")
.\gradlew :androidApp:assembleRelease     # APK
.\gradlew :androidApp:bundleRelease       # AAB para Google Play

# iOS (en macOS)
./gradlew :shared:iosSimulatorArm64Test
```

## Configuración de la API por ambiente

La URL base **no está en el código**: `androidApp/build.gradle.kts` la define como
`BuildConfig.API_BASE_URL`, `GasolineApplication` se la pasa a `initKoin` y los clientes HTTP la reciben
con el qualifier `API_BASE_URL` de Koin. Debe terminar en `/`.

| Ambiente | URL | Notas |
|---|---|---|
| Producción (actual, debug y release) | `http://13.222.72.73/api/` | Servidor en AWS (EC2). Sin HTTPS: riesgo aceptado, ver `docs/ARQUITECTURA.md` |
| API corriendo en tu PC + emulador | `http://10.0.2.2:<puerto>/api/` | `10.0.2.2` es la PC vista desde el emulador de Android |
| API en tu PC + celular físico | `http://<IP-de-tu-PC>:<puerto>/api/` | Celular y PC en la misma red Wi-Fi; abre el puerto en el firewall de Windows y haz que la API escuche en `0.0.0.0`, no solo en `localhost` |

Para cambiarla:

1. Edita el `buildConfigField("String", "API_BASE_URL", ...)` en `androidApp/build.gradle.kts`. Si quieres
   una URL distinta por build, muévelo a `buildTypes { debug { ... } release { ... } }`.
2. **Si el host no es `13.222.72.73`**, agrégalo en `androidApp/src/main/res/xml/network_security_config.xml`
   (Android bloquea HTTP sin TLS hacia cualquier otro host). Para el emulador sería
   `<domain includeSubdomains="false">10.0.2.2</domain>`. Con HTTPS no hace falta.
3. iOS: `IOS_API_BASE_URL` en `shared/src/iosMain/.../MainViewController.kt`.

## Firebase

Las notificaciones push (respuesta a una solicitud de vehículo, solicitudes nuevas para quien aprueba)
usan Firebase Cloud Messaging, proyecto **`apigasoline`**, app Android `org.vicpoo.gasolineproject`.

1. Pide acceso al proyecto en la [consola de Firebase](https://console.firebase.google.com/).
2. Configuración del proyecto → Tus apps → Android → descarga `google-services.json`.
3. Ponlo en `androidApp/google-services.json`. **No se versiona** (`.gitignore`); la estructura está en
   `androidApp/google-services.json.example`.

**API key.** La key del archivo es pública por diseño (va dentro del APK), pero debe estar restringida.
Una versión vieja del archivo quedó en el historial de git (commit `580514f`) y el repositorio es público, así
que el dueño del proyecto debe, en [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
(proyecto `apigasoline`), editar la key de Android y:
- Restricción de aplicación: **apps para Android**, paquete `org.vicpoo.gasolineproject` + huella **SHA-1**
  del keystore de release (y la de debug si se prueba con ese).
- Restricción de API: solo las de Firebase que se usan (Firebase Installations y Cloud Messaging).
- Si no se puede restringir, **regenerarla** y descargar un `google-services.json` nuevo.

La SHA-1 de un keystore se obtiene con:
```powershell
keytool -list -v -keystore C:/llaves/gasoline-release.jks -alias gasoline
```

**Probar un push.**
1. Instala la app en un celular o emulador con Google Play, inicia sesión y acepta el permiso de notificaciones.
2. Con otra cuenta (web o app) que pueda aprobar solicitudes, aprueba o rechaza una solicitud de la primera
   cuenta: la API manda el push. También sirve hacer una solicitud y verla llegar a quien aprueba.
3. Prueba con la app abierta, en segundo plano y cerrada (cerrada, la muestra Firebase en el canal `solicitudes`).

## Build de release

1. **Crear el keystore** una sola vez (guárdalo FUERA del repositorio y respáldalo; si se pierde no se
   pueden publicar actualizaciones):
   ```powershell
   keytool -genkeypair -v -keystore C:/llaves/gasoline-release.jks -alias gasoline -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Copia `keystore.properties.example` como `keystore.properties` en la raíz y llena la ruta y las
   contraseñas. Está en `.gitignore`, igual que `*.jks` y `*.keystore`.
3. Arma el release:
   ```powershell
   .\gradlew :androidApp:assembleRelease   # androidApp/build/outputs/apk/release/androidApp-release.apk
   .\gradlew :androidApp:bundleRelease     # androidApp/build/outputs/bundle/release/androidApp-release.aab
   ```
   Sin `keystore.properties` el APK sale sin firmar (sirve para revisar que R8 no rompa nada, no para instalar).
4. Instálalo y prueba lo de la lista "Release con R8" de [`docs/PRUEBAS.md`](docs/PRUEBAS.md). R8 quita y
   renombra clases: lo que falla **solo** en release casi siempre es una regla que falta.
5. Guarda `androidApp/build/outputs/mapping/release/mapping.txt` de cada versión publicada: sin él los
   errores de release no se pueden leer.

**R8.** `isMinifyEnabled` e `isShrinkResources` están activos. Las librerías traen sus propias reglas;
`androidApp/proguard-rules.pro` solo agrega:

| Regla | Por qué |
|---|---|
| `-keepattributes SourceFile,LineNumberTable` | Números de línea en los errores (con `mapping.txt`) |
| `-dontwarn org.slf4j.**`, `java.lang.management.**`, `io.ktor.util.debug.**` | Clases opcionales de Ktor que solo existen en JVM de escritorio |
| `-keep class * implements io.ktor.client.HttpClientEngineContainer` | Coil busca el motor de Ktor con `ServiceLoader` |
| `@Serializable` de `org.vicpoo.gasolineproject.**` | DTOs, rutas de navegación y lo que se guarda en el teléfono |
| `net.zetetic.database.**` | SQLCipher: su código nativo llama a esas clases por JNI |
| `PendingSyncWorker` | WorkManager crea el worker por su nombre de clase |

Antes de cada versión sube `versionCode` y `versionName` en `androidApp/build.gradle.kts`.

## Estado de iOS

iOS **compila** y usa el mismo código compartido (domain, data, ui), pero no tiene paridad con Android:

| Función | Estado en iOS |
|---|---|
| Iniciar sesión, listas, formularios, reservas, reportes | Funciona (código compartido) |
| Sesión (tokens) | Keychain, solo en el dispositivo |
| Base local | SQLite **sin cifrar** (TODO: SQLCipher + Keychain) |
| Fotos pendientes | **Sin cifrar** y sin quitar EXIF (`IosImageCompressor` las regresa igual) |
| GPS del viaje | **Pendiente** (`IosLocationProvider` no da ubicaciones: el viaje se registra sin ruta) |
| Cámara y selector de fotos | **Pendientes** (regresan "cancelado") |
| Compartir reportes | Funciona (hoja de compartir del sistema) |
| Notificaciones push | **Pendientes** (falta APNs y el SDK de Firebase en `iosApp`) |
| Sincronización en segundo plano | Solo con la app abierta (al abrir, al volver a primer plano y al volver la red) |
| Carpeta de datos | `Documents` (TODO: `Application Support` y excluir del respaldo de iCloud) |
| Pruebas | `SharedLogicIOSTest.kt` sigue siendo la plantilla |

Todos los pendientes están marcados con `TODO(iOS` en el código.
