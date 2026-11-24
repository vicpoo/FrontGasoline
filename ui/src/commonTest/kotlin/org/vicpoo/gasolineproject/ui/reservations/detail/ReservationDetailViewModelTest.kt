package org.vicpoo.gasolineproject.ui.reservations.detail

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.domain.common.AppError
import org.vicpoo.gasolineproject.domain.common.AppResult
import org.vicpoo.gasolineproject.domain.common.ValidationReason
import org.vicpoo.gasolineproject.domain.rbac.Privileges
import org.vicpoo.gasolineproject.domain.reservations.ReservationStatus
import org.vicpoo.gasolineproject.domain.reservations.usecase.ApproveReservationUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.GetReservationDetailUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.ObserveCompanyReservationUseCase
import org.vicpoo.gasolineproject.domain.reservations.usecase.RejectReservationUseCase
import org.vicpoo.gasolineproject.testing.FakePermissionChecker
import org.vicpoo.gasolineproject.testing.FakeReservationRepository
import org.vicpoo.gasolineproject.testing.MainDispatcherTest
import org.vicpoo.gasolineproject.testing.testReservation

/** Detalle de una solicitud: abre sin red con lo guardado, aceptar y rechazar con su privilegio. */
class ReservationDetailViewModelTest : MainDispatcherTest() {

    private val repository = FakeReservationRepository(initialCompany = listOf(testReservation("req-1")))
    private val permissions = FakePermissionChecker(setOf(Privileges.SOLICITUD_APROBAR, Privileges.SOLICITUD_RECHAZAR))

    private fun viewModel(id: String = "req-1") = ReservationDetailViewModel(
        reservationId = id,
        observeRequest = ObserveCompanyReservationUseCase(repository),
        permissionChecker = permissions,
        getDetail = GetReservationDetailUseCase(repository),
        approveReservation = ApproveReservationUseCase(repository),
        rejectReservation = RejectReservationUseCase(repository),
    )

    @Test
    fun alAbrir_muestraLoGuardado_yTraeElDetalleDeLaApi() = runTest {
        val vm = viewModel()

        val state = vm.state.value
        assertEquals("req-1", state.request?.id)
        assertNotNull(state.detail)
        assertFalse(state.isLoading)
        assertTrue(state.canDecide)
    }

    @Test
    fun sinPrivilegios_noPuedeDecidir() = runTest {
        permissions.privileges.value = emptySet()

        val vm = viewModel()

        assertFalse(vm.state.value.canApprove)
        assertFalse(vm.state.value.canReject)
        assertFalse(vm.state.value.canDecide)
    }

    @Test
    fun soloConPrivilegioDeAceptar_puedeAceptarPeroNoRechazar() = runTest {
        permissions.privileges.value = setOf(Privileges.SOLICITUD_APROBAR)

        val vm = viewModel()

        assertTrue(vm.state.value.canApprove)
        assertFalse(vm.state.value.canReject)
    }

    @Test
    fun sinRed_muestraLoGuardado_sinError_yNoDejaDecidir() = runTest {
        repository.detailResult = AppResult.Failure(AppError.NoConnection)

        val vm = viewModel()

        val state = vm.state.value
        assertTrue(state.isOffline)
        assertNull(state.error)
        assertEquals("req-1", state.current?.id)
        assertFalse(state.canDecide)
    }

    @Test
    fun reintentar_vuelveAPedirElDetalle() = runTest {
        repository.detailResult = AppResult.Failure(AppError.NoConnection)
        val vm = viewModel()

        repository.detailResult = null
        vm.onEvent(ReservationDetailEvent.Retry)

        assertEquals(2, repository.count("getDetail:req-1"))
        assertFalse(vm.state.value.isOffline)
        assertNotNull(vm.state.value.detail)
    }

    @Test
    fun yaResuelta_noSePuedeDecidir() = runTest {
        repository.company.value = listOf(testReservation("req-1", ReservationStatus.APPROVED))

        val vm = viewModel()

        assertFalse(vm.state.value.canDecide)
    }

    @Test
    fun aceptar_pideConfirmacion_yAlConfirmarLaAcepta() = runTest {
        val vm = viewModel()

        vm.onEvent(ReservationDetailEvent.ApproveClicked)
        assertTrue(vm.state.value.showApproveConfirm)
        assertFalse("approve:req-1" in repository.calls)

        vm.onEvent(ReservationDetailEvent.ApproveConfirmed)
        assertEquals(ReservationDecision.APPROVED, vm.state.value.decision)
        assertFalse(vm.state.value.showApproveConfirm)
        assertFalse(vm.state.value.isSending)
        assertEquals(ReservationStatus.APPROVED, vm.state.value.request?.status)
    }

    @Test
    fun siAceptarFalla_muestraElError() = runTest {
        repository.approveResult = AppResult.Failure(AppError.Server(409, "El carro ya está ocupado"))
        val vm = viewModel()

        vm.onEvent(ReservationDetailEvent.ApproveClicked)
        vm.onEvent(ReservationDetailEvent.ApproveConfirmed)

        assertNull(vm.state.value.decision)
        assertEquals(AppError.Server(409, "El carro ya está ocupado"), vm.state.value.decisionError)
    }

    @Test
    fun rechazarSinMotivo_marcaElCampo_yNoLlamaALaApi() = runTest {
        val vm = viewModel()

        vm.onEvent(ReservationDetailEvent.RejectClicked)
        vm.onEvent(ReservationDetailEvent.RejectReasonChanged("   "))
        vm.onEvent(ReservationDetailEvent.RejectConfirmed)

        assertEquals(ValidationReason.REQUIRED, vm.state.value.rejectReasonError)
        assertTrue(vm.state.value.showRejectDialog, "El diálogo sigue abierto para escribir el motivo")
        assertTrue(repository.calls.none { it.startsWith("reject:") })
    }

    @Test
    fun rechazarConMotivo_laRechaza() = runTest {
        val vm = viewModel()

        vm.onEvent(ReservationDetailEvent.RejectClicked)
        vm.onEvent(ReservationDetailEvent.RejectReasonChanged("  No hay carros disponibles  "))
        vm.onEvent(ReservationDetailEvent.RejectConfirmed)

        assertTrue("reject:req-1:No hay carros disponibles" in repository.calls)
        assertEquals(ReservationDecision.REJECTED, vm.state.value.decision)
        assertFalse(vm.state.value.showRejectDialog)
    }

    @Test
    fun escribirElMotivo_quitaElError() = runTest {
        val vm = viewModel()
        vm.onEvent(ReservationDetailEvent.RejectClicked)
        vm.onEvent(ReservationDetailEvent.RejectConfirmed)

        vm.onEvent(ReservationDetailEvent.RejectReasonChanged("Motivo"))

        assertNull(vm.state.value.rejectReasonError)
    }

    @Test
    fun cerrarDialogos_dejaLaPantallaComoEstaba() = runTest {
        val vm = viewModel()
        vm.onEvent(ReservationDetailEvent.LicenseClicked)
        assertTrue(vm.state.value.showLicense)

        vm.onEvent(ReservationDetailEvent.DialogDismissed)

        assertFalse(vm.state.value.showLicense)
        assertFalse(vm.state.value.showApproveConfirm)
        assertFalse(vm.state.value.showRejectDialog)
    }
}
