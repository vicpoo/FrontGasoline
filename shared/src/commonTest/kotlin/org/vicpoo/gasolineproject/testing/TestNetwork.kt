package org.vicpoo.gasolineproject.testing

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import okio.fakefilesystem.FakeFileSystem
import org.vicpoo.gasolineproject.core.network.TokenProvider
import org.vicpoo.gasolineproject.core.network.UnauthorizedHandler
import org.vicpoo.gasolineproject.core.network.createHttpClient
import org.vicpoo.gasolineproject.core.platform.AppDirectories
import org.vicpoo.gasolineproject.core.sync.BackgroundSyncScheduler

/** Mismo Json que coreModule. */
val testJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * API falsa: usa el cliente real de la app (createHttpClient: expectSuccess, JSON, AuthPlugin)
 * con un MockEngine, así también se prueba cómo safeApiCall convierte cada respuesta en AppError.
 *
 * - [handler] decide la respuesta de cada petición (cámbialo en cada prueba).
 * - [requests] guarda "MÉTODO ruta" de cada petición, ej. "POST carros".
 */
class FakeApiServer {
    val requests = mutableListOf<String>()

    var handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = {
        error("La prueba no definió respuesta para ${it.method.value} ${it.url.encodedPath}")
    }

    val client: HttpClient = createHttpClient(
        engine = MockEngine { request ->
            requests += "${request.method.value} ${request.url.encodedPath.removePrefix("/api/")}"
            handler(request)
        },
        tokenProvider = TokenProvider { null },
        unauthorizedHandler = UnauthorizedHandler { },
        json = testJson,
    )
}

/** Respuesta JSON con el código indicado. */
fun MockRequestHandleScope.respondJson(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

/** Error con ProblemDetails, como lo manda APIGasoline. */
fun MockRequestHandleScope.respondProblem(status: HttpStatusCode, detail: String): HttpResponseData =
    respondJson("""{"status":${status.value},"title":"Error","detail":"$detail"}""", status)

/** Simula que no hay red (safeApiCall lo convierte en AppError.NoConnection). */
fun noConnection(): Nothing = throw IOException("Sin red (prueba)")

/** Sistema de archivos en memoria con la carpeta de la app (funciona igual en Windows, Linux y macOS). */
class TestStorage {
    val fileSystem = FakeFileSystem()
    val directories = AppDirectories(filesDir = (fileSystem.workingDirectory / "app").toString())
}

/** Planificador que solo cuenta las llamadas. */
class RecordingSyncScheduler : BackgroundSyncScheduler {
    var periodicCalls = 0
    var whenOnlineCalls = 0
    var cancelCalls = 0

    override fun schedulePeriodic() { periodicCalls++ }
    override fun requestWhenOnline() { whenOnlineCalls++ }
    override fun cancel() { cancelCalls++ }
}
