package org.vicpoo.gasolineproject.data.triprecording

import org.vicpoo.gasolineproject.domain.common.GeoPoint
import org.vicpoo.gasolineproject.domain.location.LocationFix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Reglas de la ruta del viaje: qué lecturas del GPS cuentan y cuánta distancia suman. */
class RouteFilterTest {

    // Tuxtla Gutiérrez; 0.001° de latitud ≈ 111 m
    private val baseLat = 16.7528
    private val baseLng = -93.1152

    private fun fix(
        dLat: Double = 0.0,
        seconds: Long = 0,
        accuracy: Double? = 10.0,
        speed: Double? = null,
        dLng: Double = 0.0,
    ) = LocationFix(
        lat = baseLat + dLat,
        lng = baseLng + dLng,
        accuracyMeters = accuracy,
        speedMetersPerSecond = speed,
        timeMillis = 1_000_000L + seconds * 1000,
    )

    @Test
    fun laPrimeraLecturaCuenta_ySumaCero() {
        assertEquals(0.0, RouteFilter().accept(fix()))
    }

    @Test
    fun lecturaImprecisa_noCuenta() {
        assertNull(RouteFilter().accept(fix(accuracy = 80.0)))
    }

    @Test
    fun sinPrecision_seAcepta() {
        assertEquals(0.0, RouteFilter().accept(fix(accuracy = null)))
    }

    @Test
    fun movimientoNormal_sumaLaDistancia() {
        val filter = RouteFilter()
        filter.accept(fix())

        val added = assertNotNull(filter.accept(fix(dLat = 0.001, seconds = 10)))

        assertTrue(added in 105.0..118.0, "Se esperaban ~111 m y fueron $added")
    }

    @Test
    fun ruidoEstandoDetenido_noCuenta() {
        val filter = RouteFilter()
        filter.accept(fix())

        // ~1 m: menos que MIN_STEP_M
        assertNull(filter.accept(fix(dLat = 0.00001, seconds = 5)))
    }

    @Test
    fun saltoImposible_noCuenta_yNoMueveElPuntoAnterior() {
        val filter = RouteFilter()
        filter.accept(fix())

        // ~11 km en 1 s
        assertNull(filter.accept(fix(dLat = 0.1, seconds = 1)))
        // La siguiente lectura normal se mide desde el último punto válido
        val added = assertNotNull(filter.accept(fix(dLat = 0.001, seconds = 10)))
        assertTrue(added < 200.0)
    }

    @Test
    fun alReiniciar_loMovidoMientrasTantoNoSeSuma() {
        val filter = RouteFilter()
        filter.accept(fix())

        filter.reset()

        assertEquals(0.0, filter.accept(fix(dLat = 0.05, seconds = 600)))
    }

    // ---------------- GPS cada segundo ----------------

    @Test
    fun detenidoSegunElGps_noSuma_aunqueLaLecturaSeMueva() {
        val filter = RouteFilter()
        filter.accept(fix())

        // ~11 m de "temblor" con el carro parado (velocidad 0.2 m/s)
        assertNull(filter.accept(fix(dLat = 0.0001, seconds = 1, speed = 0.2)))
    }

    @Test
    fun alArrancarDespuesDeDetenerse_seMideDesdeElUltimoPuntoValido() {
        val filter = RouteFilter()
        filter.accept(fix())
        filter.accept(fix(dLat = 0.0001, seconds = 30, speed = 0.1)) // parado: no cuenta

        val added = assertNotNull(filter.accept(fix(dLat = 0.0005, seconds = 40, speed = 8.0)))

        assertTrue(added in 52.0..59.0, "Se esperaban ~55 m desde el último punto válido y fueron $added")
    }

    @Test
    fun conSenalRegular_elTemblorDentroDelMargenDeErrorNoSuma() {
        val filter = RouteFilter()
        filter.accept(fix(accuracy = 20.0))

        // ~6 m con ±20 m de error: menos que el paso mínimo (8 m)
        assertNull(filter.accept(fix(dLat = 0.000055, seconds = 1, accuracy = 20.0)))
    }

    @Test
    fun pasoMinimo_creceConElErrorHastaElTope() {
        assertEquals(RouteFilter.MIN_STEP_M, RouteFilter.minStepMeters(null))
        assertEquals(RouteFilter.MIN_STEP_M, RouteFilter.minStepMeters(4.0))
        assertEquals(5.0, RouteFilter.minStepMeters(10.0))
        assertEquals(RouteFilter.MAX_ADAPTIVE_STEP_M, RouteFilter.minStepMeters(25.0))
    }

    @Test
    fun curvaMuestreadaCadaSegundo_mideMasQueCadaCincoSegundos() {
        // Un cuarto de círculo de ~100 m de radio recorrido a ~10 m/s: con lecturas cada segundo la
        // distancia se acerca al arco real (~157 m); cada 5 s se "corta" la curva.
        val radiusDeg = 0.0009 // ~100 m
        fun onArc(step: Int, steps: Int): Pair<Double, Double> {
            val angle = (kotlin.math.PI / 2) * step / steps
            return radiusDeg * kotlin.math.sin(angle) to radiusDeg * (1 - kotlin.math.cos(angle))
        }
        fun measure(everySeconds: Int): Double {
            val filter = RouteFilter()
            var total = 0.0
            val steps = 15 // 0..15: con lecturas cada 1 s y cada 5 s se termina en el mismo punto
            for (i in 0..steps step everySeconds) {
                val (dLat, dLng) = onArc(i, steps)
                total += filter.accept(fix(dLat = dLat, dLng = dLng, seconds = i.toLong(), accuracy = 5.0, speed = 10.0)) ?: 0.0
            }
            return total
        }

        val cadaSegundo = measure(1)
        val cadaCinco = measure(5)

        assertTrue(cadaSegundo > cadaCinco, "1 s: $cadaSegundo m, 5 s: $cadaCinco m")
        assertTrue(cadaSegundo > 140.0, "Con lecturas cada segundo se esperaban ~150 m y fueron $cadaSegundo")
    }

    @Test
    fun alRestaurar_seMideDesdeElUltimoPuntoGuardado() {
        val filter = RouteFilter()
        filter.reset(from = GeoPoint(baseLat, baseLng))

        val added = assertNotNull(filter.accept(fix(dLat = 0.001, seconds = 10)))

        assertTrue(added in 105.0..118.0)
    }
}
