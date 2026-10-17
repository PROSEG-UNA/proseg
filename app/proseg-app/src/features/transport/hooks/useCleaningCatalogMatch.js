import { useCallback, useMemo } from 'react';
import { PERMISSIONS } from '../../../common/constants/permissions';
import { usePermissions } from '../../../common/hooks/index.js';
import { driverMatchKey, plateMatchKey } from '../components/cleaning/cleaningCatalogMatch.js';
import { useDriverOptions, useVehicleOptions } from './useTransportOptions.js';

const DRIVER_VIEW_PERMISSIONS = [
    PERMISSIONS.TRANSPORT.DRIVERS.READ,
    PERMISSIONS.TRANSPORT.DRIVERS.MANAGE,
    PERMISSIONS.TRANSPORT.DRIVERS.DELETE,
];
const VEHICLE_VIEW_PERMISSIONS = [
    PERMISSIONS.TRANSPORT.VEHICLES.READ,
    PERMISSIONS.TRANSPORT.VEHICLES.MANAGE,
    PERMISSIONS.TRANSPORT.VEHICLES.DELETE,
];

export function useCleaningCatalogMatch() {
    const { hasPermission, hasAnyPermission } = usePermissions();
    const canReadDrivers = hasAnyPermission(DRIVER_VIEW_PERMISSIONS);
    const canReadVehicles = hasAnyPermission(VEHICLE_VIEW_PERMISSIONS);
    const canAddDrivers = hasPermission(PERMISSIONS.TRANSPORT.DRIVERS.MANAGE);
    const canAddVehicles = hasPermission(PERMISSIONS.TRANSPORT.VEHICLES.MANAGE);

    const drivers = useDriverOptions(canReadDrivers);
    const vehicles = useVehicleOptions(canReadVehicles);

    const driverNames = useMemo(() => drivers.options.map((option) => option.label), [drivers.options]);
    const vehiclePlates = useMemo(() => vehicles.options.map((option) => option.label), [vehicles.options]);
    const driverKeys = useMemo(() => new Set(driverNames.map(driverMatchKey)), [driverNames]);
    const plateKeys = useMemo(() => new Set(vehiclePlates.map(plateMatchKey)), [vehiclePlates]);

    const driversLoaded = canReadDrivers && !drivers.loading && !drivers.error;
    const vehiclesLoaded = canReadVehicles && !vehicles.loading && !vehicles.error;

    const isDriverMissing = useCallback((value) => {
        const key = driverMatchKey(value);
        return driversLoaded && key !== '' && !driverKeys.has(key);
    }, [driversLoaded, driverKeys]);

    const isVehicleMissing = useCallback((value) => {
        const key = plateMatchKey(value);
        return vehiclesLoaded && key !== '' && !plateKeys.has(key);
    }, [vehiclesLoaded, plateKeys]);

    return useMemo(() => ({
        driver: { options: driverNames, isMissing: isDriverMissing, canAdd: canAddDrivers },
        vehicle: { options: vehiclePlates, isMissing: isVehicleMissing, canAdd: canAddVehicles },
    }), [driverNames, isDriverMissing, canAddDrivers, vehiclePlates, isVehicleMissing, canAddVehicles]);
}
