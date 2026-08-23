import { fetchCompanyById } from '../../services/company/companiesService';
import { queryKeys } from '../../../../common/query';

export function companyDetailQueryOptions(companyId) {
    return {
        queryKey: queryKeys.maintenance.companyDetail(companyId),
        queryFn: () => fetchCompanyById(companyId),
    };
}
