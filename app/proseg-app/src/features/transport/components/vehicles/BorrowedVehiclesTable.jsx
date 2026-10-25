import { useCallback, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { deleteBorrowedVehicle } from '../../services/borrowedVehicles/borrowedVehiclesService';
import { usePersistedColumnVisibility } from '../../hooks/usePersistedColumnVisibility';
import { useTransportBorrowedVehiclesData } from '../../hooks/useTransportBorrowedVehiclesData';
import { getBorrowedVehicleColumns, renderBorrowedVehicleActions } from './borrowedVehicleColumns.jsx';
import { queryKeys } from '../../../../common/query';

const COLUMN_VISIBILITY_STORAGE_KEY = 'borrowed-vehicles-table-column-visibility';

const COLUMN_TO_BACKEND_KEY = {
    plate: 'plate',
    type: 'type',
    capacity: 'capacity',
};

export default function BorrowedVehiclesTable({ onEditBorrowedVehicle }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [columnVisibility, setColumnVisibility] = usePersistedColumnVisibility(COLUMN_VISIBILITY_STORAGE_KEY);
    const [alert, setAlert] = useState(null);
    const [vehicleToDelete, setVehicleToDelete] = useState(null);
    const queryClient = useQueryClient();
    const { hasPermission } = usePermissions();

    const canEdit = hasPermission(PERMISSIONS.TRANSPORT.VEHICLES.MANAGE);
    const canDelete = hasPermission(PERMISSIONS.TRANSPORT.VEHICLES.DELETE);

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

    const { rows, loading, fetching, error, totalElements } = useTransportBorrowedVehiclesData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
    });

    const columns = useMemo(() => getBorrowedVehicleColumns(), []);

    const deleteVehicleMutation = useMutation({
        mutationFn: (vehicleId) => deleteBorrowedVehicle(vehicleId),
        onSuccess: async () => {
            setVehicleToDelete(null);
            setAlert({ type: 'success', message: 'Vehículo prestado eliminado correctamente' });
            await queryClient.invalidateQueries({ queryKey: queryKeys.transport.borrowedVehicles() });
        },
        onError: (deleteError) => {
            setAlert({ type: 'error', message: deleteError?.response?.data?.message ?? deleteError?.message ?? 'No se pudo eliminar el vehículo prestado' });
        },
    });

    const deleting = deleteVehicleMutation.isPending;

    const handleDelete = useCallback((row) => setVehicleToDelete(row), []);
    const handleDeleteCancel = useCallback(() => {
        if (deleting) return;
        setVehicleToDelete(null);
    }, [deleting]);

    const handleDeleteConfirm = useCallback(() => {
        if (!vehicleToDelete) return;
        deleteVehicleMutation.mutate(vehicleToDelete.id);
    }, [vehicleToDelete, deleteVehicleMutation]);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                fetching={fetching}
                error={error}
                enableRowActions={canEdit || canDelete}
                onRowClick={canEdit ? (row) => onEditBorrowedVehicle(row.original) : undefined}
                renderRowActions={renderBorrowedVehicleActions({
                    onEdit: onEditBorrowedVehicle,
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
                    onColumnVisibilityChange: setColumnVisibility,
                    state: { pagination, globalFilter, columnFilters, sorting, columnVisibility },
                }}
                enableGlobalFilter
            />

            <DialogModal
                type="delete"
                open={!!vehicleToDelete}
                title="Eliminar vehículo prestado"
                message={`¿Seguro que deseas eliminar el vehículo prestado "${vehicleToDelete?.plate}"?\nEsta acción no se puede deshacer.`}
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
