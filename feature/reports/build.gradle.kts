/*
 * :feature:reports — Reportes de la flota (módulo de auditoría: privilegio auditoria.ver).
 * La API calcula todo y genera el PDF / Excel; la app solo muestra la vista previa y comparte el archivo.
 */
plugins {
    id("gasoline.kmp.feature")
}

kotlin {
    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.feature.reports"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Necesario para Compose Resources (fuentes, imágenes, textos) en AGP 8.x
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true

        // Pruebas en la JVM: ./gradlew :feature:reports:testAndroidHostTest
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(project(":core:testing"))
        }
    }
}

compose.resources {
    publicResClass = false
    generateResClass = always
    packageOfResClass = "org.vicpoo.gasolineproject.feature.reports.resources"
}
