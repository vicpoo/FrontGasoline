package org.vicpoo.gasolineproject.testing

import org.vicpoo.gasolineproject.domain.device.ClientDeviceInfo

/** Datos del teléfono para pruebas. */
data class FakeClientDeviceInfo(
    override val platform: String = "android",
    override val name: String? = "Teléfono de pruebas",
) : ClientDeviceInfo
