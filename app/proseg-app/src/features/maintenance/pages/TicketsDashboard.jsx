import { useEffect, useMemo, useState } from 'react';
import {
    Box,
    Card,
    CardContent,
    Chip,
    CircularProgress,
    Container,
    Divider,
    Grid,
    Stack,
    Typography,
} from '@mui/material';
import PendingActionsIcon from '@mui/icons-material/PendingActions';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircle';
import HighlightOffIcon from '@mui/icons-material/HighlightOff';
import AssignmentIndOutlinedIcon from '@mui/icons-material/AssignmentIndOutlined';
import InsightsOutlinedIcon from '@mui/icons-material/InsightsOutlined';
import { fetchMaintenanceTicketsDashboardSummary } from '../services/ticketsService.js';
import { panelSurfaceSx } from '../../../common/theme/sxStyles';

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
            <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', mb: 0.7 }}>
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

export default function TicketsDashboard() {
    const [summary, setSummary] = useState(null);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let mounted = true;
        setLoading(true);
        fetchMaintenanceTicketsDashboardSummary()
            .then((res) => { if (mounted) setSummary(res); })
            .catch(() => { if (mounted) setError('No se pudo cargar el dashboard inicial de tickets.'); })
            .finally(() => { if (mounted) setLoading(false); });
        return () => { mounted = false; };
    }, []);

    const kpiCards = useMemo(() => {
        if (!summary) return [];
        return [
            { key: 'pending', title: 'Pendientes', value: summary.pendingTickets ?? 0, caption: 'Abiertos, en progreso o reabiertos', icon: <PendingActionsIcon fontSize="small" />, accent: 'warning.main' },
            { key: 'overdue', title: 'Vencidos', value: summary.overdueTickets ?? 0, caption: 'Pendientes por encima del SLA interno', icon: <WarningAmberIcon fontSize="small" />, accent: 'error.main' },
            { key: 'resolved', title: 'Resueltos', value: summary.resolvedTickets ?? 0, caption: `${summary.resolutionRate ?? 0}% de resolucion`, icon: <CheckCircleOutlineIcon fontSize="small" />, accent: 'success.main' },
            { key: 'cancelled', title: 'Cancelados', value: summary.cancelledTickets ?? 0, caption: 'Incidencias cerradas por cancelacion', icon: <HighlightOffIcon fontSize="small" />, accent: 'text.secondary' },
            { key: 'unassigned', title: 'Sin asignar', value: summary.unassignedTickets ?? 0, caption: 'Tickets sin tecnico asignado', icon: <AssignmentIndOutlinedIcon fontSize="small" />, accent: 'info.main' },
            { key: 'recent', title: 'Ultimos 7 dias', value: summary.recentTicketsLast7Days ?? 0, caption: `Promedio resolucion: ${summary.averageResolutionHours ?? 0}h`, icon: <InsightsOutlinedIcon fontSize="small" />, accent: 'primary.main' },
        ];
    }, [summary]);

    const statisticalRatios = useMemo(() => {
        if (!summary || !summary.totalTickets) return { backlogRate: 0, overdueRate: 0, assignmentRate: 0, cancellationRate: 0 };
        const total = summary.totalTickets;
        return {
            backlogRate: Math.round(((summary.pendingTickets ?? 0) * 100) / total),
            overdueRate: Math.round(((summary.overdueTickets ?? 0) * 100) / total),
            assignmentRate: Math.round((((total - (summary.unassignedTickets ?? 0)) * 100) / total)),
            cancellationRate: Math.round(((summary.cancelledTickets ?? 0) * 100) / total),
        };
    }, [summary]);

    const topTechnicians = useMemo(() => (summary?.byTechnician ?? []).filter((item) => item.count > 0).slice(0, 5), [summary]);
    const topCompanies = useMemo(() => (summary?.byCompany ?? []).filter((item) => item.count > 0).slice(0, 5), [summary]);

    return (
        <Box>
            <Container maxWidth="xl" sx={{ pt: 3, pb: 3 }}>
                <SectionHeading title="Dashboard de Tickets" subtitle="Vista ejecutiva de carga operativa y distribucion de tickets." />

                {loading ? (
                    <Box sx={{ py: 3, display: 'flex', alignItems: 'center', gap: 1.5 }}>
                        <CircularProgress size={20} />
                        <Typography sx={{ fontSize: 14, color: 'text.secondary' }}>Cargando dashboard inicial...</Typography>
                    </Box>
                ) : null}

                {error ? <Box sx={{ mb: 2 }}><Typography color="warning.main">{error}</Typography></Box> : null}

                {summary ? (
                    <Stack spacing={2.5}>
                        <Grid container spacing={3}>
                            {kpiCards.map((card) => (
                                <Grid item xs={12} sm={6} md={4} lg={3} key={card.key}>
                                    <KpiCard title={card.title} value={card.value} caption={card.caption} icon={card.icon} accent={card.accent} />
                                </Grid>
                            ))}
                        </Grid>

                        <Grid container spacing={3}>
                            <Grid item xs={12} md={8}>
                                <Card variant="outlined" sx={dashboardCardSx}>
                                    <CardContent>
                                        <Typography sx={{ fontWeight: 800, fontSize: 15, mb: 1.5 }}>Area estadistica</Typography>

                                        <Grid container spacing={2}>
                                            <Grid item xs={12} sm={6}>
                                                <InsightProgress label="Carga en backlog" value={statisticalRatios.backlogRate} helper="Porcentaje del total que sigue pendiente." color="warning" />
                                            </Grid>
                                            <Grid item xs={12} sm={6}>
                                                <InsightProgress label="Riesgo de vencimiento" value={statisticalRatios.overdueRate} helper="Tickets que ya superaron el umbral interno." color="error" />
                                            </Grid>
                                            <Grid item xs={12} sm={6}>
                                                <InsightProgress label="Cobertura de asignacion" value={statisticalRatios.assignmentRate} helper="Tickets actualmente asignados a tecnico." color="success" />
                                            </Grid>
                                            <Grid item xs={12} sm={6}>
                                                <InsightProgress label="Tasa de cancelacion" value={statisticalRatios.cancellationRate} helper="Participacion de tickets cancelados." color="info" />
                                            </Grid>
                                        </Grid>

                                        <Divider sx={{ my: 2 }} />

                                        <Grid container spacing={2}>
                                            <Grid item xs={12} sm={6}>
                                                <Typography sx={{ fontSize: 13, color: 'text.secondary', mb: 0.4 }}>Tickets totales</Typography>
                                                <Typography sx={{ fontSize: 28, fontWeight: 900, lineHeight: 1.1 }}>{summary.totalTickets ?? 0}</Typography>
                                            </Grid>
                                            <Grid item xs={12} sm={6}>
                                                <Typography sx={{ fontSize: 13, color: 'text.secondary', mb: 0.4 }}>Promedio de resolucion</Typography>
                                                <Typography sx={{ fontSize: 28, fontWeight: 900, lineHeight: 1.1 }}>{summary.averageResolutionHours ?? 0}h</Typography>
                                            </Grid>
                                        </Grid>
                                    </CardContent>
                                </Card>
                            </Grid>

                            <Grid item xs={12} md={4}>
                                <Card variant="outlined" sx={dashboardCardSx}>
                                    <CardContent>
                                        <Typography sx={{ fontWeight: 800, fontSize: 15, mb: 1.5 }}>Top de atencion</Typography>
                                        <Stack spacing={1.1}>
                                            <Typography sx={{ fontSize: 12, color: 'text.secondary', textTransform: 'uppercase', letterSpacing: '0.08em' }}>Tecnicos</Typography>
                                            {topTechnicians.length === 0 ? (
                                                <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>Sin datos</Typography>
                                            ) : topTechnicians.map((item, index) => (
                                                <Stack key={`tech-${item.key}`} direction="row" justifyContent="space-between">
                                                    <Typography sx={{ fontSize: 13 }}>{index + 1}. {item.label}</Typography>
                                                    <Chip size="small" label={item.count} />
                                                </Stack>
                                            ))}

                                            <Divider sx={{ my: 0.8 }} />

                                            <Typography sx={{ fontSize: 12, color: 'text.secondary', textTransform: 'uppercase', letterSpacing: '0.08em' }}>Empresas</Typography>
                                            {topCompanies.length === 0 ? (
                                                <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>Sin datos</Typography>
                                            ) : topCompanies.map((item, index) => (
                                                <Stack key={`company-${item.key}`} direction="row" justifyContent="space-between">
                                                    <Typography sx={{ fontSize: 13 }}>{index + 1}. {item.label}</Typography>
                                                    <Chip size="small" label={item.count} />
                                                </Stack>
                                            ))}
                                        </Stack>
                                    </CardContent>
                                </Card>
                            </Grid>

                            <Grid item xs={12} md={6}>
                                <MetricList title="Distribucion por prioridad" items={summary.byPriority ?? []} color="warning.main" />
                            </Grid>
                            <Grid item xs={12} md={6}>
                                <MetricList title="Distribucion por estado" items={summary.byStatus ?? []} color="info.main" />
                            </Grid>
                            <Grid item xs={12} md={6}>
                                <MetricList title="Tickets por tecnico" items={summary.byTechnician ?? []} color="primary.main" />
                            </Grid>
                            <Grid item xs={12} md={6}>
                                <MetricList title="Tickets por empresa" items={summary.byCompany ?? []} color="success.main" />
                            </Grid>
                        </Grid>
                    </Stack>
                ) : null}

            </Container>
        </Box>
    );
}
