package org.vicpoo.gasolineproject.data.session

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import org.vicpoo.gasolineproject.domain.session.AuthTokens
import org.vicpoo.gasolineproject.domain.session.TokenStore

/**
 * iOS: los tokens se guardan en el Keychain como "generic password".
 * kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly: se pueden leer en segundo plano después del primer
 * desbloqueo (sincronización), y NO viajan en respaldos de iCloud ni a otro iPhone.
 */
@OptIn(ExperimentalForeignApi::class)
class KeychainTokenStore(private val json: Json) : TokenStore {

    override suspend fun read(): AuthTokens? = withContext(Dispatchers.Default) {
        val text = readItem() ?: return@withContext null
        val tokens = runCatching { json.decodeFromString(AuthTokens.serializer(), text) }.getOrNull()
        if (tokens == null) deleteItem() // dato dañado: no sirve
        tokens
    }

    override suspend fun write(tokens: AuthTokens) = withContext(Dispatchers.Default) {
        val text = json.encodeToString(AuthTokens.serializer(), tokens)
        deleteItem()
        addItem(text.encodeToByteArray())
    }

    override suspend fun clear() = withContext(Dispatchers.Default) {
        deleteItem()
    }

    private fun readItem(): String? = withQuery { query ->
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        memScoped {
            val result = alloc<CFTypeRefVar>()
            if (SecItemCopyMatching(query, result.ptr) != errSecSuccess) return@memScoped null

            val data: CFDataRef = result.value?.reinterpret() ?: return@memScoped null
            try {
                val length = CFDataGetLength(data).toInt()
                val bytes = CFDataGetBytePtr(data)
                if (length <= 0 || bytes == null) null else bytes.readBytes(length).decodeToString()
            } finally {
                CFRelease(data)
            }
        }
    }

    private fun addItem(bytes: ByteArray) {
        val data = bytes.usePinned { pinned ->
            CFDataCreate(null, pinned.addressOf(0).reinterpret<UByteVar>(), bytes.size.convert())
        }
        try {
            withQuery { query ->
                CFDictionaryAddValue(query, kSecValueData, data)
                CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
                SecItemAdd(query, null)
            }
        } finally {
            if (data != null) CFRelease(data)
        }
    }

    private fun deleteItem() {
        withQuery { query -> SecItemDelete(query) }
    }

    /**
     * Consulta base (clase, servicio y cuenta) armada con CoreFoundation. El diccionario se crea SIN
     * callbacks de retención, así que los valores deben vivir hasta que termine [block]: los que se
     * crean aquí se liberan al final.
     */
    private fun <T> withQuery(block: (CFMutableDictionaryRef?) -> T): T {
        val service = cfString(SERVICE)
        val account = cfString(ACCOUNT)
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        try {
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, service)
            CFDictionaryAddValue(query, kSecAttrAccount, account)
            return block(query)
        } finally {
            if (query != null) CFRelease(query)
            if (service != null) CFRelease(service)
            if (account != null) CFRelease(account)
        }
    }

    private fun cfString(value: String): CFStringRef? =
        CFStringCreateWithCString(null, value, kCFStringEncodingUTF8)

    private companion object {
        const val SERVICE = "org.vicpoo.gasolineproject.auth"
        const val ACCOUNT = "tokens"
    }
}
