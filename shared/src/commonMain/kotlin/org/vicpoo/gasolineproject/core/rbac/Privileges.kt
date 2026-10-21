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
    /** Ver la información de un solo vehículo (GET /api/carros/{id}). */
    const val CARRO_VER_DETALLE = "carro.ver_detalle"
    const val CARRO_CREAR = "carro.crear"
    const val CARRO_EDITAR = "carro.editar"
    const val CARRO_ELIMINAR = "carro.eliminar"

    // Siguientes módulos (reservas, viajes, cargas, reportes) se agregan aquí
    const val SOLICITUD_CREAR = "solicitud.crear"
    const val SOLICITUD_VER_PROPIAS = "solicitud.ver_propias"
    const val SOLICITUD_VER_TODAS = "solicitud.ver_todas"
    const val SOLICITUD_APROBAR = "solicitud.aprobar"
    const val SOLICITUD_RECHAZAR = "solicitud.rechazar"
    const val VIAJE_VER_PROPIOS = "viaje.ver_propios"
    const val VIAJE_VER_TODOS = "viaje.ver_todos"
    /** Detalle de un viaje: datos, pausas, mapa y cargas. */
    const val VIAJE_VER_DETALLE = "viaje.ver_detalle"

    /** Pestaña "Registrar": iniciar y finalizar viajes con un carro asignado. */
    const val VIAJE_REGISTRAR = "viaje.registrar"

    /** Agregar / editar / eliminar recargas de gasolina durante el viaje. */
    const val CARGA_REGISTRAR = "carga.registrar"
    const val CARGA_EDITAR = "carga.editar"
    const val CARGA_ELIMINAR = "carga.eliminar"

    // Mismas claves que APIGasoline (Application/Common/Security/Permisos.cs).
    // La API también autoriza por privilegio, así que el nombre del rol nunca importa.
    const val CARRO_CAMBIAR_ESTATUS = "carro.cambiar_estatus"
    const val CENTRO_COSTO_VER = "centro_costo.ver"
    const val VIAJE_CORREGIR_CERRADO = "viaje.corregir_cerrado"
    const val AUDITORIA_VER = "auditoria.ver"
    const val EMPLEADO_VER = "empleado.ver"
}
