package org.vicpoo.gasolineproject.core.network

import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

/**
 * Cuerpo multipart/form-data con un solo archivo en el campo "archivo"
 * (así lo esperan los endpoints de fotos de APIGasoline: SubirArchivoRequest).
 */
fun singleFileMultipart(bytes: ByteArray, fileName: String, mimeType: String): MultiPartFormDataContent =
    MultiPartFormDataContent(
        formData {
            append(
                key = "archivo",
                value = bytes,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                },
            )
        },
    )
