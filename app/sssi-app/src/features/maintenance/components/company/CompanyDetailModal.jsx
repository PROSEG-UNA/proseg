import { useEffect, useState } from 'react';
import {
    Box, Typography, Skeleton, Divider, Button, Chip, Grid,
    useTheme, useMediaQuery, Tabs, Tab,
} from '@mui/material';
import BusinessIcon from '@mui/icons-material/Business';
import PeopleIcon from '@mui/icons-material/People';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import AlternateEmailIcon from '@mui/icons-material/AlternateEmail';
import PhoneIcon from '@mui/icons-material/Phone';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import { fetchCompanyById, fetchCompanyUsers } from '../../services/company/companiesService';
import { formatDateTime } from '../../maintenanceUtils';

function InfoCard({ icon: Icon, label, value, variant = 'text' }) {
    return (
        <Box sx={{
            display: 'flex',
            gap: 1.5,
            alignItems: variant === 'text' ? 'baseline' : 'flex-start',
            p: 1.5,
            borderRadius: '10px',
            bgcolor: 'hsla(220, 20%, 50%, 0.03)',
        }}>
            {Icon && (
                <Icon sx={{
                    fontSize: 18,
                    color: 'text.disabled',
                    flexShrink: 0,
                    mt: 0.25,
                }} />
            )}
            <Box sx={{ minWidth: 0, flex: 1 }}>
                <Typography sx={{
                    fontSize: 11,
                    fontWeight: 700,
                    color: 'text.disabled',
                    letterSpacing: '0.08em',
                    textTransform: 'uppercase',
                    mb: 0.5,
                }}>
                    {label}
                </Typography>
                <Typography sx={{
                    fontSize: 13.5,
                    fontWeight: 600,
                    color: 'text.primary',
                    wordBreak: 'break-word',
                    lineHeight: 1.5,
                }}>
                    {value || '—'}
                </Typography>
            </Box>
        </Box>
    );
}

function UserCard({ user }) {
    const theme = useTheme();
    const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username || '—';

    return (
        <Box sx={{
            display: 'flex',
            flexDirection: 'column',
            gap: 0.75,
            p: 1.5,
            border: '1px solid',
            borderColor: 'divider',
            borderRadius: '10px',
            bgcolor: 'hsla(220, 20%, 50%, 0.02)',
            transition: 'all 0.2s ease',
            '&:hover': {
                bgcolor: 'hsla(220, 20%, 50%, 0.05)',
                borderColor: theme.vars.palette.tones.rose.fg,
            },
        }}>
            <Typography sx={{
                fontSize: 13,
                fontWeight: 700,
                color: 'text.primary',
            }}>
                {fullName}
            </Typography>
            {user.email && (
                <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                    <AlternateEmailIcon sx={{ fontSize: 14, color: 'text.disabled' }} />
                    <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>
                        {user.email}
                    </Typography>
                </Box>
            )}
        </Box>
    );
}

function SkeletonLoader() {
    return (
        <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'flex', flexDirection: 'column', gap: 2.5 }}>
            <Box>
                <Skeleton variant="text" width={200} height={16} sx={{ mb: 1.5 }} />
                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1.5 }}>
                    {[1, 2, 3, 4].map((i) => (
                        <Skeleton key={i} variant="rounded" height={90} />
                    ))}
                </Box>
            </Box>
            <Divider />
            <Box>
                <Skeleton variant="text" width={180} height={16} sx={{ mb: 1.5 }} />
                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1.5 }}>
                    {[1, 2, 3].map((i) => (
                        <Skeleton key={i} variant="rounded" height={85} />
                    ))}
                </Box>
            </Box>
        </Box>
    );
}

