import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * :core:common — tipos y utilidades puros de Kotlin que usa toda la app:
 * AppResult / AppError, AppClock, AppScope, fechas y geometría.
 * Sin Compose, sin red, sin Koin: cualquier módulo puede depender de él.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    androidLibrary {
        namespace = "org.vicpoo.gasolineproject.core.common"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Pruebas en la JVM: ./gradlew :core:common:testAndroidHostTest
        withHostTestBuilder {}

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_11)
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
