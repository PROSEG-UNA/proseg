import { useEffect, useState } from 'react';
import { Box, Stack, TextField, Typography } from '@mui/material';
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import GeneralModal from './GeneralModal.jsx';

export default function CancelWithReasonModal({
    open,
    onClose,
    onConfirm,
    loading = false,
    title = 'Cancelar',
    subtitle,
    details = [],
    reasonLabel = 'Motivo de cancelación (opcional)',
    reasonPlaceholder = 'Describe el motivo de la cancelación…',
    confirmLabel = 'Confirmar cancelación',
    cancelLabel = 'Volver',
    maxLength = 600,
}) {
    const [reason, setReason] = useState('');

    useEffect(() => {
        function resetReasonOnOpen() {
            if (open) {
                setReason('');
            }
        }
        resetReasonOnOpen();
    }, [open]);

    function handleConfirm() {
        const trimmed = reason.trim();
        onConfirm?.(trimmed ? trimmed : null);
    }

    return (
        <GeneralModal
            open={open}
            onClose={onClose}
            maxWidth="sm"
            icon={CancelOutlinedIcon}
            title={title}
            subtitle={subtitle}
            loading={loading}
            primaryButton={{
                label: confirmLabel,
                onClick: handleConfirm,
                disabled: loading,
                loading,
            }}
            secondaryButton={{
                label: cancelLabel,
                onClick: onClose,
                disabled: loading,
            }}
        >
            <Box sx={{ p: { xs: 2.5, sm: 3 }, display: 'flex', flexDirection: 'column', gap: 2 }}>
                {details.length > 0 && (
                    <Stack spacing={1}>
                        {details.map((item) => (
                            <Box key={item.label} sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}>
                                <Typography sx={{ fontSize: 13, color: 'text.secondary', fontWeight: 600 }}>
                                    {item.label}
                                </Typography>
                                <Typography sx={{ fontSize: 13, color: 'text.primary', textAlign: 'right' }}>
                                    {item.value ?? '—'}
                                </Typography>
                            </Box>
                        ))}
                    </Stack>
                )}
                <TextField
                    label={reasonLabel}
                    placeholder={reasonPlaceholder}
                    value={reason}
                    onChange={(event) => setReason(event.target.value)}
                    multiline
                    minRows={3}
                    fullWidth
                    disabled={loading}
                    slotProps={{ htmlInput: { maxLength } }}
                />
            </Box>
        </GeneralModal>
    );
}
