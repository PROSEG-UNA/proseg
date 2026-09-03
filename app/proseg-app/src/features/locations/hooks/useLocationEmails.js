import { useQuery } from '@tanstack/react-query';
import { fetchEmailsByBuilding, fetchEmailsByCampus } from '../services/buildingMailService.js';
import { queryKeys } from '../../../common/query';

const EMPTY_EMAILS = [];

export function useBuildingEmails(buildingId, enabled = true) {
    const { data, isLoading } = useQuery({
        queryKey: queryKeys.locations.buildingEmails(buildingId ?? null),
        queryFn: () => fetchEmailsByBuilding(buildingId),
        enabled: Boolean(enabled && buildingId),
    });

    return { emails: data ?? EMPTY_EMAILS, loading: isLoading };
}

export function useCampusEmails(campusId, enabled = true) {
    const { data, isLoading } = useQuery({
        queryKey: queryKeys.locations.campusEmails(campusId ?? null),
        queryFn: () => fetchEmailsByCampus(campusId),
        enabled: Boolean(enabled && campusId),
    });

    return { emails: data ?? EMPTY_EMAILS, loading: isLoading };
}
