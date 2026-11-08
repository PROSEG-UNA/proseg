import { useQuery } from '@tanstack/react-query';
import { fetchDriverMonthSurplusCap } from '../services/drivers/driversService';
import { queryKeys } from '../../../common/query';

export function useTransportDriverMonthSurplusCap(year, month) {
    const { data } = useQuery({
        queryKey: queryKeys.transport.driverMonthSurplusCap(year, month),
        queryFn: () => fetchDriverMonthSurplusCap(year, month),
        enabled: Boolean(year && month),
    });

    return {
        businessDays: data?.businessDays ?? null,
        surplusHoursPerDriver: data?.surplusHoursPerDriver ?? null,
    };
}
