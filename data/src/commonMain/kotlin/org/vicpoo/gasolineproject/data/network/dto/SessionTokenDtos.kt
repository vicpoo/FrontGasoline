package org.vicpoo.gasolineproject.data.network.dto

import kotlinx.serialization.Serializable

/** Cuerpo de POST /api/auth/refresh y POST /api/auth/logout (RefreshTokenRequest). */
@Serializable
data class RefreshTokenRequestDto(
    val refreshToken: String,
)

/** Respuesta de POST /api/auth/refresh (SesionTokensDto): JWT nuevo + refresh token nuevo (rotación). */
@Serializable
data class SessionTokensDto(
    val token: String,
    val tipoToken: String? = null,
    val expiraEnUtc: String? = null,
    val refreshToken: String,
    val refreshExpiraEnUtc: String? = null,
    val sesionId: String? = null,
    val versionPrivilegios: String? = null,
)
