import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * gasoline.kmp.library — módulo Kotlin Multiplatform para Android e iOS:
 * - plugins de Kotlin Multiplatform y de librería KMP de Android,
 * - targets iosArm64 e iosSimulatorArm64,
 * - Kotlin compila para JVM 11 en Android.
 *
 * El bloque `kotlin { androidLibrary { namespace = ...; compileSdk = ...; minSdk = ... } }`
 * se queda en cada módulo: el namespace es propio de cada uno.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")

            extensions.configure<KotlinMultiplatformExtension> {
                iosArm64()
                iosSimulatorArm64()
            }

            tasks.withType<KotlinJvmCompile>().configureEach {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
            }
        }
    }
}
