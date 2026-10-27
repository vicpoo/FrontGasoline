package org.vicpoo.gasolineproject.feature.triprecording.domain

import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.platform.PickedFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FinishTripValidatorTest {

    private val validator = FinishTripValidator()
    private val photo = PickedFile(name = "odometro.jpg", mimeType = "image/jpeg", bytes = ByteArray(10))

    @Test
    fun acceptsFormattedMileage() {
        val result = validator.validate("1,250.5 km", initialMileage = 1000.0, photo = photo)
        assertEquals(1250.5, assertIs<FinishTripValidation.Valid>(result).finalMileage)
    }

    @Test
    fun mileageLowerThanInitial_isOutOfRange() {
        val result = assertIs<FinishTripValidation.Invalid>(validator.validate("900", 1000.0, photo))
        assertEquals(ValidationReason.OUT_OF_RANGE, result.errors[FinishTripField.FINAL_MILEAGE])
    }

    @Test
    fun requiresMileageAndPhoto() {
        val result = assertIs<FinishTripValidation.Invalid>(validator.validate("", 1000.0, null))
        assertEquals(ValidationReason.REQUIRED, result.errors[FinishTripField.FINAL_MILEAGE])
        assertEquals(ValidationReason.REQUIRED, result.errors[FinishTripField.ODOMETER_PHOTO])
    }

    @Test
    fun rejectsPdfAsOdometerPhoto() {
        val pdf = PickedFile(name = "a.pdf", mimeType = "application/pdf", bytes = ByteArray(10))
        val result = assertIs<FinishTripValidation.Invalid>(validator.validate("1200", 1000.0, pdf))
        assertEquals(ValidationReason.INVALID_FILE_TYPE, result.errors[FinishTripField.ODOMETER_PHOTO])
    }
}
