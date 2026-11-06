package org.vicpoo.gasolineproject.domain.session

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Almacenamiento SEGURO de los tokens (cada plataforma pone el suyo en el platformModule):
 * - Android: [AuthTokens] cifrados con AES-256-GCM con una llave del Android Keystore (no se puede extraer).
 * - iOS: Keychain, solo en este dispositivo (no viaja en respaldos de iCloud).
 * Si no se pueden leer (llave perdida tras restaurar un respaldo, datos dañados) regresa null y la
 * app pide iniciar sesión.
 */
interface TokenStore {
    suspend fun read(): AuthTokens?
    suspend fun write(tokens: AuthTokens)
    suspend fun clear()
}

/**
 * Guarda en memoria lo último leído o escrito: el cliente HTTP pide el token en CADA petición
 * y así no se descifra (Keystore / Keychain) cada vez.
 */
class CachedTokenStore(private val delegate: TokenStore) : TokenStore {
    private val mutex = Mutex()
    private var loaded = false
    private var cached: AuthTokens? = null

    override suspend fun read(): AuthTokens? = mutex.withLock {
        if (!loaded) {
            cached = delegate.read()
            loaded = true
        }
        cached
    }

    override suspend fun write(tokens: AuthTokens) = mutex.withLock {
        delegate.write(tokens)
        cached = tokens
        loaded = true
    }

    override suspend fun clear() = mutex.withLock {
        delegate.clear()
        cached = null
        loaded = true
    }
}

/** En memoria (pruebas). */
class InMemoryTokenStore(initial: AuthTokens? = null) : TokenStore {
    var tokens: AuthTokens? = initial
        private set

    override suspend fun read(): AuthTokens? = tokens
    override suspend fun write(tokens: AuthTokens) { this.tokens = tokens }
    override suspend fun clear() { tokens = null }
}
