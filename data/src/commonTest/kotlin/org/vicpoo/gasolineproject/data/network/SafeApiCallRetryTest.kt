package org.vicpoo.gasolineproject.data.network

import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.sync.TemporarySyncFailures
import org.vicpoo.gasolineproject.testing.FakeApiServer
import org.vicpoo.gasolineproject.testing.respondProblem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** safeApiCall anota los errores temporales (5xx, 408, 429) y el Retry-After para la vuelta en segundo plano. */
class SafeApiCallRetryTest {

    private val api = FakeApiServer()

    private suspend fun call(failures: TemporarySyncFailures): AppResult<Unit> =
        withContext(failures) { safeApiCall { api.client.get("viajes"); Unit } }

    @Test
    fun error503_seAnota_conSuRetryAfter() = runTest {
        api.handler = {
            respond(
                """{"status":503,"title":"Error","detail":"Inténtalo en unos segundos"}""",
                HttpStatusCode.ServiceUnavailable,
                headersOf(
                    HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                    HttpHeaders.RetryAfter to listOf("90"),
                ),
            )
        }
        val failures = TemporarySyncFailures()

        val result = call(failures)

        assertIs<AppResult.Failure>(result)
        assertEquals(503, (result.error as AppError.Server).code)
        assertEquals(1, failures.count)
        assertEquals(90_000L, failures.maxRetryAfterMillis)
    }

    @Test
    fun error429_sinRetryAfter_seAnotaSinEspera() = runTest {
        api.handler = { respondProblem(HttpStatusCode.TooManyRequests, "Demasiadas peticiones") }
        val failures = TemporarySyncFailures()

        call(failures)

        assertEquals(1, failures.count)
        assertNull(failures.maxRetryAfterMillis)
    }

    @Test
    fun rechazoDefinitivo4xx_noSeAnota() = runTest {
        api.handler = { respondProblem(HttpStatusCode.Conflict, "Las placas ya existen") }
        val failures = TemporarySyncFailures()

        call(failures)

        assertEquals(0, failures.count)
    }
}
