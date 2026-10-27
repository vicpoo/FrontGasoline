package org.vicpoo.gasolineproject.core.push

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import org.vicpoo.gasolineproject.core.common.AppResult
import org.vicpoo.gasolineproject.core.common.map
import org.vicpoo.gasolineproject.core.common.AppScope
import org.vicpoo.gasolineproject.core.network.ApiRoutes
import org.vicpoo.gasolineproject.core.network.safeApiCall
import org.vicpoo.gasolineproject.core.session.SessionReader
import org.vicpoo.gasolineproject.core.sync.PendingSyncTask
import org.vicpoo.gasolineproject.core.sync.stopsSync

/** RegistrarDispositivoRequest de APIGasoline (POST /api/dispositivos). */
@Serializable
private data class RegisterDeviceRequestDto(
    val empleadoId: String,
    val tokenFcm: String,
    /** "android" | "ios" | "web" */
    val plataforma: String,
)

/** DispositivoDto: solo se usa el id (para darlo de baja al cerrar sesión). */
@Serializable
private data class DeviceDto(val dispositivoEmpleadoId: String)

/**
 * Registra el celular en la API para que le lleguen las notificaciones push del empleado:
 * - Al iniciar sesión y cada vez que se abre la app con sesión (la API no duplica: reasigna el token).
 * - Cuando Firebase renueva el token ([register] con el token nuevo).
 * - Al cerrar sesión se da de baja ([unregister]) para que no le lleguen avisos del usuario anterior.
 * Sin red, [DevicePushSyncTask] lo reintenta en la sincronización en segundo plano.
 */
class DeviceRegistrar(
    private val client: HttpClient,
    private val tokenProvider: PushTokenProvider,
    private val sessionReader: SessionReader,
    private val dataStore: DataStore<Preferences>,
    private val appScope: AppScope,
) {
    private val mutex = Mutex()
    private val deviceIdKey = stringPreferencesKey("push_device_id")

    init {
        appScope.scope.launch {
            sessionReader.session
                .map { it?.employeeId }
                .distinctUntilChanged()
                .collect { employeeId -> if (employeeId != null) register() }
        }
    }

    suspend fun isRegistered(): Boolean = dataStore.data.first()[deviceIdKey] != null

    /** Manda el token a la API. Sin sesión o sin token no hace nada (regresa Success). */
    suspend fun register(newToken: String? = null): AppResult<Unit> = mutex.withLock {
        val employeeId = sessionReader.current()?.employeeId ?: return AppResult.Success(Unit)
        val token = newToken ?: tokenProvider.currentToken() ?: return AppResult.Success(Unit)
        safeApiCall {
            client.post(ApiRoutes.DEVICES) {
                contentType(ContentType.Application.Json)
                setBody(RegisterDeviceRequestDto(employeeId, token, tokenProvider.platform))
            }.body<DeviceDto>()
        }.map { device ->
            dataStore.edit { it[deviceIdKey] = device.dispositivoEmpleadoId }
            Unit
        }
    }

    /**
     * Al cerrar sesión (corre ANTES de borrar la sesión, todavía hay token de la API).
     * Se desactiva el celular en la API; el token de Firebase NO se borra: si se borrara y el
     * usuario volviera a entrar enseguida, se registraría el token viejo justo antes de invalidarse
     * y dejarían de llegar los push. Si no hay red se rinde rápido: el siguiente usuario que entre
     * en este celular se queda con el token (la API lo reasigna).
     */
    suspend fun unregister() = mutex.withLock {
        val deviceId = dataStore.data.first()[deviceIdKey]
        if (deviceId != null) {
            withTimeoutOrNull(UNREGISTER_TIMEOUT_MS) { safeApiCall { client.delete(ApiRoutes.device(deviceId)) } }
        }
        dataStore.edit { it.remove(deviceIdKey) }
    }

    private companion object {
        const val UNREGISTER_TIMEOUT_MS = 3_000L
    }
}

/** Reintenta el registro del celular en la sincronización en segundo plano (si quedó pendiente). */
class DevicePushSyncTask(private val registrar: DeviceRegistrar) : PendingSyncTask {
    override suspend fun syncPending(): AppResult<Unit> {
        if (registrar.isRegistered()) return AppResult.Success(Unit)
        val result = registrar.register()
        // Solo "sin red / sesión vencida" detiene la sincronización; otro error no bloquea lo demás
        return if (result is AppResult.Failure && result.error.stopsSync()) result else AppResult.Success(Unit)
    }
}
