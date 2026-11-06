package org.vicpoo.gasolineproject.data.network

import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder

/**
 * Tiempos largos para una petición que sube un archivo (multipart). Úsala dentro del bloque de la
 * petición: `client.post(ruta) { withUploadTimeout(); setBody(...) }`.
 */
fun HttpRequestBuilder.withUploadTimeout() {
    timeout {
        requestTimeoutMillis = ApiConfig.UPLOAD_TIMEOUT_MS
        socketTimeoutMillis = ApiConfig.UPLOAD_SOCKET_TIMEOUT_MS
    }
}
