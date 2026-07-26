import { useCallback, useMemo, useState } from 'react';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { deleteVehicle } from '../../services/vehicles/vehiclesService';
import { useTransportVehiclesData } from '../../hooks/useTransportVehiclesData';
import { getVehicleColumns, renderVehicleActions } from './vehicleColumns.jsx';

const COLUMN_TO_BACKEND_KEY = {
    plate: 'plate',
    brand: 'brand',
    model: 'model',
    year: 'year',
    capacity: 'capacity',
    statusRaw: 'status',
};

export default function VehiclesTable({ refreshKey = 0, onRefresh, onEditVehicle }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [alert, setAlert] = useState(null);
    const [vehicleToDelete, setVehicleToDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);
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

    const { rows, loading, error, totalElements } = useTransportVehiclesData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
        refreshKey,
    });

    const columns = useMemo(() => getVehicleColumns(), []);

    const handleDelete = useCallback((row) => setVehicleToDelete(row), []);
    const handleDeleteCancel = useCallback(() => {
        if (deleting) return;
        setVehicleToDelete(null);
    }, [deleting]);

    const handleDeleteConfirm = useCallback(async () => {
        if (!vehicleToDelete) return;
        setDeleting(true);
        try {
            await deleteVehicle(vehicleToDelete.id);
            setVehicleToDelete(null);
            setAlert({ type: 'success', message: 'Vehículo eliminado correctamente' });
            onRefresh?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo eliminar el vehículo' });
        } finally {
            setDeleting(false);
        }
    }, [vehicleToDelete, onRefresh]);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                error={error}
                enableRowActions={canEdit || canDelete}
                renderRowActions={renderVehicleActions({
                    onEdit: onEditVehicle,
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
                open={!!vehicleToDelete}
                title="Eliminar vehículo"
                message={`¿Seguro que deseas eliminar el vehículo "${vehicleToDelete?.plate}"?\nEsta acción no se puede deshacer.`}
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
