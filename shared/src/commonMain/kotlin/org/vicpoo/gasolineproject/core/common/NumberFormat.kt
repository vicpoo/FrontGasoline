package org.vicpoo.gasolineproject.core.common

import kotlin.math.roundToLong

/** 50.0 -> "50", 45.5 -> "45.5", 18230.25 -> "18230.25" (máximo 2 decimales). */
fun formatDecimal(value: Double): String {
    val rounded = (value * 100).roundToLong() / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}
