import { useEffect, useMemo, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
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
import { useTheme, useMediaQuery, Divider, LinearProgress, alpha } from '@mui/material';
import ManageAccountsIcon from '@mui/icons-material/ManageAccounts';

const RED = {
    50:  '#fff1f2',
    400: '#f87171',
    600: '#dc2626',
    700: '#b91c1c',
    800: '#991b1b',
    900: '#7f1d1d',
};

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
    const theme = useTheme();
    const isDark = theme.palette.mode === 'dark';
    const isSmall = useMediaQuery(theme.breakpoints.down('md'));

    const [roles, setRoles] = useState([]);
    const [selectedRoleId, setSelectedRoleId] = useState(null);
    const [initialRoleId, setInitialRoleId] = useState(null);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);
    const [alert, setAlert] = useState(null);

    const headerGradient = isDark
        ? `linear-gradient(135deg, ${RED[900]} 0%, ${RED[800]} 100%)`
        : `linear-gradient(135deg, ${RED[600]} 0%, ${RED[800]} 100%)`;
    const accentColor = isDark ? RED[400] : RED[600];

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
            slotProps={{ backdrop: { sx: { backdropFilter: 'blur(3px)' } } }}
            PaperProps={{
                sx: {
                    maxHeight: isSmall ? '100vh' : '92vh',
                    height: isSmall ? '100vh' : 'auto',
                    borderRadius: isSmall ? 0 : '16px',
                    overflow: 'hidden',
                    display: 'flex',
                    flexDirection: 'column',
                    boxShadow: isDark
                        ? '0 24px 48px rgba(0,0,0,0.55), 0 0 0 1px rgba(220,38,38,0.3)'
                        : '0 24px 48px rgba(0,0,0,0.14)',
                    bgcolor: 'background.paper',
                },
            }}
        >
            <Box sx={{ flexShrink: 0, background: headerGradient, px: { xs: 2.5, sm: 3 }, py: 2.25, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <Box sx={{ width: 38, height: 38, borderRadius: '10px', background: 'rgba(255,255,255,0.15)', border: '1px solid rgba(255,255,255,0.22)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <ManageAccountsIcon sx={{ color: '#fff', fontSize: 20 }} />
                    </Box>
                    <Box>
                        <Typography sx={{ color: '#fff', fontWeight: 800, fontSize: { xs: 15, sm: 15.5 }, lineHeight: 1.2, letterSpacing: '-0.01em' }}>
                            Asignar rol
                        </Typography>
                        <Typography sx={{ color: 'rgba(255,255,255,0.65)', fontSize: { xs: 11, sm: 11.5 } }}>
                            Seleccione el rol que tendrá el usuario
                        </Typography>
                    </Box>
                </Box>
                <IconButton onClick={onClose} size="small" sx={{ color: 'rgba(255,255,255,0.75)', border: '1px solid rgba(255,255,255,0.2)', p: 0.625, '&:hover': { bgcolor: 'rgba(255,255,255,0.15)', color: '#fff' } }}>
                    <CloseIcon sx={{ fontSize: 16 }} />
                </IconButton>
            </Box>

            {saving && (
                <LinearProgress sx={{ flexShrink: 0, height: 2, bgcolor: alpha(accentColor, 0.15), '& .MuiLinearProgress-bar': { bgcolor: accentColor } }} />
            )}

            <DialogContent sx={{ p: 0, flex: 1, minHeight: 0, overflowY: 'auto', display: 'flex', flexDirection: 'column', bgcolor: 'background.paper' }}>
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 2, flexShrink: 0 }}>
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

            <Divider sx={{ flexShrink: 0 }} />

            <Box
                sx={{
                    flexShrink: 0,
                    px: { xs: 2.5, sm: 3 }, py: 1.75,
                    borderTop: '1px solid', borderColor: 'divider',
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    bgcolor: isDark ? alpha('#000', 0.25) : alpha(RED[50], 0.7),
                }}
            >
                <Typography sx={{ fontSize: 11.5, color: 'text.disabled', fontWeight: 500 }}>
                    {selectedRoleId ? 'Rol seleccionado' : 'Sin rol seleccionado'}
                </Typography>
                <Box sx={{ display: 'flex', gap: 1 }}>
                    <Button
                        onClick={onClose}
                        variant="outlined"
                        size="small"
                        sx={{ borderColor: 'divider', color: 'text.secondary', textTransform: 'none', fontWeight: 600, fontSize: 12.5, borderRadius: '8px', '&:hover': { borderColor: accentColor, color: accentColor, bgcolor: alpha(accentColor, 0.05) } }}
                    >
                        Cancelar
                    </Button>
                    <Button
                        onClick={handleAssign}
                        disabled={saving}
                        variant="contained"
                        size="small"
                        sx={{
                            background: headerGradient,
                            textTransform: 'none', fontWeight: 700, fontSize: 12.5, borderRadius: '8px',
                            boxShadow: `0 4px 14px ${alpha(RED[600], isDark ? 0.4 : 0.3)}`,
                            px: 2.5, letterSpacing: '0.01em',
                            '&:hover': {
                                background: isDark
                                    ? `linear-gradient(135deg, ${RED[800]}, ${RED[700]})`
                                    : `linear-gradient(135deg, ${RED[700]}, ${RED[900]})`,
                                boxShadow: `0 6px 18px ${alpha(RED[600], 0.4)}`,
                            },
                            '&:disabled': { opacity: 0.55 },
                        }}
                    >
                        {saving ? 'Guardando…' : 'Guardar cambios'}
                    </Button>
                </Box>
            </Box>

            <AlertModal open={!!alert} type={alert?.type} message={alert?.message} onClose={() => setAlert(null)} />
        </Dialog>
    );
}
