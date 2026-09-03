import { fetchMaintenanceRequestById } from '../../services/request/requestsService';
import { queryKeys } from '../../../../common/query';

export function requestDetailQueryOptions(requestId) {
    return {
        queryKey: queryKeys.maintenance.requestDetail(requestId),
        queryFn: () => fetchMaintenanceRequestById(requestId),
    };
}
