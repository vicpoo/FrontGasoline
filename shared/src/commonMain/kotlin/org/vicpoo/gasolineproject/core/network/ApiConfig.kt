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
    const val ORIGIN_HEADER = "X-Origen"
    const val ORIGIN_MOBILE = "movil"
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
    const val TRIPS_SYNC = "viajes/sincronizar"
    fun tripFinish(tripId: String) = "viajes/$tripId/finalizar"
    fun tripPause(tripId: String) = "viajes/$tripId/pausar"
    fun tripResume(tripId: String) = "viajes/$tripId/reanudar"
    /** momento = "inicial" | "final" (foto del odómetro). */
    fun tripDashboardPhoto(tripId: String, momento: String) = "viajes/$tripId/foto-tablero/$momento"
    fun tripRoutePoints(tripId: String) = "viajes/$tripId/puntos-ruta"
    fun tripRouteMap(tripId: String) = "viajes/$tripId/ruta-procesada/mapa"
    const val FUEL_LOADS = "cargas-combustible"
    fun fuelLoad(id: String) = "cargas-combustible/$id"

    // Centros de costo (CentrosCostoController)
    const val COST_CENTERS = "centros-costo"

    // Solicitudes de reserva
    const val RESERVATION_REQUESTS = "solicitudes-reserva"
    fun reservationApprove(id: String) = "solicitudes-reserva/$id/aprobar"
    fun reservationReject(id: String) = "solicitudes-reserva/$id/rechazar"
    fun reservation(id: String) = "solicitudes-reserva/$id"

    // Notificaciones (badge de la campana)
    const val NOTIFICATIONS = "notificaciones"
    fun notificationRead(id: String) = "notificaciones/$id/marcar-leida"

    // Vehículos (CarrosController)
    const val VEHICLES = "carros"
    fun vehicle(id: String) = "carros/$id"
    fun vehiclePhoto(id: String) = "carros/$id/foto"
}
