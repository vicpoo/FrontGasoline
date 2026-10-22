package org.vicpoo.gasolineproject.core.rbac

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.testing.FakePermissionChecker
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AccessGateViewModelTest : MainDispatcherTest() {

    @Test
    fun allowed_followsPrivilegesLive() = runTest {
        val checker = FakePermissionChecker(setOf(Privileges.CARRO_VER))
        val viewModel = AccessGateViewModel(checker, AppAccess.FLEET_TAB)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.allowed.collect {} }

        assertEquals(false, viewModel.allowed.value)

        // Le asignan carro.crear sin cerrar sesión: la puerta se abre sola
        checker.privileges.value = setOf(Privileges.CARRO_VER, Privileges.CARRO_CREAR)
        assertEquals(true, viewModel.allowed.value)
    }
}
