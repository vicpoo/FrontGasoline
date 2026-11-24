package org.vicpoo.gasolineproject.domain.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.profile.usecase.UpdateProfilePhotoUseCase

/** Foto de perfil: nunca se sube la original de la galería (EXIF con ubicación), solo la recodificada. */
class UpdateProfilePhotoUseCaseTest {

    private class FakeProfileRepository : ProfileRepository {
        val uploaded = mutableListOf<PickedFile>()
        override fun observeProfile(): Flow<Profile?> = flowOf(null)
        override suspend fun refresh(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateName(name: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updatePhoto(file: PickedFile): AppResult<Unit> {
            uploaded += file
            return AppResult.Success(Unit)
        }
    }

    private val original = PickedFile(name = "perfil.jpg", mimeType = "image/jpeg", bytes = ByteArray(10) { 1 })
    private val recoded = PickedFile(name = "perfil.jpg", mimeType = "image/jpeg", bytes = ByteArray(5) { 2 })

    @Test
    fun seSubeLaFotoRecodificada() = runTest {
        val repository = FakeProfileRepository()

        val result = UpdateProfilePhotoUseCase(repository, ImageCompressor { recoded })(original)

        assertEquals(AppResult.Success(Unit), result)
        assertSame(recoded, repository.uploaded.single())
    }

    @Test
    fun fotoQueNoSePuedeRecodificar_seRechazaYNoSeSubeNada() = runTest {
        val repository = FakeProfileRepository()

        val result = UpdateProfilePhotoUseCase(repository, ImageCompressor { null })(original)

        assertEquals(AppResult.Failure(AppError.Validation(ValidationReason.INVALID_FILE_TYPE)), result)
        assertTrue(repository.uploaded.isEmpty())
    }

    @Test
    fun tipoNoPermitido_seRechazaSinComprimir() = runTest {
        val repository = FakeProfileRepository()
        var compressorCalls = 0
        val gif = PickedFile(name = "perfil.gif", mimeType = "image/gif", bytes = ByteArray(10))

        val result = UpdateProfilePhotoUseCase(repository, ImageCompressor { compressorCalls++; it })(gif)

        assertEquals(AppResult.Failure(AppError.Validation(ValidationReason.INVALID_FILE_TYPE)), result)
        assertEquals(0, compressorCalls)
        assertTrue(repository.uploaded.isEmpty())
    }
}
