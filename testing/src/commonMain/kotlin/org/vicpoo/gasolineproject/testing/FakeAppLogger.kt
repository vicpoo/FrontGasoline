package org.vicpoo.gasolineproject.testing

import org.vicpoo.gasolineproject.domain.common.AppLogLevel
import org.vicpoo.gasolineproject.domain.common.AppLogger

/** Bitácora en memoria: la prueba revisa qué se registró y con qué nivel. */
class FakeAppLogger : AppLogger {
    data class Entry(val level: AppLogLevel, val tag: String, val message: String, val throwable: Throwable?)

    val entries = mutableListOf<Entry>()

    fun count(level: AppLogLevel): Int = entries.count { it.level == level }

    override fun isEnabled(level: AppLogLevel): Boolean = true

    override fun log(level: AppLogLevel, tag: String, message: String, throwable: Throwable?) {
        entries += Entry(level, tag, message, throwable)
    }
}
