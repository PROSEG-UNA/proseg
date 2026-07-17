import { useCallback, useMemo, useState } from 'react';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { deleteTour } from '../../services/tours/toursService';
import { useTransportToursData } from '../../hooks/useTransportToursData';
import { getTourColumns, renderTourActions } from './tourColumns.jsx';

const COLUMN_TO_BACKEND_KEY = {
    name: 'name',
    origin: 'origin',
    destination: 'destination',
    driverName: 'driver.name',
    vehiclePlate: 'vehicle.plate',
    statusRaw: 'status',
};

export default function ToursTable({ refreshKey = 0, onRefresh, onEditTour }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [alert, setAlert] = useState(null);
    const [tourToDelete, setTourToDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const { hasPermission } = usePermissions();

    const canEdit = hasPermission(PERMISSIONS.TRANSPORT.TOURS.MANAGE);
    const canDelete = hasPermission(PERMISSIONS.TRANSPORT.TOURS.DELETE);

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

    const { rows, loading, error, totalElements } = useTransportToursData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
        refreshKey,
    });

    const columns = useMemo(() => getTourColumns(), []);

    const handleDelete = useCallback((row) => setTourToDelete(row), []);
    const handleDeleteCancel = useCallback(() => {
        if (deleting) return;
        setTourToDelete(null);
    }, [deleting]);

    const handleDeleteConfirm = useCallback(async () => {
        if (!tourToDelete) return;
        setDeleting(true);
        try {
            await deleteTour(tourToDelete.id);
            setTourToDelete(null);
            setAlert({ type: 'success', message: 'Gira eliminada correctamente' });
            onRefresh?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo eliminar la gira' });
        } finally {
            setDeleting(false);
        }
    }, [tourToDelete, onRefresh]);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                error={error}
                enableRowActions={canEdit || canDelete}
                renderRowActions={renderTourActions({
                    onEdit: onEditTour,
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
                            endDate: false,
                        },
                    },
                }}
                enableGlobalFilter
            />

            <DialogModal
                type="delete"
                open={!!tourToDelete}
                title="Eliminar gira"
                message={`¿Seguro que deseas eliminar la gira "${tourToDelete?.name}"?\nEsta acción no se puede deshacer.`}
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
