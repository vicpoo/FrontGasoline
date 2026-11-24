/*
 * :data — capa de datos: implementa las interfaces de :domain.
 * Red (Ktor + safeApiCall + renovación de la sesión), sesión (DataStore; los tokens los cifra platform),
 * base local (LocalDatabase; el esquema SQLDelight vive en :database), cachés JSON en archivos (okio, se van pasando a la base),
 * colas offline-first y sincronizadores de cada feature.
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
            api(project(":database"))
            implementation(libs.sqldelight.coroutines)

            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.serialization.json)
        }
        // Sin androidMain / iosMain: lo de cada plataforma (motor HTTP, tokens, FileSystem) lo pone platformModule
        commonTest.dependencies {
            implementation(project(":testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
