package org.vicpoo.gasolineproject.domain.session

/**
 * De quién son los datos guardados en el teléfono (viajes, recargas, vehículos y solicitudes sin
 * enviar, y las copias offline de las listas).
 *
 * Si la sesión termina sola (token vencido o revocado) los datos NO se borran: el mismo usuario
 * vuelve a entrar y se envían. Pero si entra OTRA cuenta no se deben mostrar ni enviar con su sesión:
 * se borran antes de entrar (avisando si había algo sin enviar).
 */
data class LocalDataOwner(val userId: String, val email: String) {
    fun isSameAccount(otherEmail: String): Boolean = email.trim().equals(otherEmail.trim(), ignoreCase = true)
}

/** Dueño de los datos del teléfono (lo implementa data con DataStore). */
interface LocalDataOwnerStore {
    /** null = no se sabe (no hay datos, se cerró sesión, o instalaciones anteriores a este cambio). */
    suspend fun current(): LocalDataOwner?

    suspend fun save(owner: LocalDataOwner)

    suspend fun clear()
}
