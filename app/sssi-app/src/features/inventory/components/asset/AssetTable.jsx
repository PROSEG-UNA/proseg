import { useMemo, useState, useCallback } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions,
    Button, Box,
} from '@mui/material';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlined';
import TableBase from '../../../../common/components/TablaBase.jsx';
import AlertModal from '../../../../common/components/AlertModal.jsx';
import { useAssetsData } from '../../hooks/useAssetsData';
import { getAssetsColumns, renderAssetActions } from './assetColumns.jsx';
import AssetDetailPanel from './AssetDetailPanel.jsx';
import AssetFormModal from './AssetFormModal.jsx';
import { deleteAsset } from '../../services/assetsService.js';

export default function AssetTable({ refreshKey = 0, onRefresh }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [alert, setAlert] = useState(null);
    const [editAssetId, setEditAssetId] = useState(null);
    const [assetToDelete, setAssetToDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);

    const { rows, loading, error, totalElements } = useAssetsData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
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
                enableRowActions
                renderRowActions={renderAssetActions({
                    onEdit: handleEdit,
                    onDelete: handleDelete,
                })}
                tableOptions={{
                    positionActionsColumn: 'last',
                    manualPagination: true,
                    rowCount: totalElements,
                    onPaginationChange: setPagination,
                    state: { pagination },
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

            <Dialog
                open={!!assetToDelete}
                onClose={handleDeleteCancel}
                maxWidth="xs"
                fullWidth
                slotProps={{ backdrop: { sx: { backdropFilter: 'blur(4px)' } } }}
            >
                <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <DeleteOutlineIcon sx={{ color: 'error.main', fontSize: 26 }} />
                    <Box component="span" sx={{ fontWeight: 600 }}>Eliminar activo</Box>
                </DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        ¿Seguro que deseas eliminar el activo
                        {assetToDelete?.name ? <strong> &quot;{assetToDelete.name}&quot;</strong> : null}?
                        Esta acción no se puede deshacer.
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={handleDeleteCancel} disabled={deleting}>Cancelar</Button>
                    <Button
                        onClick={handleDeleteConfirm}
                        color="error"
                        variant="contained"
                        disabled={deleting}
                        loading={deleting}
                    >
                        Eliminar
                    </Button>
                </DialogActions>
            </Dialog>

            <AlertModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}
