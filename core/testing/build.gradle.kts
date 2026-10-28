/*
 * :core:testing — fakes y ayudas para pruebas (sesión, permisos, red, API falsa con MockEngine,
 * archivos en memoria, dispatcher Main de pruebas). Solo se usa desde commonTest:
 *     commonTest.dependencies { implementation(project(":core:testing")) }
 */
plugins {
    id("gasoline.kmp.library")
}

kotlin {
    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.core.testing"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
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
