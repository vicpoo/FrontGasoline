package org.vicpoo.gasolineproject.core.session

import kotlinx.coroutines.flow.Flow

/** Solo lectura de la sesión (Interface Segregation: quien solo lee no puede escribir). */
interface SessionReader {
    val session: Flow<Session?>
    suspend fun current(): Session?
}

/** Solo escritura de la sesión. */
interface SessionWriter {
    suspend fun save(session: Session)
    suspend fun markLicenseUploaded()
    suspend fun updatePrivileges(privileges: Set<String>, version: String?)

    /** Cambia cualquier dato de la sesión actual (ej. nombre o foto después de editarlos). */
    suspend fun update(transform: (Session) -> Session)
    suspend fun clear()
}

interface SessionStore : SessionReader, SessionWriter
