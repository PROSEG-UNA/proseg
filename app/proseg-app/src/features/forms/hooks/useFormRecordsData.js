import { useQuery } from '@tanstack/react-query';
import { keepPreviousPage, queryKeys } from '../../../common/query';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { fetchFormRecords } from '../services/formsService';

function mapRecordToRow(record) {
    return {
        id: record.id,
        formTypeId: record.formTypeId ?? '',
        formTypeName: record.formTypeName ?? '—',
        formTypeCode: record.formTypeCode ?? '',
        createdBy: record.createdBy ?? '—',
        createdByName: record.createdByName ?? record.createdBy ?? '—',
        createdAt: record.createdAt ?? null,
        updatedAt: record.updatedAt ?? null,
        data: record.data ?? {},
    };
}

export function useFormRecordsData({ pageIndex = 0, pageSize = 10, filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, filters, sort };
    const listQueryKey = queryKeys.forms.recordList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchFormRecords(requestParams);
            return {
                rows: (response.content ?? []).map(mapRecordToRow),
                totalElements: response.totalElements ?? 0,
                totalPages: response.totalPages ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar formularios') : null,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}
