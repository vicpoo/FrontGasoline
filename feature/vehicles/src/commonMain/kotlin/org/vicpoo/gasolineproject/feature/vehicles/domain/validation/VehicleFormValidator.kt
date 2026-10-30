package org.vicpoo.gasolineproject.feature.vehicles.domain.validation

import org.vicpoo.gasolineproject.core.common.ValidationReason
import org.vicpoo.gasolineproject.core.platform.PickedFile
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleDraft
import org.vicpoo.gasolineproject.feature.vehicles.domain.model.VehicleType

/** Campos del formulario de vehículo. */
enum class VehicleField { BRAND, MODEL, PLATES, YEAR, COLOR, MILEAGE, TANK }

/** Lo que el usuario escribió (texto crudo, aún sin validar). */
data class VehicleFormInput(
    val brand: String = "",
    val model: String = "",
    val plates: String = "",
    val year: String = "",
    val color: String = "",
    val mileage: String = "",
    val tank: String = "",
    /** Se elige con tarjetas (no se escribe), por eso no es un [VehicleField]. */
    val type: VehicleType = VehicleType.CAR,
    val newPhoto: PickedFile? = null,
    val removePhoto: Boolean = false,
) {
    fun valueOf(field: VehicleField): String = when (field) {
        VehicleField.BRAND -> brand
        VehicleField.MODEL -> model
        VehicleField.PLATES -> plates
        VehicleField.YEAR -> year
        VehicleField.COLOR -> color
        VehicleField.MILEAGE -> mileage
        VehicleField.TANK -> tank
    }

    fun with(field: VehicleField, value: String): VehicleFormInput = when (field) {
        VehicleField.BRAND -> copy(brand = value.take(VehicleFormValidator.MAX_BRAND))
        VehicleField.MODEL -> copy(model = value.take(VehicleFormValidator.MAX_MODEL))
        VehicleField.PLATES -> copy(plates = value.uppercase().take(VehicleFormValidator.MAX_PLATES))
        VehicleField.YEAR -> copy(year = value.filter { it.isDigit() }.take(4))
        VehicleField.COLOR -> copy(color = value.take(VehicleFormValidator.MAX_COLOR))
        VehicleField.MILEAGE -> copy(mileage = value.filter { it.isDigit() || it == '.' || it == ',' }.take(11))
        VehicleField.TANK -> copy(tank = value.filter { it.isDigit() || it == '.' || it == ',' }.take(7))
    }
}

/**
 * Reglas del formulario de vehículo (sin UI: se pueden probar con tests).
 * Son las mismas que valida la entidad Carro de APIGasoline, para avisar antes de sincronizar.
 */
class VehicleFormValidator {

    fun validate(
        input: VehicleFormInput,
        fields: Collection<VehicleField> = VehicleField.entries,
    ): Map<VehicleField, ValidationReason> = buildMap {
        fields.forEach { field -> validateField(field, input)?.let { put(field, it) } }
    }

    /** Foto del vehículo: jpg, png o webp, máx. 5 MB (igual que la API). */
    fun validatePhoto(file: PickedFile): ValidationReason? = when {
        file.mimeType.lowercase() !in PHOTO_TYPES -> ValidationReason.INVALID_FILE_TYPE
        file.sizeBytes > MAX_PHOTO_BYTES -> ValidationReason.FILE_TOO_LARGE
        else -> null
    }

    /** Convierte el texto ya validado en un VehicleDraft. Llamar solo si validate() no regresó errores. */
    fun toDraft(input: VehicleFormInput) = VehicleDraft(
        brand = input.brand.trim(),
        model = input.model.trim(),
        plates = input.plates.trim().uppercase(),
        year = input.year.trim().toInt(),
        color = input.color.trim(),
        mileageKm = parseDecimal(input.mileage) ?: 0.0,
        tankCapacityLiters = parseDecimal(input.tank) ?: 0.0,
        type = input.type,
        newPhoto = input.newPhoto,
        removePhoto = input.removePhoto,
    )

    private fun validateField(field: VehicleField, input: VehicleFormInput): ValidationReason? {
        val value = input.valueOf(field).trim()
        return when (field) {
            VehicleField.BRAND, VehicleField.MODEL, VehicleField.COLOR ->
                if (value.isEmpty()) ValidationReason.REQUIRED else null
            VehicleField.PLATES -> when {
                value.isEmpty() -> ValidationReason.REQUIRED
                !PLATES_REGEX.matches(value.uppercase()) -> ValidationReason.INVALID_FORMAT
                else -> null
            }
            VehicleField.YEAR -> {
                val year = value.toIntOrNull()
                when {
                    value.isEmpty() -> ValidationReason.REQUIRED
                    year == null -> ValidationReason.INVALID_FORMAT
                    year !in MIN_YEAR..MAX_YEAR -> ValidationReason.OUT_OF_RANGE
                    else -> null
                }
            }
            VehicleField.MILEAGE -> {
                val km = parseDecimal(value)
                when {
                    value.isEmpty() -> ValidationReason.REQUIRED
                    km == null -> ValidationReason.INVALID_FORMAT
                    km < 0 || km > MAX_MILEAGE -> ValidationReason.OUT_OF_RANGE
                    else -> null
                }
            }
            VehicleField.TANK -> {
                val liters = parseDecimal(value)
                when {
                    value.isEmpty() -> ValidationReason.REQUIRED
                    liters == null -> ValidationReason.INVALID_FORMAT
                    liters <= 0.0 || liters > MAX_TANK_LITERS -> ValidationReason.OUT_OF_RANGE
                    else -> null
                }
            }
        }
    }

    private fun parseDecimal(value: String): Double? = value.trim().replace(',', '.').toDoubleOrNull()

    companion object {
        // Mismos límites que la entidad Carro de la API
        const val MAX_BRAND = 80
        const val MAX_MODEL = 80
        const val MAX_COLOR = 50
        const val MAX_PLATES = 20
        const val MIN_YEAR = 1900
        const val MAX_YEAR = 2100
        const val MAX_TANK_LITERS = 9999.99
        const val MAX_MILEAGE = 99_999_999.99
        const val MAX_PHOTO_BYTES = 5 * 1024 * 1024

        private val PLATES_REGEX = Regex("^[A-Z0-9-]{3,20}$")
        private val PHOTO_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
        val PHOTO_MIME_TYPES = listOf("image/jpeg", "image/png", "image/webp")
    }
}
