package org.vicpoo.gasolineproject.core.session

import org.vicpoo.gasolineproject.core.rbac.AppAccess

/**
 * La licencia de conducir es un REQUISITO de la función de manejar, no una función aparte:
 * solo se le pide a quien puede registrar viajes (AppAccess.TRIP_RECORDING = viaje.registrar). A los demás (ej. quien solo acepta solicitudes
 * o consulta el historial) nunca se les pide ni se les muestra.
 */
val Session.drives: Boolean
    get() = employeeId != null && AppAccess.TRIP_RECORDING.isSatisfiedBy(privileges)

/** true si maneja y todavía no sube su licencia (login, pestaña de viajes y perfil). */
val Session.mustUploadLicense: Boolean
    get() = drives && requiresLicense
