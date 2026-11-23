package org.vicpoo.gasolineproject.domain.common

/** Nivel de un mensaje de la bitácora. */
enum class AppLogLevel { DEBUG, INFO, WARN, ERROR }

/**
 * Bitácora de la app (Logcat en Android, NSLog en iOS). La implementa platform y se registra en
 * platformModule; domain y data solo conocen esta interfaz.
 *
 * En release solo se escriben avisos y errores ([minLogLevel]); debug e info se silencian.
 *
 * NUNCA registrar: tokens (JWT, refresh), el encabezado Authorization, cuerpos de login
 * (contraseña), coordenadas GPS ni fotos. Para la API basta método, ruta y código HTTP.
 */
interface AppLogger {
    fun isEnabled(level: AppLogLevel): Boolean
    fun log(level: AppLogLevel, tag: String, message: String, throwable: Throwable? = null)
}

fun AppLogger.d(tag: String, message: String) = log(AppLogLevel.DEBUG, tag, message)
fun AppLogger.i(tag: String, message: String) = log(AppLogLevel.INFO, tag, message)
fun AppLogger.w(tag: String, message: String, throwable: Throwable? = null) = log(AppLogLevel.WARN, tag, message, throwable)
fun AppLogger.e(tag: String, message: String, throwable: Throwable? = null) = log(AppLogLevel.ERROR, tag, message, throwable)

/** Nivel mínimo que se escribe: todo en debug; en release solo avisos y errores. */
fun minLogLevel(isDebug: Boolean): AppLogLevel = if (isDebug) AppLogLevel.DEBUG else AppLogLevel.WARN

/** No escribe nada: para pruebas y antes de que arranque Koin. */
object SilentAppLogger : AppLogger {
    override fun isEnabled(level: AppLogLevel): Boolean = false
    override fun log(level: AppLogLevel, tag: String, message: String, throwable: Throwable?) = Unit
}

/** Datos del build que necesitan varias capas (lo registra initKoin desde BuildConfig.DEBUG). */
data class AppBuildInfo(val isDebug: Boolean)

/**
 * Acceso a la bitácora para el código que NO se crea con Koin (ej. safeApiCall, que es una función
 * suelta usada por todos los repositorios). initKoin instala el [AppLogger] de la plataforma al arrancar;
 * mientras tanto (y en las pruebas) no escribe nada. Las clases que sí se crean con Koin reciben el
 * [AppLogger] por constructor.
 */
object AppLog {
    var logger: AppLogger = SilentAppLogger
        private set

    fun install(logger: AppLogger) {
        this.logger = logger
    }

    /** Solo para pruebas: vuelve a no escribir nada. */
    fun reset() {
        logger = SilentAppLogger
    }

    fun d(tag: String, message: String) = logger.d(tag, message)
    fun i(tag: String, message: String) = logger.i(tag, message)
    fun w(tag: String, message: String, throwable: Throwable? = null) = logger.w(tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = logger.e(tag, message, throwable)
}
