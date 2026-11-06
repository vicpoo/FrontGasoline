package org.vicpoo.gasolineproject.data.network

import android.os.Build

actual object ClientDevice {
    actual val platform: String = "android"

    actual val name: String?
        get() = runCatching {
            val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
            val model = Build.MODEL.orEmpty()
            when {
                model.isBlank() -> manufacturer.ifBlank { null }
                model.startsWith(manufacturer, ignoreCase = true) -> model
                else -> "$manufacturer $model".trim()
            }
        }.getOrNull()
}
