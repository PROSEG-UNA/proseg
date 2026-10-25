import { useQuery } from '@tanstack/react-query';
import {
    fetchCleaningDraft,
    fetchCleaningDraftDriverSchedule,
    fetchCleaningDraftDriverSummaries,
    fetchCleaningDraftRows,
    fetchCleaningDrafts,
} from '../services/cleaning/cleaningService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { keepPreviousPage, queryKeys } from '../../../common/query';

const ALL_ROWS_PAGE_SIZE = 5000;

function mapDraftToRow(draft) {
    return {
        id: draft.id,
        fileName: draft.fileName ?? '—',
        year: draft.year ?? null,
        month: draft.month ?? null,
        status: draft.status ?? '',
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

export function useCleaningDraftAllRows(draftId) {
    const requestParams = { page: 0, size: ALL_ROWS_PAGE_SIZE };
    const { data, isLoading, error } = useQuery({
        queryKey: queryKeys.transport.cleaningDraftRows(draftId, requestParams),
        enabled: Boolean(draftId),
        queryFn: async () => {
            const response = await fetchCleaningDraftRows(draftId, requestParams);
            return (response.content ?? []).map(mapDraftRowToRow);
        },
    });

    return {
        rows: data ?? [],
        loading: isLoading,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudieron cargar las filas del borrador') : null,
    };
}

export function useCleaningDraftDriverSummaries(draftId) {
    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: queryKeys.transport.cleaningDraftDriverSummaries(draftId),
        queryFn: () => fetchCleaningDraftDriverSummaries(draftId),
        enabled: Boolean(draftId),
    });

    return {
        surplusCapPerDriver: data?.surplusCapPerDriver ?? 0,
        drivers: data?.drivers ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudo cargar el resumen de choferes') : null,
    };
}

export function useCleaningDraftDriverSchedule(draftId, driverId, { pageIndex = 0, pageSize = 10 } = {}) {
    const requestParams = { page: pageIndex, size: pageSize };
    const listQueryKey = queryKeys.transport.cleaningDraftDriverSchedule(draftId, driverId, requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        enabled: Boolean(draftId) && Boolean(driverId),
        queryFn: async () => {
            const response = await fetchCleaningDraftDriverSchedule(draftId, driverId, requestParams);
            return {
                tours: response.content ?? [],
                totalElements: response.totalElements ?? 0,
            };
        },
    });

    return {
        tours: data?.tours ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'No se pudieron cargar las giras del chofer') : null,
        totalElements: data?.totalElements ?? 0,
    };
}
