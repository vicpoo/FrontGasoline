# Arquitectura de la app (Etapa 2)

Un módulo de Gradle por capa y, dentro de cada capa, una carpeta (paquete) por feature.

```
domain/    Reglas del negocio: modelos, interfaces y casos de uso. Kotlin puro.
data/      Implementa domain: API (Ktor), sesión y tokens, cachés JSON, colas offline y sincronización.
platform/  Lo del sistema operativo: GPS, conectividad, push, WorkManager, cámara, archivos y permisos.
ui/        Pantallas (Compose), ViewModels, estados / eventos / efectos, navegación y sistema de diseño.
testing/   Fakes para las pruebas (sesión, API falsa, archivos, dispatcher Main).
shared/    Arma la app: App.kt, navegación principal (AppNavHost) e initKoin. Framework de iOS.
androidApp/ Application, MainActivity y el Manifest.
```

Features: `welcome`, `auth`, `license`, `profile`, `vehicles`, `reservations`, `trips`, `triprecording`, `reports`.
Piezas compartidas: `domain/common`, `session`, `rbac`, `sync`, `push`, `location`, `files`, `connectivity`;
`ui/common` (tema, componentes, íconos, mapa, RBAC en la UI).

## Quién puede usar a quién

```
ui ──► domain ◄── data
 │       ▲
 └──► platform
```

- `domain` no conoce a nadie (sin Ktor, DataStore, okio, Android ni Compose).
- `data` y `platform` solo conocen `domain`.
- `ui` usa casos de uso de `domain` y los lanzadores de Compose de `platform` (cámara, permisos...). Nunca `data`.
- `shared` junta todo. `ArchitectureTest` (shared, androidHostTest) revisa estas reglas en cada corrida de pruebas.

## Inyección de dependencias (Koin DSL por capa)

Cada capa declara sus módulos en su carpeta `di/` y los expone en una lista:

| Capa | Archivo | Lista |
|---|---|---|
| domain | `domain/di/DomainModule.kt` | `domainModules` (casos de uso, un módulo por feature) |
| data | `data/di/*DataModule.kt` | `dataModules` (repositorios, APIs, cachés, sincronizadores) |
| platform | `platform/di/PlatformModule*.kt` | `platformModule` (expect / actual por plataforma) |
| ui | `ui/di/UiModule.kt` | `uiModules` (ViewModels) |

`shared/di/KoinInit.kt` solo suma las listas. `KoinGraphTest` arma el grafo real y pide cada ViewModel.

## Agregar una feature nueva

1. `domain/<feature>/`: modelos, interfaz del repositorio y casos de uso → regístralos en `DomainModule.kt`.
2. `data/<feature>/`: API, caché y `RepositoryImpl` → nuevo `data/di/<Feature>DataModule.kt` y agrégalo a `dataModules`.
3. `ui/<feature>/`: por pantalla `XScreen.kt`, `XViewModel.kt`, `XUiState.kt`, `XEvent.kt` y `XEffect.kt` (si tiene efectos),
   más su `navigation/` → ViewModels en `UiModule.kt`.
4. Textos en `ui/src/commonMain/composeResources/values/strings.xml` (un solo `Res`: `org.vicpoo.gasolineproject.ui.resources`).
5. Engancha su grafo de navegación en `shared/navigation`.
