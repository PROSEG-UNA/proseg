import { useCallback, useEffect, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import CancelWithReasonModal from '../../../../common/components/CancelWithReasonModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import {
    cancelMaintenanceRequest,
    deleteMaintenanceRequest,
} from '../../services/request/requestsService';
import { useMaintenanceRequestsData } from '../../hooks/request/useMaintenanceRequestsData';
import { formatDate } from '../../maintenanceUtils';
import { getMaintenanceRequestColumns, renderMaintenanceRequestActions } from './requestColumns.jsx';
import MaintenanceRequestDetailPanel from './MaintenanceRequestDetailPanel.jsx';
import { queryKeys } from '../../../../common/query';

const COLUMN_TO_BACKEND_KEY = {
    companyName: 'company.name',
    companyLegalId: 'company.legalId',
    statusRaw: 'status',
    startDate: 'startDate',
};

export default function MaintenanceRequestTable({ onEditRequest }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [alert, setAlert] = useState(null);
    const [requestToDelete, setRequestToDelete] = useState(null);
    const [requestToCancel, setRequestToCancel] = useState(null);
    const queryClient = useQueryClient();
    const { hasPermission } = usePermissions();

    const canEdit = hasPermission(PERMISSIONS.MAINTENANCE.REQUESTS.UPDATE);
    const canDelete = hasPermission(PERMISSIONS.MAINTENANCE.REQUESTS.DELETE);
    const canCancel = hasPermission(PERMISSIONS.MAINTENANCE.REQUESTS.CANCEL);

    const debouncedGlobalFilter = useDebounce(globalFilter, 350);
    const debouncedColumnFilters = useDebounce(columnFilters, 350);

    const backendFilters = useMemo(() => {
        const out = {};
        debouncedColumnFilters.forEach(({ id, value }) => {
            const key = COLUMN_TO_BACKEND_KEY[id];
            if (!key) return;
            if (value === null || value === undefined || value === '') return;
            out[key] = value;
        });
        return out;
    }, [debouncedColumnFilters]);

    const backendSort = useMemo(
        () => sorting
            .map(({ id, desc }) => {
                const key = COLUMN_TO_BACKEND_KEY[id];
                if (!key) return null;
                return `${key},${desc ? 'desc' : 'asc'}`;
            })
            .filter(Boolean),
        [sorting]
    );

    useEffect(() => {
        setPagination((prev) => (prev.pageIndex === 0 ? prev : { ...prev, pageIndex: 0 }));
    }, [debouncedGlobalFilter, backendFilters, backendSort]);

    const { rows, loading, fetching, error, totalElements } = useMaintenanceRequestsData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
    });

    const columns = useMemo(() => getMaintenanceRequestColumns(), []);

    const invalidateRequests = () => queryClient.invalidateQueries({
        queryKey: queryKeys.maintenance.requests(),
    });

    const deleteRequestMutation = useMutation({
        mutationFn: (requestId) => deleteMaintenanceRequest(requestId),
        onSuccess: async () => {
            setRequestToDelete(null);
            setAlert({ type: 'success', message: 'Solicitud eliminada correctamente' });
            await invalidateRequests();
        },
        onError: (deleteError) => {
            setAlert({ type: 'error', message: deleteError?.response?.data?.message ?? deleteError?.message ?? 'No se pudo eliminar la solicitud' });
        },
    });

    const cancelRequestMutation = useMutation({
        mutationFn: ({ requestId, reason }) => cancelMaintenanceRequest(requestId, reason),
        onSuccess: async () => {
            setRequestToCancel(null);
            setAlert({ type: 'success', message: 'Solicitud cancelada correctamente' });
            await invalidateRequests();
        },
        onError: (cancelError) => {
            setAlert({ type: 'error', message: cancelError?.response?.data?.message ?? cancelError?.message ?? 'No se pudo cancelar la solicitud' });
        },
    });

    const deleting = deleteRequestMutation.isPending;
    const cancelling = cancelRequestMutation.isPending;

    const handleDelete = useCallback((row) => setRequestToDelete(row), []);
    const handleDeleteCancel = useCallback(() => {
        if (deleting) return;
        setRequestToDelete(null);
    }, [deleting]);

    const handleDeleteConfirm = useCallback(() => {
        if (!requestToDelete) return;
        deleteRequestMutation.mutate(requestToDelete.id);
    }, [requestToDelete, deleteRequestMutation]);

    const handleCancel = useCallback((row) => setRequestToCancel(row), []);
    const handleCancelClose = useCallback(() => {
        if (cancelling) return;
        setRequestToCancel(null);
    }, [cancelling]);

    const handleCancelConfirm = useCallback((reason) => {
        if (!requestToCancel) return;
        cancelRequestMutation.mutate({ requestId: requestToCancel.id, reason });
    }, [requestToCancel, cancelRequestMutation]);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                fetching={fetching}
                error={error}
                enableRowActions={canEdit || canDelete || canCancel}
                renderRowActions={renderMaintenanceRequestActions({
                    onEdit: onEditRequest,
                    onDelete: handleDelete,
                    onCancel: handleCancel,
                    canEdit,
                    canDelete,
                    canCancel,
                })}
                renderDetailPanel={({ row }) => (
                    <MaintenanceRequestDetailPanel
                        requestId={row.original.id}
                    />
                )}
                tableOptions={{
                    positionActionsColumn: 'last',
                    manualPagination: true,
                    manualFiltering: true,
                    manualSorting: true,
                    rowCount: totalElements,
                    onPaginationChange: setPagination,
                    onGlobalFilterChange: setGlobalFilter,
                    onColumnFiltersChange: setColumnFilters,
                    onSortingChange: setSorting,
                    state: { pagination, globalFilter, columnFilters, sorting },
                    initialState: {
                        columnVisibility: {
                            companyLegalId: false,
                            createdAt: false,
                        },
                    },
                }}
                enableGlobalFilter
            />

            <DialogModal
                type="delete"
                open={!!requestToDelete}
                title="Eliminar solicitud"
                message={`¿Seguro que deseas eliminar esta solicitud de ${requestToDelete?.companyName ?? 'la empresa'}?\nEsta acción no se puede deshacer.`}
                onClose={handleDeleteCancel}
                onConfirm={handleDeleteConfirm}
                confirmLabel="Eliminar"
            />

            <CancelWithReasonModal
                open={!!requestToCancel}
                onClose={handleCancelClose}
                onConfirm={handleCancelConfirm}
                loading={cancelling}
                title="Cancelar solicitud"
                subtitle={requestToCancel?.companyName}
                details={[
                    { label: 'Empresa', value: requestToCancel?.companyName },
                    { label: 'Estado actual', value: requestToCancel?.status },
                    { label: 'Inicio', value: formatDate(requestToCancel?.startDate) },
                    { label: 'Fin', value: formatDate(requestToCancel?.endDate) },
                ]}
                confirmLabel="Cancelar solicitud"
            />

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}


