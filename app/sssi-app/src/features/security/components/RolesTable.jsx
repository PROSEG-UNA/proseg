import { useMemo, useState } from 'react';
import { useRolesData } from '../hooks/useRolesData';
import { deleteRole } from '../services/rolesService';
import { getRolesColumns, renderRolesActions } from './rolesColumns.jsx';
import TableBase from '../../../common/components/TablaBase.jsx';
import RoleFormModal from './RoleFormModal.jsx';
import DialogModal from '../../../common/components/DialogModal.jsx';

export default function RolesTable({ refreshKey, onRefresh }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const { rows, loading, error, totalElements } = useRolesData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        refreshKey,
    });
    const [editingRole, setEditingRole] = useState(null);
    const [deletingRole, setDeletingRole] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [alert, setAlert] = useState(null);

    const handleEdit = (row) => setEditingRole(row);
    const handleDelete = (row) => setDeletingRole(row);

    const handleConfirmDelete = async () => {
        setDeleting(true);
        try {
            await deleteRole(deletingRole.name);
            setDeletingRole(null);
            onRefresh();
        } catch (err) {
            setDeletingRole(null);
            setAlert({ type: 'error', message: err.response?.data?.message ?? err.message ?? 'Error al eliminar el rol' });
        } finally {
            setDeleting(false);
        }
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
                error={error}
                enableRowActions
                renderRowActions={renderRolesActions({
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
                onSaved={onRefresh}
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
