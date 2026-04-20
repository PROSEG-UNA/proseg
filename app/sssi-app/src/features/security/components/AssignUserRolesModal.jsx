import { useEffect, useMemo, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import IconButton from '@mui/material/IconButton';
import Button from '@mui/material/Button';
import CloseIcon from '@mui/icons-material/Close';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import TableBase from '../../../common/components/TablaBase.jsx';
import AlertModal from '../../../common/components/AlertModal.jsx';
import { fetchRoles } from '../services/rolesService';
import { assignRoleToUser, removeRoleFromUser, fetchRolesByUserId } from '../services/usersService';

const roleColumns = [
    {
        accessorKey: 'name',
        header: 'Rol',
        size: 180,
        grow: true,
    },
    {
        accessorKey: 'description',
        header: 'Descripción',
        size: 260,
        grow: 2,
    },
];

export default function AssignUserRolesModal({ open, user, onClose, onSaved }) {
    const [roles, setRoles] = useState([]);
    const [selectedIds, setSelectedIds] = useState([]);
    const [initialSelectedIds, setInitialSelectedIds] = useState([]);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);
    const [alert, setAlert] = useState(null);

    useEffect(() => {
        let ignore = false;

        const loadRoles = async () => {
            if (!open || !user?.id) return;
            try {
                setLoading(true);
                setError(null);
                setSelectedIds([]);

                const [rolesResponse, assignedRoles] = await Promise.all([
                    fetchRoles(),
                    fetchRolesByUserId(user.id),
                ]);
                if (ignore) return;

                const mappedRoles = (rolesResponse ?? []).map((role) => ({
                    id: role.id,
                    name: role.name,
                    description: role.description || '—',
                }));
                setRoles(mappedRoles);

                const assignedRoleIds = new Set((assignedRoles ?? []).map((role) => role.id));
                const preselectedIds = mappedRoles
                    .filter((role) => assignedRoleIds.has(role.id))
                    .map((role) => role.id);
                setSelectedIds(preselectedIds);
                setInitialSelectedIds(preselectedIds);
            } catch (err) {
                if (!ignore) {
                    setError(err?.message || 'Error al cargar roles');
                }
            } finally {
                if (!ignore) {
                    setLoading(false);
                }
            }
        };

        void loadRoles();
        return () => {
            ignore = true;
        };
    }, [open, user?.id]);

    const rowSelection = useMemo(
        () => Object.fromEntries(selectedIds.map((id) => [id, true])),
        [selectedIds]
    );

    const handleRowSelectionChange = (updater) => {
        const next = typeof updater === 'function' ? updater(rowSelection) : updater;
        setSelectedIds(Object.keys(next).filter((key) => next[key]));
    };

    const handleAssign = async () => {
        if (!user?.id) {
            setAlert({ type: 'error', message: 'No se encontró el usuario seleccionado.' });
            return;
        }

        const initialSet = new Set(initialSelectedIds);
        const selectedSet = new Set(selectedIds);
        const rolesToAssign = selectedIds.filter((id) => !initialSet.has(id));
        const rolesToRemove = initialSelectedIds.filter((id) => !selectedSet.has(id));

        if (rolesToAssign.length === 0 && rolesToRemove.length === 0) {
            setAlert({ type: 'info', message: 'No hay cambios por guardar.' });
            return;
        }

        setSaving(true);
        try {
            await Promise.all(
                rolesToAssign.map((roleId) => assignRoleToUser(user.id, roleId))
            );

            await Promise.all(
                rolesToRemove.map((roleId) => removeRoleFromUser(user.id, roleId))
            );

            setInitialSelectedIds(selectedIds);
            setAlert({ type: 'success', message: 'Roles actualizados correctamente.' });
            onSaved?.();
        } catch (err) {
            setAlert({ type: 'error', message: err?.message || 'Error al actualizar roles del usuario.' });
        } finally {
            setSaving(false);
        }
    };

    const handleAlertClose = () => {
        if (alert?.type === 'success') {
            onClose?.();
        }
        setAlert(null);
    };

    return (
        <Dialog
            open={open}
            onClose={() => !saving && onClose?.()}
            maxWidth="md"
            fullWidth
            slotProps={{
                backdrop: { sx: { backdropFilter: 'blur(4px)' } },
            }}
        >
            <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', pb: 1 }}>
                <Typography component="span" variant="h6" fontWeight={600}>
                    Asignar roles a {user?.username || 'usuario'}
                </Typography>
                <IconButton size="small" onClick={() => onClose?.()} disabled={saving}>
                    <CloseIcon fontSize="small" />
                </IconButton>
            </DialogTitle>

            <DialogContent sx={{ pt: 1 }}>
                <Box sx={{ width: '100%' }}>
                    <TableBase
                        maxHeight={'600px'}
                        columns={roleColumns}
                        data={roles}
                        loading={loading}
                        error={error}
                        enableRowSelection
                        rowSelection={rowSelection}
                        onRowSelectionChange={handleRowSelectionChange}
                        tableOptions={{ positionToolbarAlertBanner: 'none' }}
                    />
                </Box>
            </DialogContent>

            <DialogActions sx={{ justifyContent: 'flex-start', px: 3, pb: 2 }}>
                <Button variant="contained" onClick={handleAssign} loading={saving}>
                    Guardar cambios
                </Button>
            </DialogActions>

            <AlertModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={handleAlertClose}
            />
        </Dialog>
    );
}
