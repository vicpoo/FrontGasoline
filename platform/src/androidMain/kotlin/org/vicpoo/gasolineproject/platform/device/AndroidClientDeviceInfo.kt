package org.vicpoo.gasolineproject.platform.device

import android.os.Build
import org.vicpoo.gasolineproject.domain.device.ClientDeviceInfo

/** Android: "android" y la marca y modelo del teléfono ("Samsung SM-A245M"). */
class AndroidClientDeviceInfo : ClientDeviceInfo {
    override val platform: String = "android"

    override val name: String?
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
