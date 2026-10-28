/*
 * :feature:reservations:api — API pública de reservas: lo único que otras features pueden usar.
 * Hoy: VehicleRequester (vehículos lo usa para "Solicitar", también sin conexión). Sin Compose ni implementación.
 */
plugins {
    id("gasoline.kmp.library")
}

kotlin {
    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.feature.reservations.api"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:common"))
        }
    }
}