export default function CompanyDetailModal({
    open,
    onClose,
    companyId,
    onEdit,
    onManageUsers,
    canEdit = false,
    canManageUsers = false,
}) {
    const theme = useTheme();
    useMediaQuery(theme.breakpoints.down('sm'));
    const [company, setCompany] = useState(null);
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [tabIndex, setTabIndex] = useState(0);

    useEffect(() => {
        let cancelled = false;

        if (!open || !companyId) {
            if (!cancelled) {
                setCompany(null);
                setUsers([]);
                setLoading(true);
                setTabIndex(0);
            }
            return;
        }

        setLoading(true);

        Promise.all([
            fetchCompanyById(companyId),
            fetchCompanyUsers(companyId),
        ])
            .then(([companyData, usersData]) => {
                if (!cancelled) {
                    setCompany(companyData);
                    setUsers(Array.isArray(usersData) ? usersData : []);
                    setLoading(false);
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setCompany(null);
                    setUsers([]);
                    setLoading(false);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [open, companyId]);

    const handleManageUsers = () => {
        onManageUsers?.(company);
        onClose?.();
    };

    const contentSx = {
        overflowY: 'auto',
        '&::-webkit-scrollbar': { width: '5px' },
        '&::-webkit-scrollbar-track': { background: 'transparent' },
        '&::-webkit-scrollbar-thumb': {
            background: `color-mix(in srgb, ${theme.vars.palette.tones.rose.fg} 25%, transparent)`,
            borderRadius: '4px',
        },
    };

    if (!open || loading) {
        return (
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="lg"
                icon={BusinessIcon}
                title="Detalle de empresa"
                subtitle="Cargando información..."
                loading={loading}
                contentSx={contentSx}
                showCloseButton
            >
                <SkeletonLoader />
            </GeneralModal>
        );
    }

    if (!company) {
        return (
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="lg"
                icon={BusinessIcon}
                title="Detalle de empresa"
                contentSx={contentSx}
                showCloseButton
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, py: 4, textAlign: 'center' }}>
                    <Typography sx={{ color: 'text.secondary' }}>
                        No se pudo cargar la información de la empresa.
                    </Typography>
                </Box>
            </GeneralModal>
        );
    }

    return (
        <GeneralModal
            open={open}
            onClose={onClose}
            maxWidth="lg"
            icon={BusinessIcon}
            title={company.name || 'Empresa'}
            subtitle={company.legalId ? `Cédula: ${company.legalId}` : ''}
            loading={false}
            contentSx={contentSx}
            showCloseButton
            primaryButton={{
                label: 'Cerrar',
                onClick: onClose,
            }}
            footerLeft={canManageUsers ? (
                <Button
                    size="small"
                    variant="contained"
                    startIcon={<PeopleIcon sx={{ fontSize: 16 }} />}
                    onClick={handleManageUsers}
                    sx={{
                        textTransform: 'none',
                        fontWeight: 700,
                        background: `linear-gradient(135deg, ${theme.vars.palette.tones.rose.headerBg} 0%, ${theme.vars.palette.tones.rose.headerBgEnd} 100%)`,
                        boxShadow: theme.vars.palette.tones.rose.buttonShadow,
                        '&:hover': {
                            background: `linear-gradient(135deg, ${theme.vars.palette.tones.rose.hoverBg}, ${theme.vars.palette.tones.rose.hoverBg})`,
                            boxShadow: theme.vars.palette.tones.rose.buttonShadowHover,
                        },
                    }}
                >
                    Gestionar usuarios
                </Button>
            ) : undefined}
        >
            <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3 }}>
                <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2.5 }}>
                    <Tabs
                        value={tabIndex}
                        onChange={(e, v) => setTabIndex(v)}
                        variant="scrollable"
                        scrollButtons="auto"
                        allowScrollButtonsMobile
                        sx={{
                            '& .MuiTabs-indicator': {
                                background: `linear-gradient(90deg, ${theme.vars.palette.tones.rose.fg} 0%, ${theme.vars.palette.tones.rose.fg} 100%)`,
                            },
                        }}
                    >
                        <Tab
                            label="Información general"
                            icon={<BusinessIcon sx={{ fontSize: 16 }} />}
                            iconPosition="start"
                            sx={{
                                textTransform: 'none',
                                fontWeight: 600,
                                fontSize: 13.5,
                            }}
                        />
                        <Tab
                            label={`Usuarios (${users.length})`}
                            icon={<PeopleIcon sx={{ fontSize: 16 }} />}
                            iconPosition="start"
                            sx={{
                                textTransform: 'none',
                                fontWeight: 600,
                                fontSize: 13.5,
                            }}
                        />
                    </Tabs>
                </Box>
                {tabIndex === 0 && (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                        <Box>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
                                <BusinessIcon sx={{ fontSize: 18, color: 'text.disabled' }} />
                                <Typography sx={{
                                    fontSize: 11.5,
                                    fontWeight: 700,
                                    color: 'text.disabled',
                                    letterSpacing: '0.08em',
                                    textTransform: 'uppercase',
                                }}>
                                    Información de la empresa
                                </Typography>
                            </Box>
                            <Grid container spacing={1.5}>
                                <Grid item xs={12} sm={6}>
                                    <InfoCard
                                        icon={BusinessIcon}
                                        label="Nombre comercial"
                                        value={company.name}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <InfoCard
                                        label="Cédula jurídica"
                                        value={company.legalId}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <InfoCard
                                        icon={AlternateEmailIcon}
                                        label="Correo de contacto"
                                        value={company.contactEmail}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <InfoCard
                                        icon={PhoneIcon}
                                        label="Teléfono"
                                        value={company.contactPhone}
                                    />
                                </Grid>
                                <Grid item xs={12}>
                                    <InfoCard
                                        icon={LocationOnIcon}
                                        label="Dirección"
                                        value={company.address}
                                    />
                                </Grid>
                            </Grid>
                        </Box>

                        <Divider />
                        <Box>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
                                <CalendarTodayIcon sx={{ fontSize: 18, color: 'text.disabled' }} />
                                <Typography sx={{
                                    fontSize: 11.5,
                                    fontWeight: 700,
                                    color: 'text.disabled',
                                    letterSpacing: '0.08em',
                                    textTransform: 'uppercase',
                                }}>
                                    Información de auditoría
                                </Typography>
                            </Box>
                            <Grid container spacing={1.5}>
                                <Grid item xs={12} sm={6}>
                                    <InfoCard
                                        label="Creada el"
                                        value={formatDateTime(company.createdAt)}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={6}>
                                    <InfoCard
                                        label="Actualizada el"
                                        value={formatDateTime(company.updatedAt)}
                                    />
                                </Grid>
                            </Grid>
                        </Box>

                        <Divider />
                        <Box>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
                                <PeopleIcon sx={{ fontSize: 18, color: 'text.disabled' }} />
                                <Typography sx={{
                                    fontSize: 11.5,
                                    fontWeight: 700,
                                    color: 'text.disabled',
                                    letterSpacing: '0.08em',
                                    textTransform: 'uppercase',
                                }}>
                                    Estadísticas
                                </Typography>
                            </Box>
                            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                                <Chip
                                    icon={<PeopleIcon />}
                                    label={`${users.length} usuario${users.length !== 1 ? 's' : ''}`}
                                    variant="outlined"
                                    color="primary"
                                />
                            </Box>
                        </Box>
                    </Box>
                )}

                {tabIndex === 1 && (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                        {users.length === 0 ? (
                            <Box sx={{ textAlign: 'center', py: 3 }}>
                                <PeopleIcon sx={{ fontSize: 48, color: 'text.disabled', opacity: 0.5, mb: 1 }} />
                                <Typography sx={{ color: 'text.secondary' }}>
                                    No hay usuarios asociados a esta empresa.
                                </Typography>
                                {canManageUsers && (
                                    <Button
                                        size="small"
                                        variant="text"
                                        onClick={handleManageUsers}
                                        sx={{ mt: 1 }}
                                    >
                                        Agregar usuarios
                                    </Button>
                                )}
                            </Box>
                        ) : (
                            <>
                                <Typography sx={{
                                    fontSize: 12,
                                    color: 'text.secondary',
                                    mb: 1,
                                }}>
                                    {users.length} usuario{users.length !== 1 ? 's' : ''} vinculado{users.length !== 1 ? 's' : ''}
                                </Typography>
                                <Grid container spacing={1.5}>
                                    {users.map((user) => (
                                        <Grid item xs={12} sm={6} key={user.id}>
                                            <UserCard user={user} />
                                        </Grid>
                                    ))}
                                </Grid>
                            </>
                        )}
                    </Box>
                )}
            </Box>
        </GeneralModal>
    );
}



