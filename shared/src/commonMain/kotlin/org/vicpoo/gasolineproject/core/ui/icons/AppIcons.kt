package org.vicpoo.gasolineproject.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Íconos propios de la app (trazo de 2px, estilo lineal como en Figma).
 * Se dibujan en código para no depender de librerías de íconos.
 * El color se aplica con el parámetro tint de Icon().
 */
object AppIcons {

    val ChevronLeft: ImageVector by lazy {
        lineIcon("ChevronLeft") {
            strokePath {
                moveTo(15f, 18f)
                lineTo(9f, 12f)
                lineTo(15f, 6f)
            }
        }
    }

    val FileDocument: ImageVector by lazy {
        lineIcon("FileDocument") {
            strokePath {
                moveTo(7f, 3f)
                lineTo(14f, 3f)
                lineTo(19f, 8f)
                lineTo(19f, 19f)
                quadTo(19f, 21f, 17f, 21f)
                lineTo(7f, 21f)
                quadTo(5f, 21f, 5f, 19f)
                lineTo(5f, 5f)
                quadTo(5f, 3f, 7f, 3f)
                close()
            }
            strokePath {
                moveTo(14f, 3f)
                lineTo(14f, 8f)
                lineTo(19f, 8f)
            }
            strokePath {
                moveTo(9f, 13f)
                lineTo(15f, 13f)
                moveTo(9f, 17f)
                lineTo(13f, 17f)
            }
        }
    }

    val Eye: ImageVector by lazy {
        lineIcon("Eye") {
            eyeOutline()
            strokePath {
                moveTo(15f, 12f)
                arcTo(3f, 3f, 0f, true, true, 9f, 12f)
                arcTo(3f, 3f, 0f, true, true, 15f, 12f)
                close()
            }
        }
    }

    val EyeOff: ImageVector by lazy {
        lineIcon("EyeOff") {
            eyeOutline()
            strokePath {
                moveTo(3f, 3f)
                lineTo(21f, 21f)
            }
        }
    }

    val Search: ImageVector by lazy {
        lineIcon("Search") {
            strokePath {
                moveTo(18f, 11f)
                arcTo(7f, 7f, 0f, true, true, 4f, 11f)
                arcTo(7f, 7f, 0f, true, true, 18f, 11f)
                close()
            }
            strokePath {
                moveTo(16.5f, 16.5f)
                lineTo(21f, 21f)
            }
        }
    }

    val Plus: ImageVector by lazy {
        lineIcon("Plus") {
            strokePath {
                moveTo(12f, 5f)
                lineTo(12f, 19f)
                moveTo(5f, 12f)
                lineTo(19f, 12f)
            }
        }
    }

