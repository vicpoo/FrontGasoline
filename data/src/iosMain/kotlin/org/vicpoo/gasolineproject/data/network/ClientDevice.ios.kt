package org.vicpoo.gasolineproject.data.network

import platform.UIKit.UIDevice

actual object ClientDevice {
    actual val platform: String = "ios"

    /** "iPhone" / "iPad" (iOS no expone el nombre del modelo sin pedir permisos). */
    actual val name: String?
        get() = runCatching { UIDevice.currentDevice.model }.getOrNull()
}
