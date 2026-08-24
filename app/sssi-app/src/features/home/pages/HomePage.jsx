import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    Container,
    Divider,
    Grid,
    Stack,
    Typography,
} from '@mui/material';
import StorageIcon from '@mui/icons-material/Storage';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import PendingActionsIcon from '@mui/icons-material/PendingActions';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircle';
import HighlightOffIcon from '@mui/icons-material/HighlightOff';
import AssignmentIndOutlinedIcon from '@mui/icons-material/AssignmentIndOutlined';

import InsightsOutlinedIcon from '@mui/icons-material/InsightsOutlined';
import KeyboardArrowDownRoundedIcon from '@mui/icons-material/KeyboardArrowDownRounded';
import CleaningServicesOutlinedIcon from '@mui/icons-material/CleaningServicesOutlined';
import ConfirmationNumberIcon from '@mui/icons-material/ConfirmationNumber';
import EmailIcon from '@mui/icons-material/Email';
import { FeatureCard } from '../../../common/components/FeatureCard.jsx';
import { APP_CONFIG } from '../../../config/appConfig.js';
import { usePermissions } from '../../../common/hooks/usePermissions.js';
import { PERMISSIONS } from '../../../common/constants/permissions.js';
import { useNavSections } from '../../../common/components/Sidebar/useNavSections';
import { fetchMaintenanceTicketsDashboardSummary } from '../../maintenance/services/ticketsService.js';

const dashboardCardSx = {
    borderRadius: 2.5,
    height: '100%',
    borderColor: 'divider',
    backgroundColor: 'background.paper',
    boxShadow: '0px 1px 3px rgba(15, 23, 42, 0.08)',
};

function SectionHeading({ title, subtitle }) {
    return (
        <Box sx={{ mb: 2 }}>
            <Typography sx={{ fontSize: { xs: 18, md: 22 }, fontWeight: 800, color: 'primary.icon' }}>
                {title}
            </Typography>
            {subtitle ? (
                <Typography sx={{ fontSize: 13, color: 'text.secondary', mt: 0.4 }}>
                    {subtitle}
                </Typography>
            ) : null}
        </Box>
    );
}

function KpiCard({ title, value, caption, icon, accent = 'primary.main' }) {
    return (
        <Card
            variant="outlined"
            sx={{
                ...dashboardCardSx,
                minHeight: 148,
            }}
        >
            <CardContent
                sx={{
                    p: 2.25,
                    '&:last-child': { pb: 2.25 },
                    display: 'grid',
                    gridTemplateColumns: '1fr auto',
                    gridTemplateRows: 'auto auto 1fr',
                    gridTemplateAreas: `
                        "title icon"
                        "value icon"
                        "caption caption"
                    `,
                    rowGap: 0.75,
                    columnGap: 1.5,
                    height: '100%',
                }}
            >
                <Typography sx={{ gridArea: 'title', fontSize: 12, color: 'text.secondary', fontWeight: 600 }}>
                    {title}
                </Typography>
                <Typography sx={{ gridArea: 'value', fontSize: { xs: 30, md: 34 }, fontWeight: 800, lineHeight: 1.05, color: 'text.primary' }}>
                    {value}
                </Typography>
                <Box
                    sx={{
                        gridArea: 'icon',
                        width: 40,
                        height: 40,
                        borderRadius: 2,
                        display: 'grid',
                        placeItems: 'center',
                        backgroundColor: 'action.hover',
                        color: accent,
                        border: '1px solid',
                        borderColor: 'divider',
                        alignSelf: 'start',
                    }}
                >
                    {icon}
                </Box>
                <Typography sx={{ gridArea: 'caption', fontSize: 12, color: 'text.secondary', lineHeight: 1.45, mt: 0.5 }}>
                    {caption}
                </Typography>
            </CardContent>
        </Card>
    );
}

function MetricList({ title, items = [], color = 'primary.main' }) {
    const total = useMemo(() => items.reduce((acc, item) => acc + (item.count || 0), 0), [items]);

    return (
        <Card variant="outlined" sx={dashboardCardSx}>
            <CardContent>
                <Typography sx={{ fontWeight: 800, fontSize: 15, mb: 1.5 }}>{title}</Typography>

                {items.length === 0 ? (
                    <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>Sin datos</Typography>
                ) : (
                    <Stack spacing={1.2}>
                        {items.map((item) => {
                            const percent = total > 0 ? Math.round((item.count / total) * 100) : 0;
                            return (
                                <Box key={`${title}-${item.key}`}>
                                    <Stack direction="row" justifyContent="space-between" sx={{ mb: 0.6 }}>
                                        <Typography sx={{ fontSize: 13 }}>{item.label}</Typography>
                                        <Typography sx={{ fontSize: 13, fontWeight: 700 }}>
                                            {item.count} ({percent}%)
                                        </Typography>
                                    </Stack>
                                    <Box
                                        sx={(theme) => ({
                                            width: '100%',
                                            height: 8,
                                            borderRadius: 999,
                                            backgroundColor: theme.vars.palette.action.hover,
                                            overflow: 'hidden',
                                        })}
                                    >
                                        <Box
                                            sx={{
                                                width: `${percent}%`,
                                                height: '100%',
                                                backgroundColor: color,
                                                borderRadius: 999,
                                            }}
                                        />
                                    </Box>
                                </Box>
                            );
                        })}
                    </Stack>
                )}
            </CardContent>
        </Card>
    );
}

