/*
 * :ui — capa de presentación: pantallas de Compose, ViewModels, estados / eventos / efectos y navegación
 * de cada feature, más el sistema de diseño (ui.common: tema, componentes, íconos, mapa, RBAC en la UI).
 * Usa casos de uso de :domain y los lanzadores de :platform (cámara, permisos...). Nunca usa :data.
 * Todos los textos, fuentes e imágenes viven aquí (un solo Res: org.vicpoo.gasolineproject.ui.resources).
 * Paquetes: org.vicpoo.gasolineproject.ui.<welcome|auth|license|profile|vehicles|reservations|trips|triprecording|reports|common|di>.
 */
plugins {
    id("gasoline.kmp.compose")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true

        withHostTestBuilder {}
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            api(project(":platform"))
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
            implementation(libs.kotlinx.serialization.json) // rutas tipadas de navegación
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        androidMain.dependencies {
            implementation(libs.maplibre.compose) // mapa del viaje en curso
        }
        commonTest.dependencies {
            implementation(project(":testing"))
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

compose.resources {
    publicResClass = false
    generateResClass = always
    packageOfResClass = "org.vicpoo.gasolineproject.ui.resources"
}
