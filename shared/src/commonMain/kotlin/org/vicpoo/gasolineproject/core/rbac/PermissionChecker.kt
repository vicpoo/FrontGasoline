package org.vicpoo.gasolineproject.core.rbac

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.vicpoo.gasolineproject.core.session.SessionReader

/**
 * Consulta de privilegios del usuario actual.
 * Ojo: el front solo OCULTA/BLOQUEA opciones; la API siempre valida (403).
 */
interface PermissionChecker {
    fun observePrivileges(): Flow<Set<String>>
    fun observe(privilege: String): Flow<Boolean>
    suspend fun has(privilege: String): Boolean

    /** true mientras el usuario cumpla la [rule] (cambia en vivo si le quitan o dan privilegios). */
    fun observe(rule: AccessRule): Flow<Boolean> =
        observePrivileges().map { rule.isSatisfiedBy(it) }.distinctUntilChanged()
}

/** Lee los privilegios de la sesión guardada, así funcionan también sin red. */
class SessionPermissionChecker(
    private val sessionReader: SessionReader,
) : PermissionChecker {

    override fun observePrivileges(): Flow<Set<String>> =
        sessionReader.session.map { it?.privileges.orEmpty() }.distinctUntilChanged()

    override fun observe(privilege: String): Flow<Boolean> =
        observePrivileges().map { privilege in it }.distinctUntilChanged()

    override suspend fun has(privilege: String): Boolean =
        sessionReader.current()?.privileges?.contains(privilege) == true
}
