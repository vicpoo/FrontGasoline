package org.vicpoo.gasolineproject.platform.log

import org.vicpoo.gasolineproject.domain.common.AppLogLevel
import org.vicpoo.gasolineproject.domain.common.AppLogger
import platform.Foundation.NSLog

/** Bitácora en la consola de iOS (NSLog). [minLevel]: en release solo WARN y ERROR. */
class IosAppLogger(private val minLevel: AppLogLevel) : AppLogger {

    override fun isEnabled(level: AppLogLevel): Boolean = level >= minLevel

    override fun log(level: AppLogLevel, tag: String, message: String, throwable: Throwable?) {
        if (!isEnabled(level)) return
        val text = buildString {
            append("[Gasoline/").append(tag).append("] ").append(level.name).append(": ").append(message)
            throwable?.let { append('\n').append(it.stackTraceToString()) }
        }
        // El texto va como formato de NSLog: se escapan los "%" para que no se interpreten
        NSLog(text.replace("%", "%%"))
    }
}
