import { useMemo, useState, useCallback, useEffect } from 'react';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useAssetsData } from '../../hooks/useAssetsData';
import { getAssetsColumns, renderAssetActions } from './assetColumns.jsx';
import AssetDetailPanel from './AssetDetailPanel.jsx';
import AssetFormModal from './AssetFormModal.jsx';
import { deleteAsset } from '../../services/assetsService.js';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';

const STORAGE_KEY = 'asset-table-column-visibility';

const DEFAULT_COLUMN_VISIBILITY = {
    campus: false,
    executingUnit: false,
    responsibleEmployee: false,
    responsibleEmployeeId: false,
    status: false,
    acquisitionDate: false,
    warrantyEndDate: false,
    firmwareSupportEndDate: false,
    decommissionDate: false,
    latitude: false,
    longitude: false,
};

const COLUMN_TO_BACKEND_KEY = {
    assetNumber: 'assetNumber',
    serialNumber: 'serialNumber',
    executingUnit: 'executingUnit',
    responsibleEmployee: 'responsibleEmployee',
    responsibleEmployeeId: 'responsibleEmployeeId',
    type: 'model.type.name',
    brand: 'model.brand.name',
    model: 'model.name',
    campus: 'location.floor.building.campus.name',
    location: 'location.description',
    status: 'status',
    acquisitionDate: 'acquisitionDate',
    warrantyEndDate: 'warrantyEndDate',
    firmwareSupportEndDate: 'firmwareSupportEndDate',
};

export default function AssetTable({ refreshKey = 0, onRefresh }) {
    const [columnVisibility, setColumnVisibility] = useState(() => {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved) {
            try { return JSON.parse(saved); } catch { return DEFAULT_COLUMN_VISIBILITY; }
        }
        return DEFAULT_COLUMN_VISIBILITY;
    });
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [alert, setAlert] = useState(null);
    const [editAssetId, setEditAssetId] = useState(null);
    const [assetToDelete, setAssetToDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const { hasPermission } = usePermissions();
    const canManageAssets = hasPermission(PERMISSIONS.INVENTORY.MANAGE);
    const canDeleteAssets = hasPermission(PERMISSIONS.INVENTORY.DELETE);
    const canUseActions = canManageAssets || canDeleteAssets;

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

    const persistColumnVisibility = () => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(columnVisibility));
    };

    useEffect(persistColumnVisibility, [columnVisibility]);

    const resetPageOnFilterChange = () => {
        setPagination((prev) => (prev.pageIndex === 0 ? prev : { ...prev, pageIndex: 0 }));
    };

    useEffect(resetPageOnFilterChange, [debouncedGlobalFilter, backendFilters, backendSort]);

    const { rows, loading, error, totalElements } = useAssetsData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
        refreshKey,
    });

    const columns = useMemo(
        () =>
            getAssetsColumns().map((column) => ({
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
        setEditAssetId(row.id);
    }, []);

    const handleDelete = useCallback((row) => {
        setAssetToDelete(row);
    }, []);

    const handleEditClose = useCallback(() => setEditAssetId(null), []);

    const handleEditSaved = useCallback(() => {
        setAlert({ type: 'success', message: 'Activo actualizado correctamente' });
        onRefresh?.();
    }, [onRefresh]);

    const handleDeleteCancel = useCallback(() => {
        if (deleting) return;
        setAssetToDelete(null);
    }, [deleting]);

    const handleDeleteConfirm = useCallback(async () => {
        if (!assetToDelete) return;
        setDeleting(true);
        try {
            await deleteAsset(assetToDelete.id);
            setAssetToDelete(null);
            setAlert({ type: 'success', message: 'Activo eliminado correctamente' });
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
    }, [assetToDelete, onRefresh]);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                error={error}
                enableRowActions={canUseActions}
                renderRowActions={canUseActions ? renderAssetActions({
                    onEdit: handleEdit,
                    onDelete: handleDelete,
                    canEdit: canManageAssets,
                    canDelete: canDeleteAssets,
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
                    onColumnVisibilityChange: setColumnVisibility,
                    state: { pagination, globalFilter, columnFilters, sorting, columnVisibility },
                    displayColumnDefOptions: {
                        'mrt-row-expand': {
                            muiTableBodyCellProps: {
                                sx: { borderTop: 'none', borderBottom: '1px solid', borderBottomColor: 'divider' },
                            },
                        },
                        'mrt-row-actions': {
                            muiTableBodyCellProps: {
                                sx: { py: 1.15, borderBottom: '1px solid', borderBottomColor: 'divider' },
                            },
                        },
                    },
                }}
                enableGlobalFilter
                renderDetailPanel={({ row }) => <AssetDetailPanel assetId={row.original.id} />}
            />

            <AssetFormModal
                open={!!editAssetId}
                assetId={editAssetId}
                onClose={handleEditClose}
                onSaved={handleEditSaved}
            />

            <DialogModal
                type="delete"
                open={!!assetToDelete}
                title="Eliminar activo"
                message={`¿Seguro que deseas eliminar el activo "${assetToDelete?.assetNumber}"?\nEsta acción no se puede deshacer.`}
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
