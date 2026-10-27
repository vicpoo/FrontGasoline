package org.vicpoo.gasolineproject.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import org.jetbrains.compose.resources.DrawableResource
import org.vicpoo.gasolineproject.core.ui.resources.Res
import org.vicpoo.gasolineproject.core.ui.resources.car_hero_dark
import org.vicpoo.gasolineproject.core.ui.resources.car_hero_light

/**
 * ============================================================
 *  IMÁGENES QUE CAMBIAN SEGÚN EL TEMA (claro / oscuro)
 * ============================================================
 * Archivos en shared/src/commonMain/composeResources/drawable/:
 *   - car_hero_light.png -> modo claro  (carro NEGRO sobre fondo transparente)
 *   - car_hero_dark.png  -> modo oscuro (carro BLANCO sobre fondo transparente)
 *
 * Para cambiar una imagen basta con reemplazar el archivo con el mismo nombre.
 * Para agregar otra imagen con versión clara/oscura, agrega aquí otra propiedad igual.
 */
object AppImages {

    /** Carro de la pantalla de bienvenida. */
    val carHero: DrawableResource
        @Composable @ReadOnlyComposable
        get() = if (AppTheme.colors.isDark) Res.drawable.car_hero_dark else Res.drawable.car_hero_light
}
