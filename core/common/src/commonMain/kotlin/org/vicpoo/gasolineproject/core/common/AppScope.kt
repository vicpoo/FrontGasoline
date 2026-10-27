package org.vicpoo.gasolineproject.core.common

import kotlinx.coroutines.CoroutineScope

/** Scope de toda la app: para sincronizar en segundo plano aunque la pantalla se cierre. */
class AppScope(val scope: CoroutineScope)
