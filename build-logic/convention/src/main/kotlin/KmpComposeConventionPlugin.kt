import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * gasoline.kmp.compose — gasoline.kmp.library + Compose Multiplatform.
 * Declara compose.components.resources directo en el módulo: el plugin de recursos solo genera
 * la clase Res si el módulo lo declara él mismo (no le basta recibirlo de otro módulo).
 *
 * El bloque `compose.resources { packageOfResClass = ... }` se queda en cada módulo.
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("gasoline.kmp.library")
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies.add("commonMainImplementation", libs.findLibrary("compose-components-resources").get())
        }
    }
}
