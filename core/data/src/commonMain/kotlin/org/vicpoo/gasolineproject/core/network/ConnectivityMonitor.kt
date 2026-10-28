package org.vicpoo.gasolineproject.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

/**
 * Estado de la red del teléfono (Android: ConnectivityManager; iOS: NWPathMonitor).
 *
 * Ojo: "hay red" no garantiza que la API responda (red sin salida a internet, servidor caído).
 * Por eso las pantallas siguen marcando "sin conexión" también cuando una petición falla con
 * AppError.NoConnection; este monitor solo permite enterarse ANTES y reaccionar al volver la red.
 */
interface ConnectivityMonitor {
    /** true si el teléfono tiene una red con salida a internet. */
    val isOnline: StateFlow<Boolean>
}

/** Emite cada vez que se recupera la conexión (pasa de sin red a con red). No emite al empezar. */
fun ConnectivityMonitor.reconnections(): Flow<Unit> = flow {
    var wasOnline: Boolean? = null
    isOnline.collect { online ->
        if (online && wasOnline == false) emit(Unit)
        wasOnline = online
    }
}
