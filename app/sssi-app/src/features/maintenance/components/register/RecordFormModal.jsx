import { useEffect, useState } from 'react';
import { Box, TextField, Typography, useTheme } from '@mui/material';
import AddCircleOutlinedIcon from '@mui/icons-material/AddCircleOutlined';
import BuildIcon from '@mui/icons-material/Build';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { createMaintenanceRecord } from '../../services/register/registerService';
import { formatAssetIdentity } from '../../../../common/utils/formatters.js';

export default function RecordFormModal({ open, onClose, onSaved, registerId, asset }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const [description, setDescription] = useState('');
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null);

    const resetState = () => {
        setDescription('');
        setError('');
        setSaving(false);
        setAlert(null);
    };

    useEffect(() => {
        if (!open) resetState();
    }, [open]);

    const handleSave = async () => {
        const trimmed = description.trim();
        if (!trimmed) {
            setError('La descripción es requerida');
            return;
        }

        setSaving(true);
        try {
            await createMaintenanceRecord(registerId, { assetId: asset?.id, description: trimmed });
            onSaved?.();
            onClose?.();
        } catch (err) {
            setAlert({ type: 'error', message: err?.response?.data?.message ?? err?.message ?? 'No se pudo registrar el mantenimiento' });
        } finally {
            setSaving(false);
        }
    };

    const fieldSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': { borderColor: 'divider' },
            '&:hover fieldset': { borderColor: `color-mix(in srgb, ${accentColor} 50%, transparent)` },
            '&.Mui-focused fieldset': { borderColor: accentColor },
        },
        '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="sm"
                icon={BuildIcon}
                title="Registrar mantenimiento"
                subtitle={formatAssetIdentity(asset)}
                loading={saving}
                secondaryButton={{ label: 'Cancelar', onClick: onClose, disabled: saving }}
                primaryButton={{
                    label: saving ? 'Guardando…' : 'Registrar',
                    onClick: handleSave,
                    disabled: saving,
                    startIcon: <AddCircleOutlinedIcon />,
                }}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                    <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>
                        Describe el mantenimiento realizado a este activo.
                    </Typography>
                    <TextField
                        label="Descripción"
                        value={description}
                        onChange={(e) => {
                            setDescription(e.target.value);
                            if (error) setError('');
                        }}
                        required
                        fullWidth
                        size="small"
                        disabled={saving}
                        multiline
                        minRows={4}
                        error={!!error}
                        helperText={error || ' '}
                        sx={fieldSx}
                    />
                </Box>
            </GeneralModal>

            <DialogModal open={!!alert} type={alert?.type} message={alert?.message} onClose={() => setAlert(null)} />
        </>
    );
}
