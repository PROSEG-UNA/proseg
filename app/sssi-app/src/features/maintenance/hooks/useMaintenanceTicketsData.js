import { useQuery } from '@tanstack/react-query';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { fetchMaintenanceTickets } from '../services/ticketsService';
import { keepPreviousPage, queryKeys } from '../../../common/query';

function mapTicketToRow(ticket) {
    return {
        id: ticket.id,
        title: ticket.title ?? '—',
        description: ticket.description ?? '—',
        status: ticket.status ?? '—',
        statusRaw: ticket.status ?? '',
        createdBy: ticket.createdBy ?? '—',
        createdByName: ticket.createdByName ?? ticket.createdBy ?? '—',
        createdAt: ticket.createdAt ?? null,
        updatedAt: ticket.updatedAt ?? null,
    };
}

export function useMaintenanceTicketsData({ pageIndex = 0, pageSize = 10, search = '', filters = {}, sort = [] } = {}) {
    const requestParams = { page: pageIndex, size: pageSize, search, filters, sort };

    const listQueryKey = queryKeys.maintenance.ticketList(requestParams);

    const { data, isLoading, isFetching, error } = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchMaintenanceTickets(requestParams);
            return {
                rows: (response.content ?? []).map(mapTicketToRow),
                totalElements: response.totalElements ?? 0,
                totalPages: response.totalPages ?? 0,
            };
        },
    });

    return {
        rows: data?.rows ?? [],
        loading: isLoading,
        fetching: isFetching,
        error: error ? getFriendlyApiErrorMessage(error, 'Error al cargar tickets de mantenimiento') : null,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
    };
}
