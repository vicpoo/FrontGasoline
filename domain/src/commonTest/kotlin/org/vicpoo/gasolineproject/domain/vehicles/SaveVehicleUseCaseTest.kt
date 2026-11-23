package org.vicpoo.gasolineproject.domain.vehicles

import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.vehicles.model.Vehicle
import org.vicpoo.gasolineproject.domain.vehicles.model.VehicleDraft
import org.vicpoo.gasolineproject.domain.vehicles.repository.VehicleWriter
import org.vicpoo.gasolineproject.domain.vehicles.usecase.SaveVehicleUseCase
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleFormInput
import org.vicpoo.gasolineproject.domain.vehicles.validation.VehicleFormValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Guardar vehículo: la foto nueva se guarda recodificada (sin EXIF), nunca la original de la galería. */
class SaveVehicleUseCaseTest {

    /** Solo registra lo que le llega; el resultado no importa en estas pruebas. */
    private class RecordingWriter : VehicleWriter {
        val drafts = mutableListOf<VehicleDraft>()
        override suspend fun create(draft: VehicleDraft): AppResult<Vehicle> {
            drafts += draft
            return AppResult.Failure(AppError.NotFound)
        }
        override suspend fun update(id: String, draft: VehicleDraft): AppResult<Vehicle> = create(draft)
        override suspend fun delete(id: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    private val original = PickedFile(name = "carro.jpg", mimeType = "image/jpeg", bytes = ByteArray(10) { 1 })
    private val recoded = PickedFile(name = "carro.jpg", mimeType = "image/jpeg", bytes = ByteArray(5) { 2 })

    private fun input(photo: PickedFile?) = VehicleFormInput(
        brand = "Nissan",
        model = "Versa",
        plates = "ABC-123",
        year = "2022",
        color = "Blanco",
        mileage = "15000",
        tank = "41",
        newPhoto = photo,
    )

    @Test
    fun fotoNueva_seGuardaLaRecodificada() = runTest {
        val writer = RecordingWriter()

        SaveVehicleUseCase(writer, VehicleFormValidator(), ImageCompressor { recoded })(null, input(original))

        assertSame(recoded, writer.drafts.single().newPhoto)
    }

    @Test
    fun fotoQueNoSePuedeRecodificar_seRechazaYNoSeGuarda() = runTest {
        val writer = RecordingWriter()

        val result = SaveVehicleUseCase(writer, VehicleFormValidator(), ImageCompressor { null })("carro-1", input(original))

        assertEquals(AppResult.Failure(AppError.Validation(ValidationReason.INVALID_FILE_TYPE)), result)
        assertTrue(writer.drafts.isEmpty())
    }

    @Test
    fun sinFotoNueva_noSeLlamaAlCompresor() = runTest {
        val writer = RecordingWriter()
        var compressorCalls = 0

        SaveVehicleUseCase(writer, VehicleFormValidator(), ImageCompressor { compressorCalls++; it })(null, input(null))

        assertEquals(0, compressorCalls)
        assertNull(writer.drafts.single().newPhoto)
    }
}
