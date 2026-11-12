import { useCallback, useMemo } from 'react';
import { PERMISSIONS } from '../../../common/constants/permissions';
import { usePermissions } from '../../../common/hooks/index.js';
import { buildDriverCatalogEntry, buildVehicleCatalogEntry, driverMatchKey, plateMatchKey } from '../components/tourProcessing/tourProcessingCatalogMatch.js';
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

export function useTourProcessingCatalogMatch() {
    const { hasPermission, hasAnyPermission } = usePermissions();
    const canReadDrivers = hasAnyPermission(DRIVER_VIEW_PERMISSIONS);
    const canReadVehicles = hasAnyPermission(VEHICLE_VIEW_PERMISSIONS);
    const canAddDrivers = hasPermission(PERMISSIONS.TRANSPORT.DRIVERS.MANAGE);
    const canAddVehicles = hasPermission(PERMISSIONS.TRANSPORT.VEHICLES.MANAGE);

    const drivers = useDriverOptions(canReadDrivers);
    const vehicles = useVehicleOptions(canReadVehicles);

    const driverNames = useMemo(() => drivers.options.map((option) => option.label), [drivers.options]);
    const vehiclePlates = useMemo(() => vehicles.options.map((option) => option.label), [vehicles.options]);
    const selectableVehicles = useMemo(
        () => vehicles.options.filter((option) => option.available !== false),
        [vehicles.options],
    );
    const driverEntries = useMemo(() => drivers.options.map(buildDriverCatalogEntry), [drivers.options]);
    const vehicleEntries = useMemo(() => selectableVehicles.map(buildVehicleCatalogEntry), [selectableVehicles]);
    const driverKeys = useMemo(() => new Set(driverNames.map(driverMatchKey)), [driverNames]);
    const plateKeys = useMemo(() => new Set(vehiclePlates.map(plateMatchKey)), [vehiclePlates]);
    const unavailablePlateKeys = useMemo(
        () => new Set(vehicles.options.filter((option) => option.available === false).map((option) => plateMatchKey(option.label))),
        [vehicles.options],
    );

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

    const isVehicleUnavailable = useCallback((value) => {
        const key = plateMatchKey(value);
        return vehiclesLoaded && key !== '' && unavailablePlateKeys.has(key);
    }, [vehiclesLoaded, unavailablePlateKeys]);

    return useMemo(() => ({
        driver: { options: driverEntries, isMissing: isDriverMissing, canAdd: canAddDrivers },
        vehicle: {
            options: vehicleEntries,
            catalog: vehicles.options,
            isMissing: isVehicleMissing,
            isUnavailable: isVehicleUnavailable,
            canAdd: canAddVehicles,
        },
    }), [driverEntries, isDriverMissing, canAddDrivers, vehicleEntries, vehicles.options, isVehicleMissing, isVehicleUnavailable, canAddVehicles]);
}
