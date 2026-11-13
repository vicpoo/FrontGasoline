package org.vicpoo.gasolineproject.data.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse

/**
 * Listas largas de la API (viajes y solicitudes): se piden de 10 en 10.
 * Al abrir se ven los 10 más recientes y al bajar se piden los 10 siguientes.
 */
const val LIST_PAGE_SIZE = 10

/** Máximo que la API deja pedir en una sola página (?tamanoPagina=). */
const val API_MAX_PAGE_SIZE = 200

/** Una página de la API: los elementos y el total con esos filtros (encabezado X-Total-Count). */
data class PageDto<T>(val items: List<T>, val total: Int?)

/** Lee el cuerpo como lista y el total del encabezado X-Total-Count. */
suspend inline fun <reified T> HttpResponse.toPage(): PageDto<T> =
    PageDto(items = body<List<T>>(), total = headers["X-Total-Count"]?.toIntOrNull())

/**
 * ¿Quedan más elementos después de las primeras [loadedItems] posiciones?
 * Si la API no mandó el total, se supone que hay más cuando la página llegó llena.
 */
fun hasMoreAfter(loadedItems: Int, received: Int, requested: Int, total: Int?): Boolean =
    if (total != null) loadedItems < total else received >= requested

/**
 * Al actualizar se vuelven a pedir TODAS las páginas que el usuario ya había visto, en una sola
 * petición (pagina=1 con un tamaño mayor). Así recargar no lo regresa a los primeros 10.
 */
fun refreshSize(loadedPages: Int): Int =
    (loadedPages.coerceAtLeast(1) * LIST_PAGE_SIZE).coerceAtMost(API_MAX_PAGE_SIZE)
