package org.vicpoo.gasolineproject.core.session

import kotlinx.serialization.Serializable

/**
 * Sesión del usuario guardada en el dispositivo (permite abrir la app sin red).
 * Se arma con la respuesta de POST /api/auth/login.
 */
@Serializable
data class Session(
    val token: String,
    /** Fecha de expiración del JWT en UTC (ISO-8601, viene de expiraEnUtc). */
    val expiresAtUtc: String? = null,
    val userId: String,
    val email: String,
    val name: String? = null,
    /** URL temporal (MinIO) de la foto de perfil del empleado. */
    val photoUrl: String? = null,
    /** null = usuario sin empleado (ej. Administrador interno). */
    val employeeId: String? = null,
    val companyId: String? = null,
    val roles: List<String> = emptyList(),
    /** Claves de privilegios RBAC (ej. "carro.crear"). */
    val privileges: Set<String> = emptySet(),
    /** Hash de los privilegios; se manda en If-None-Match para saber si cambiaron. */
    val privilegesVersion: String? = null,
    /** true mientras el empleado no haya subido su licencia. */
    val requiresLicense: Boolean = false,
)
