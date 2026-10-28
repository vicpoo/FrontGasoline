import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    // AGP 8.x: el target Android de KMP se configura con androidLibrary { }
    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Necesario para Compose Resources (fuentes, imágenes, strings) en AGP 8.x
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true

        // Pruebas unitarias en la JVM: corren commonTest con ./gradlew :shared:testAndroidHostTest
        withHostTestBuilder {}

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_11)
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            // shared es el ensamblador de la app: App.kt, navegación raíz, KoinInit y el framework de iOS.
            // Todo lo demás vive en :core:* y :feature:*.
            implementation(project(":core:common"))
            implementation(project(":core:data"))
            implementation(project(":core:ui"))
            implementation(project(":feature:reservations:api"))
            implementation(project(":feature:auth"))
            implementation(project(":feature:profile"))
            implementation(project(":feature:reservations"))
            implementation(project(":feature:trips"))
            implementation(project(":feature:triprecording"))
            implementation(project(":feature:vehicles"))
            implementation(project(":feature:reports"))

            // UI (App.kt, MainScreen y los textos de las pestañas)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation.compose)

            // Inyección de dependencias
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Rutas tipadas (@Serializable) y corrutinas
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            // Imágenes: el ImageLoader global se configura en App.kt
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":core:testing")) // fakes, API falsa y archivos en memoria
        }
    }
}
