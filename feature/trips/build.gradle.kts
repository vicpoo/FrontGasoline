/*
 * :feature:trips — Historial de viajes y su detalle.
 * gasoline.kmp.feature ya trae :core:ui, Koin para Compose, serialización y las librerías de prueba.
 */
plugins {
    id("gasoline.kmp.feature")
}

kotlin {
    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.feature.trips"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Necesario para Compose Resources (fuentes, imágenes, textos) en AGP 8.x
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true

        // Pruebas en la JVM: ./gradlew <módulo>:testAndroidHostTest
        withHostTestBuilder {}
    }
}

compose.resources {
    publicResClass = false
    generateResClass = always
    packageOfResClass = "org.vicpoo.gasolineproject.feature.trips.resources"
}
