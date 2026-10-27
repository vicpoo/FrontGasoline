package org.vicpoo.gasolineproject.core.common

/** Resultado de cualquier operación de datos: éxito o fallo tipado (sin excepciones hacia la UI). */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

/** Errores de la app. La UI los traduce a texto con AppError.asText(). */
sealed interface AppError {
    data object NoConnection : AppError
    data object Unauthorized : AppError

    /** 403. message = "detail" del ProblemDetails de la API (ej. "El usuario está inactivo."). */
    data class Forbidden(val message: String? = null) : AppError
    data object NotFound : AppError

    /** El usuario no está ligado a un empleado/empresa (ej. Administrador interno). */
    data object NoCompany : AppError
    data class Validation(val reason: ValidationReason) : AppError
    data class Server(val code: Int, val message: String?) : AppError
    data class Unknown(val cause: Throwable? = null) : AppError
}

enum class ValidationReason {
    REQUIRED,
    INVALID_FORMAT,
    OUT_OF_RANGE,
    INVALID_FILE_TYPE,
    FILE_TOO_LARGE,
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

/** El dato si salió bien, o null si falló (para llamadas opcionales). */
fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success<T>)?.data

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

/** Mensaje del servidor (si lo hay) para guardarlo junto a un cambio que no se pudo sincronizar. */
fun AppError.serverMessage(): String? = when (this) {
    is AppError.Server -> message
    is AppError.Forbidden -> message
    else -> null
}

/** Código HTTP equivalente (para depurar errores de sincronización). */
fun AppError.httpCode(): Int? = when (this) {
    AppError.Unauthorized -> 401
    is AppError.Forbidden -> 403
    AppError.NotFound -> 404
    is AppError.Server -> code
    else -> null
}
