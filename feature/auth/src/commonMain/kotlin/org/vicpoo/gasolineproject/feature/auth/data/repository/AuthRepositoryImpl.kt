package org.vicpoo.gasolineproject.feature.auth.data.repository

import org.vicpoo.gasolineproject.core.common.AppError
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.map
import org.vicpoo.gasolineproject.core.common.onSuccess
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.core.session.LogoutListener
import org.vicpoo.gasolineproject.core.session.Session
import org.vicpoo.gasolineproject.core.session.SessionReader
import org.vicpoo.gasolineproject.core.session.SessionWriter
import org.vicpoo.gasolineproject.feature.auth.data.remote.AuthApi
import org.vicpoo.gasolineproject.feature.auth.data.remote.dto.LoginRequestDto
import org.vicpoo.gasolineproject.feature.auth.data.remote.dto.LoginResponseDto
import org.vicpoo.gasolineproject.feature.auth.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val sessionReader: SessionReader,
    private val sessionWriter: SessionWriter,
    /** Features que borran sus datos offline al cerrar sesión. */
    private val logoutListeners: List<LogoutListener> = emptyList(),
) : AuthRepository {

    override suspend fun login(email: String, password: String): AppResult<Session> =
        safeApiCall { api.login(LoginRequestDto(email = email, password = password)) }
            .map { it.toSession() }
            .onSuccess { sessionWriter.save(it) }

    override suspend fun uploadLicense(file: PickedFile): AppResult<Unit> {
        val employeeId = sessionReader.current()?.employeeId
            ?: return AppResult.Failure(AppError.NoCompany)
        return safeApiCall { api.uploadLicense(employeeId, file) }
            .onSuccess { sessionWriter.markLicenseUploaded() }
    }

    override suspend fun refreshPrivileges(): AppResult<Unit> {
        val session = sessionReader.current() ?: return AppResult.Success(Unit)
        return safeApiCall { api.getPrivileges(session.privilegesVersion) }.map { dto ->
            // null = 304: no cambiaron, se conserva la copia local
            if (dto != null) sessionWriter.updatePrivileges(dto.claves.toSet(), dto.version)
        }
    }

    override suspend fun logout() {
        logoutListeners.forEach { runCatching { it.onLogout() } }
        sessionWriter.clear()
    }
}

private fun LoginResponseDto.toSession(): Session {
    val employee = perfil.empleado
    return Session(
        token = token,
        expiresAtUtc = expiraEnUtc,
        userId = perfil.usuario.usuarioId,
        email = perfil.usuario.email,
        name = employee?.nombreCompleto,
        photoUrl = employee?.fotoPerfilUrl,
        employeeId = employee?.empleadoId,
        companyId = employee?.empresaId,
        roles = perfil.roles,
        privileges = perfil.privilegios.toSet(),
        privilegesVersion = perfil.versionPrivilegios,
        // HU-LOG-01: el empleado debe subir su licencia antes de usar la app
        requiresLicense = employee != null && !employee.tieneLicencia,
    )
}
