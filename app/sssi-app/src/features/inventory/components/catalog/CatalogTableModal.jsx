import { useState, useCallback, useEffect, useMemo } from 'react';
import { Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import TableBase from '../../../../common/components/TablaBase.jsx';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import { useCatalogData } from '../../hooks/useCatalogData';
import { deleteCatalogItem } from '../../services/catalogService';
import CatalogFormModal from './CatalogFormModal.jsx';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import {getFriendlyApiErrorMessage} from "../../../../common/utils/index.js";

const getInventoryPermissionGroup = (baseUrl = '') => {
    if (baseUrl.includes('/sites') || baseUrl.includes('/locations')) {
        return PERMISSIONS.INVENTORY.LOCATIONS;
    }

    return PERMISSIONS.INVENTORY.CATALOG ?? PERMISSIONS.INVENTORY;
};

export default function CatalogTableModal({ open, onClose, config }) {
    const { title = '', pluralTitle = '', baseUrl = '', icon: Icon = null, columns: configColumns = [] } = config ?? {};

    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [localRefreshKey, setLocalRefreshKey] = useState(0);
    const [formOpen, setFormOpen] = useState(false);
    const [formRow, setFormRow] = useState(null);
    const [deletingRow, setDeletingRow] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [alert, setAlert] = useState(null);
    const { hasPermission } = usePermissions();
    const inventoryPermissions = useMemo(() => getInventoryPermissionGroup(baseUrl), [baseUrl]);
    const canViewCatalog = hasPermission(inventoryPermissions.READ)
        || hasPermission(inventoryPermissions.MANAGE)
        || hasPermission(inventoryPermissions.DELETE);
    const canManageCatalog = hasPermission(inventoryPermissions.MANAGE);
    const canDeleteCatalog = hasPermission(inventoryPermissions.DELETE);
    const hasCatalogAction = useCallback((action) => {
        if (!Array.isArray(config?.actions)) return true;
        return config.actions.includes(action);
    }, [config]);

    const canCreateCatalog = canManageCatalog && hasCatalogAction('create');
    const canEditCatalog = canManageCatalog && hasCatalogAction('edit');
    const canRemoveCatalog = canDeleteCatalog && hasCatalogAction('delete');

    const triggerRefresh = useCallback(() => setLocalRefreshKey((k) => k + 1), []);

    const resetOnOpen = () => {
        if (!open) return;
        setPagination({ pageIndex: 0, pageSize: 10 });
    };

    useEffect(resetOnOpen, [open]);

    const { rows, loading, error, totalElements } = useCatalogData({
        baseUrl,
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        refreshKey: localRefreshKey,
    });

    const handleCreate = () => { setFormRow(null); setFormOpen(true); };
    const handleEdit = useCallback((row) => { setFormRow(row); setFormOpen(true); }, []);
    const handleDelete = useCallback((row) => setDeletingRow(row), []);

    const handleConfirmDelete = async () => {
        setDeleting(true);
        try {
            await deleteCatalogItem(baseUrl, deletingRow.id);
            setDeletingRow(null);
            triggerRefresh();
            setAlert({ type: 'success', message: `${title} eliminado correctamente` });
        } catch (e) {
            setDeletingRow(null);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(e, 'Error al eliminar') });
        } finally {
            setDeleting(false);
        }
    };

    const handleFormSaved = () => {
        const wasEditing = !!formRow;
        setFormOpen(false);
        setFormRow(null);
        triggerRefresh();
        setAlert({
            type: 'success',
            message: wasEditing ? `${title} actualizado correctamente` : `${title} creado correctamente`,
        });
    };

    const renderRowActions = useCallback(({ row }) => {
        const actions = [
            {
                key: 'edit',
                label: 'Editar',
                icon: <EditIcon fontSize="small" />,
                hidden: !canEditCatalog,
                onClick: () => handleEdit(row.original),
            },
            {
                key: 'delete',
                label: 'Eliminar',
                icon: <DeleteIcon fontSize="small" />,
                color: 'error',
                    hidden: !canRemoveCatalog,
                onClick: () => handleDelete(row.original),
            },
        ];
        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    }, [handleEdit, handleDelete, canEditCatalog, canRemoveCatalog]);

    const columns = useMemo(
        () => configColumns.map((col) => ({
            ...col,
            muiTableBodyCellProps: {
                ...(col.muiTableBodyCellProps ?? {}),
                sx: { ...(col.muiTableBodyCellProps?.sx ?? {}), py: 1.15 },
            },
        })),
        [configColumns]
    );

    const footerLeft = (
        <Typography sx={{ fontSize: 11.5, color: 'text.disabled', fontWeight: 500 }}>
            {totalElements > 0
                ? `${totalElements} registro${totalElements !== 1 ? 's' : ''}`
                : 'Sin registros'}
        </Typography>
    );

    const isAccessDeniedError = error && (error.includes('permisos') || error.includes('403') || error.includes('Acceso denegado'));

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="md"
                fullScreenAt="md"
                icon={Icon}
                title={pluralTitle}
                subtitle={`Gestión de ${pluralTitle.toLowerCase()}`}
                loading={loading}
                footerLeft={footerLeft}
                secondaryButton={{ label: 'Cerrar', onClick: onClose }}
                primaryButton={canCreateCatalog ? { label: `Crear ${title}`, onClick: handleCreate, startIcon: <AddIcon /> } : null}
            >
                {!canViewCatalog || isAccessDeniedError ? (
                    <AccessDeniedState />
                ) : (
                    <TableBase
                        columns={columns}
                        data={rows}
                        loading={loading}
                        error={error}
                        enableRowActions={canEditCatalog || canRemoveCatalog}
                        renderRowActions={canEditCatalog || canRemoveCatalog ? renderRowActions : undefined}
                        tableOptions={{
                            positionActionsColumn: 'last',
                            manualPagination: true,
                            rowCount: totalElements,
                            onPaginationChange: setPagination,
                            state: { pagination },
                            displayColumnDefOptions: {
                                'mrt-row-actions': {
                                    muiTableBodyCellProps: { sx: { py: 1.15 } },
                                },
                            },
                        }}
                        enableGlobalFilter
                    />
                )}
            </GeneralModal>

            <CatalogFormModal
                open={formOpen}
                onClose={() => { setFormOpen(false); setFormRow(null); }}
                onSaved={handleFormSaved}
                config={config}
                row={formRow}
            />

            <DialogModal
                type="delete"
                open={!!deletingRow}
                title={`Eliminar ${title}`}
                message={`¿Estás seguro de que deseas eliminar "${deletingRow?.name}"?\nEsta acción no se puede deshacer.`}
                onClose={() => !deleting && setDeletingRow(null)}
                onConfirm={handleConfirmDelete}
                confirmLabel="Eliminar"
            />

            <DialogModal open={!!alert} type={alert?.type} message={alert?.message} onClose={() => setAlert(null)} />
        </>
    );
}
