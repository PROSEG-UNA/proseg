import { useState, useEffect, useCallback } from 'react';
import {
    Box,
    Typography,
    Skeleton,
    Chip,
    Divider,
    Collapse,
    IconButton,
} from '@mui/material';
import EmailIcon from '@mui/icons-material/Email';
import ApartmentIcon from '@mui/icons-material/Apartment';
import BusinessIcon from '@mui/icons-material/Business';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import { useTheme } from '@mui/material/styles';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import {fetchEmailsByCampus} from "../../services/buildingMailService.js";

function EmailSkeleton() {
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, mt: 1 }}>
            {[1, 2].map((i) => (
                <Box key={i}>
                    <Skeleton variant="rounded" width={160} height={22} sx={{ mb: 1 }} />
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75, pl: 2 }}>
                        <Skeleton variant="text" width={220} height={18} />
                        <Skeleton variant="text" width={180} height={18} />
                    </Box>
                </Box>
            ))}
        </Box>
    );
}

function BuildingEmailGroup({ buildingName, emails, accentColor }) {
    const [open, setOpen] = useState(true);

    return (
        <Box>
            <Box
                onClick={() => setOpen((v) => !v)}
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1,
                    cursor: 'pointer',
                    py: 0.75,
                    borderRadius: '8px',
                    px: 1,
                    mx: -1,
                    transition: 'background 0.15s',
                    '&:hover': { bgcolor: `color-mix(in srgb, ${accentColor} 5%, transparent)` },
                    userSelect: 'none',
                }}
            >
                <ApartmentIcon sx={{ fontSize: 14, color: accentColor, flexShrink: 0 }} />
                <Typography sx={{ fontSize: '0.8rem', fontWeight: 700, color: 'text.primary', flex: 1 }}>
                    {buildingName}
                </Typography>
                <Chip
                    label={emails.length}
                    size="small"
                    sx={{
                        height: 18,
                        fontSize: '0.66rem',
                        fontWeight: 700,
                        bgcolor: `color-mix(in srgb, ${accentColor} 12%, transparent)`,
                        color: accentColor,
                        border: 'none',
                    }}
                />
                <IconButton size="small" sx={{ p: 0.25, color: 'text.disabled' }}>
                    {open ? <ExpandLessIcon sx={{ fontSize: 15 }} /> : <ExpandMoreIcon sx={{ fontSize: 15 }} />}
                </IconButton>
            </Box>

            <Collapse in={open}>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, pl: 2.5, pt: 0.5, pb: 0.75 }}>
                    {emails.map((email) => (
                        <Box
                            key={email.id}
                            sx={{ display: 'flex', alignItems: 'center', gap: 1 }}
                        >
                            <EmailIcon sx={{ fontSize: 13, color: 'text.disabled', flexShrink: 0 }} />
                            <Typography
                                sx={{
                                    fontSize: '0.815rem',
                                    fontWeight: 500,
                                    color: 'text.secondary',
                                    fontFamily: '"Roboto Mono", monospace',
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap',
                                }}
                            >
                                {email.email}
                            </Typography>
                        </Box>
                    ))}
                </Box>
            </Collapse>
        </Box>
    );
}

export default function CampusEmailsModal({ open, onClose, campus }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const [emails, setEmails] = useState([]);
    const [loading, setLoading] = useState(false);

    const loadEmails = useCallback(() => {
        if (!open || !campus?.id) return;
        let cancelled = false;
        setLoading(true);
        setEmails([]);

        fetchEmailsByCampus(campus.id)
            .then((data) => { if (!cancelled) setEmails(data); })
            .catch(() => { if (!cancelled) setEmails([]); })
            .finally(() => { if (!cancelled) setLoading(false); });

        return () => { cancelled = true; };
    }, [open, campus?.id]);

    useEffect(() => {
        if (open) loadEmails();
    }, [open, loadEmails]);

    const grouped = emails.reduce((acc, email) => {
        const bid = email.building?.id ?? 'unknown';
        const bname = email.building?.name ?? 'Edificio desconocido';
        if (!acc[bid]) acc[bid] = { name: bname, emails: [] };
        acc[bid].emails.push(email);
        return acc;
    }, {});

    const groups = Object.values(grouped);
    const totalEmails = emails.length;

    return (
        <GeneralModal
            open={open}
            onClose={onClose}
            icon={EmailIcon}
            title="Correos del Campus"
            subtitle={campus?.name ?? ''}
            loading={loading}
            secondaryButton={{ label: 'Cerrar', onClick: onClose }}
            maxWidth="sm"
        >
            <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2, pb: 3, display: 'flex', flexDirection: 'column', gap: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <BusinessIcon sx={{ fontSize: 15, color: 'text.disabled' }} />
                    <Typography sx={{ fontSize: '0.78rem', color: 'text.secondary', fontWeight: 500 }}>
                        {campus?.name}
                    </Typography>
                    {!loading && (
                        <Chip
                            label={`${totalEmails} correo${totalEmails !== 1 ? 's' : ''}`}
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
                    )}
                </Box>

                <Divider />

                {loading ? (
                    <EmailSkeleton />
                ) : groups.length === 0 ? (
                    <Box
                        sx={{
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            py: 5,
                            gap: 1,
                            borderRadius: '12px',
                            border: '1px dashed',
                            borderColor: 'divider',
                        }}
                    >
                        <EmailIcon sx={{ fontSize: 28, color: 'text.disabled', opacity: 0.5 }} />
                        <Typography sx={{ fontSize: '0.8rem', color: 'text.disabled' }}>
                            No hay correos registrados en este campus
                        </Typography>
                    </Box>
                ) : (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.25 }}>
                        {groups.map((group, i) => (
                            <Box key={group.name}>
                                <BuildingEmailGroup
                                    buildingName={group.name}
                                    emails={group.emails}
                                    accentColor={accentColor}
                                />
                                {i < groups.length - 1 && <Divider sx={{ my: 0.5, opacity: 0.5 }} />}
                            </Box>
                        ))}
                    </Box>
                )}
            </Box>
        </GeneralModal>
    );
}
