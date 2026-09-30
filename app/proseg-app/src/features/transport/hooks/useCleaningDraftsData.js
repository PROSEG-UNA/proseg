import { useQuery } from '@tanstack/react-query';
import {
    fetchCleaningDraft,
    fetchCleaningDraftRows,
    fetchCleaningDrafts,
} from '../services/cleaning/cleaningService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

function mapDraftToRow(draft) {
    return {
        id: draft.id,
        fileName: draft.fileName ?? '—',
        fileType: draft.fileType ?? '—',
        status: draft.status ?? '',
        importedBy: draft.importedBy ?? '—',
        importedAt: draft.importedAt ?? null,
        totalRows: draft.totalRows ?? 0,
        duplicateRows: draft.duplicateRows ?? 0,
        conflictingRows: draft.conflictingRows ?? 0,
    };
}

function mapDraftRowToRow(row) {
    return {
        ...row,
        departureTime: row.departureTime ? String(row.departureTime).slice(0, 5) : null,
        returnTime: row.returnTime ? String(row.returnTime).slice(0, 5) : null,
    };
}

export function useCleaningDraftsData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };
    const listQueryKey = queryKeys.transport.cleaningDraftList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchCleaningDrafts(requestParams);
            return {
                rows: (response.content ?? []).map(mapDraftToRow),
                totalElements: response.totalElements ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudieron cargar los borradores') : null,
        totalElements: data?.totalElements ?? 0,
    };
}

export function useCleaningDraft(draftId) {
    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: queryKeys.transport.cleaningDraftDetail(draftId),
        queryFn: () => fetchCleaningDraft(draftId),
        enabled: Boolean(draftId),
    });

    return {
        draft: data ?? null,
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudo cargar el borrador') : null,
    };
}

export function useCleaningDraftRows(draftId, { pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };
    const listQueryKey = queryKeys.transport.cleaningDraftRows(draftId, requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        enabled: Boolean(draftId),
        queryFn: async () => {
            const response = await fetchCleaningDraftRows(draftId, requestParams);
            return {
                rows: (response.content ?? []).map(mapDraftRowToRow),
                totalElements: response.totalElements ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudieron cargar las filas del borrador') : null,
        totalElements: data?.totalElements ?? 0,
    };
}
