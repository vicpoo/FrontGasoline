package org.vicpoo.gasolineproject.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.core.data.LastRefreshStore
import org.vicpoo.gasolineproject.core.network.ConnectivityMonitor
import org.vicpoo.gasolineproject.core.rbac.PermissionChecker
import org.vicpoo.gasolineproject.core.session.Session
import org.vicpoo.gasolineproject.core.session.SessionReader

/** Sesión de prueba con los privilegios indicados. */
fun testSession(
    privileges: Set<String> = emptySet(),
    employeeId: String? = "emp-1",
    requiresLicense: Boolean = false,
) = Session(
    token = "token",
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
