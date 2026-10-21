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

    val ChevronLeft: ImageVector get() = DesignIcons.ArrowLeft

    val FileDocument: ImageVector get() = DesignIcons.DocumentAttachment

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

    val Search: ImageVector get() = DesignIcons.Search

    val Plus: ImageVector get() = DesignIcons.Plus

    val Close: ImageVector get() = DesignIcons.X

    val Car: ImageVector get() = DesignIcons.CarFront

    val User: ImageVector get() = DesignIcons.User

    val Camera: ImageVector get() = DesignIcons.Camera

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
    val History: ImageVector get() = DesignIcons.TransactionHistory

    val Bell: ImageVector get() = DesignIcons.Notification

    // ---------------- Viajes ----------------

    /** Triángulo relleno (botón "comienzo" / "Comenzar viaje"). */
    val Play: ImageVector get() = DesignIcons.Play

    /** Dos barras rellenas (pausar viaje). */
    val Pause: ImageVector by lazy {
        lineIcon("Pause") {
            fillPath {
                moveTo(7f, 5f)
                lineTo(10f, 5f)
                lineTo(10f, 19f)
                lineTo(7f, 19f)
                close()
                moveTo(14f, 5f)
                lineTo(17f, 5f)
                lineTo(17f, 19f)
                lineTo(14f, 19f)
                close()
            }
        }
    }

    /** Cuadro relleno (finalizar viaje). */
    val Stop: ImageVector by lazy {
        lineIcon("Stop") {
            fillPath {
                moveTo(8f, 6f)
                lineTo(16f, 6f)
                quadTo(18f, 6f, 18f, 8f)
                lineTo(18f, 16f)
                quadTo(18f, 18f, 16f, 18f)
                lineTo(8f, 18f)
                quadTo(6f, 18f, 6f, 16f)
                lineTo(6f, 8f)
                quadTo(6f, 6f, 8f, 6f)
                close()
            }
        }
    }

    val Trash: ImageVector by lazy {
        lineIcon("Trash") {
            strokePath {
                moveTo(4f, 7f)
                lineTo(20f, 7f)
                moveTo(10f, 11f)
                lineTo(10f, 17f)
                moveTo(14f, 11f)
                lineTo(14f, 17f)
            }
            strokePath {
                moveTo(5f, 7f)
                lineTo(6f, 19f)
                quadTo(6f, 21f, 8f, 21f)
                lineTo(16f, 21f)
                quadTo(18f, 21f, 18f, 19f)
                lineTo(19f, 7f)
            }
            strokePath {
                moveTo(9f, 7f)
                lineTo(9f, 4f)
                lineTo(15f, 4f)
                lineTo(15f, 7f)
            }
        }
    }

    /** Mira de GPS (señal activa). */
    val Gps: ImageVector by lazy {
        lineIcon("Gps") {
            strokePath {
                moveTo(19f, 12f)
                arcTo(7f, 7f, 0f, true, true, 5f, 12f)
                arcTo(7f, 7f, 0f, true, true, 19f, 12f)
                close()
            }
            strokePath {
                moveTo(12f, 2f)
                lineTo(12f, 5f)
                moveTo(12f, 19f)
                lineTo(12f, 22f)
                moveTo(2f, 12f)
                lineTo(5f, 12f)
                moveTo(19f, 12f)
                lineTo(22f, 12f)
            }
            fillPath {
                moveTo(14.5f, 12f)
                arcTo(2.5f, 2.5f, 0f, true, true, 9.5f, 12f)
                arcTo(2.5f, 2.5f, 0f, true, true, 14.5f, 12f)
                close()
            }
        }
    }

    val ChevronDown: ImageVector by lazy {
        lineIcon("ChevronDown") {
            strokePath {
                moveTo(6f, 9f)
                lineTo(12f, 15f)
                lineTo(18f, 9f)
            }
        }
    }

    val Upload: ImageVector by lazy {
        lineIcon("Upload") {
            strokePath {
                moveTo(12f, 16f)
                lineTo(12f, 4f)
                moveTo(7f, 9f)
                lineTo(12f, 4f)
                lineTo(17f, 9f)
            }
            strokePath {
                moveTo(4f, 15f)
                lineTo(4f, 18f)
                quadTo(4f, 20f, 6f, 20f)
                lineTo(18f, 20f)
                quadTo(20f, 20f, 20f, 18f)
                lineTo(20f, 15f)
            }
        }
    }

    /** Ruta entre dos puntos (pestaña "Registrar"). */
    val Route: ImageVector by lazy {
        lineIcon("Route") {
            strokePath {
                moveTo(8f, 6f)
                arcTo(2f, 2f, 0f, true, true, 4f, 6f)
                arcTo(2f, 2f, 0f, true, true, 8f, 6f)
                close()
                moveTo(20f, 18f)
                arcTo(2f, 2f, 0f, true, true, 16f, 18f)
                arcTo(2f, 2f, 0f, true, true, 20f, 18f)
                close()
            }
            strokePath {
                moveTo(8f, 6f)
                lineTo(15f, 6f)
                quadTo(18f, 6f, 18f, 9f)
                quadTo(18f, 12f, 15f, 12f)
                lineTo(9f, 12f)
                quadTo(6f, 12f, 6f, 15f)
                quadTo(6f, 18f, 9f, 18f)
                lineTo(16f, 18f)
            }
        }
    }

    /** Bomba de gasolina (recargas). */
    val Fuel: ImageVector get() = DesignIcons.PetrolPump

    // ---------------- Diseño (SVG de Figma, ver DesignIcons) ----------------

    /** Pestaña Inicio: carros para solicitar. */
    val Home: ImageVector get() = DesignIcons.Home

    /** Pestaña Solicitudes de la empresa (llave). */
    val Requests: ImageVector get() = DesignIcons.Access

    /** Pestaña Gestión de carros (libreta con lápiz). */
    val Fleet: ImageVector get() = DesignIcons.NotebookPen

    /** Diálogo de éxito / error (Figma). */
    val BadgeCheck: ImageVector get() = DesignIcons.BadgeCheck
    val BadgeX: ImageVector get() = DesignIcons.BadgeX

    val Pdf: ImageVector get() = DesignIcons.Pdf
    val Xls: ImageVector get() = DesignIcons.Xls
    val Chart: ImageVector get() = DesignIcons.Chart
    val FileDollar: ImageVector get() = DesignIcons.FileDollar
    val FileEdit: ImageVector get() = DesignIcons.FileEdit
    val FileX: ImageVector get() = DesignIcons.FileX
    val BackgroundEraser: ImageVector get() = DesignIcons.BackgroundEraser
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

/** Agrega una figura rellena (el color también sale del tint). */
private fun ImageVector.Builder.fillPath(pathBuilder: PathBuilder.() -> Unit) {
    path(
        fill = SolidColor(Color.Black),
        stroke = null,
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
