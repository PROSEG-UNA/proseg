import { useMemo, useState, useCallback, useEffect } from 'react';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useMaintenanceRequestsData } from '../../hooks/useMaintenanceRequestsData';
import { getMaintenanceRequestColumns, renderMaintenanceRequestActions } from './maintenanceColumns.jsx';
import MaintenanceFormModal from './MaintenanceFormModal.jsx';
import { deleteMaintenanceRequest } from '../../services/maintenanceService.js';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';

const COLUMN_TO_BACKEND_KEY = {
    title: 'title',
    description: 'description',
    assetId: 'assetId',
    company: 'company.name',
    status: 'status',
    priority: 'priority',
    scheduledDate: 'scheduledDate',
    observations: 'observations',
};

export default function MaintenanceRequestTable({ refreshKey = 0, onRefresh }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [alert, setAlert] = useState(null);
    const [editRequestId, setEditRequestId] = useState(null);
    const [requestToDelete, setRequestToDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const { hasPermission } = usePermissions();
    const canManageRequests = hasPermission(PERMISSIONS.MAINTENANCE?.MANAGE) || hasPermission('ROLE_ADMIN');
    const canDeleteRequests = hasPermission(PERMISSIONS.MAINTENANCE?.DELETE) || hasPermission('ROLE_ADMIN');
    const canUseActions = canManageRequests || canDeleteRequests;

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

    const resetPageOnFilterChange = () => {
        setPagination((prev) => (prev.pageIndex === 0 ? prev : { ...prev, pageIndex: 0 }));
    };

    useEffect(resetPageOnFilterChange, [debouncedGlobalFilter, backendFilters, backendSort]);

    const { rows, loading, error, totalElements } = useMaintenanceRequestsData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
        refreshKey,
    });

    const columns = useMemo(
        () =>
            getMaintenanceRequestColumns().map((column) => ({
                ...column,
                muiTableBodyCellProps: {
                    ...(column.muiTableBodyCellProps ?? {}),
                    sx: {
                        ...(column.muiTableBodyCellProps?.sx ?? {}),
                        py: 1.15,
                        borderBottom: '1px solid',
                        borderBottomColor: 'divider',
                    },
                },
            })),
        []
    );

    const handleEdit = useCallback((row) => {
        setEditRequestId(row.id);
    }, []);

    const handleDelete = useCallback((row) => {
        setRequestToDelete(row);
    }, []);

    const handleEditClose = useCallback(() => setEditRequestId(null), []);

    const handleEditSaved = useCallback(() => {
        setAlert({ type: 'success', message: 'Solicitud actualizada correctamente' });
        onRefresh?.();
    }, [onRefresh]);

    const handleDeleteCancel = useCallback(() => {
        if (deleting) return;
        setRequestToDelete(null);
    }, [deleting]);

    const handleDeleteConfirm = useCallback(async () => {
        if (!requestToDelete) return;
        setDeleting(true);
        try {
            await deleteMaintenanceRequest(requestToDelete.id);
            setRequestToDelete(null);
            setAlert({ type: 'success', message: 'Solicitud eliminada correctamente' });
            onRefresh?.();
        } catch (e) {
            const data = e?.response?.data;
            const mainMsg = data?.message ?? e?.message ?? 'Error al eliminar';
            const fieldErrors = data?.errors;
            const fullMsg = fieldErrors?.length
                ? `${mainMsg}:\n${fieldErrors.map((err) => `• ${err}`).join('\n')}`
                : mainMsg;
            setAlert({ type: 'error', message: fullMsg });
        } finally {
            setDeleting(false);
        }
    }, [requestToDelete, onRefresh]);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                error={error}
                enableRowActions={canUseActions}
                renderRowActions={canUseActions ? renderMaintenanceRequestActions({
                    onEdit: handleEdit,
                    onDelete: handleDelete,
                    canEdit: canManageRequests,
                    canDelete: canDeleteRequests,
                }) : undefined}
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
                            description: false,
                            observations: false,
                        },
                    },
                    displayColumnDefOptions: {
                        'mrt-row-actions': {
                            muiTableBodyCellProps: {
                                sx: { py: 1.15, borderBottom: '1px solid', borderBottomColor: 'divider' },
                            },
                        },
                    },
                }}
                enableGlobalFilter
            />

            <MaintenanceFormModal
                open={!!editRequestId}
                requestId={editRequestId}
                onClose={handleEditClose}
                onSaved={handleEditSaved}
            />

            <DialogModal
                type="delete"
                open={!!requestToDelete}
                title="Eliminar solicitud"
                message={`¿Seguro que deseas eliminar la solicitud "${requestToDelete?.title}"?\nEsta acción no se puede deshacer.`}
                onClose={handleDeleteCancel}
                onConfirm={handleDeleteConfirm}
                confirmLabel="Eliminar"
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