    val Close: ImageVector by lazy {
        lineIcon("Close") {
            strokePath {
                moveTo(6f, 6f)
                lineTo(18f, 18f)
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
        }
    }

    val Car: ImageVector by lazy {
        lineIcon("Car") {
            strokePath {
                moveTo(3f, 16f)
                lineTo(3f, 12f)
                lineTo(5f, 7f)
                quadTo(5.5f, 6f, 6.5f, 6f)
                lineTo(17.5f, 6f)
                quadTo(18.5f, 6f, 19f, 7f)
                lineTo(21f, 12f)
                lineTo(21f, 16f)
                quadTo(21f, 17f, 20f, 17f)
                lineTo(4f, 17f)
                quadTo(3f, 17f, 3f, 16f)
                close()
            }
            strokePath {
                moveTo(3f, 12f)
                lineTo(21f, 12f)
            }
            strokePath {
                moveTo(9f, 17f)
                arcTo(2f, 2f, 0f, true, true, 5f, 17f)
                moveTo(19f, 17f)
                arcTo(2f, 2f, 0f, true, true, 15f, 17f)
            }
        }
    }

    val User: ImageVector by lazy {
        lineIcon("User") {
            strokePath {
                moveTo(16f, 8f)
                arcTo(4f, 4f, 0f, true, true, 8f, 8f)
                arcTo(4f, 4f, 0f, true, true, 16f, 8f)
                close()
            }
            strokePath {
                moveTo(4f, 21f)
                quadTo(4f, 15f, 12f, 15f)
                quadTo(20f, 15f, 20f, 21f)
            }
        }
    }

    val Camera: ImageVector by lazy {
        lineIcon("Camera") {
            strokePath {
                moveTo(4f, 7f)
                lineTo(7.5f, 7f)
                lineTo(9f, 4.5f)
                lineTo(15f, 4.5f)
                lineTo(16.5f, 7f)
                lineTo(20f, 7f)
                quadTo(21f, 7f, 21f, 8f)
                lineTo(21f, 18f)
                quadTo(21f, 19f, 20f, 19f)
                lineTo(4f, 19f)
                quadTo(3f, 19f, 3f, 18f)
                lineTo(3f, 8f)
                quadTo(3f, 7f, 4f, 7f)
                close()
            }
            strokePath {
                moveTo(15.5f, 13f)
                arcTo(3.5f, 3.5f, 0f, true, true, 8.5f, 13f)
                arcTo(3.5f, 3.5f, 0f, true, true, 15.5f, 13f)
                close()
            }
        }
    }

    val CloudOff: ImageVector by lazy {
        lineIcon("CloudOff") {
            strokePath {
                moveTo(7f, 18f)
                lineTo(17f, 18f)
                quadTo(21f, 18f, 21f, 14f)
                quadTo(21f, 10.5f, 17.5f, 10f)
                quadTo(16.5f, 5f, 12f, 5f)
                quadTo(8f, 5f, 6.8f, 8.5f)
                quadTo(3f, 9f, 3f, 13.5f)
                quadTo(3f, 18f, 7f, 18f)
                close()
            }
            strokePath {
                moveTo(3f, 3f)
                lineTo(21f, 21f)
            }
        }
    }

    val Check: ImageVector by lazy {
        lineIcon("Check") {
            strokePath {
                moveTo(5f, 12.5f)
                lineTo(10f, 17.5f)
                lineTo(19f, 7.5f)
            }
        }
    }

    val Pencil: ImageVector by lazy {
        lineIcon("Pencil") {
            strokePath {
                moveTo(4f, 20f)
                lineTo(8f, 19f)
                lineTo(18.5f, 8.5f)
                lineTo(15.5f, 5.5f)
                lineTo(5f, 16f)
                lineTo(4f, 20f)
                close()
            }
            strokePath {
                moveTo(13.5f, 7.5f)
                lineTo(16.5f, 10.5f)
            }
        }
    }

    /** Galería: marco de foto con montaña y destello (Figma: "Galeria"). */
    val Gallery: ImageVector by lazy {
        lineIcon("Gallery") {
            strokePath {
                moveTo(4f, 6f)
                lineTo(4f, 4f)
                lineTo(6f, 4f)
                moveTo(4f, 18f)
                lineTo(4f, 20f)
                lineTo(6f, 20f)
                moveTo(18f, 20f)
                lineTo(20f, 20f)
                lineTo(20f, 18f)
            }
            strokePath {
                moveTo(14f, 11f)
                arcTo(2f, 2f, 0f, true, true, 10f, 11f)
                arcTo(2f, 2f, 0f, true, true, 14f, 11f)
                close()
            }
            strokePath {
                moveTo(7.5f, 17f)
                quadTo(8f, 14.5f, 12f, 14.5f)
                quadTo(16f, 14.5f, 16.5f, 17f)
            }
            strokePath {
                moveTo(19f, 3f)
                lineTo(19f, 7f)
                moveTo(17f, 5f)
                lineTo(21f, 5f)
            }
        }
    }

    /** Insignia ondulada (Figma: icono de los diálogos de éxito / error). */
    val Badge: ImageVector by lazy {
        lineIcon("Badge") {
            strokePath {
                val points = 12
                val outer = 9.5f
                val inner = 8f
                for (i in 0..points * 2) {
                    val angle = (i * kotlin.math.PI / points).toFloat()
                    val r = if (i % 2 == 0) outer else inner
                    val x = 12f + r * kotlin.math.cos(angle).toFloat()
                    val y = 12f + r * kotlin.math.sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        }
    }

    /** Historial: hoja con reloj (Figma: pestaña de historial de viajes). */
    val History: ImageVector by lazy {
        lineIcon("History") {
            strokePath {
                moveTo(12f, 21f)
                lineTo(6f, 21f)
                quadTo(4f, 21f, 4f, 19f)
                lineTo(4f, 5f)
                quadTo(4f, 3f, 6f, 3f)
                lineTo(16f, 3f)
                quadTo(18f, 3f, 18f, 5f)
                lineTo(18f, 11f)
            }
            strokePath {
                moveTo(8f, 8f)
                lineTo(14f, 8f)
                moveTo(8f, 12f)
                lineTo(12f, 12f)
            }
            strokePath {
                moveTo(22f, 17f)
                arcTo(4f, 4f, 0f, true, true, 14f, 17f)
                arcTo(4f, 4f, 0f, true, true, 22f, 17f)
                close()
            }
            strokePath {
                moveTo(18f, 15f)
                lineTo(18f, 17f)
                lineTo(19.5f, 18f)
            }
        }
    }

    val Bell: ImageVector by lazy {
        lineIcon("Bell") {
            strokePath {
                moveTo(6f, 17f)
                lineTo(6f, 11f)
                quadTo(6f, 5f, 12f, 5f)
                quadTo(18f, 5f, 18f, 11f)
                lineTo(18f, 17f)
                lineTo(20f, 19f)
                lineTo(4f, 19f)
                lineTo(6f, 17f)
                close()
            }
            strokePath {
                moveTo(10f, 21f)
                lineTo(14f, 21f)
                moveTo(12f, 3f)
                lineTo(12f, 5f)
            }
        }
    }
}

/** Crea un ícono de 24x24 con el contenido indicado. */
private fun lineIcon(name: String, content: ImageVector.Builder.() -> Unit): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    builder.content()
    return builder.build()
}

/** Agrega un trazo lineal (sin relleno, 2px, puntas redondeadas). */
private fun ImageVector.Builder.strokePath(pathBuilder: PathBuilder.() -> Unit) {
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    )
}

/** Contorno del ojo (compartido por Eye y EyeOff). */
private fun ImageVector.Builder.eyeOutline() {
    strokePath {
        moveTo(2f, 12f)
        curveTo(4.5f, 7f, 8f, 5f, 12f, 5f)
        curveTo(16f, 5f, 19.5f, 7f, 22f, 12f)
        curveTo(19.5f, 17f, 16f, 19f, 12f, 19f)
        curveTo(8f, 19f, 4.5f, 17f, 2f, 12f)
        close()
    }
}
