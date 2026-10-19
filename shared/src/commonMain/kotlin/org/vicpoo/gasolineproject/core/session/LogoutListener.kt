package org.vicpoo.gasolineproject.core.session

/**
 * Cada feature que guarda datos offline implementa esta interfaz para borrarlos al cerrar sesión.
 * Así el login no depende de vehículos, viajes, etc. (Open/Closed + Dependency Inversion):
 * una feature nueva solo registra su LogoutListener en Koin.
 */
fun interface LogoutListener {
    suspend fun onLogout()
}
