import { useCallback, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { deleteDriver } from '../../services/drivers/driversService';
import { useTransportDriversData } from '../../hooks/useTransportDriversData';
import { getDriverColumns, renderDriverActions } from './driverColumns.jsx';
import { queryKeys } from '../../../../common/query';

const COLUMN_TO_BACKEND_KEY = {
    fullName: 'name',
    documentId: 'documentId',
    licenseNumber: 'licenseNumber',
    phone: 'phone',
    email: 'email',
    statusRaw: 'status',
};

export default function DriversTable({ onEditDriver }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [alert, setAlert] = useState(null);
    const [driverToDelete, setDriverToDelete] = useState(null);
    const queryClient = useQueryClient();
    const { hasPermission } = usePermissions();

    const canEdit = hasPermission(PERMISSIONS.TRANSPORT.DRIVERS.MANAGE);
    const canDelete = hasPermission(PERMISSIONS.TRANSPORT.DRIVERS.DELETE);

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

    const { rows, loading, fetching, error, totalElements } = useTransportDriversData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
    });

    const columns = useMemo(() => getDriverColumns(), []);

    const deleteDriverMutation = useMutation({
        mutationFn: (driverId) => deleteDriver(driverId),
        onSuccess: async () => {
            setDriverToDelete(null);
            setAlert({ type: 'success', message: 'Chofer eliminado correctamente' });
            await queryClient.invalidateQueries({ queryKey: queryKeys.transport.drivers() });
        },
        onError: (deleteError) => {
            setAlert({ type: 'error', message: deleteError?.response?.data?.message ?? deleteError?.message ?? 'No se pudo eliminar el chofer' });
        },
    });

    const deleting = deleteDriverMutation.isPending;

    const handleDelete = useCallback((row) => setDriverToDelete(row), []);
    const handleDeleteCancel = useCallback(() => {
        if (deleting) return;
        setDriverToDelete(null);
    }, [deleting]);

    const handleDeleteConfirm = useCallback(() => {
        if (!driverToDelete) return;
        deleteDriverMutation.mutate(driverToDelete.id);
    }, [driverToDelete, deleteDriverMutation]);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                fetching={fetching}
                error={error}
                enableRowActions={canEdit || canDelete}
                renderRowActions={renderDriverActions({
                    onEdit: onEditDriver,
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
                            phone: false,
                        },
                    },
                }}
                enableGlobalFilter
            />

            <DialogModal
                type="delete"
                open={!!driverToDelete}
                title="Eliminar chofer"
                message={`¿Seguro que deseas eliminar al chofer "${driverToDelete?.fullName}"?\nEsta acción no se puede deshacer.`}
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
