package org.vicpoo.gasolineproject.feature.triprecording.data

import org.vicpoo.gasolineproject.core.common.GeoPoint
import org.vicpoo.gasolineproject.core.location.LocationFix
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

    private fun fix(dLat: Double = 0.0, seconds: Long = 0, accuracy: Double? = 10.0) = LocationFix(
        lat = baseLat + dLat,
        lng = baseLng,
        accuracyMeters = accuracy,
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

    @Test
    fun alRestaurar_seMideDesdeElUltimoPuntoGuardado() {
        val filter = RouteFilter()
        filter.reset(from = GeoPoint(baseLat, baseLng))

        val added = assertNotNull(filter.accept(fix(dLat = 0.001, seconds = 10)))

        assertTrue(added in 105.0..118.0)
    }
}
