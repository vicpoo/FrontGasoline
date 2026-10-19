package org.vicpoo.gasolineproject.core.rbac

/**
 * Claves de privilegios RBAC (columna privilegio.clave de APIGasoline, formato modulo.accion).
 * La app decide qué mostrar SOLO con estas claves, nunca con el nombre del rol.
 *
 * IMPORTANTE: estas claves deben existir en la tabla privilegio (POST /api/privilegios)
 * y estar asignadas a los roles (POST /api/rolprivilegios). Si cambias una clave en la BD,
 * cámbiala aquí; las pantallas solo usan estas constantes.
 */
object Privileges {
    // Vehículos (carros)
    const val CARRO_VER = "carro.ver"
    const val CARRO_CREAR = "carro.crear"
    const val CARRO_EDITAR = "carro.editar"
    const val CARRO_ELIMINAR = "carro.eliminar"

    // Siguientes módulos (reservas, viajes, cargas, reportes) se agregan aquí
    const val SOLICITUD_CREAR = "solicitud.crear"
    const val SOLICITUD_APROBAR = "solicitud.aprobar"
    const val VIAJE_VER_PROPIOS = "viaje.ver_propios"
    const val VIAJE_VER_TODOS = "viaje.ver_todos"
}
