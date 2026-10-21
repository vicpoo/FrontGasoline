package org.vicpoo.gasolineproject.core.network.dto

import kotlinx.serialization.Serializable

/*
 * Contratos de APIGasoline que usan VARIAS features (auth, perfil, historial).
 * Viven en core para que ninguna feature dependa de los DTOs internos de otra.
 */

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
