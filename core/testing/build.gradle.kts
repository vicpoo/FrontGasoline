import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * :core:testing — fakes y ayudas para pruebas (sesión, permisos, API falsa con MockEngine,
 * archivos en memoria, dispatcher Main de pruebas). Solo se usa desde commonTest:
 *     commonTest.dependencies { implementation(project(":core:testing")) }
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.core.testing"
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
            api(project(":core:data"))
            api(libs.kotlin.test)
            api(libs.kotlinx.coroutines.test)
            api(libs.ktor.client.mock)
            api(libs.okio.fakefilesystem)
            api(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            // kotlin-test necesita su variante JUnit en el target Android (BeforeTest / AfterTest)
            api(libs.kotlin.testJunit)
        }
    }
}
