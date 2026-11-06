package org.vicpoo.gasolineproject.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Reglas de la arquitectura por capas (docs/ARQUITECTURA.md). Lee el código fuente, sin librerías extra:
 * - domain no conoce a nadie; data y platform solo conocen domain; ui conoce domain y platform (nunca data).
 * - domain es Kotlin puro: sin Ktor, DataStore, okio, Android ni Compose.
 * - En ui, cada UiState / Event / Effect vive en su propio archivo (XUiState.kt, XEvent.kt, XEffect.kt).
 */
class ArchitectureTest {

    private val root: File = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }

    private val allowed = mapOf(
        "domain" to setOf("domain"),
        "data" to setOf("domain", "data"),
        "platform" to setOf("domain", "platform"),
        "ui" to setOf("domain", "platform", "ui"),
    )

    private val forbiddenInDomain = listOf("io.ktor.", "androidx.", "android.", "okio.", "org.jetbrains.compose.", "platform.")

    private fun sources(layer: String): List<File> =
        File(root, "$layer/src").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.path.replace('\\', '/').contains("Test/") } // pruebas: pueden usar :testing
            .toList()

    private fun imports(file: File): List<String> =
        file.readLines().filter { it.startsWith("import ") }.map { it.removePrefix("import ").trim() }

    @Test
    fun cadaCapaSoloDependeDeLasCapasPermitidas() {
        val errors = mutableListOf<String>()
        for ((layer, ok) in allowed) {
            val files = sources(layer)
            assertTrue(files.isNotEmpty(), "No encontré el código de :$layer")
            for (file in files) {
                for (imp in imports(file)) {
                    if (!imp.startsWith(BASE)) continue
                    val target = imp.removePrefix(BASE).substringBefore('.')
                    if (target !in ok) errors += "${file.relativeTo(root)} -> $imp"
                }
            }
        }
        if (errors.isNotEmpty()) fail("Dependencias entre capas no permitidas:\n" + errors.joinToString("\n"))
    }

    @Test
    fun domainEsKotlinPuro() {
        val errors = sources("domain")
            .filterNot { it.path.replace('\\', '/').contains("/domain/di/") } // los módulos de Koin
            .flatMap { file -> imports(file).filter { imp -> forbiddenInDomain.any(imp::startsWith) }.map { "${file.relativeTo(root)} -> $it" } }
        if (errors.isNotEmpty()) fail("domain no debe usar infraestructura:\n" + errors.joinToString("\n"))
    }

    @Test
    fun estadosEventosYEfectosVivenEnSuPropioArchivo() {
        val declaration = Regex("""^(?:data |sealed |enum )?(?:class|interface|object) (\w+(?:UiState|Event|Effect))\b""")
        val errors = sources("ui").flatMap { file ->
            file.readLines().mapNotNull { declaration.find(it)?.groupValues?.get(1) }
                .filter { name -> file.nameWithoutExtension != name }
                .map { "${file.relativeTo(root)}: $it debe ir en $it.kt" }
        }
        if (errors.isNotEmpty()) fail(errors.joinToString("\n"))
    }

    private companion object {
        const val BASE = "org.vicpoo.gasolineproject."
    }
}
