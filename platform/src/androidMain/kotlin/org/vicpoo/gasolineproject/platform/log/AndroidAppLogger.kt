package org.vicpoo.gasolineproject.platform.log

import android.util.Log
import org.vicpoo.gasolineproject.domain.common.AppLogLevel
import org.vicpoo.gasolineproject.domain.common.AppLogger

/**
 * Bitácora en Logcat. Las etiquetas salen como "Gasoline/<tag>" (filtra por "Gasoline").
 * [minLevel]: en release solo WARN y ERROR (ver minLogLevel).
 */
class AndroidAppLogger(private val minLevel: AppLogLevel) : AppLogger {

    override fun isEnabled(level: AppLogLevel): Boolean = level >= minLevel

    override fun log(level: AppLogLevel, tag: String, message: String, throwable: Throwable?) {
        if (!isEnabled(level)) return
        val fullTag = "$TAG_PREFIX$tag".take(MAX_TAG_LENGTH)
        val text = if (throwable == null) message else "$message\n${Log.getStackTraceString(throwable)}"
        Log.println(level.priority(), fullTag, text)
    }

    private fun AppLogLevel.priority(): Int = when (this) {
        AppLogLevel.DEBUG -> Log.DEBUG
        AppLogLevel.INFO -> Log.INFO
        AppLogLevel.WARN -> Log.WARN
        AppLogLevel.ERROR -> Log.ERROR
    }

    private companion object {
        const val TAG_PREFIX = "Gasoline/"

        /** Antes de Android 8 las etiquetas de más de 23 caracteres fallan. */
        const val MAX_TAG_LENGTH = 23
    }
}
