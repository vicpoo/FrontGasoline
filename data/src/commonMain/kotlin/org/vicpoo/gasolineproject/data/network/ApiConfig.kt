package org.vicpoo.gasolineproject.data.network

/**
 * Configuración de APIGasoline.
 *
 * La URL base NO vive aquí: la app la pasa al iniciar Koin (androidApp la lee de
 * BuildConfig.API_BASE_URL, definida en androidApp/build.gradle.kts) y los clientes HTTP la reciben
 * con el qualifier API_BASE_URL (data/di/CoreDataModule.kt). Para apuntar a otro servidor solo se
 * cambia ese buildConfigField.
 */
object ApiConfig {
    const val REQUEST_TIMEOUT_MS = 30_000L
    const val CONNECT_TIMEOUT_MS = 15_000L

    /**
     * Subidas con archivo (foto del recibo o del odómetro): con señal débil una foto puede tardar más
     * de 30 s en subir. Antes se cortaba a los 30 s, se tomaba como "sin conexión" y se reintentaba
     * con el mismo límite, así que con mala señal nunca llegaba.
     */
    const val UPLOAD_TIMEOUT_MS = 120_000L

    /** Tiempo máximo SIN recibir ni mandar datos durante una subida (si los datos fluyen, sigue). */
    const val UPLOAD_SOCKET_TIMEOUT_MS = 45_000L
    const val ORIGIN_HEADER = "X-Origen"
    const val ORIGIN_MOBILE = "movil"
}

/** Rutas de APIGasoline (relativas a la URL base). */
object ApiRoutes {
    // Autenticación (AuthController)
    const val LOGIN = "auth/login"
    const val REFRESH = "auth/refresh"
    const val LOGOUT = "auth/logout"
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
    fun tripInterrupt(tripId: String) = "viajes/$tripId/interrumpir"
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
    /** El empleado deja SU carro ("Dejar carro"). */
    fun reservationRelease(id: String) = "solicitudes-reserva/$id/liberar"

    // Asignaciones (quién tiene cada carro; asignacion.gestionar)
    const val ASSIGNMENTS = "asignaciones"
    fun assignmentUnlink(reservationId: String) = "asignaciones/$reservationId/desvincular"

    // Notificaciones (badge de la campana)
    const val NOTIFICATIONS = "notificaciones"
    fun notificationRead(id: String) = "notificaciones/$id/marcar-leida"

    // Celulares que reciben notificaciones push (DispositivosController)
    const val DEVICES = "dispositivos"
    fun device(id: String) = "dispositivos/$id"

    // Vehículos (CarrosController)
    const val VEHICLES = "carros"
    fun vehicle(id: String) = "carros/$id"
    fun vehiclePhoto(id: String) = "carros/$id/foto"

    // Reportes de la flota (ReportesController, privilegio auditoria.ver)
    const val FLEET_REPORT = "reportes/flota"
    const val FLEET_REPORT_PDF = "reportes/flota/pdf"
    const val FLEET_REPORT_EXCEL = "reportes/flota/excel"
}
