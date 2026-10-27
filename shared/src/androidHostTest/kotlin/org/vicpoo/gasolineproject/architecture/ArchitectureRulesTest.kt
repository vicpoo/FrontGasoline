package org.vicpoo.gasolineproject.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Reglas de dependencias de la Fase 1 (mientras todo vive en :shared).
 * Cuando cada feature sea su propio módulo de Gradle, el compilador hará cumplir estas reglas
 * y esta prueba se podrá borrar.
 *
 * 1. Una feature solo puede importar de otra su paquete `api` (ej. feature.reservations.api).
 * 2. core no importa features, ni la navegación ni la composición de la app (di).
 */
class ArchitectureRulesTest {

    private val basePackage = "org.vicpoo.gasolineproject"

    /** Gradle corre las pruebas desde la carpeta del módulo (shared/). */
    private val sourceRoots = listOf("src/commonMain", "src/androidMain", "src/iosMain")
        .map { File(it, "kotlin/${basePackage.replace('.', '/')}") }
        .filter { it.isDirectory }

    @Test
    fun unaFeatureSoloUsaLaApiDeOtra() {
        val violations = kotlinFiles("feature").flatMap { (file, relative) ->
            val owner = relative.split('/')[1] // feature/<owner>/...
            importsOf(file)
                .filter { it.startsWith("$basePackage.feature.") }
                .filterNot { it.startsWith("$basePackage.feature.$owner.") }
                .filterNot { imported -> imported.removePrefix("$basePackage.feature.").split('.').getOrNull(1) == "api" }
                .map { "$relative -> $it" }
        }
        assertNoViolations("Una feature importa algo de otra que no es su API pública", violations)
    }

    @Test
    fun coreNoConoceFeaturesNiLaApp() {
        val forbidden = listOf("$basePackage.feature.", "$basePackage.navigation.", "$basePackage.di.")
        val violations = kotlinFiles("core").flatMap { (file, relative) ->
            importsOf(file)
                .filter { imported -> forbidden.any { imported.startsWith(it) } }
                .map { "$relative -> $it" }
        }
        assertNoViolations("core importa código de features o de la app", violations)
    }

    // ---------------- Utilidades ----------------

    /** Archivos .kt bajo <paquete base>/<folder>, con su ruta relativa al paquete base. */
    private fun kotlinFiles(folder: String): List<Pair<File, String>> {
        if (sourceRoots.isEmpty()) fail("No se encontró el código fuente; ¿cambió la carpeta de trabajo de las pruebas?")
        return sourceRoots.flatMap { root ->
            File(root, folder).walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .map { it to it.relativeTo(root).invariantSeparatorsPath }
                .toList()
        }
    }

    private fun importsOf(file: File): List<String> =
        file.readLines()
            .map { it.trim() }
            .filter { it.startsWith("import ") }
            .map { it.removePrefix("import ").substringBefore(" as ").trim() }

    private fun assertNoViolations(message: String, violations: List<String>) {
        assertTrue(violations.isEmpty(), "$message:\n" + violations.joinToString("\n"))
    }
}
