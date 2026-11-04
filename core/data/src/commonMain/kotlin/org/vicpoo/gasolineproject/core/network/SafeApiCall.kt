package org.vicpoo.gasolineproject.core.network

import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult

/**
 * ProblemDetails de APIGasoline (ExceptionHandlingMiddleware): { status, title, detail } y, en algunos 409,
 * extensiones como { codigo: "VIAJE_EN_CURSO", viajeId, empleadoOperadorId, fechaInicio }.
 */
private class ParsedProblem(val message: String?, val code: String?, val data: Map<String, String>)

/** Campos estándar del ProblemDetails: todo lo demás se guarda en [ParsedProblem.data]. */
private val STANDARD_PROBLEM_KEYS = setOf("type", "title", "status", "detail", "instance", "message", "codigo", "traceId", "errors")

private fun parseProblem(body: String): ParsedProblem {
    val obj = errorJson.parseToJsonElement(body).jsonObject
    fun text(key: String): String? = (obj[key] as? JsonPrimitive)?.contentOrNull
    val data = obj
        .filterKeys { it !in STANDARD_PROBLEM_KEYS }
        .mapNotNull { (key, value) -> (value as? JsonPrimitive)?.contentOrNull?.let { key to it } }
        .toMap()
    return ParsedProblem(
        message = text("detail") ?: text("message") ?: text("title"),
        code = text("codigo"),
        data = data,
    )
}

private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Ejecuta una llamada a la API y convierte cualquier excepción en AppResult.Failure.
 * Así los repositorios y ViewModels nunca manejan excepciones de Ktor.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        val status = e.response.status.value
        println("[API] HTTP $status ${e.response.call.request.method.value} ${e.response.call.request.url}")
        val problem = runCatching { parseProblem(e.response.bodyAsText()) }.getOrNull()
        val serverMessage = problem?.message
        AppResult.Failure(
            when (status) {
                401 -> AppError.Unauthorized
                403 -> AppError.Forbidden(serverMessage)
                404 -> AppError.NotFound
                else -> AppError.Server(status, serverMessage, problem?.code, problem?.data.orEmpty())
            },
        )
    } catch (e: SerializationException) {
        logApiError(e)
        AppResult.Failure(AppError.Unknown(e))
    } catch (e: ContentConvertException) {
        // La respuesta llegó pero no se pudo leer (el JSON no coincide con el DTO): NO es falta de red
        logApiError(e)
        AppResult.Failure(AppError.Unknown(e))
    } catch (e: IOException) {
        // Timeout, sin red, DNS, conexión rechazada o cortada...
        logApiError(e)
        AppResult.Failure(AppError.NoConnection)
    } catch (e: Exception) {
        // Cualquier otro error del lado de la app: antes se mostraba como "sin conexión" y confundía
        logApiError(e)
        AppResult.Failure(AppError.Unknown(e))
    }

/** Deja la causa real en el Logcat (filtra por "[API]") para poder diagnosticar. */
private fun logApiError(e: Throwable) {
    println("[API] ${e::class.simpleName}: ${e.message}")
}
