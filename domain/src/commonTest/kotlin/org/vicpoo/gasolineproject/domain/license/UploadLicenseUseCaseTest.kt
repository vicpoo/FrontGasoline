package org.vicpoo.gasolineproject.domain.license

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.auth.repository.AuthRepository
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.files.ImageCompressor
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.license.usecase.UploadLicenseUseCase
import org.vicpoo.gasolineproject.domain.session.Session

/** Subir la licencia: nunca se manda la foto original (EXIF con ubicación), solo la recodificada. */
class UploadLicenseUseCaseTest {

    private class FakeAuthRepository : AuthRepository {
        val uploaded = mutableListOf<PickedFile>()

        override suspend fun login(email: String, password: String, clearLocalData: Boolean): AppResult<Session> =
            error("No se usa en esta prueba")

        override suspend fun uploadLicense(file: PickedFile): AppResult<Unit> {
            uploaded += file
            return AppResult.Success(Unit)
        }

        override suspend fun refreshPrivileges(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun logout() = Unit
    }

    private val photo = PickedFile(name = "licencia.jpg", mimeType = "image/jpeg", bytes = ByteArray(10) { 1 })
    private val recoded = PickedFile(name = "licencia.jpg", mimeType = "image/jpeg", bytes = ByteArray(5) { 2 })

    @Test
    fun fotoRecodificada_seSubeLaRecodificadaYNoLaOriginal() = runTest {
        val repository = FakeAuthRepository()
        val useCase = UploadLicenseUseCase(repository, ImageCompressor { recoded })

        val result = useCase(photo)

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(1, repository.uploaded.size)
        assertSame(recoded, repository.uploaded.single())
    }

    @Test
    fun fotoQueNoSePuedeRecodificar_seRechazaYNoSeSubeNada() = runTest {
        val repository = FakeAuthRepository()
        val useCase = UploadLicenseUseCase(repository, ImageCompressor { null })

        val result = useCase(photo)

        assertEquals(AppResult.Failure(AppError.Validation(ValidationReason.INVALID_FILE_TYPE)), result)
        assertTrue(repository.uploaded.isEmpty())
    }

    @Test
    fun pdf_noPasaPorElCompresorYSeSubeTalCual() = runTest {
        val repository = FakeAuthRepository()
        var compressorCalls = 0
        val useCase = UploadLicenseUseCase(repository, ImageCompressor { compressorCalls++; null })
        val pdf = PickedFile(name = "licencia.pdf", mimeType = "application/pdf", bytes = ByteArray(10))

        useCase(pdf)

        assertEquals(0, compressorCalls)
        assertSame(pdf, repository.uploaded.single())
    }
}
