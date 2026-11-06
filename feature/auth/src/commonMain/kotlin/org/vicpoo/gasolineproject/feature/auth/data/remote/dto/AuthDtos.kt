package org.vicpoo.gasolineproject.feature.auth.data.remote.dto

import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.network.dto.PerfilUsuarioDto

/**
 * Contrato con APIGasoline (AuthController). ASP.NET serializa en camelCase.
 * Los ids son Guid -> String.
 */

/** POST /api/auth/login (LoginRequest) */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
    /** "android" | "ios": solo para que el usuario reconozca la sesión en su lista de sesiones abiertas. */
    val plataforma: String? = null,
    /** Marca y modelo del teléfono (informativo). */
    val nombreDispositivo: String? = null,
)

/**
 * Respuesta de POST /api/auth/login (LoginResponseDto).
 * token = JWT corto (15 min); refreshToken = sesión larga (30 días) para POST /api/auth/refresh.
 * refreshToken es opcional solo para seguir funcionando con una API anterior que no lo manda.
 */
@Serializable
data class LoginResponseDto(
    val token: String,
    val tipoToken: String? = null,
    val expiraEnUtc: String? = null,
    val refreshToken: String? = null,
    val refreshExpiraEnUtc: String? = null,
    val sesionId: String? = null,
    val perfil: PerfilUsuarioDto,
)

/** GET /api/auth/privilegios (PrivilegiosUsuarioDto) */
@Serializable
data class PrivilegiosUsuarioDto(
    val claves: List<String> = emptyList(),
    val version: String? = null,
)
