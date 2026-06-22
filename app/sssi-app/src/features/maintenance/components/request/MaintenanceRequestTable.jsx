import { useCallback, useEffect, useMemo, useState } from 'react';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { deleteMaintenanceRequest } from '../../services/request/requestsService';
import { useMaintenanceRequestsData } from '../../hooks/request/useMaintenanceRequestsData';
import { getMaintenanceRequestColumns, renderMaintenanceRequestActions } from './requestColumns.jsx';
import MaintenanceRequestDetailPanel from './MaintenanceRequestDetailPanel.jsx';

const COLUMN_TO_BACKEND_KEY = {
    companyName: 'company.name',
    companyLegalId: 'company.legalId',
    statusRaw: 'status',
    startDate: 'startDate',
};

export default function MaintenanceRequestTable({ refreshKey = 0, onRefresh, onEditRequest }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [alert, setAlert] = useState(null);
    const [requestToDelete, setRequestToDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const { hasPermission } = usePermissions();

    const canEdit = hasPermission(PERMISSIONS.MAINTENANCE.REQUESTS.UPDATE);
    const canDelete = hasPermission(PERMISSIONS.MAINTENANCE.REQUESTS.DELETE);

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

    const { rows, loading, error, totalElements } = useMaintenanceRequestsData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
        refreshKey,
    });

    const columns = useMemo(() => getMaintenanceRequestColumns(), []);

    const handleDelete = useCallback((row) => setRequestToDelete(row), []);
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
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo eliminar la solicitud' });
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
                enableRowActions={canEdit || canDelete}
                renderRowActions={renderMaintenanceRequestActions({
                    onEdit: onEditRequest,
                    onDelete: handleDelete,
                    canEdit,
                    canDelete,
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

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}


