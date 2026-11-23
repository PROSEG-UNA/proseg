import { useCallback, useEffect, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { deleteExternalVehicle } from '../../services/externalVehicles/externalVehiclesService';
import { usePersistedColumnVisibility } from '../../hooks/usePersistedColumnVisibility';
import { usePersistedPagination } from '../../hooks/usePersistedPagination.js';
import { useTransportExternalVehiclesData } from '../../hooks/useTransportExternalVehiclesData';
import { getExternalVehicleColumns, renderExternalVehicleActions } from './externalVehicleColumns.jsx';
import { queryKeys } from '../../../../common/query';

const COLUMN_VISIBILITY_STORAGE_KEY = 'external-vehicles-table-column-visibility';

const DEFAULT_COLUMN_VISIBILITY = { available: false };

const COLUMN_TO_BACKEND_KEY = {
    provider: 'provider',
    plate: 'plate',
    capacity: 'capacity',
    available: 'available',
};

export default function ExternalVehiclesTable({ onEditExternalVehicle, availabilityFilter = 'all' }) {
    const [pagination, setPagination] = usePersistedPagination('external-vehicles-table-page-size', 20);
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [columnVisibility, setColumnVisibility] = usePersistedColumnVisibility(COLUMN_VISIBILITY_STORAGE_KEY, DEFAULT_COLUMN_VISIBILITY);
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
        if (availabilityFilter !== 'all') out.available = availabilityFilter;
        return out;
    }, [debouncedColumnFilters, availabilityFilter]);

    useEffect(() => {
        setPagination((previousValue) => (previousValue.pageIndex === 0 ? previousValue : { ...previousValue, pageIndex: 0 }));
    }, [availabilityFilter, setPagination]);

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
    }, [setPagination]);

    const handleColumnFiltersChange = useCallback((updater) => {
        setColumnFilters((currentValue) => (typeof updater === 'function' ? updater(currentValue) : updater));
        setPagination((previousValue) => (previousValue.pageIndex === 0 ? previousValue : { ...previousValue, pageIndex: 0 }));
    }, [setPagination]);

    const handleSortingChange = useCallback((updater) => {
        setSorting((currentValue) => (typeof updater === 'function' ? updater(currentValue) : updater));
        setPagination((previousValue) => (previousValue.pageIndex === 0 ? previousValue : { ...previousValue, pageIndex: 0 }));
    }, [setPagination]);

    const { rows, loading, fetching, error, totalElements } = useTransportExternalVehiclesData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
    });

    const columns = useMemo(() => getExternalVehicleColumns(), []);

    const deleteVehicleMutation = useMutation({
        mutationFn: (vehicleId) => deleteExternalVehicle(vehicleId),
        onSuccess: () => {
            setVehicleToDelete(null);
            setAlert({ type: 'success', message: 'Vehículo externo eliminado correctamente' });
            void queryClient.invalidateQueries({ queryKey: queryKeys.transport.externalVehicles() });
            void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessingDrafts() });
        },
        onError: (deleteError) => {
            setVehicleToDelete(null);
            setAlert({ type: 'error', message: deleteError?.response?.data?.message ?? deleteError?.message ?? 'No se pudo eliminar el vehículo externo' });
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
                onRowClick={canEdit ? (row) => onEditExternalVehicle(row.original) : undefined}
                renderRowActions={renderExternalVehicleActions({
                    onEdit: onEditExternalVehicle,
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
                title="Eliminar vehículo externo"
                message={`¿Seguro que deseas eliminar el vehículo "${vehicleToDelete?.plate}" de ${vehicleToDelete?.provider}?\nEsta acción no se puede deshacer.`}
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