function InsightProgress({ label, value, helper, color = 'primary' }) {
    return (
        <Box>
            <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 0.7 }}>
                <Typography sx={{ fontSize: 13, fontWeight: 600 }}>{label}</Typography>
                <Chip size="small" label={`${value}%`} color={color} variant="outlined" />
            </Stack>
            <Box sx={{ height: 7, borderRadius: 100, mb: 0.4, background: 'action.hover' }}>
                <Box sx={{ width: `${Math.min(Math.max(value, 0), 100)}%`, height: '100%', background: (t) => t.palette[color]?.main || t.palette.primary.main, borderRadius: 100 }} />
            </Box>
            <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>{helper}</Typography>
        </Box>
    );
}

export function HomePage() {
    const navigate = useNavigate();
    const statsSectionRef = useRef(null);
    const { hasAnyPermission } = usePermissions();
    const { sections } = useNavSections();
    const [showStats, setShowStats] = useState(false);
    const [summary, setSummary] = useState(null);
    const [summaryError, setSummaryError] = useState('');

    const canSeeTicketsDashboard = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.TICKETS.READ,
        PERMISSIONS.MAINTENANCE.TICKETS.VIEW_ALL,
        PERMISSIONS.MAINTENANCE.TICKETS.CREATE,
        PERMISSIONS.MAINTENANCE.TICKETS.EDIT,
    ]);
    const loadingSummary = canSeeTicketsDashboard && summary === null && summaryError === '';


    const kpiCards = useMemo(() => {
        if (!summary) return [];
        return [
            {
                key: 'pending',
                title: 'Pendientes',
                value: summary.pendingTickets ?? 0,
                caption: 'Abiertos, en progreso o reabiertos',
                icon: <PendingActionsIcon fontSize="small" />,
                accent: 'warning.main',
            },
            {
                key: 'overdue',
                title: 'Vencidos',
                value: summary.overdueTickets ?? 0,
                caption: 'Pendientes por encima del SLA interno',
                icon: <WarningAmberIcon fontSize="small" />,
                accent: 'error.main',
            },
            {
                key: 'resolved',
                title: 'Resueltos',
                value: summary.resolvedTickets ?? 0,
                caption: `${summary.resolutionRate ?? 0}% de resolucion`,
                icon: <CheckCircleOutlineIcon fontSize="small" />,
                accent: 'success.main',
            },
            {
                key: 'cancelled',
                title: 'Cancelados',
                value: summary.cancelledTickets ?? 0,
                caption: 'Incidencias cerradas por cancelacion',
                icon: <HighlightOffIcon fontSize="small" />,
                accent: 'text.secondary',
            },
            {
                key: 'unassigned',
                title: 'Sin asignar',
                value: summary.unassignedTickets ?? 0,
                caption: 'Tickets sin tecnico asignado',
                icon: <AssignmentIndOutlinedIcon fontSize="small" />,
                accent: 'info.main',
            },
            {
                key: 'recent',
                title: 'Ultimos 7 dias',
                value: summary.recentTicketsLast7Days ?? 0,
                caption: `Promedio resolucion: ${summary.averageResolutionHours ?? 0}h`,
            icon: <CheckCircleOutlineIcon fontSize="small" />,
                accent: 'primary.main',
            },

        ];
    }, [summary]);

    const statisticalRatios = useMemo(() => {
        if (!summary || !summary.totalTickets) {
            return {
                backlogRate: 0,
                overdueRate: 0,
                assignmentRate: 0,
                cancellationRate: 0,
            };
        }
        const total = summary.totalTickets;
        return {
            backlogRate: Math.round(((summary.pendingTickets ?? 0) * 100) / total),
            overdueRate: Math.round(((summary.overdueTickets ?? 0) * 100) / total),
            assignmentRate: Math.round((((total - (summary.unassignedTickets ?? 0)) * 100) / total)),
            cancellationRate: Math.round(((summary.cancelledTickets ?? 0) * 100) / total),
        };
    }, [summary]);

    const topTechnicians = useMemo(
        () => (summary?.byTechnician ?? []).filter((item) => item.count > 0).slice(0, 5),
        [summary]
    );
    const topCompanies = useMemo(
        () => (summary?.byCompany ?? []).filter((item) => item.count > 0).slice(0, 5),
        [summary]
    );
    const handleScrollToStats = () => {
        if (!showStats) {
            setShowStats(true);
            return;
        }
        statsSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    };


    return (
        <Box sx={{ minHeight: '100dvh', display: 'flex', flexDirection: 'column' }}>
            <Container maxWidth="lg" sx={{ pt: { xs: 6, md: 9 }, pb: { xs: 4, md: 5 } }}>
                <Box sx={{ maxWidth: 580 }}>
                    <Typography
                        sx={{
                            fontWeight: 800,
                            fontSize: { xs: '2rem', sm: '2.6rem', md: '3rem' },
                            letterSpacing: '-0.03em',
                            lineHeight: 1.1,
                            color: 'text.primary',
                            mb: 2,
                        }}
                    >
                        ¡Bienvenido a{' '}
                        <Box
                            component="span"
                            sx={(t) => ({ color: t.vars.palette.tones.rose.fg })}
                        >
                            {APP_CONFIG.name}
                        </Box>
                        !
                    </Typography>
                    <Typography
                        sx={{
                            fontSize: { xs: '0.9rem', sm: '1rem' },
                            color: 'text.secondary',
                            lineHeight: 1.75,
                            maxWidth: 520,
                        }}
                    >
                        Panel inicial con indicadores operativos para monitorear incidencias y
                        acelerar la toma de decisiones del equipo.
                    </Typography>
                </Box>
            </Container>

            <Container maxWidth="lg" sx={{ pb: { xs: 6, md: 8 }, flex: 1, order: 1 }}>
                <Box sx={{ mb: 2.5, display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <Box
                        sx={(t) => ({
                            width: 20, height: 2, borderRadius: '2px',
                            background: t.vars.palette.tones.rose.fg,
                            flexShrink: 0,
                        })}
                    />
                    <Typography
                        sx={{
                            fontSize: '0.72rem',
                            fontWeight: 800,
                            letterSpacing: '0.1em',
                            textTransform: 'uppercase',
                            color: 'text.disabled',
                        }}
                    >
                        Módulos disponibles
                    </Typography>
                </Box>

                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2.5 }}>
                    {(() => {
                        const hasPath = (path) => sections && sections.some((s) => (s.items || []).some((i) => i.path === path));
                        const cards = [];

                        const pushCard = (key, node) => {
                            cards.push(
                                <Box key={key} sx={{ display: 'flex', flex: { xs: '0 0 100%', sm: '0 0 50%', md: '0 0 calc(33.333% - 16px)' }, boxSizing: 'border-box' }}>
                                    <Box sx={{ width: '100%', display: 'flex' }}>
                                        {node}
                                    </Box>
                                </Box>
                            );
                        };

                        if (hasPath('/inventario/activos')) {
                            pushCard('activos', (
                                <FeatureCard
                                    fullHeight
                                    icon={<StorageIcon fontSize="inherit" />}
                                    title="Gestión de Activos"
                                    description="Administra todos los activos de la organización"
                                    buttonLabel="Ir a Activos"
                                    onNavigate={() => navigate('/inventario/activos')}
                                />
                            ));
                        }

                        if (hasPath('/seguridad/usuarios')) {
                            pushCard('usuarios', (
                                <FeatureCard
                                    fullHeight
                                    icon={<PeopleIcon fontSize="inherit" />}
                                    title="Gestión de Usuarios"
                                    description="Administra usuarios y su estado dentro del sistema"
                                    buttonLabel="Ir a Usuarios"
                                    onNavigate={() => navigate('/seguridad/usuarios')}
                                />
                            ));
                        }

                        if (hasPath('/seguridad/roles')) {
                            pushCard('roles', (
                                <FeatureCard
                                    fullHeight
                                    icon={<VerifiedUserIcon fontSize="inherit" />}
                                    title="Gestión de Roles"
                                    description="Administra roles y permisos de acceso"
                                    buttonLabel="Ir a Roles"
                                    onNavigate={() => navigate('/seguridad/roles')}
                                />
                            ));
                        }

                        if (hasPath('/transporte/depuracion')) {
                            pushCard('depuracion', (
                                <FeatureCard
                                    fullHeight
                                    icon={<CleaningServicesOutlinedIcon fontSize="inherit" />}
                                    title="Transporte"
                                    description="Depuración de giras"
                                    buttonLabel="Ir a Depuración"
                                    onNavigate={() => navigate('/transporte/depuracion')}
                                />
                            ));
                        }

                        if (hasPath('/mantenimiento/tickets')) {
                            pushCard('tickets', (
                                <FeatureCard
                                    fullHeight
                                    icon={<ConfirmationNumberIcon fontSize="inherit" />}
                                    title="Mantenimiento"
                                    description="Gestión de tickets y seguimiento"
                                    buttonLabel="Ir a Tickets"
                                    onNavigate={() => navigate('/mantenimiento/tickets')}
                                />
                            ));
                        }

                        if (hasPath('/ubicaciones/correos')) {
                            pushCard('correos', (
                                <FeatureCard
                                    fullHeight
                                    icon={<EmailIcon fontSize="inherit" />}
                                    title="Ubicaciones"
                                    description="Gestión de correos institucionales"
                                    buttonLabel="Ir a Correos"
                                    onNavigate={() => navigate('/ubicaciones/correos')}
                                />
                            ));
                        }

                        if (cards.length === 0) {
                            return (
                                <Box sx={{ width: '100%' }}>
                                    <Alert severity="info">No hay módulos disponibles para su cuenta.</Alert>
                                </Box>
                            );
                        }

                        return cards;
                    })()}
                </Box>
            </Container>
        </Box>
    );
}

export default HomePage;
