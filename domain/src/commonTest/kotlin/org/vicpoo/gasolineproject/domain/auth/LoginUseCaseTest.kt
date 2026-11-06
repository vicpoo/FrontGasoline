package org.vicpoo.gasolineproject.domain.auth

import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.auth.repository.AuthRepository
import org.vicpoo.gasolineproject.domain.auth.usecase.LoginResult
import org.vicpoo.gasolineproject.domain.auth.usecase.LoginUseCase
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.files.PickedFile
import org.vicpoo.gasolineproject.domain.session.LocalDataOwner
import org.vicpoo.gasolineproject.domain.session.Session
import org.vicpoo.gasolineproject.domain.sync.CountPendingUseCase
import org.vicpoo.gasolineproject.domain.sync.PendingSyncTask
import org.vicpoo.gasolineproject.testing.FakeLocalDataOwnerStore
import org.vicpoo.gasolineproject.testing.testSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Iniciar sesión sin mezclar los datos del teléfono de otra cuenta. */
class LoginUseCaseTest {

    private class FakeAuthRepository(private val result: AppResult<Session>) : AuthRepository {
        val calls = mutableListOf<Pair<String, Boolean>>()

        override suspend fun login(email: String, password: String, clearLocalData: Boolean): AppResult<Session> {
            calls += email to clearLocalData
            return result
        }

        override suspend fun uploadLicense(file: PickedFile): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun refreshPrivileges(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun logout() = Unit
    }

    private class Pending(private val count: Int) : PendingSyncTask {
        override suspend fun syncPending(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun pendingCount(): Int = count
    }

    private val chofer = LocalDataOwner(userId = "user-1", email = "test@polarsoft.mx")
    private val otra = Session(userId = "user-2", email = "otra@polarsoft.mx")

    private fun useCase(repository: AuthRepository, owners: FakeLocalDataOwnerStore, pending: Int) =
        LoginUseCase(repository, owners, CountPendingUseCase(listOf(Pending(pending))))

    @Test
    fun mismaCuenta_conservaLosPendientes() = runTest {
        val repository = FakeAuthRepository(AppResult.Success(testSession()))
        val owners = FakeLocalDataOwnerStore(chofer)

        val result = useCase(repository, owners, pending = 3)(" Test@PolarSoft.mx ", "x")

        assertIs<LoginResult.Success>(result)
        assertEquals(listOf("test@polarsoft.mx" to false), repository.calls)
    }

    @Test
    fun otraCuentaConPendientes_avisaYNoEntra() = runTest {
        val repository = FakeAuthRepository(AppResult.Success(otra))
        val owners = FakeLocalDataOwnerStore(chofer)

        val result = useCase(repository, owners, pending = 3)("otra@polarsoft.mx", "x")

        assertEquals(LoginResult.OtherAccountPendingData("test@polarsoft.mx", 3), result)
        assertTrue(repository.calls.isEmpty())
        assertEquals(chofer, owners.owner)
    }

    @Test
    fun otraCuentaAceptandoBorrar_entraBorrandoYQuedaComoDuena() = runTest {
        val repository = FakeAuthRepository(AppResult.Success(otra))
        val owners = FakeLocalDataOwnerStore(chofer)

        val result = useCase(repository, owners, pending = 3)("otra@polarsoft.mx", "x", discardOtherAccountData = true)

        assertIs<LoginResult.Success>(result)
        assertEquals(listOf("otra@polarsoft.mx" to true), repository.calls)
        assertEquals(LocalDataOwner("user-2", "otra@polarsoft.mx"), owners.owner)
    }

    @Test
    fun otraCuentaSinPendientes_entraBorrandoLasCopiasSinPreguntar() = runTest {
        val repository = FakeAuthRepository(AppResult.Success(otra))

        val result = useCase(repository, FakeLocalDataOwnerStore(chofer), pending = 0)("otra@polarsoft.mx", "x")

        assertIs<LoginResult.Success>(result)
        assertEquals(listOf("otra@polarsoft.mx" to true), repository.calls)
    }

    @Test
    fun contrasenaIncorrecta_noCambiaElDueno() = runTest {
        val repository = FakeAuthRepository(AppResult.Failure(AppError.Unauthorized))
        val owners = FakeLocalDataOwnerStore(chofer)

        val result = useCase(repository, owners, pending = 0)("otra@polarsoft.mx", "mala")

        assertEquals(LoginResult.Failure(AppError.Unauthorized), result)
        assertEquals(chofer, owners.owner)
    }

    @Test
    fun primerInicio_sinDuenoConocido_noBorraNada() = runTest {
        val repository = FakeAuthRepository(AppResult.Success(testSession()))
        val owners = FakeLocalDataOwnerStore()

        useCase(repository, owners, pending = 2)("test@polarsoft.mx", "x")

        assertEquals(listOf("test@polarsoft.mx" to false), repository.calls)
        assertEquals("user-1", owners.owner?.userId)
    }
}
