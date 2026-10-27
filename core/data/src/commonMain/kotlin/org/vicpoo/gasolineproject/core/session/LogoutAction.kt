package org.vicpoo.gasolineproject.core.session

/**
 * "Cerrar sesión" visto desde cualquier feature: borra lo guardado (LogoutListener) y la sesión.
 *
 * La regla vive en auth (LogoutUseCase la implementa); aquí solo está el contrato, para que
 * features como Perfil puedan ofrecer "Salir" sin depender de auth.
 */
fun interface LogoutAction {
    suspend operator fun invoke()
}
