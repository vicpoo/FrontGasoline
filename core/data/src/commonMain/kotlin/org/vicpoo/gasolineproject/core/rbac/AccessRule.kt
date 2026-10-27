package org.vicpoo.gasolineproject.core.rbac

/**
 * Regla de acceso por privilegios (nunca por nombre de rol).
 * Se cumple si el usuario tiene AL MENOS UNO de [anyOf] (vacío = no se exige) y ADEMÁS TODOS los de [allOf].
 * Es un valor puro: se puede probar sin UI ni Koin.
 */
data class AccessRule(
    val anyOf: Set<String> = emptySet(),
    val allOf: Set<String> = emptySet(),
) {
    fun isSatisfiedBy(granted: Set<String>): Boolean =
        (anyOf.isEmpty() || anyOf.any { it in granted }) && allOf.all { it in granted }

    /** Identificador estable (para la llave del ViewModel que la observa). */
    val key: String
        get() = "any=${anyOf.sorted().joinToString(",")};all=${allOf.sorted().joinToString(",")}"

    companion object {
        /** Sin requisitos (ej. Perfil). */
        val Everyone = AccessRule()

        fun anyOf(vararg privileges: String) = AccessRule(anyOf = privileges.toSet())
        fun allOf(vararg privileges: String) = AccessRule(allOf = privileges.toSet())
    }
}
