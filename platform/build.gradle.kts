/*
 * :platform — lo que depende del sistema operativo: GPS y servicio del viaje en curso, conectividad,
 * notificaciones push (Firebase), sincronización en segundo plano (WorkManager), compresión de fotos,
 * la conexión a la base local (SQLCipher en Android), y los lanzadores de Compose para cámara,
 * documentos, compartir archivos y permisos.
 * Implementa interfaces de :domain; no conoce :data ni :ui.
 * Paquetes: org.vicpoo.gasolineproject.platform.<location|connectivity|push|sync|files|di>.
 */
plugins {
    id("gasoline.kmp.compose")
}

kotlin {
    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.platform"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // commonTest en la JVM: ./gradlew :platform:testAndroidHostTest
        withHostTestBuilder {}

        // Pruebas en celular o emulador (fotos con Bitmap y EXIF reales): ./gradlew :platform:connectedAndroidDeviceTest
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            implementation(libs.compose.runtime)
            implementation(libs.koin.core)

            // Lo que antes ponía data por plataforma (dataPlatformModule): archivos, motor HTTP y tokens
            implementation(libs.okio)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.koin.android) // androidContext() en el platformModule
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.activity.compose) // cámara, documentos y permisos
            implementation(libs.androidx.core.ktx) // LocationManagerCompat, NotificationCompat, FileProvider
            implementation(libs.androidx.exifinterface) // orientación de las fotos
            implementation(libs.androidx.work.runtime) // sincronización en segundo plano

            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)

            implementation(libs.ktor.client.okhttp) // motor HTTP

            // Base local cifrada: SQLDelight sobre SQLCipher
            implementation(libs.sqldelight.androidDriver)
            implementation(libs.sqlcipher.android)
            implementation(libs.androidx.sqlite)
        }
        iosMain.dependencies {
            // TODO(iOS): sin cifrado por ahora (iOS fuera del alcance de la revisión); ver docs/ARQUITECTURA.md
            implementation(libs.sqldelight.nativeDriver)
            implementation(libs.ktor.client.darwin) // motor HTTP
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.kotlin.testJunit)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.androidx.testRunner)
        }
    }
}
