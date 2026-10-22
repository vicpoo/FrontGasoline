package org.vicpoo.gasolineproject.core.rbac

import org.vicpoo.gasolineproject.core.rbac.Privileges.CARGA_EDITAR
import org.vicpoo.gasolineproject.core.rbac.Privileges.CARGA_REGISTRAR
import org.vicpoo.gasolineproject.core.rbac.Privileges.CARRO_CREAR
import org.vicpoo.gasolineproject.core.rbac.Privileges.CARRO_EDITAR
import org.vicpoo.gasolineproject.core.rbac.Privileges.CARRO_ELIMINAR
import org.vicpoo.gasolineproject.core.rbac.Privileges.CARRO_VER
import org.vicpoo.gasolineproject.core.rbac.Privileges.CARRO_VER_DETALLE
import org.vicpoo.gasolineproject.core.rbac.Privileges.SOLICITUD_CREAR
import org.vicpoo.gasolineproject.core.rbac.Privileges.SOLICITUD_VER_PROPIAS
import org.vicpoo.gasolineproject.core.rbac.Privileges.SOLICITUD_VER_TODAS
import org.vicpoo.gasolineproject.core.rbac.Privileges.VIAJE_REGISTRAR
import org.vicpoo.gasolineproject.core.rbac.Privileges.VIAJE_VER_DETALLE
import org.vicpoo.gasolineproject.core.rbac.Privileges.VIAJE_VER_PROPIOS
import org.vicpoo.gasolineproject.core.rbac.Privileges.VIAJE_VER_TODOS

/**
 * Catálogo ÚNICO de qué privilegios abre cada parte de la app (pestañas, pantallas y la campana).
 * Las rutas, la barra de pestañas y los componentes solo usan estas reglas: para cambiar quién
 * ve algo se edita aquí y en ningún otro archivo.
 *
 * Los botones dentro de una pantalla (editar, eliminar, aprobar...) se deciden en su ViewModel
 * con PermissionChecker.observe(Privileges.X): una función = un privilegio.
 */
object AppAccess {

    // ---------------- Pestañas ----------------

    /**
     * Inicio: los carros de la empresa para PEDIR uno (carro.ver Y solicitud.crear).
     * Quien solo administra carros ya los ve en Flota, así no le salen dos pestañas de vehículos.
     */
    val HOME_TAB = AccessRule(anyOf = setOf(CARRO_VER), allOf = setOf(SOLICITUD_CREAR))

    /** Registrar viajes (iniciar, pausar, terminar, cargas). También exige subir la licencia. */
    val TRIP_RECORDING = AccessRule.anyOf(VIAJE_REGISTRAR)

    /** Flota: gestión de carros (agregar, editar o eliminar). */
    val FLEET_TAB = AccessRule.anyOf(CARRO_CREAR, CARRO_EDITAR, CARRO_ELIMINAR)

    /** Solicitudes de la empresa (aceptar / rechazar). */
    val COMPANY_REQUESTS = AccessRule.anyOf(SOLICITUD_VER_TODAS)

    /** Historial de viajes (propios o de todos). */
    val TRIP_HISTORY = AccessRule.anyOf(VIAJE_VER_PROPIOS, VIAJE_VER_TODOS)

    /** Perfil: todos. */
    val PROFILE = AccessRule.Everyone

    // ---------------- Pantallas ----------------

    val VEHICLE_LIST = AccessRule.anyOf(CARRO_VER)
    val VEHICLE_DETAIL = AccessRule.anyOf(CARRO_VER_DETALLE)
    val VEHICLE_FORM = AccessRule.anyOf(CARRO_CREAR, CARRO_EDITAR)

    /** Campana y "Mis solicitudes": estado de las solicitudes propias. */
    val MY_RESERVATIONS = AccessRule.anyOf(SOLICITUD_VER_PROPIAS)

    val TRIP_DETAIL = AccessRule.anyOf(VIAJE_VER_DETALLE)

    /** Formulario de recarga: se abre para agregar o para corregir una. */
    val FUEL_FORM = AccessRule.anyOf(CARGA_REGISTRAR, CARGA_EDITAR)

    /** Actualizar la licencia desde Perfil: solo quien maneja. */
    val UPDATE_LICENSE = AccessRule.anyOf(VIAJE_REGISTRAR)
}
