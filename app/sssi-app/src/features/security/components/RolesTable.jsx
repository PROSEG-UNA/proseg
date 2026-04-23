import { useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import { useRolesData } from '../hooks/useRolesData';
import { deleteRole } from '../services/rolesService';
import { getRolesColumns, renderRolesActions } from './rolesColumns.jsx';
import TableBase from '../../../common/components/TablaBase.jsx';
import RoleFormModal from './RoleFormModal.jsx';
import AlertModal from '../../../common/components/AlertModal.jsx';

export default function RolesTable({ refreshKey, onRefresh }) {
    const { rows, loading, error } = useRolesData(refreshKey);
    const [editingRole, setEditingRole] = useState(null);
    const [deletingRole, setDeletingRole] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [alert, setAlert] = useState(null); // { type, message }

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
            setAlert({ type: 'error', message: err instanceof Error ? err.message : 'Error al eliminar el rol' });
        } finally {
            setDeleting(false);
        }
    };

    const columns = getRolesColumns();

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
                }}
                enableGlobalFilter
            />

            <RoleFormModal
                role={editingRole}
                open={!!editingRole}
                onClose={() => setEditingRole(null)}
                onSaved={onRefresh}
            />

            <Dialog
                open={!!deletingRole}
                onClose={() => !deleting && setDeletingRole(null)}
                maxWidth="xs"
                fullWidth
                slotProps={{
                    backdrop: { sx: { backdropFilter: 'blur(4px)' } },
                }}
            >
                <DialogTitle>Eliminar rol</DialogTitle>
                <DialogContent>
                    <Typography>
                        ¿Estás seguro de que deseas eliminar el rol <strong>{deletingRole?.name}</strong>? Esta acción no se puede deshacer.
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setDeletingRole(null)} disabled={deleting}>
                        Cancelar
                    </Button>
                    <Button variant="contained" color="error" onClick={handleConfirmDelete} loading={deleting}>
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
