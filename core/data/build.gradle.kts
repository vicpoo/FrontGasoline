/*
 * :core:data — infraestructura de datos compartida, sin UI:
 * red (Ktor + safeApiCall), conectividad, sesión (DataStore), RBAC (privilegios), caché JSON,
 * servicios de plataforma (directorios, GPS, compresión de imágenes), push y
 * sincronización offline-first (PendingSyncTask, WorkManager en Android, al abrir / reconectar en iOS).
 * También define los módulos de Koin de infraestructura y el platformModule.
 */
plugins {
    id("gasoline.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.core.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Pruebas en la JVM: ./gradlew <módulo>:testAndroidHostTest
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            // api: estos tipos aparecen en las firmas públicas (AppResult, HttpClient, Module, DataStore, FileSystem)
            api(project(":core:common"))
            api(libs.koin.core)
            api(libs.ktor.client.core)
            api(libs.datastore.preferences.core)
            api(libs.okio)
            api(libs.kotlinx.coroutines.core)

            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.koin.android) // androidContext() en el platformModule
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core.ktx) // LocationManagerCompat, NotificationCompat
            implementation(libs.androidx.exifinterface) // orientación de las fotos
            implementation(libs.androidx.work.runtime) // sincronización en segundo plano

            // Notificaciones push (Firebase Cloud Messaging)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
