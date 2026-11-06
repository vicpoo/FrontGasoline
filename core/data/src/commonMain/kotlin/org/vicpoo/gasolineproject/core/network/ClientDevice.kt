package org.vicpoo.gasolineproject.core.network

/**
 * Datos informativos del teléfono que se mandan al iniciar sesión, para que el usuario reconozca
 * sus sesiones abiertas ("android · Samsung SM-A245M"). Nunca se usan para autorizar.
 */
expect object ClientDevice {
    /** "android" | "ios" (como lo espera la API). */
    val platform: String

    /** Marca y modelo, si se pueden leer. */
    val name: String?
}
