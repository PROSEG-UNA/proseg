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
import Radio from '@mui/material/Radio';
import TableBase from '../../../common/components/TablaBase.jsx';
import AlertModal from '../../../common/components/AlertModal.jsx';
import { fetchRoles } from '../services/rolesService';
import { assignSingleRoleToUser, fetchRolesByUserId } from '../services/usersService';

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
    const [selectedRoleId, setSelectedRoleId] = useState(null);
    const [initialRoleId, setInitialRoleId] = useState(null);
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
                setSelectedRoleId(null);

                const [rolesResponse, assignedRoles] = await Promise.all([
                    fetchRoles({ size: 200 }),
                    fetchRolesByUserId(user.id),
                ]);
                if (ignore) return;

                const mappedRoles = (rolesResponse.content ?? []).map((role) => ({
                    id: role.id,
                    name: role.name,
                    description: role.description || '—',
                }));
                setRoles(mappedRoles);

                const assignedRoleIds = new Set((assignedRoles ?? []).map((role) => role.id));
                const preselectedRoleId = mappedRoles.find((role) => assignedRoleIds.has(role.id))?.id ?? null;
                setSelectedRoleId(preselectedRoleId);
                setInitialRoleId(preselectedRoleId);
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

    const columns = useMemo(
        () => [
            {
                id: 'selection',
                header: 'Seleccionar',
                size: 90,
                grow: false,
                Cell: ({ row }) => (
                    <Radio
                        checked={selectedRoleId === row.original.id}
                        onChange={() => setSelectedRoleId(row.original.id)}
                        value={row.original.id}
                        inputProps={{ 'aria-label': `Seleccionar rol ${row.original.name}` }}
                    />
                ),
                enableColumnFilter: false,
                enableSorting: false,
            },
            ...roleColumns,
        ],
        [selectedRoleId]
    );

    const handleAssign = async () => {
        if (!user?.id) {
            setAlert({ type: 'error', message: 'No se encontró el usuario seleccionado.' });
            return;
        }

        if (!selectedRoleId) {
            setAlert({ type: 'error', message: 'Debes seleccionar un rol para el usuario.' });
            return;
        }

        if (selectedRoleId === initialRoleId) {
            setAlert({ type: 'info', message: 'No hay cambios por guardar.' });
            return;
        }

        setSaving(true);
        try {
            await assignSingleRoleToUser(user.id, selectedRoleId);
            setInitialRoleId(selectedRoleId);
            setAlert({ type: 'success', message: 'Rol actualizado correctamente.' });
            onSaved?.();
        } catch (err) {
            setAlert({ type: 'error', message: err?.message || 'Error al actualizar el rol del usuario.' });
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
                        columns={columns}
                        data={roles}
                        loading={loading}
                        error={error}
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
