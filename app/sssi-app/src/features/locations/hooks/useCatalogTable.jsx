import { useState, useCallback, useEffect, useMemo } from 'react';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import EmailIcon from '@mui/icons-material/Email';
import { useDebounce } from '../../../common/hooks/useDebounce.js';
import RowActionsMenu from '../../../common/components/RowActionsMenu.jsx';
import { usePermissions } from '../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../common/constants/permissions';
import { getFriendlyApiErrorMessage } from '../../../common/utils/index.js';
import { useCatalogData } from './useCatalogData';
import { deleteCatalogItem } from '../services/catalogService';

const getLocationPermissionGroup = (baseUrl = '') => {
    if (baseUrl.includes('/campuses') || baseUrl.includes('/locations')) {
        return PERMISSIONS.INVENTORY.LOCATIONS;
    }
    return PERMISSIONS.INVENTORY.CATALOG ?? PERMISSIONS.INVENTORY;
};

const isBuilding = (baseUrl = '') => baseUrl.includes('/buildings');
const isCampus   = (baseUrl = '') => baseUrl.includes('/campuses');

export function useCatalogTable(config, { enabled = true } = {}) {
    const { title = '', pluralTitle = '', baseUrl = '', icon = null, columns: configColumns = [] } = config ?? {};

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
    const locationPermissions = useMemo(() => getLocationPermissionGroup(baseUrl), [baseUrl]);
    const canViewCatalog = hasPermission(locationPermissions.READ)
        || hasPermission(locationPermissions.MANAGE)
        || hasPermission(locationPermissions.DELETE);
    const canManageCatalog = hasPermission(locationPermissions.MANAGE);
    const canDeleteCatalog = hasPermission(locationPermissions.DELETE);

    const canReadEmails = hasPermission(PERMISSIONS.INVENTORY.LOCATIONS?.READ)
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

    const resetState = useCallback(() => {
        setPagination({ pageIndex: 0, pageSize: 10 });
        setGlobalFilter('');
        setColumnFilters([]);
        setSorting([]);
    }, []);

    const resetPageOnFilterChange = () => {
        setPagination((prev) => (prev.pageIndex === 0 ? prev : { ...prev, pageIndex: 0 }));
    };
    useEffect(resetPageOnFilterChange, [debouncedGlobalFilter, backendFilters, backendSort]);

    const { rows, loading, error, totalElements } = useCatalogData({
        baseUrl: enabled ? baseUrl : '',
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
        refreshKey: localRefreshKey,
    });

    const handleCreate = useCallback(() => { setFormRow(null); setFormOpen(true); }, []);
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

    const isAccessDeniedError = error && (error.includes('permisos') || error.includes('403') || error.includes('Acceso denegado'));
    const hasRowActions = canEditCatalog || canRemoveCatalog || showBuildingEmailAction || showCampusEmailAction;

    return {
        config,
        title, pluralTitle, icon, columns,
        rows, loading, error, totalElements,
        pagination, setPagination,
        globalFilter, setGlobalFilter,
        columnFilters, setColumnFilters,
        sorting, setSorting,
        isAccessDeniedError,
        permissions: {
            canViewCatalog,
            canCreateCatalog,
            canEditCatalog,
            canRemoveCatalog,
            showBuildingEmailAction,
            showCampusEmailAction,
            hasRowActions,
        },
        renderRowActions,
        handleCreate,
        handleConfirmDelete,
        handleFormSaved,
        formOpen, formRow, setFormOpen, setFormRow,
        deletingRow, deleting, setDeletingRow,
        buildingEmailsRow, setBuildingEmailsRow,
        campusEmailsRow, setCampusEmailsRow,
        alert, setAlert,
        resetState,
    };
}

export default useCatalogTable;
