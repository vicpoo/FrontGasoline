package org.vicpoo.gasolineproject.feature.auth.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.core.rbac.Privileges
import org.vicpoo.gasolineproject.feature.auth.domain.usecase.ObserveMustUploadLicenseUseCase
import org.vicpoo.gasolineproject.feature.auth.presentation.license.LicenseGateViewModel
import org.vicpoo.gasolineproject.testing.FakeSessionReader
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import org.vicpoo.gasolineproject.testing.testSession
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LicenseGateViewModelTest : MainDispatcherTest() {

    @Test
    fun asksForLicenseOnlyWhileTheDriverHasNotUploadedIt() = runTest {
        val sessions = FakeSessionReader(testSession(privileges = emptySet(), requiresLicense = true))
        val viewModel = LicenseGateViewModel(ObserveMustUploadLicenseUseCase(sessions))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.mustUploadLicense.collect {} }

        // No maneja: no se le pide
        assertEquals(false, viewModel.mustUploadLicense.value)

        // Le dan viaje.registrar: ahora sí se le pide
        sessions.state.value = testSession(privileges = setOf(Privileges.VIAJE_REGISTRAR), requiresLicense = true)
        assertEquals(true, viewModel.mustUploadLicense.value)

        // La sube
        sessions.state.value = testSession(privileges = setOf(Privileges.VIAJE_REGISTRAR), requiresLicense = false)
        assertEquals(false, viewModel.mustUploadLicense.value)
    }
}
