package org.vicpoo.gasolineproject.feature.auth.data.remote.dto

import kotlinx.serialization.Serializable

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

/** PerfilUsuarioDto: cuenta, empleado (si tiene), roles y claves de privilegios. */
@Serializable
data class PerfilUsuarioDto(
    val usuario: UsuarioDto,
    val empleado: EmpleadoDto? = null,
    val roles: List<String> = emptyList(),
    val privilegios: List<String> = emptyList(),
    val versionPrivilegios: String? = null,
)

@Serializable
data class UsuarioDto(
    val usuarioId: String,
    val email: String,
)

@Serializable
data class EmpleadoDto(
    val empleadoId: String,
    val empresaId: String,
    val nombreCompleto: String? = null,
    val fotoPerfilUrl: String? = null,
    val tieneLicencia: Boolean = false,
)

/** GET /api/auth/privilegios (PrivilegiosUsuarioDto) */
@Serializable
data class PrivilegiosUsuarioDto(
    val claves: List<String> = emptyList(),
    val version: String? = null,
)
