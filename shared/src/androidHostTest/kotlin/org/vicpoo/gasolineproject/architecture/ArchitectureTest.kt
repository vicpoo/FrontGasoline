package org.vicpoo.gasolineproject.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Reglas de la arquitectura por capas (docs/ARQUITECTURA.md). Lee el código fuente, sin librerías extra:
 * - domain no conoce a nadie; data y platform solo conocen domain; ui conoce domain y platform (nunca data).
 * - El esquema de la base (:database, solo código generado por SQLDelight) lo usa únicamente data.
 * - domain es Kotlin puro: sin Ktor, DataStore, okio, Android ni Compose.
 * - En ui, cada UiState / Event / Effect vive en su propio archivo (XUiState.kt, XEvent.kt, XEffect.kt).
 * - expect/actual solo para el armado de Koin y composables: un único `expect val platformModule` y, fuera
 *   de eso, solo `@Composable expect fun` en platform (cámara, documentos, permisos...) y en ui (mapa).
 *   Lo demás se resuelve con una interfaz de domain registrada en platformModule (punto 2 del code review).
 */
class ArchitectureTest {

    private val root: File = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }

    private val allowed = mapOf(
        "domain" to setOf("domain"),
        "data" to setOf("domain", "data", "database"),
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
    fun expectSoloParaElModuloDePlataformaYComposables() {
        val expectDeclaration = Regex("""^\s*(?:public |internal )?expect\s+(?:fun|val|var|class|object|interface|annotation|enum)\b""")
        val errors = mutableListOf<String>()
        for (layer in listOf("domain", "data", "database", "platform", "ui", "shared")) {
            for (file in sources(layer)) {
                val lines = file.readLines()
                lines.forEachIndexed { index, line ->
                    if (!expectDeclaration.containsMatchIn(line)) return@forEachIndexed
                    val composable = lines.subList(maxOf(0, index - 3), index).any { it.trim() == "@Composable" }
                    val ok = when (layer) {
                        "platform" -> composable || line.trim() == "expect val platformModule: Module"
                        "ui" -> composable
                        else -> false
                    }
                    if (!ok) errors += "${file.relativeTo(root)}:${index + 1}: ${line.trim()}"
                }
            }
        }
        if (errors.isNotEmpty()) {
            fail("expect solo para platformModule y composables; lo demás va por interfaz en platformModule:\n" + errors.joinToString("\n"))
        }
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
