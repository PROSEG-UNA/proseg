import { useRef } from 'react';
import { Dialog, Box, Button, Typography } from '@mui/material';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import ErrorOutlinedIcon from '@mui/icons-material/ErrorOutlined';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';

const TYPES = {
    success:     { Icon: CheckCircleOutlinedIcon, colorKey: 'success', defaultTitle: 'Éxito' },
    error:       { Icon: WarningAmberIcon,        colorKey: 'error',   defaultTitle: 'Error' },
    delete:      { Icon: ErrorOutlinedIcon,       colorKey: 'error',   defaultTitle: 'Eliminar registros' },
    warning:     { Icon: WarningAmberIcon,        colorKey: 'warning', defaultTitle: 'Advertencia' },
    info:        { Icon: InfoOutlinedIcon,        colorKey: 'info',    defaultTitle: 'Información' },
};

const DELETE_TYPES = new Set(['delete']);

const paperSx = (t) => ({
    borderRadius: '16px',
    border: '1px solid',
    borderColor: 'divider',
    backdropFilter: 'blur(14px) saturate(140%)',
    WebkitBackdropFilter: 'blur(14px) saturate(140%)',
    background: 'hsl(220, 30%, 100%)',
    boxShadow: '0 8px 32px hsla(220, 20%, 10%, 0.12)',
    ...t.applyStyles('dark', {
        background: 'linear-gradient(180deg, hsl(228, 16%, 11%) 0%, hsl(228, 16%, 9%) 100%)',
        boxShadow: '0 8px 32px hsla(220, 20%, 0%, 0.4)',
    }),
});

const cancelBtnSx = (t) => ({
    textTransform: 'none',
    fontWeight: 600,
    fontSize: '0.85rem',
    borderRadius: '10px',
    px: 2,
    py: 0.75,
    width: { xs: '100%', sm: 'auto' },
    color: 'text.secondary',
    border: '1px solid',
    borderColor: 'divider',
    transition: 'color 0.2s ease, background 0.2s ease, border-color 0.2s ease',
    '&:hover': {
        color: 'text.primary',
        bgcolor: 'hsla(220, 20%, 50%, 0.08)',
        borderColor: 'text.disabled',
    },
    ...t.applyStyles('dark', {
        '&:hover': {
            bgcolor: 'hsla(220, 20%, 80%, 0.06)',
            borderColor: 'text.disabled',
        },
    }),
});

const makeConfirmBtnSx = (colorKey) => (t) => ({
    textTransform: 'none',
    fontWeight: 700,
    fontSize: '0.85rem',
    borderRadius: '10px',
    px: 2.5,
    py: 0.75,
    width: { xs: '100%', sm: 'auto' },
    color: t.vars.palette[colorKey].main,
    background: `color-mix(in srgb, ${t.vars.palette[colorKey].main} 10%, transparent)`,
    boxShadow: 'none',
    transition: 'background 0.2s ease, transform 0.22s ease',
    '&:hover': {
        background: `color-mix(in srgb, ${t.vars.palette[colorKey].main} 16%, transparent)`,
        boxShadow: 'none',
        transform: 'translateY(-1px)',
    },
    ...t.applyStyles('dark', {
        color: t.vars.palette[colorKey].light,
        background: `color-mix(in srgb, ${t.vars.palette[colorKey].main} 22%, transparent)`,
        '&:hover': {
            background: `color-mix(in srgb, ${t.vars.palette[colorKey].main} 30%, transparent)`,
        },
    }),
});

export default function DialogModal({
    type = 'delete',
    title,
    message,
    open,
    onClose,
    onConfirm,
    confirmLabel = 'Sí, continuar',
    cancelLabel,
}) {
    const frozen = useRef({ type, title, message });
    if (open) frozen.current = { type, title, message };
    const { type: frozenType, title: frozenTitle, message: frozenMessage } = frozen.current;

    const { Icon, colorKey, defaultTitle } = TYPES[frozenType] ?? TYPES.warning;
    const isDelete = DELETE_TYPES.has(frozenType);
    const resolvedCancelLabel = cancelLabel ?? (onConfirm ? 'Cancelar' : 'Aceptar');

    return (
        <Dialog
            open={open}
            onClose={onClose}
            maxWidth="xs"
            fullWidth
            slotProps={{
                backdrop: { sx: { backdropFilter: 'blur(6px)' } },
                paper: { sx: paperSx },
            }}
        >
            {isDelete ? (
                <Box sx={{ px: 2.5, pt: 3, pb: 2, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 1.5 }}>
                    <Box
                        sx={(t) => ({
                            width: 56, height: 56,
                            borderRadius: '14px',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            background: `color-mix(in srgb, ${t.vars.palette[colorKey].main} 12%, transparent)`,
                            ...t.applyStyles('dark', {
                                background: `color-mix(in srgb, ${t.vars.palette[colorKey].main} 24%, transparent)`,
                            }),
                        })}
                    >
                        <DeleteForeverOutlinedIcon sx={(t) => ({ fontSize: 26, color: t.vars.palette[colorKey].main, ...t.applyStyles('dark', { color: t.vars.palette[colorKey].light }) })} />
                    </Box>
                    <Typography sx={{ fontWeight: 700, fontSize: '1rem', letterSpacing: '-0.01em', color: 'text.primary', lineHeight: 1.3, textAlign: 'center' }}>
                        {frozenTitle ?? defaultTitle}
                    </Typography>
                </Box>
            ) : (
                <Box sx={{ px: 2.5, pt: 2.5, pb: 1.75, display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <Box
                        sx={(t) => ({
                            width: 44, height: 44, flexShrink: 0,
                            borderRadius: '12px',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            background: `color-mix(in srgb, ${t.vars.palette[colorKey].main} 12%, transparent)`,
                            ...t.applyStyles('dark', {
                                background: `color-mix(in srgb, ${t.vars.palette[colorKey].main} 24%, transparent)`,
                            }),
                        })}
                    >
                        <Icon sx={(t) => ({ fontSize: 22, color: t.vars.palette[colorKey].main, ...t.applyStyles('dark', { color: t.vars.palette[colorKey].light }) })} />
                    </Box>
                    <Typography sx={{ fontWeight: 700, fontSize: '1rem', letterSpacing: '-0.01em', color: 'text.primary', lineHeight: 1.3 }}>
                        {frozenTitle ?? defaultTitle}
                    </Typography>
                </Box>
            )}

            <Box sx={{ px: isDelete ? 3 : 2.5, pt: 1.5, pb: 1.5 }}>
                <Typography sx={{ fontSize: '0.875rem', color: 'text.secondary', lineHeight: 1.65, whiteSpace: 'pre-line', textAlign: isDelete ? 'center' : 'left' }}>
                    {frozenMessage}
                </Typography>
            </Box>

            <Box sx={(t) => ({
                px: 2.5, pt: 2.5, pb: 2.5,
                display: 'flex', justifyContent: 'flex-end', gap: 1,
                flexDirection: { xs: 'column-reverse', sm: 'row' },
                borderTop: '1px solid',
                borderColor: 'divider',
                bgcolor: 'background.paper',
                ...t.applyStyles('dark', { bgcolor: 'background.paper' }),
            })}>
                <Button disableElevation onClick={onClose} sx={cancelBtnSx}>
                    {resolvedCancelLabel}
                </Button>
                {onConfirm && (
                    <Button
                        variant="contained"
                        disableElevation
                        onClick={onConfirm}
                        autoFocus
                        sx={makeConfirmBtnSx(colorKey)}
                    >
                        {confirmLabel}
                    </Button>
                )}
            </Box>
        </Dialog>
    );
}
