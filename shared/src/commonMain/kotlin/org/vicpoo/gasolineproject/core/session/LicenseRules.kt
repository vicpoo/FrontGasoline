package org.vicpoo.gasolineproject.core.session

import org.vicpoo.gasolineproject.core.rbac.Privileges

/**
 * La licencia de conducir es un REQUISITO de la función de manejar, no una función aparte:
 * solo se le pide a quien tiene viaje.registrar. A los demás (ej. quien solo acepta solicitudes
 * o consulta el historial) nunca se les pide ni se les muestra.
 */
val Session.drives: Boolean
    get() = employeeId != null && Privileges.VIAJE_REGISTRAR in privileges

/** true si maneja y todavía no sube su licencia (login, pestaña de viajes y perfil). */
val Session.mustUploadLicense: Boolean
    get() = drives && requiresLicense
