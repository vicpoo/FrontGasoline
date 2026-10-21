package org.vicpoo.gasolineproject.feature.profile.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.map
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.core.session.Session
import org.vicpoo.gasolineproject.core.session.SessionReader
import org.vicpoo.gasolineproject.core.session.SessionWriter
import org.vicpoo.gasolineproject.core.session.drives
import org.vicpoo.gasolineproject.feature.profile.data.remote.ProfileApi
import org.vicpoo.gasolineproject.feature.profile.data.remote.UpdateEmpleadoRequestDto
import org.vicpoo.gasolineproject.feature.profile.domain.Profile
import org.vicpoo.gasolineproject.feature.profile.domain.ProfileRepository

/**
 * El perfil se muestra desde la sesión guardada (offline-first).
 * Editar nombre o foto sí requiere conexión: si falla, la UI muestra "Reintentar".
 */
class ProfileRepositoryImpl(
    private val api: ProfileApi,
    private val sessionReader: SessionReader,
    private val sessionWriter: SessionWriter,
) : ProfileRepository {

    override fun observeProfile(): Flow<Profile?> = sessionReader.session.map { it?.toProfile() }

    override suspend fun refresh(): AppResult<Unit> {
        if (sessionReader.current() == null) return AppResult.Success(Unit)
        return safeApiCall { api.getProfile() }.map { perfil ->
            val employee = perfil.empleado
            sessionWriter.update { session ->
                session.copy(
                    name = employee?.nombreCompleto ?: session.name,
                    photoUrl = employee?.fotoPerfilUrl,
                    roles = perfil.roles,
                    requiresLicense = employee != null && !employee.tieneLicencia,
                )
            }
        }
    }

    override suspend fun updateName(name: String): AppResult<Unit> {
        val session = sessionReader.current() ?: return AppResult.Failure(AppError.Unauthorized)
        val employeeId = session.employeeId ?: return AppResult.Failure(AppError.NoCompany)
        val companyId = session.companyId ?: return AppResult.Failure(AppError.NoCompany)

        val request = UpdateEmpleadoRequestDto(
            empresaId = companyId,
            usuarioId = session.userId,
            nombreCompleto = name,
        )
        return safeApiCall { api.updateEmployee(employeeId, request) }.map { dto ->
            sessionWriter.update { it.copy(name = dto.nombreCompleto ?: name, photoUrl = dto.fotoPerfilUrl ?: it.photoUrl) }
        }
    }

    override suspend fun updatePhoto(file: PickedFile): AppResult<Unit> {
        val employeeId = sessionReader.current()?.employeeId ?: return AppResult.Failure(AppError.NoCompany)
        return safeApiCall { api.uploadProfilePhoto(employeeId, file) }.map { dto ->
            sessionWriter.update { it.copy(photoUrl = dto.fotoPerfilUrl) }
        }
    }
}

private fun Session.toProfile() = Profile(
    name = name ?: email,
    email = email,
    photoUrl = photoUrl,
    roles = roles,
    hasEmployee = employeeId != null,
    drives = drives,
    hasLicense = employeeId != null && !requiresLicense,
)
