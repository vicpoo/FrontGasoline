package org.vicpoo.gasolineproject.core.network

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult

/** ProblemDetails de APIGasoline (ExceptionHandlingMiddleware): { status, title, detail }. */
@Serializable
private data class ProblemDetailsDto(
    val title: String? = null,
    val detail: String? = null,
    val message: String? = null,
)

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
        val serverMessage = runCatching {
            val dto = errorJson.decodeFromString(ProblemDetailsDto.serializer(), e.response.bodyAsText())
            dto.detail ?: dto.message ?: dto.title
        }.getOrNull()
        AppResult.Failure(
            when (status) {
                401 -> AppError.Unauthorized
                403 -> AppError.Forbidden(serverMessage)
                404 -> AppError.NotFound
                else -> AppError.Server(status, serverMessage)
            },
        )
    } catch (e: SerializationException) {
        AppResult.Failure(AppError.Unknown(e))
    } catch (e: Exception) {
        // Timeout, sin red, DNS, conexión rechazada...
        AppResult.Failure(AppError.NoConnection)
    }
