import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * :feature:reservations:api — API pública de reservas: lo único que otras features pueden usar.
 * Hoy: VehicleRequester (vehículos lo usa para "Solicitar"). Sin Compose ni implementación.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.feature.reservations.api"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_11)
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core:common"))
        }
    }
}
