package org.vicpoo.gasolineproject.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.domain.common.LastRefreshStore
import org.vicpoo.gasolineproject.domain.connectivity.ConnectivityMonitor
import org.vicpoo.gasolineproject.domain.rbac.PermissionChecker
import org.vicpoo.gasolineproject.domain.session.LocalDataOwner
import org.vicpoo.gasolineproject.domain.session.LocalDataOwnerStore
import org.vicpoo.gasolineproject.domain.session.Session
import org.vicpoo.gasolineproject.domain.session.SessionReader
import org.vicpoo.gasolineproject.domain.session.SessionStore

/** Sesión de prueba con los privilegios indicados. */
fun testSession(
    privileges: Set<String> = emptySet(),
    employeeId: String? = "emp-1",
    requiresLicense: Boolean = false,
) = Session(
    userId = "user-1",
    email = "test@polarsoft.mx",
    employeeId = employeeId,
    companyId = "company-1",
    privileges = privileges,
    requiresLicense = requiresLicense,
)

/** SessionReader en memoria: la prueba cambia [state] para simular login, logout o nuevos privilegios. */
class FakeSessionReader(initial: Session? = null) : SessionReader {
    val state = MutableStateFlow(initial)
    override val session: Flow<Session?> = state
    override suspend fun current(): Session? = state.value
}

/** SessionStore en memoria (lectura y escritura): para probar login, logout y fin de sesión. */
class FakeSessionStore(initial: Session? = null) : SessionStore {
    val state = MutableStateFlow(initial)
    override val session: Flow<Session?> = state
    override suspend fun current(): Session? = state.value
    override suspend fun save(session: Session) { state.value = session }
    override suspend fun markLicenseUploaded() { state.value = state.value?.copy(requiresLicense = false) }
    override suspend fun updatePrivileges(privileges: Set<String>, version: String?) {
        state.value = state.value?.copy(privileges = privileges, privilegesVersion = version)
    }
    override suspend fun update(transform: (Session) -> Session) { state.value = state.value?.let(transform) }
    override suspend fun clear() { state.value = null }
}

/** PermissionChecker en memoria. */
class FakePermissionChecker(initial: Set<String> = emptySet()) : PermissionChecker {
    val privileges = MutableStateFlow(initial)
    override fun observePrivileges(): Flow<Set<String>> = privileges
    override fun observe(privilege: String): Flow<Boolean> = privileges.map { privilege in it }.distinctUntilChanged()
    override suspend fun has(privilege: String): Boolean = privilege in privileges.value
}

/** Red falsa: la prueba cambia [online] para simular que se va o vuelve la conexión. */
class FakeConnectivityMonitor(initial: Boolean = true) : ConnectivityMonitor {
    val online = MutableStateFlow(initial)
    override val isOnline: StateFlow<Boolean> = online
}

/** "Actualizado hace X" en memoria. */
class FakeLastRefreshStore(private val now: () -> Long = { 1_000L }) : LastRefreshStore {
    val values = MutableStateFlow<Map<String, Long>>(emptyMap())
    override fun observe(key: String): Flow<Long?> = values.map { it[key] }
    override suspend fun markNow(key: String) { values.value = values.value + (key to now()) }
    override suspend fun clear() { values.value = emptyMap() }
}

/** Dueño de los datos del teléfono en memoria. */
class FakeLocalDataOwnerStore(var owner: LocalDataOwner? = null) : LocalDataOwnerStore {
    override suspend fun current(): LocalDataOwner? = owner

    override suspend fun save(owner: LocalDataOwner) {
        this.owner = owner
    }

    override suspend fun clear() {
        owner = null
    }
}
