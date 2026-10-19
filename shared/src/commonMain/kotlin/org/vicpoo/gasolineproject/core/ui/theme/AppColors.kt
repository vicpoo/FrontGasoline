package org.vicpoo.gasolineproject.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * ============================================================
 *  ÚNICO ARCHIVO DE COLORES DE LA APP
 * ============================================================
 * Si cambias un valor aquí, cambia en TODA la app.
 * Ninguna pantalla ni componente debe escribir Color(0xFF...) a mano:
 * todos leen los colores con AppTheme.colors.<nombre>.
 *
 * 1) AppPalette  -> los colores "crudos" de la guía de Figma.
 * 2) AppColors   -> el significado de cada color (botón principal, texto, etc.).
 * 3) LightAppColors / DarkAppColors -> qué color de la paleta usa cada significado.
 */
object AppPalette {
    // ---- Guía de enmaquetado (tema claro) ----
    val Blue = Color(0xFF476D9E)        // Botones principales y de confirmación
    val Navy = Color(0xFF080D38)        // Títulos, subtítulos y botones de mantenimiento / ver recibo
    val Crimson = Color(0xFFA83F50)     // Cancelar / reintentar / no / eliminar / salir
    val Gray = Color(0xFFE0E0E0)        // Fondo de labels y campos de texto
    val Ink = Color(0xFF1F1F1F)         // Texto general y secundario
    val White = Color(0xFFFFFFFF)       // Fondo
    val Black = Color(0xFF000000)       // Fondo del tema oscuro
}

@Immutable
data class AppColors(
    // Fondos
    val background: Color,
    val surface: Color,
    // Textos
    val title: Color,
    val subtitle: Color,
    val text: Color,
    val textSecondary: Color,
    // Botones (según la guía)
    val primary: Color,          // Guardar, Continuar, Aceptar, Iniciar sesión, Agregar
    val onPrimary: Color,
    val confirm: Color,          // Confirmar, Sí, Guardar cambios
    val onConfirm: Color,
    val danger: Color,           // Cancelar, No, Reintentar, Eliminar, Salir
    val onDanger: Color,
    val secondary: Color,        // Ver recibo, Ver detalles, Mantenimiento, Consultar
    val onSecondary: Color,
    /** Borde del botón de mantenimiento / ver recibo (en oscuro, para que resalte sobre el negro). */
    val secondaryBorder: Color,
    // Labels y campos
    val labelBackground: Color,
    val onLabel: Color,
    val inputBackground: Color,
    val onInput: Color,
    val inputPlaceholder: Color,
    // Estados
    val error: Color,
    val divider: Color,
    val illustration: Color,     // Tinte de ilustraciones (ej. el carro de la bienvenida)
    val isDark: Boolean,
)

val LightAppColors = AppColors(
    background = AppPalette.White,
    surface = AppPalette.White,
    title = AppPalette.Navy,
    subtitle = AppPalette.Navy,
    text = AppPalette.Ink,
    textSecondary = AppPalette.Ink,
    primary = AppPalette.Blue,
    onPrimary = AppPalette.White,
    confirm = AppPalette.Blue,
    onConfirm = AppPalette.White,
    danger = AppPalette.Crimson,
    onDanger = AppPalette.White,
    secondary = AppPalette.Navy,
    onSecondary = AppPalette.White,
    secondaryBorder = Color.Transparent,
    labelBackground = AppPalette.Gray,
    onLabel = AppPalette.Ink,
    inputBackground = AppPalette.Gray,
    onInput = AppPalette.Ink,
    inputPlaceholder = AppPalette.Ink.copy(alpha = 0.5f),
    error = AppPalette.Crimson,
    divider = AppPalette.Gray,
    illustration = AppPalette.Ink,
    isDark = false,
)

/**
 * Tema oscuro: usa SOLO los colores de la guía de enmaquetado.
 * Fondo #000000, textos claros #FFFFFF / #E0E0E0 y los mismos botones y labels.
 */
val DarkAppColors = AppColors(
    background = AppPalette.Black,
    surface = AppPalette.Black,
    title = AppPalette.White,
    subtitle = AppPalette.White,
    text = AppPalette.Gray,
    textSecondary = AppPalette.Gray,
    primary = AppPalette.Blue,
    onPrimary = AppPalette.White,
    confirm = AppPalette.Blue,
    onConfirm = AppPalette.White,
    danger = AppPalette.Crimson,
    onDanger = AppPalette.White,
    secondary = AppPalette.Navy,
    onSecondary = AppPalette.White,
    secondaryBorder = AppPalette.Gray,
    labelBackground = AppPalette.Gray,
    onLabel = AppPalette.Ink,
    inputBackground = AppPalette.Gray,
    onInput = AppPalette.Ink,
    inputPlaceholder = AppPalette.Ink.copy(alpha = 0.5f),
    error = AppPalette.Crimson,
    divider = AppPalette.Navy,
    illustration = AppPalette.Gray,
    isDark = true,
)
