/*
 * :platform — lo que depende del sistema operativo: GPS y servicio del viaje en curso, conectividad,
 * notificaciones push (Firebase), sincronización en segundo plano (WorkManager), compresión de fotos,
 * y los lanzadores de Compose para cámara, documentos, compartir archivos y permisos.
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
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":domain"))
            implementation(libs.compose.runtime)
            implementation(libs.koin.core)
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
        }
    }
}
