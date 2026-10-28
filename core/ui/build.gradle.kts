/*
 * :core:ui — sistema de diseño y piezas de Compose compartidas:
 * tema (colores, tipografía Poppins, imágenes), componentes, íconos, diseño adaptativo,
 * mapa, navegación con privilegios (RBAC en la UI) y lanzadores de cámara, documentos y permisos.
 * Sus textos e imágenes viven en sus propios recursos (Res interno de este módulo).
 */
plugins {
    id("gasoline.kmp.compose")
}

kotlin {
    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.core.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Necesario para Compose Resources (fuentes, imágenes, textos) en AGP 8.x
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true

        // Pruebas en la JVM: ./gradlew <módulo>:testAndroidHostTest
        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            // api: las features usan estos tipos en sus pantallas (Modifier, NavGraphBuilder, AccessRule...)
            api(project(":core:data"))
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            api(libs.compose.components.resources)
            api(libs.navigation.compose)
            api(libs.androidx.lifecycle.viewmodelCompose)
            api(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose) // cámara, documentos y permisos
            implementation(libs.androidx.core.ktx) // FileProvider, ContextCompat
            implementation(libs.maplibre.compose) // mapa del viaje en curso
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    publicResClass = false
    generateResClass = always
    packageOfResClass = "org.vicpoo.gasolineproject.core.ui.resources"
}
