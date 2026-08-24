import { useCallback, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { deleteVehicleMaintenance } from '../../services/maintenance/maintenanceService';
import { useTransportMaintenanceData } from '../../hooks/useTransportMaintenanceData';
import { getTransportMaintenanceColumns, renderTransportMaintenanceActions } from './maintenanceColumns.jsx';
import { queryKeys } from '../../../../common/query';

const COLUMN_TO_BACKEND_KEY = {
    vehiclePlate: 'vehicle.plate',
    title: 'title',
    type: 'type',
    statusRaw: 'status',
    scheduledDate: 'scheduledDate',
};

export default function TransportMaintenanceTable({ onEditMaintenance }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [alert, setAlert] = useState(null);
    const [maintenanceToDelete, setMaintenanceToDelete] = useState(null);
    const queryClient = useQueryClient();
    const { hasPermission } = usePermissions();

    const canEdit = hasPermission(PERMISSIONS.TRANSPORT.MAINTENANCE.MANAGE);
    const canDelete = hasPermission(PERMISSIONS.TRANSPORT.MAINTENANCE.DELETE);

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

    const handleGlobalFilterChange = useCallback((updater) => {
        setGlobalFilter((currentValue) => (typeof updater === 'function' ? updater(currentValue) : updater));
        setPagination((previousValue) => (previousValue.pageIndex === 0 ? previousValue : { ...previousValue, pageIndex: 0 }));
    }, []);

    const handleColumnFiltersChange = useCallback((updater) => {
        setColumnFilters((currentValue) => (typeof updater === 'function' ? updater(currentValue) : updater));
        setPagination((previousValue) => (previousValue.pageIndex === 0 ? previousValue : { ...previousValue, pageIndex: 0 }));
    }, []);

    const handleSortingChange = useCallback((updater) => {
        setSorting((currentValue) => (typeof updater === 'function' ? updater(currentValue) : updater));
        setPagination((previousValue) => (previousValue.pageIndex === 0 ? previousValue : { ...previousValue, pageIndex: 0 }));
    }, []);

    const { rows, loading, fetching, error, totalElements } = useTransportMaintenanceData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
    });

    const columns = useMemo(() => getTransportMaintenanceColumns(), []);

    const handleDelete = useCallback((row) => setMaintenanceToDelete(row), []);
    const deleteMaintenanceMutation = useMutation({
        mutationFn: (maintenanceId) => deleteVehicleMaintenance(maintenanceId),
        onSuccess: async () => {
            setMaintenanceToDelete(null);
            setAlert({ type: 'success', message: 'Mantenimiento eliminado correctamente' });
            await queryClient.invalidateQueries({ queryKey: queryKeys.transport.maintenance() });
        },
        onError: (deleteError) => {
            setAlert({ type: 'error', message: deleteError?.response?.data?.message ?? deleteError?.message ?? 'No se pudo eliminar el mantenimiento' });
        },
    });

    const deleting = deleteMaintenanceMutation.isPending;

    const handleDeleteCancel = useCallback(() => {
        if (deleting) return;
        setMaintenanceToDelete(null);
    }, [deleting]);

    const handleDeleteConfirm = useCallback(() => {
        if (!maintenanceToDelete) return;
        deleteMaintenanceMutation.mutate(maintenanceToDelete.id);
    }, [maintenanceToDelete, deleteMaintenanceMutation]);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                fetching={fetching}
                error={error}
                enableRowActions={canEdit || canDelete}
                renderRowActions={renderTransportMaintenanceActions({
                    onEdit: onEditMaintenance,
                    onDelete: handleDelete,
                    canEdit,
                    canDelete,
                })}
                tableOptions={{
                    positionActionsColumn: 'last',
                    manualPagination: true,
                    manualFiltering: true,
                    manualSorting: true,
                    rowCount: totalElements,
                    onPaginationChange: setPagination,
                    onGlobalFilterChange: handleGlobalFilterChange,
                    onColumnFiltersChange: handleColumnFiltersChange,
                    onSortingChange: handleSortingChange,
                    state: { pagination, globalFilter, columnFilters, sorting },
                    initialState: {
                        columnVisibility: {
                            updatedAt: false,
                            createdAt: false,
                        },
                    },
                }}
                enableGlobalFilter
            />

            <DialogModal
                type="delete"
                open={!!maintenanceToDelete}
                title="Eliminar mantenimiento"
                message={`¿Seguro que deseas eliminar el mantenimiento "${maintenanceToDelete?.title}"?\nEsta acción no se puede deshacer.`}
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
