package org.vicpoo.gasolineproject.domain.di

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.vicpoo.gasolineproject.domain.triprecording.FinishTripValidator
import org.vicpoo.gasolineproject.domain.triprecording.FuelRecordValidator
import org.vicpoo.gasolineproject.domain.triprecording.usecase.CaptureFuelLocationUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.CheckLocationPermissionUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.DeleteFuelRecordUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.FinishTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.GetAssignedVehicleUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.GetFuelRecordUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.LocationPermissionChangedUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.ObserveAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.ObserveCostCentersUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.ObserveFuelRecordsUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.ObserveReleaseBlocksUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.ObserveSyncStatusUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.ObserveTrackingUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.PauseTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.RefreshAssignedVehiclesUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.RefreshCostCentersUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.ReleaseVehicleUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.ResumeTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.RetryFinishTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.RetryFuelRecordUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.SaveFuelRecordUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.StartTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.StopTripUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.SyncNowUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.SyncPendingTripsUseCase
import org.vicpoo.gasolineproject.domain.triprecording.usecase.UndoStopTripUseCase

val tripRecordingDomainModule = module {
    factoryOf(::FuelRecordValidator)
    factoryOf(::ObserveAssignedVehiclesUseCase)
    factoryOf(::RefreshAssignedVehiclesUseCase)
    factoryOf(::GetAssignedVehicleUseCase)
    factoryOf(::ObserveReleaseBlocksUseCase)
    factoryOf(::ReleaseVehicleUseCase)
    factoryOf(::ObserveCostCentersUseCase)
    factoryOf(::RefreshCostCentersUseCase)
    factoryOf(::ObserveTrackingUseCase)
    factoryOf(::StartTripUseCase)
    factoryOf(::FinishTripValidator)
    factoryOf(::FinishTripUseCase)
    factoryOf(::RetryFinishTripUseCase)
    factoryOf(::PauseTripUseCase)
    factoryOf(::ResumeTripUseCase)
    factoryOf(::StopTripUseCase)
    factoryOf(::UndoStopTripUseCase)
    factoryOf(::ObserveFuelRecordsUseCase)
    factoryOf(::GetFuelRecordUseCase)
    factoryOf(::CaptureFuelLocationUseCase)
    factoryOf(::SaveFuelRecordUseCase)
    factoryOf(::RetryFuelRecordUseCase)
    factoryOf(::DeleteFuelRecordUseCase)
    factoryOf(::CheckLocationPermissionUseCase)
    factoryOf(::LocationPermissionChangedUseCase)
    factoryOf(::SyncPendingTripsUseCase)
    factoryOf(::ObserveSyncStatusUseCase)
    factoryOf(::SyncNowUseCase)
}
