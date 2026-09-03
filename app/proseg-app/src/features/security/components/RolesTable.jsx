import { useMemo, useState, useEffect, useCallback } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRolesData } from '../hooks/useRolesData';
import { deleteRole } from '../services/rolesService';
import { getRolesColumns, renderRolesActions } from './rolesColumns.jsx';
import TableBase from '../../../common/components/TablaBase.jsx';
import RoleFormModal from './RoleFormModal.jsx';
import DialogModal from '../../../common/components/DialogModal.jsx';
import { usePermissions } from '../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../common/constants/permissions';
import { getFriendlyApiErrorMessage } from '../../../common/utils/index.js';
import { queryKeys } from '../../../common/query';

const STORAGE_KEY = 'roles-table-column-visibility';
const DEFAULT_COLUMN_VISIBILITY = {};

export default function RolesTable() {
    const [columnVisibility, setColumnVisibility] = useState(() => {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved) {
            try { return JSON.parse(saved); } catch { return DEFAULT_COLUMN_VISIBILITY; }
        }
        return DEFAULT_COLUMN_VISIBILITY;
    });
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const { rows, loading, fetching, error, totalElements } = useRolesData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
    });
    const [editingRole, setEditingRole] = useState(null);
    const [deletingRole, setDeletingRole] = useState(null);
    const [alert, setAlert] = useState(null);
    const queryClient = useQueryClient();
    const { hasPermission } = usePermissions();
    const canEditRole = hasPermission(PERMISSIONS.ROLES.UPDATE);
    const canDeleteRole = hasPermission(PERMISSIONS.ROLES.DELETE);
    const canUseActions = canEditRole || canDeleteRole;

    const persistColumnVisibility = () => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(columnVisibility));
    };

    useEffect(persistColumnVisibility, [columnVisibility]);

    const handleEdit = (row) => setEditingRole(row);
    const handleDelete = (row) => setDeletingRole(row);

    const invalidateRoles = useCallback(
        () => { void queryClient.invalidateQueries({ queryKey: queryKeys.security.roles() }); },
        [queryClient]
    );

    const deleteRoleMutation = useMutation({
        mutationFn: (roleName) => deleteRole(roleName),
        onSuccess: () => {
            setDeletingRole(null);
            invalidateRoles();
        },
        onError: (deleteError) => {
            setDeletingRole(null);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(deleteError, 'Error al eliminar el rol') });
        },
    });

    const deleting = deleteRoleMutation.isPending;

    const handleConfirmDelete = () => {
        if (!deletingRole) return;
        deleteRoleMutation.mutate(deletingRole.name);
    };

    const columns = useMemo(
        () =>
            getRolesColumns().map((column) => ({
                ...column,
                muiTableBodyCellProps: {
                    ...(column.muiTableBodyCellProps ?? {}),
                    sx: {
                        ...(column.muiTableBodyCellProps?.sx ?? {}),
                        py: 1.15,
                    },
                },
            })),
        []
    );

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                fetching={fetching}
                error={error}
                enableRowActions={canUseActions}
                renderRowActions={canUseActions ? renderRolesActions({
                    onEdit: handleEdit,
                    onDelete: handleDelete,
                    canEdit: canEditRole,
                    canDelete: canDeleteRole,
                }) : undefined}
                tableOptions={{
                    positionActionsColumn: 'last',
                    manualPagination: true,
                    rowCount: totalElements,
                    onPaginationChange: setPagination,
                    onColumnVisibilityChange: setColumnVisibility,
                    state: { pagination, columnVisibility },
                    displayColumnDefOptions: {
                        'mrt-row-actions': {
                            muiTableBodyCellProps: {
                                sx: { py: 1.15 },
                            },
                        },
                    },
                }}
                enableGlobalFilter
            />

            <RoleFormModal
                role={editingRole}
                open={!!editingRole}
                onClose={() => setEditingRole(null)}
                onSaved={invalidateRoles}
            />

            <DialogModal
                type="delete"
                open={!!deletingRole}
                title="Eliminar rol"
                message={`¿Estás seguro de que deseas eliminar el rol "${deletingRole?.name}"?\nEsta acción no se puede deshacer.`}
                onClose={() => !deleting && setDeletingRole(null)}
                onConfirm={handleConfirmDelete}
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
