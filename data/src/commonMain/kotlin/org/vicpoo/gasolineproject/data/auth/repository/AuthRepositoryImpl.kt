package org.vicpoo.gasolineproject.data.auth.repository

import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.map
import org.vicpoo.gasolineproject.domain.common.onSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.vicpoo.gasolineproject.data.network.AuthTokenManager
import org.vicpoo.gasolineproject.data.network.ClientDevice
import org.vicpoo.gasolineproject.data.network.SessionApi
import org.vicpoo.gasolineproject.data.network.safeApiCall
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.session.AuthTokens
import org.vicpoo.gasolineproject.domain.session.LogoutListener
import org.vicpoo.gasolineproject.domain.session.Session
import org.vicpoo.gasolineproject.domain.session.SessionReader
import org.vicpoo.gasolineproject.domain.session.SessionWriter
import org.vicpoo.gasolineproject.data.auth.remote.AuthApi
import org.vicpoo.gasolineproject.data.auth.remote.dto.LoginRequestDto
import org.vicpoo.gasolineproject.data.auth.remote.dto.LoginResponseDto
import org.vicpoo.gasolineproject.domain.auth.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val sessionReader: SessionReader,
    private val sessionWriter: SessionWriter,
    /** Tokens cifrados (Keystore / Keychain) y su renovación. */
    private val tokenManager: AuthTokenManager,
    /** Logout en la API (revoca el refresh token). */
    private val sessionApi: SessionApi,
    /** Features que borran sus datos offline al cerrar sesión. */
    private val logoutListeners: List<LogoutListener> = emptyList(),
    private val device: DeviceInfo = DeviceInfo(ClientDevice.platform, ClientDevice.name),
) : AuthRepository {

    /** Plataforma y modelo que se mandan al iniciar sesión (informativo, para la lista de sesiones). */
    data class DeviceInfo(val platform: String?, val name: String?)

    override suspend fun login(email: String, password: String, clearLocalData: Boolean): AppResult<Session> =
        safeApiCall {
            api.login(
                LoginRequestDto(
                    email = email,
                    password = password,
                    plataforma = device.platform,
                    nombreDispositivo = device.name?.take(MAX_DEVICE_NAME),
                ),
            )
        }.map { response ->
            // Entra otra cuenta: se borra lo que dejó la anterior ANTES de guardar la sesión nueva
            // (así nada de la cuenta anterior se muestra ni se envía con esta sesión)
            if (clearLocalData) clearLocalData()
            // Primero los tokens (cifrados) y después el perfil: al guardar la sesión la app entra
            // y sus primeras peticiones ya necesitan el token
            tokenManager.saveNewSession(response.toTokens())
            response.toSession()
        }.onSuccess { sessionWriter.save(it) }

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

    /**
     * Cierra sesión:
     * 1. Cada feature borra sus datos (algunas, como push, todavía llaman a la API con el token).
     * 2. Revoca el refresh token en la API para que ya no sirva aunque alguien lo haya copiado.
     *    Sin red no se espera: se cierra igual en el teléfono (el token expira solo en el servidor).
     * 3. Borra los tokens cifrados y la sesión local.
     */
    override suspend fun logout() {
        clearLocalData()
        revokeRemoteSession()
        tokenManager.clear()
        sessionWriter.clear()
    }

    /** Cada feature borra sus datos del teléfono (algunas, como push, todavía llaman a la API). */
    private suspend fun clearLocalData() {
        logoutListeners.forEach { listener ->
            try {
                listener.onLogout()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("[Sesión] No se pudieron borrar unos datos del teléfono: ${e.message}")
            }
        }
    }

    private suspend fun revokeRemoteSession() {
        val refreshToken = tokenManager.currentTokens()?.refreshToken ?: return
        try {
            withTimeoutOrNull(LOGOUT_TIMEOUT_MS) { sessionApi.logout(refreshToken) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("[API] No se pudo cerrar la sesión en la API (se cierra solo en el teléfono): ${e.message}")
        }
    }

    private companion object {
        const val LOGOUT_TIMEOUT_MS = 5_000L
        const val MAX_DEVICE_NAME = 100
    }
}

private fun LoginResponseDto.toTokens() = AuthTokens(
    accessToken = token,
    accessExpiresAtUtc = expiraEnUtc,
    refreshToken = refreshToken,
    refreshExpiresAtUtc = refreshExpiraEnUtc,
    sessionId = sesionId,
)

private fun LoginResponseDto.toSession(): Session {
    val employee = perfil.empleado
    return Session(
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
