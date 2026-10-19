package org.vicpoo.gasolineproject.core.network

/**
 * Configuración de APIGasoline.
 * - Emulador de Android: 10.0.2.2 = "localhost de tu PC".
 * - Celular físico: usa la IP de tu PC en la red (ej. http://192.168.1.50:5234/api/).
 * Corre la API con el perfil "http" o "emulador-android" (http://localhost:5234).
 * Debe terminar en "/".
 */
object ApiConfig {
    const val BASE_URL = "http://10.0.2.2:5234/api/"
    const val REQUEST_TIMEOUT_MS = 30_000L
    const val CONNECT_TIMEOUT_MS = 15_000L
}

/** Rutas de APIGasoline (relativas a BASE_URL). */
object ApiRoutes {
    // Autenticación (AuthController)
    const val LOGIN = "auth/login"
    const val PROFILE = "auth/perfil"
    const val PRIVILEGES = "auth/privilegios"

    // Empleados (EmpleadosController)
    fun employee(employeeId: String) = "empleados/$employeeId"
    fun employeeLicense(employeeId: String) = "empleados/$employeeId/foto-licencia"
    fun employeeProfilePhoto(employeeId: String) = "empleados/$employeeId/foto-perfil"

    const val EMPLOYEES = "empleados"

    // Viajes, rutas y cargas
    const val TRIPS = "viajes"
    fun tripRouteMap(tripId: String) = "viajes/$tripId/ruta-procesada/mapa"
    const val FUEL_LOADS = "cargas-combustible"

    // Solicitudes de reserva
    const val RESERVATION_REQUESTS = "solicitudes-reserva"

    // Vehículos (CarrosController)
    const val VEHICLES = "carros"
    fun vehicle(id: String) = "carros/$id"
    fun vehiclePhoto(id: String) = "carros/$id/foto"
}
