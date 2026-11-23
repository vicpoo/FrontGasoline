/*
 * :domain — capa de dominio: modelos, reglas, interfaces (repositorios, GPS, push, sesión...) y casos de uso.
 * Kotlin puro: sin Ktor, DataStore, Android ni Compose. Solo corrutinas, fechas, las anotaciones
 * de kotlinx-serialization (algunos modelos se guardan tal cual en el teléfono) y el runtime de
 * SQLDelight (Kotlin puro): lo necesita la interfaz SqlDriverFactory, que data usa y platform implementa.
 * Koin solo para declarar sus módulos (domain/di).
 * Paquetes: org.vicpoo.gasolineproject.domain.<feature> (auth, license, profile, vehicles, reservations,
 * trips, triprecording, reports) y piezas comunes (common, session, rbac, sync, push, location, files, connectivity).
 */
plugins {
    id("gasoline.kmp.library")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.domain"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.koin.core)
            api(libs.sqldelight.runtime)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(project(":testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
