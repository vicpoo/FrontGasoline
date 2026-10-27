package org.vicpoo.gasolineproject.core.push

/** iOS: pendiente (requiere APNs y el SDK de Firebase en iosApp). Por ahora no registra el celular. */
class IosPushTokenProvider : PushTokenProvider {
    override val platform: String = "ios"
    override suspend fun currentToken(): String? = null
    override suspend fun deleteToken() = Unit
}
