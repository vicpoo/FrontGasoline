package org.vicpoo.gasolineproject.feature.triprecording.domain

import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.platform.PickedFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FuelRecordValidatorTest {

    private val validator = FuelRecordValidator()
    private val receipt = PickedFile(name = "recibo.jpg", mimeType = "image/jpeg", bytes = ByteArray(10))
    private val costCenters = listOf(CostCenter(id = "cc-1", name = "Ventas", key = "VEN"))

    private fun draft(
        amount: String = "$1,000.50",
        liters: String = "40 L",
        costCenterId: String? = "cc-1",
        newReceipt: PickedFile? = receipt,
        hasCurrentReceipt: Boolean = false,
    ) = FuelRecordDraft(
        localId = null,
        amountText = amount,
        litersText = liters,
        costCenterId = costCenterId,
        newReceipt = newReceipt,
        hasCurrentReceipt = hasCurrentReceipt,
    )

    @Test
    fun validDraft_returnsParsedValues() {
        val result = assertIs<FuelValidation.Valid>(validator.validate(draft(), costCenters, tankCapacityLiters = 50.0))
        assertEquals(1000.5, result.values.amount)
        assertEquals(40.0, result.values.liters)
        assertEquals("cc-1", result.values.costCenter?.id)
    }

    @Test
    fun litersAboveTankCapacity_isOutOfRange() {
        val result = assertIs<FuelValidation.Invalid>(validator.validate(draft(liters = "60"), costCenters, 50.0))
        assertEquals(ValidationReason.OUT_OF_RANGE, result.errors[FuelField.LITERS])
    }

    @Test
    fun costCenterIsRequiredWhenTheCompanyHasThem() {
        val result = assertIs<FuelValidation.Invalid>(validator.validate(draft(costCenterId = null), costCenters, 50.0))
        assertEquals(ValidationReason.REQUIRED, result.errors[FuelField.COST_CENTER])
    }

    @Test
    fun costCenterIsOptionalWhenTheCompanyHasNone() {
        assertIs<FuelValidation.Valid>(validator.validate(draft(costCenterId = null), emptyList(), 50.0))
    }

    @Test
    fun receiptIsRequiredOnlyWhenThereIsNoCurrentOne() {
        val missing = assertIs<FuelValidation.Invalid>(validator.validate(draft(newReceipt = null), costCenters, 50.0))
        assertEquals(ValidationReason.REQUIRED, missing.errors[FuelField.RECEIPT])

        assertIs<FuelValidation.Valid>(
            validator.validate(draft(newReceipt = null, hasCurrentReceipt = true), costCenters, 50.0),
        )
    }
}
