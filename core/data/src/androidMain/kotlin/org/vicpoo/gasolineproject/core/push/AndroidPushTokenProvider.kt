package org.vicpoo.gasolineproject.core.push

import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Token de Firebase Cloud Messaging del teléfono. */
class AndroidPushTokenProvider : PushTokenProvider {
    override val platform: String = "android"

    // Sin Google Play Services (o sin red la primera vez) el token puede no llegar: no se espera para siempre
    override suspend fun currentToken(): String? =
        withTimeoutOrNull(TOKEN_TIMEOUT_MS) {
            runCatching { FirebaseMessaging.getInstance().token.awaitOrNull() }.getOrNull()
        }

    override suspend fun deleteToken() {
        withTimeoutOrNull(TOKEN_TIMEOUT_MS) {
            runCatching { FirebaseMessaging.getInstance().deleteToken().awaitOrNull() }
        }
    }

    private companion object {
        const val TOKEN_TIMEOUT_MS = 15_000L
    }
}

/** Espera una Task de Google Play Services sin agregar la librería kotlinx-coroutines-play-services. */
private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        continuation.resume(if (task.isSuccessful) task.result else null)
    }
}
