import { useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import IconButton from '@mui/material/IconButton';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import CloseIcon from '@mui/icons-material/Close';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import { useCreateRoleData } from '../hooks/useCreateRoleData';
import { getPrivilegesColumns } from './privilegesColumns.jsx';
import TableBase from '../../../common/components/TablaBase.jsx';
import AlertModal from '../../../common/components/AlertModal.jsx';

export default function CreateRoleModal({ open, onClose, onSaved }) {
    const { allPrivileges, selectedIds, setSelectedIds, roleName, setRoleName, loading, error, save } = useCreateRoleData();
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null); // { type, message }

    const columns = getPrivilegesColumns();

    const rowSelection = Object.fromEntries(
        selectedIds.map((id) => [id, true])
    );

    const handleRowSelectionChange = (updater) => {
        const next = typeof updater === 'function' ? updater(rowSelection) : updater;
        setSelectedIds(Object.keys(next).filter((key) => next[key]));
    };

    const handleSave = async () => {
        if (!roleName.trim()) {
            setAlert({ type: 'warning', message: 'El nombre del rol no puede estar vacío.' });
            return;
        }
        if (selectedIds.length === 0) {
            setAlert({ type: 'warning', message: 'El rol debe tener al menos un privilegio asignado.' });
            return;
        }
        setSaving(true);
        try {
            await save();
            setAlert({ type: 'success', message: `El rol "${roleName}" fue creado correctamente.` });
            onSaved?.();
        } catch (err) {
            setAlert({ type: 'error', message: err instanceof Error ? err.message : 'Error al crear el rol' });
        } finally {
            setSaving(false);
        }
    };

    const handleAlertClose = () => {
        if (alert?.type === 'success') onClose();
        setAlert(null);
    };

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="md"
            fullWidth
            slotProps={{
                backdrop: { sx: { backdropFilter: 'blur(4px)' } },
            }}
        >
            <DialogTitle sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', pb: 1 }}>
                <Typography component="span" variant="h6" fontWeight={600}>
                    Crear rol
                </Typography>
                <IconButton size="small" onClick={onClose}>
                    <CloseIcon fontSize="small" />
                </IconButton>
            </DialogTitle>

            <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, overflow: 'visible' }}>
                <Box sx={{ pt: 1 }}>
                    <TextField
                        label="Nombre del rol"
                        value={roleName}
                        onChange={(e) => setRoleName(e.target.value)}
                        size="small"
                        fullWidth
                        autoFocus
                        sx={{
                            '& .MuiInputBase-input': { color: 'primary.icon' },
                            '& .MuiInputLabel-root': { color: 'primary.icon' },
                            '& .MuiInputLabel-root.Mui-focused': { color: 'primary.icon' },
                            '& .MuiOutlinedInput-notchedOutline': { borderColor: 'primary.icon' },
                            '& .MuiOutlinedInput-root:hover .MuiOutlinedInput-notchedOutline': { borderColor: 'primary.icon' },
                            '& .MuiOutlinedInput-root.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: 'primary.icon' },
                        }}
                    />
                </Box>
                <Box>
                    <TableBase
                        columns={columns}
                        data={allPrivileges}
                        loading={loading}
                        error={error}
                        enableRowSelection
                        rowSelection={rowSelection}
                        onRowSelectionChange={handleRowSelectionChange}
                        tableOptions={{ positionToolbarAlertBanner: 'none' }}
                        maxHeight={"600px"}
                    />
                </Box>
            </DialogContent>

            <DialogActions sx={{ justifyContent: 'flex-start', px: 3, pb: 2 }}>
                <Button variant="contained" onClick={handleSave} loading={saving}>
                    Crear
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