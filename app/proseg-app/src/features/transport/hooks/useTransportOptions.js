import { useQuery } from '@tanstack/react-query';
import { fetchDrivers } from '../services/drivers/driversService';
import { fetchVehicles } from '../services/vehicles/vehiclesService';
import { fetchTours } from '../services/tours/toursService';
import { queryKeys } from '../../../common/query';

const OPTIONS_PAGE = { page: 0, size: 300 };
const OPTIONS_STALE_TIME = 5 * 60_000;
const EMPTY_OPTIONS = [];

function driverLabel(driver) {
    return [driver.firstName, driver.lastName].filter(Boolean).join(' ').trim() || 'Chofer';
}

function vehicleLabel(vehicle) {
    return vehicle.plate ? `${vehicle.plate}${vehicle.model ? ` - ${vehicle.model}` : ''}` : 'Vehículo';
}

export function useDriverOptions(enabled = true) {
    const { data, isLoading, error } = useQuery({
        queryKey: queryKeys.transport.driverOptions(),
        queryFn: async () => {
            const response = await fetchDrivers(OPTIONS_PAGE);
            return (response?.content ?? []).map((driver) => ({ id: driver.id, label: driverLabel(driver) }));
        },
        enabled: Boolean(enabled),
        staleTime: OPTIONS_STALE_TIME,
    });

    return { options: data ?? EMPTY_OPTIONS, loading: isLoading, error };
}

export function useVehicleOptions(enabled = true) {
    const { data, isLoading, error } = useQuery({
        queryKey: queryKeys.transport.vehicleOptions(),
        queryFn: async () => {
            const response = await fetchVehicles(OPTIONS_PAGE);
            return (response?.content ?? []).map((vehicle) => ({ id: vehicle.id, label: vehicleLabel(vehicle) }));
        },
        enabled: Boolean(enabled),
        staleTime: OPTIONS_STALE_TIME,
    });

    return { options: data ?? EMPTY_OPTIONS, loading: isLoading, error };
}

export function useTourOptions(enabled = true) {
    const { data, isLoading, error } = useQuery({
        queryKey: queryKeys.transport.tourOptions(),
        queryFn: async () => {
            const response = await fetchTours(OPTIONS_PAGE);
            return (response?.content ?? []).map((tour) => ({ id: tour.id, label: tour.name || 'Gira' }));
        },
        enabled: Boolean(enabled),
        staleTime: OPTIONS_STALE_TIME,
    });

    return { options: data ?? EMPTY_OPTIONS, loading: isLoading, error };
}
