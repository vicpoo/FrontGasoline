/*
 * :data — capa de datos: implementa las interfaces de :domain.
 * Red (Ktor + safeApiCall + renovación de la sesión), sesión y tokens (DataStore, Keystore / Keychain),
 * cachés JSON en archivos (okio), colas offline-first y sincronizadores de cada feature.
 * Paquetes: org.vicpoo.gasolineproject.data.<feature> + local, network, session, push, di.
 */
plugins {
    id("gasoline.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            api(libs.koin.core)
            api(libs.ktor.client.core)
            api(libs.datastore.preferences.core)
            api(libs.okio)

            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(project(":testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
