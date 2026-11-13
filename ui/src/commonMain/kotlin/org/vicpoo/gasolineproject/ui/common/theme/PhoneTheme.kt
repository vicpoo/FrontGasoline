package org.vicpoo.gasolineproject.ui.common.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * Modo REAL del teléfono (claro u oscuro), aunque la app tenga forzado otro.
 * En Android 12+ la app le pide al sistema su modo (para que el splash lo use) y desde ese momento
 * isSystemInDarkTheme() regresa el modo de la app, no el del teléfono: por eso Android lo manda aparte.
 * null = la plataforma no lo manda (iOS): se usa isSystemInDarkTheme().
 */
val LocalPhoneInDarkTheme = compositionLocalOf<Boolean?> { null }

/** true si el TELÉFONO está en modo oscuro. */
@Composable
fun isPhoneInDarkTheme(): Boolean = LocalPhoneInDarkTheme.current ?: isSystemInDarkTheme()
