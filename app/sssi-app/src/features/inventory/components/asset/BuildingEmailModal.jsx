import { useState, useEffect, useCallback } from 'react';
import {
    Box,
    Typography,
    TextField,
    IconButton,
    Skeleton,
    Chip,
    Tooltip,
    Divider,
} from '@mui/material';
import EmailIcon from '@mui/icons-material/Email';
import AddIcon from '@mui/icons-material/Add';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import ApartmentIcon from '@mui/icons-material/Apartment';
import { useTheme } from '@mui/material/styles';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import {createBuildingEmail, deleteBuildingEmail, fetchEmailsByBuilding} from "../../services/buildingMailService.js";

function EmailSkeleton() {
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {[1, 2, 3].map((i) => (
                <Box
                    key={i}
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        px: 2,
                        py: 1.25,
                        borderRadius: '10px',
                        border: '1px solid',
                        borderColor: 'divider',
                    }}
                >
                    <Skeleton variant="text" width={200} height={18} />
                    <Skeleton variant="circular" width={28} height={28} />
                </Box>
            ))}
        </Box>
    );
}

function EmailRow({ email, canDelete, onDelete, accentColor }) {
    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                px: 2,
                py: 1.1,
                borderRadius: '10px',
                border: '1px solid',
                borderColor: 'divider',
                transition: 'border-color 0.18s ease, background 0.18s ease',
                '&:hover': {
                    borderColor: `color-mix(in srgb, ${accentColor} 30%, transparent)`,
                    background: `color-mix(in srgb, ${accentColor} 4%, transparent)`,
                },
            }}
        >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, minWidth: 0 }}>
                <EmailIcon sx={{ fontSize: 15, color: 'text.disabled', flexShrink: 0 }} />
                <Typography
                    sx={{
                        fontSize: '0.835rem',
                        fontWeight: 500,
                        color: 'text.primary',
                        fontFamily: '"Roboto Mono", monospace',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                    }}
                >
                    {email.email}
                </Typography>
            </Box>
            {canDelete && (
                <Tooltip title="Eliminar correo" placement="left">
                    <IconButton
                        size="small"
                        onClick={() => onDelete(email)}
                        sx={{
                            color: 'text.disabled',
                            flexShrink: 0,
                            '&:hover': { color: 'error.main', bgcolor: 'error.lighter' },
                        }}
                    >
                        <DeleteForeverIcon sx={{ fontSize: 17 }} />
                    </IconButton>
                </Tooltip>
            )}
        </Box>
    );
}

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function BuildingEmailsModal({ open, onClose, building }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const { hasPermission } = usePermissions();
    const canManage = hasPermission(PERMISSIONS.INVENTORY.LOCATIONS.MANAGE);
    const canDelete = hasPermission(PERMISSIONS.INVENTORY.LOCATIONS.DELETE);

    const [emails, setEmails] = useState([]);
    const [loading, setLoading] = useState(false);
    const [newEmail, setNewEmail] = useState('');
    const [emailError, setEmailError] = useState('');
    const [emailTouched, setEmailTouched] = useState(false);
    const [saving, setSaving] = useState(false);
    const [deletingEmail, setDeletingEmail] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [alert, setAlert] = useState(null);

    const loadEmails = useCallback(() => {
        if (!open || !building?.id) return;
        let cancelled = false;
        setLoading(true);
        setEmails([]);

        fetchEmailsByBuilding(building.id)
            .then((data) => { if (!cancelled) setEmails(data); })
            .catch(() => { if (!cancelled) setEmails([]); })
            .finally(() => { if (!cancelled) setLoading(false); });

        return () => { cancelled = true; };
    }, [open, building?.id]);

    useEffect(() => {
        if (!open) {
            setNewEmail('');
            setEmailError('');
            setEmailTouched(false);
            setAlert(null);
        } else {
            loadEmails();
        }
    }, [open, loadEmails]);

    const validateEmail = (value) => {
        if (!value.trim()) return 'El correo electrónico es obligatorio';
        if (!EMAIL_REGEX.test(value.trim())) return 'El formato del correo no es válido';
        return '';
    };

    const handleEmailChange = (e) => {
        const val = e.target.value;
        setNewEmail(val);
        if (emailTouched) setEmailError(validateEmail(val));
    };

    const handleEmailBlur = () => {
        setEmailTouched(true);
        setEmailError(validateEmail(newEmail));
    };

    const handleAdd = async () => {
        setEmailTouched(true);
        const err = validateEmail(newEmail);
        if (err) { setEmailError(err); return; }

        setSaving(true);
        try {
            const created = await createBuildingEmail(building.id, { email: newEmail.trim() });
            setEmails((prev) => [...prev, created]);
            setNewEmail('');
            setEmailError('');
            setEmailTouched(false);
        } catch (e) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(e, 'Error al agregar correo') });
        } finally {
            setSaving(false);
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') { e.preventDefault(); handleAdd(); }
    };

    const handleConfirmDelete = async () => {
        setDeleting(true);
        try {
            await deleteBuildingEmail(deletingEmail.id);
            setEmails((prev) => prev.filter((em) => em.id !== deletingEmail.id));
            setDeletingEmail(null);
        } catch (e) {
            setDeletingEmail(null);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(e, 'Error al eliminar correo') });
        } finally {
            setDeleting(false);
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
                icon={EmailIcon}
                title="Correos del Edificio"
                subtitle={building?.name ?? ''}
                loading={saving}
                secondaryButton={{ label: 'Cerrar', onClick: onClose }}
                maxWidth="sm"
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2, pb: 3, display: 'flex', flexDirection: 'column', gap: 2.5 }}>

                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <ApartmentIcon sx={{ fontSize: 15, color: 'text.disabled' }} />
                        <Typography sx={{ fontSize: '0.78rem', color: 'text.secondary', fontWeight: 500 }}>
                            {building?.campus?.name && (
                                <Box component="span" sx={{ color: 'text.disabled' }}>
                                    {building.campus.name} /{' '}
                                </Box>
                            )}
                            {building?.name}
                        </Typography>
                        <Chip
                            label={`${emails.length} correo${emails.length !== 1 ? 's' : ''}`}
                            size="small"
                            sx={{
                                height: 20,
                                fontSize: '0.68rem',
                                fontWeight: 600,
                                bgcolor: `color-mix(in srgb, ${accentColor} 12%, transparent)`,
                                color: accentColor,
                                border: 'none',
                                ml: 'auto',
                            }}
                        />
                    </Box>

                    <Divider />
                    {canManage && (
                        <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}>
                            <TextField
                                label="Nuevo correo electrónico"
                                value={newEmail}
                                onChange={handleEmailChange}
                                onBlur={handleEmailBlur}
                                onKeyDown={handleKeyDown}
                                fullWidth
                                size="small"
                                type="email"
                                disabled={saving}
                                error={emailTouched && !!emailError}
                                helperText={emailTouched ? (emailError || ' ') : ' '}
                                sx={fieldSx}
                                placeholder="correo@ejemplo.com"
                            />
                            <Tooltip title="Agregar correo">
                                <span>
                                    <IconButton
                                        onClick={handleAdd}
                                        disabled={saving}
                                        sx={{
                                            mt: 0.25,
                                            width: 38,
                                            height: 38,
                                            borderRadius: '10px',
                                            bgcolor: `color-mix(in srgb, ${accentColor} 12%, transparent)`,
                                            color: accentColor,
                                            border: '1px solid',
                                            borderColor: `color-mix(in srgb, ${accentColor} 25%, transparent)`,
                                            flexShrink: 0,
                                            transition: 'background 0.18s, border-color 0.18s',
                                            '&:hover': {
                                                bgcolor: `color-mix(in srgb, ${accentColor} 20%, transparent)`,
                                                borderColor: accentColor,
                                            },
                                            '&.Mui-disabled': { opacity: 0.45 },
                                        }}
                                    >
                                        <AddIcon sx={{ fontSize: 19 }} />
                                    </IconButton>
                                </span>
                            </Tooltip>
                        </Box>
                    )}
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75 }}>
                        {loading ? (
                            <EmailSkeleton />
                        ) : emails.length === 0 ? (
                            <Box
                                sx={{
                                    display: 'flex',
                                    flexDirection: 'column',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    py: 4,
                                    gap: 1,
                                    borderRadius: '12px',
                                    border: '1px dashed',
                                    borderColor: 'divider',
                                }}
                            >
                                <EmailIcon sx={{ fontSize: 28, color: 'text.disabled', opacity: 0.5 }} />
                                <Typography sx={{ fontSize: '0.8rem', color: 'text.disabled' }}>
                                    Sin correos registrados
                                </Typography>
                            </Box>
                        ) : (
                            emails.map((email) => (
                                <EmailRow
                                    key={email.id}
                                    email={email}
                                    canDelete={canDelete}
                                    onDelete={setDeletingEmail}
                                    accentColor={accentColor}
                                />
                            ))
                        )}
                    </Box>
                </Box>
            </GeneralModal>

            <DialogModal
                type="delete"
                open={!!deletingEmail}
                title="Eliminar correo"
                message={`¿Deseas eliminar "${deletingEmail?.email}"?\nEsta acción no se puede deshacer.`}
                onClose={() => !deleting && setDeletingEmail(null)}
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