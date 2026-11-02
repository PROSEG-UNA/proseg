import { useQuery } from '@tanstack/react-query';
import { fetchDriverMonths } from '../services/drivers/driversService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { queryKeys } from '../../../common/query';

export function useTransportDriverMonthsData(year, month) {
    const enabled = Boolean(year && month);

    const { data, isLoading, error } = useQuery({
        queryKey: queryKeys.transport.driverMonths(year, month),
        queryFn: () => fetchDriverMonths(year, month),
        enabled,
    });

    const months = data ?? [];
    const byDriverId = new Map(months.map((driverMonth) => [driverMonth.driverId, driverMonth]));
    const hasAnyRecord = months.some((driverMonth) => driverMonth.hasRecord);

    return {
        enabled,
        byDriverId,
        hasAnyRecord,
        loading: enabled && isLoading,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar los datos del mes') : null,
    };
}
