import { useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import IconButton from '@mui/material/IconButton';
import Button from '@mui/material/Button';
import CloseIcon from '@mui/icons-material/Close';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import { useEditRoleData } from '../hooks/useEditRoleData';
import { getPrivilegesColumns } from './privilegesColumns.jsx';
import TableBase from '../../../common/components/TablaBase.jsx';
import AlertModal from '../../../common/components/AlertModal.jsx';

export default function EditRoleModal({ role, open, onClose, onSaved }) {
    const { allPrivileges, selectedIds, setSelectedIds, loading, error, save } = useEditRoleData(role);
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
        if (selectedIds.length === 0) {
            setAlert({ type: 'warning', message: 'El rol debe tener al menos un privilegio asignado.' });
            return;
        }
        setSaving(true);
        try {
            await save();
            setAlert({ type: 'success', message: `El rol "${role?.name}" fue actualizado correctamente.` });
            onSaved?.();
        } catch (err) {
            setAlert({ type: 'error', message: err instanceof Error ? err.message : 'Error al guardar' });
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
                    Editar rol: {role?.name}
                </Typography>
                <IconButton size="small" onClick={onClose}>
                    <CloseIcon fontSize="small" />
                </IconButton>
            </DialogTitle>

            <DialogContent sx={{ pt: 1 }}>
                <Box sx={{ width: '100%' }}>
                    <TableBase
                        maxHeight={"600px"}
                        columns={columns}
                        data={allPrivileges}
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
                <Button variant="contained" onClick={handleSave} loading={saving}>
                    Guardar
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
