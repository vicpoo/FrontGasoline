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
)

/** Respuesta de POST /api/auth/login (LoginResponseDto) */
@Serializable
data class LoginResponseDto(
    val token: String,
    val tipoToken: String? = null,
    val expiraEnUtc: String? = null,
    val perfil: PerfilUsuarioDto,
)

/** GET /api/auth/privilegios (PrivilegiosUsuarioDto) */
@Serializable
data class PrivilegiosUsuarioDto(
    val claves: List<String> = emptyList(),
    val version: String? = null,
)
