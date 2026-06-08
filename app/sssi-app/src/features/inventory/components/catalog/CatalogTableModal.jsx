import { useState, useCallback, useEffect, useMemo } from 'react';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import EmailIcon from '@mui/icons-material/Email';
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
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import BuildingEmailsModal from "../asset/BuildingEmailModal.jsx";
import CampusEmailsModal from "../asset/CampusEmailModal.jsx";

const getInventoryPermissionGroup = (baseUrl = '') => {
    if (baseUrl.includes('/campuses') || baseUrl.includes('/locations')) {
        return PERMISSIONS.INVENTORY.LOCATIONS;
    }
    return PERMISSIONS.INVENTORY.CATALOG ?? PERMISSIONS.INVENTORY;
};

const isBuilding = (baseUrl = '') => baseUrl.includes('/buildings');
const isCampus   = (baseUrl = '') => baseUrl.includes('/campuses');

export default function CatalogTableModal({ open, onClose, config }) {
    const { title = '', pluralTitle = '', baseUrl = '', icon: Icon = null, columns: configColumns = [] } = config ?? {};

    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [localRefreshKey, setLocalRefreshKey] = useState(0);
    const [formOpen, setFormOpen] = useState(false);
    const [formRow, setFormRow] = useState(null);
    const [deletingRow, setDeletingRow] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [alert, setAlert] = useState(null);

    const [buildingEmailsRow, setBuildingEmailsRow] = useState(null);
    const [campusEmailsRow, setCampusEmailsRow] = useState(null);

    const { hasPermission } = usePermissions();
    const inventoryPermissions = useMemo(() => getInventoryPermissionGroup(baseUrl), [baseUrl]);
    const canViewCatalog = hasPermission(inventoryPermissions.READ)
        || hasPermission(inventoryPermissions.MANAGE)
        || hasPermission(inventoryPermissions.DELETE);
    const canManageCatalog = hasPermission(inventoryPermissions.MANAGE);
    const canDeleteCatalog = hasPermission(inventoryPermissions.DELETE);

    // Email permissions (ubicaciones)
    const canReadEmails   = hasPermission(PERMISSIONS.INVENTORY.LOCATIONS?.READ)
        || hasPermission(PERMISSIONS.INVENTORY.LOCATIONS?.MANAGE)
        || hasPermission(PERMISSIONS.INVENTORY.LOCATIONS?.DELETE);

    const hasCatalogAction = useCallback((action) => {
        if (!Array.isArray(config?.actions)) return true;
        return config.actions.includes(action);
    }, [config]);

    const canCreateCatalog = canManageCatalog && hasCatalogAction('create');
    const canEditCatalog   = canManageCatalog && hasCatalogAction('edit');
    const canRemoveCatalog = canDeleteCatalog && hasCatalogAction('delete');

    const columnToBackendKey = config?.columnToBackendKey ?? {};

    const debouncedGlobalFilter  = useDebounce(globalFilter, 350);
    const debouncedColumnFilters = useDebounce(columnFilters, 350);

    const backendFilters = useMemo(() => {
        const out = {};
        debouncedColumnFilters.forEach(({ id, value }) => {
            const key = columnToBackendKey[id];
            if (!key || value === null || value === undefined || value === '') return;
            out[key] = value;
        });
        return out;
    }, [debouncedColumnFilters, columnToBackendKey]);

    const backendSort = useMemo(
        () => sorting.map(({ id, desc }) => {
            const key = columnToBackendKey[id] ?? id;
            return `${key},${desc ? 'desc' : 'asc'}`;
        }),
        [sorting, columnToBackendKey]
    );

    const triggerRefresh = useCallback(() => setLocalRefreshKey((k) => k + 1), []);

    const resetOnOpen = () => {
        if (!open) return;
        setPagination({ pageIndex: 0, pageSize: 10 });
        setGlobalFilter('');
        setColumnFilters([]);
        setSorting([]);
    };

    useEffect(resetOnOpen, [open]);

    const resetPageOnFilterChange = () => {
        setPagination((prev) => (prev.pageIndex === 0 ? prev : { ...prev, pageIndex: 0 }));
    };
    useEffect(resetPageOnFilterChange, [debouncedGlobalFilter, backendFilters, backendSort]);

    const { rows, loading, error, totalElements } = useCatalogData({
        baseUrl,
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
        refreshKey: localRefreshKey,
    });

    const handleCreate = () => { setFormRow(null); setFormOpen(true); };
    const handleEdit   = useCallback((row) => { setFormRow(row); setFormOpen(true); }, []);
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

    const showBuildingEmailAction = isBuilding(baseUrl) && canReadEmails;
    const showCampusEmailAction   = isCampus(baseUrl)   && canReadEmails;

    const renderRowActions = useCallback(({ row }) => {
        const actions = [
            {
                key: 'emails-building',
                label: 'Ver correos',
                icon: <EmailIcon fontSize="small" />,
                hidden: !showBuildingEmailAction,
                onClick: () => setBuildingEmailsRow(row.original),
            },
            {
                key: 'emails-campus',
                label: 'Ver correos del campus',
                icon: <EmailIcon fontSize="small" />,
                hidden: !showCampusEmailAction,
                onClick: () => setCampusEmailsRow(row.original),
            },
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
    }, [handleEdit, handleDelete, canEditCatalog, canRemoveCatalog, showBuildingEmailAction, showCampusEmailAction]);

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

    const hasRowActions = canEditCatalog || canRemoveCatalog || showBuildingEmailAction || showCampusEmailAction;

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
                        enableRowActions={hasRowActions}
                        renderRowActions={hasRowActions ? renderRowActions : undefined}
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

            <BuildingEmailsModal
                open={!!buildingEmailsRow}
                onClose={() => setBuildingEmailsRow(null)}
                building={buildingEmailsRow}
            />

            <CampusEmailsModal
                open={!!campusEmailsRow}
                onClose={() => setCampusEmailsRow(null)}
                campus={campusEmailsRow}
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