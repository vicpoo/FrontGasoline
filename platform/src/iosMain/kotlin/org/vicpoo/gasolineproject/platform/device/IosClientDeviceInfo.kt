package org.vicpoo.gasolineproject.platform.device

import org.vicpoo.gasolineproject.domain.device.ClientDeviceInfo
import platform.UIKit.UIDevice

/** iOS: "ios" y "iPhone" / "iPad" (iOS no expone el nombre del modelo sin pedir permisos). */
class IosClientDeviceInfo : ClientDeviceInfo {
    override val platform: String = "ios"

    override val name: String?
        get() = runCatching { UIDevice.currentDevice.model }.getOrNull()
}
