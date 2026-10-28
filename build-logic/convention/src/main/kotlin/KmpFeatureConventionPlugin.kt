import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * gasoline.kmp.feature — una feature de la app (:feature:*):
 * - gasoline.kmp.compose + serialización (DTOs y rutas tipadas),
 * - depende de :core:ui (que trae :core:data, :core:common, Compose, navegación y lifecycle),
 * - Koin para Compose y ViewModels,
 * - kotlin-test y corrutinas de prueba en commonTest.
 *
 * Cada feature solo agrega lo que es suyo (ej. :feature:reservations:api o :core:testing).
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("gasoline.kmp.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            fun lib(alias: String) = libs.findLibrary(alias).get()

            dependencies.add("commonMainImplementation", dependencies.project(mapOf("path" to ":core:ui")))
            dependencies.add("commonMainImplementation", lib("koin-compose"))
            dependencies.add("commonMainImplementation", lib("koin-compose-viewmodel"))
            dependencies.add("commonMainImplementation", lib("kotlinx-serialization-json"))

            dependencies.add("commonTestImplementation", lib("kotlin-test"))
            dependencies.add("commonTestImplementation", lib("kotlinx-coroutines-test"))
        }
    }
}
