package com.proseg.msvc_maintenance.mapper;

import com.proseg.msvc_maintenance.dto.response.InventoryAssetBuildingResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetEmployeeResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetExecutingUnitResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetFloorResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetLocationResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetModelResponseDto;
import com.proseg.msvc_maintenance.dto.response.InventoryAssetResponseDto;
import com.proseg.msvc_maintenance.dto.response.MaintenanceAssetOptionDto;

public final class MaintenanceAssetOptionMapper {

    private MaintenanceAssetOptionMapper() {
    }

    public static MaintenanceAssetOptionDto toOption(InventoryAssetResponseDto asset) {
        InventoryAssetModelResponseDto model = asset.getModel();
        InventoryAssetLocationResponseDto location = asset.getLocation();
        InventoryAssetFloorResponseDto floor = location != null ? location.getFloor() : null;
        InventoryAssetBuildingResponseDto building = floor != null ? floor.getBuilding() : null;
        InventoryAssetExecutingUnitResponseDto executingUnit = asset.getExecutingUnit();
        InventoryAssetEmployeeResponseDto employee = asset.getEmployee();

        return MaintenanceAssetOptionDto.builder()
                .id(asset.getId())
                .kind(asset.getKind())
                .status(asset.getStatus())
                .assetNumber(asset.getAssetNumber())
                .serialNumber(asset.getSerialNumber())
                .type(model != null && model.getType() != null ? model.getType().getName() : null)
                .brand(model != null && model.getBrand() != null ? model.getBrand().getName() : null)
                .modelName(model != null ? model.getName() : null)
                .campusName(building != null && building.getCampus() != null ? building.getCampus().getName() : null)
                .buildingName(building != null ? building.getName() : null)
                .floorName(floor != null ? floor.getName() : null)
                .locationName(location != null ? location.getDescription() : null)
                .executingUnit(executingUnit != null ? executingUnit.getName() : null)
                .responsibleEmployee(employee != null ? employee.getName() : null)
                .responsibleEmployeeId(employee != null ? employee.getIdentification() : null)
                .acquisitionDate(asset.getAcquisitionDate())
                .warrantyEndDate(asset.getWarrantyEndDate())
                .firmwareSupportEndDate(asset.getFirmwareSupportEndDate())
                .decommissionDate(asset.getDecommissionDate())
                .latitude(asset.getLatitude())
                .longitude(asset.getLongitude())
                .build();
    }
}
