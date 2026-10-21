import { useCallback, useMemo, useState } from 'react';
import {
    Alert,
    Box,
    Button,
    Card,
    CardActionArea,
    CardContent,
    Chip,
    Grid,
    Skeleton,
    Stack,
    ToggleButton,
    ToggleButtonGroup,
    Typography,
    useTheme,
} from '@mui/material';
import { PieChart, BarChart } from '@mui/x-charts';
import dayjs from 'dayjs';
import TableBase from '../../../../common/components/TablaBase.jsx';
import { useAssetDashboard, useDashboardAssets } from '../../hooks/useAssetDashboard';

const DAY_OPTIONS = [30, 60, 90];

const CATEGORIES = {
    WARRANTY_EXPIRED: 'WARRANTY_EXPIRED',
    WARRANTY_EXPIRING: 'WARRANTY_EXPIRING',
    FIRMWARE_EXPIRED: 'FIRMWARE_EXPIRED',
    FIRMWARE_EXPIRING: 'FIRMWARE_EXPIRING',
};

const CATEGORY_TITLES = {
    [CATEGORIES.WARRANTY_EXPIRED]: 'Garantía vencida',
    [CATEGORIES.WARRANTY_EXPIRING]: 'Garantía por vencer',
    [CATEGORIES.FIRMWARE_EXPIRED]: 'Soporte de firmware vencido',
    [CATEGORIES.FIRMWARE_EXPIRING]: 'Soporte de firmware por vencer',
};

const PIE_SLICES = [
    { id: 'expired', label: 'Vencida', statKey: 'expired' },
    { id: 'expiringSoon', label: 'Por vencer', statKey: 'expiringSoon' },
    { id: 'valid', label: 'Vigente', statKey: 'valid' },
    { id: 'noDate', label: 'Sin fecha', statKey: 'noDate' },
];

const WARRANTY_SLICE_TO_CATEGORY = {
    expired: CATEGORIES.WARRANTY_EXPIRED,
    expiringSoon: CATEGORIES.WARRANTY_EXPIRING,
};

const FIRMWARE_SLICE_TO_CATEGORY = {
    expired: CATEGORIES.FIRMWARE_EXPIRED,
    expiringSoon: CATEGORIES.FIRMWARE_EXPIRING,
};

function sumExpiryStats(stats) {
    if (!stats) return 0;
    return stats.expired + stats.expiringSoon + stats.valid + stats.noDate;
}

function buildPieSeries(stats, colors) {
    return PIE_SLICES.map((slice, index) => ({
        id: slice.id,
        label: slice.label,
        value: stats?.[slice.statKey] ?? 0,
        color: colors[index],
    }));
}

function formatDaysLabel(dateValue) {
    if (!dateValue) return { text: '—', color: 'text.secondary' };
    const diff = dayjs(dateValue).startOf('day').diff(dayjs().startOf('day'), 'day');
    if (diff < 0) {
        return { text: `Vencido hace ${Math.abs(diff)} días`, color: 'error.main' };
    }
    if (diff === 0) {
        return { text: 'Vence hoy', color: 'warning.main' };
    }
    return { text: `En ${diff} días`, color: 'warning.main' };
}

export default function AssetDashboard() {
    const theme = useTheme();
    const [days, setDays] = useState(30);
    const [category, setCategory] = useState(null);
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });

    const { summary, loading, error } = useAssetDashboard(days);
    const {
        rows,
        totalElements,
        loading: listLoading,
        fetching: listFetching,
        error: listError,
    } = useDashboardAssets({
        category,
        days,
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
    });

    const resetPagination = useCallback(() => {
        setPagination((prev) => (prev.pageIndex === 0 ? prev : { ...prev, pageIndex: 0 }));
    }, []);

    const handleDaysChange = useCallback((_event, value) => {
        if (value == null) return;
        setDays(value);
        resetPagination();
    }, [resetPagination]);

    const toggleCategory = useCallback((next) => {
        setCategory((prev) => (prev === next ? null : next));
        resetPagination();
    }, [resetPagination]);

    const handleCloseCategory = useCallback(() => {
        setCategory(null);
        resetPagination();
    }, [resetPagination]);

    const pieColors = useMemo(
        () => [
            theme.palette.error.main,
            theme.palette.warning.main,
            theme.palette.success.main,
            theme.palette.grey[400],
        ],
        [theme.palette.error.main, theme.palette.warning.main, theme.palette.success.main, theme.palette.grey]
    );

    const chartLegendSlotProps = useMemo(
        () => ({
            legend: {
                direction: 'horizontal',
                position: { vertical: 'bottom', horizontal: 'center' },
            },
        }),
        []
    );

    const cardSx = {
        borderRadius: 3,
        border: '1px solid',
        borderColor: 'divider',
        bgcolor: 'background.paper',
        boxShadow: 'none',
    };

    const warrantyPieData = useMemo(
        () => buildPieSeries(summary?.warranty, pieColors),
        [summary?.warranty, pieColors]
    );
    const firmwarePieData = useMemo(
        () => buildPieSeries(summary?.firmware, pieColors),
        [summary?.firmware, pieColors]
    );

    const typeNames = useMemo(
        () => (summary?.byType ?? []).map((item) => item.name),
        [summary?.byType]
    );
    const typeTotals = useMemo(
        () => (summary?.byType ?? []).map((item) => item.total),
        [summary?.byType]
    );

    const yAxisWidth = useMemo(() => {
        const longest = typeNames.reduce((max, name) => Math.max(max, name?.length ?? 0), 0);
        return Math.min(200, Math.max(96, longest * 8));
    }, [typeNames]);

    const handleWarrantyPieClick = useCallback(
        (_event, identifier) => {
            const slice = warrantyPieData[identifier?.dataIndex];
            const mapped = slice ? WARRANTY_SLICE_TO_CATEGORY[slice.id] : null;
            if (mapped) toggleCategory(mapped);
        },
        [toggleCategory, warrantyPieData]
    );

    const handleFirmwarePieClick = useCallback(
        (_event, identifier) => {
            const slice = firmwarePieData[identifier?.dataIndex];
            const mapped = slice ? FIRMWARE_SLICE_TO_CATEGORY[slice.id] : null;
            if (mapped) toggleCategory(mapped);
        },
        [toggleCategory, firmwarePieData]
    );

    const listColumns = useMemo(() => {
        const isWarranty = category === CATEGORIES.WARRANTY_EXPIRED || category === CATEGORIES.WARRANTY_EXPIRING;
        const dateKey = isWarranty ? 'warrantyEndDate' : 'firmwareSupportEndDate';
        const dateHeader = isWarranty ? 'Vencimiento garantía' : 'Vencimiento soporte';

        return [
            { accessorKey: 'assetNumber', header: 'N° Activo', size: 140 },
            { accessorKey: 'serialNumber', header: 'N° Serie', size: 160 },
            { accessorKey: 'type', header: 'Tipo', size: 140 },
            { accessorKey: 'executingUnit', header: 'Unidad Ejecutora', size: 180 },
            { accessorKey: 'responsibleEmployee', header: 'Funcionario', size: 160 },
            {
                accessorKey: dateKey,
                header: dateHeader,
                size: 150,
                Cell: ({ cell }) => {
                    const value = cell.getValue();
                    return value ? dayjs(value).format('DD/MM/YYYY') : '—';
                },
            },
            {
                id: 'days',
                header: 'Días',
                size: 170,
                Cell: ({ row }) => {
                    const value = row.original[dateKey];
                    const { text, color } = formatDaysLabel(value);
                    return (
                        <Typography component="span" sx={{ fontSize: 13, fontWeight: 600, color }}>
                            {text}
                        </Typography>
                    );
                },
            },
        ];
    }, [category]);

    const kpiCards = [
        {
            key: 'total',
            title: 'Activos aprobados',
            value: summary?.activeAssets ?? '—',
            chip: summary?.decommissionedAssets != null
                ? `${summary.decommissionedAssets} de baja`
                : null,
            selectable: false,
        },
        {
            key: CATEGORIES.WARRANTY_EXPIRED,
            title: 'Garantía vencida',
            value: summary?.warranty?.expired ?? '—',
            color: 'error.main',
            selectable: true,
        },
        {
            key: CATEGORIES.WARRANTY_EXPIRING,
            title: 'Garantía por vencer',
            value: summary?.warranty?.expiringSoon ?? '—',
            color: 'warning.main',
            selectable: true,
        },
        {
            key: CATEGORIES.FIRMWARE_EXPIRED,
            title: 'Soporte vencido',
            value: summary?.firmware?.expired ?? '—',
            color: 'error.main',
            selectable: true,
        },
        {
            key: CATEGORIES.FIRMWARE_EXPIRING,
            title: 'Soporte por vencer',
            value: summary?.firmware?.expiringSoon ?? '—',
            color: 'warning.main',
            selectable: true,
        },
    ];

    const renderChartPanel = (title, hasData, content) => (
        <Card sx={cardSx}>
            <CardContent sx={{ p: 2, height: 300, display: 'flex', flexDirection: 'column' }}>
                <Typography sx={{ fontWeight: 700, fontSize: '0.95rem', color: 'text.primary', mb: 1 }}>
                    {title}
                </Typography>
                <Box sx={{ flex: 1, minHeight: 0, position: 'relative' }}>
                    {loading ? (
                        <Skeleton variant="rounded" height="100%" sx={{ borderRadius: 2 }} />
                    ) : hasData ? (
                        content
                    ) : (
                        <Box sx={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                            <Typography color="text.secondary">Sin datos</Typography>
                        </Box>
                    )}
                </Box>
            </CardContent>
        </Card>
    );

    return (
        <Box sx={{ mt: 4 }}>
            <Stack
                direction={{ xs: 'column', md: 'row' }}
                spacing={2}
                justifyContent="space-between"
                alignItems={{ md: 'flex-start' }}
                sx={{ mb: 3 }}
            >
                <Box>
                    <Typography sx={{ color: 'primary.main', fontWeight: 800, fontSize: '1.25rem' }}>
                        Panel de vencimientos
                    </Typography>
                    <Typography sx={{ mt: 0.5, color: 'text.secondary', fontSize: '0.95rem' }}>
                        Garantías y soporte de firmware de activos aprobados.
                    </Typography>
                </Box>
                <Stack direction="row" spacing={1.25} alignItems="center" flexWrap="wrap" useFlexGap>
                    <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 600 }}>
                        Por vencer en
                    </Typography>
                    <ToggleButtonGroup
                        exclusive
                        size="small"
                        value={days}
                        onChange={handleDaysChange}
                    >
                        {DAY_OPTIONS.map((option) => (
                            <ToggleButton key={option} value={option} sx={{ textTransform: 'none', fontWeight: 700, px: 1.75 }}>
                                {option} días
                            </ToggleButton>
                        ))}
                    </ToggleButtonGroup>
                </Stack>
            </Stack>

            {error ? (
                <Alert severity="error" sx={{ mb: 2 }}>
                    {error}
                </Alert>
            ) : null}

            <Grid container spacing={2} sx={{ mb: 2 }}>
                {kpiCards.map((kpi) => {
                    const selected = kpi.selectable && category === kpi.key;
                    const inner = (
                        <CardContent sx={{ p: 2 }}>
                            {loading ? (
                                <>
                                    <Skeleton width="60%" height={18} />
                                    <Skeleton width="40%" height={36} sx={{ mt: 1 }} />
                                </>
                            ) : (
                                <>
                                    <Typography sx={{ fontSize: 13, color: 'text.secondary', fontWeight: 600 }}>
                                        {kpi.title}
                                    </Typography>
                                    <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.75 }}>
                                        <Typography sx={{ fontSize: '1.75rem', fontWeight: 800, lineHeight: 1.1, color: kpi.selectable ? kpi.color : 'text.primary' }}>
                                            {kpi.value}
                                        </Typography>
                                        {kpi.chip ? (
                                            <Chip label={kpi.chip} size="small" variant="outlined" sx={{ fontWeight: 600 }} />
                                        ) : null}
                                    </Stack>
                                </>
                            )}
                        </CardContent>
                    );

                    return (
                        <Grid size={{ xs: 6, md: 2.4 }} key={kpi.key}>
                            {kpi.selectable ? (
                                <Card
                                    sx={{
                                        ...cardSx,
                                        borderWidth: selected ? 2 : 1,
                                        borderColor: selected ? kpi.color : 'divider',
                                    }}
                                >
                                    <CardActionArea onClick={() => toggleCategory(kpi.key)} sx={{ height: '100%' }}>
                                        {inner}
                                    </CardActionArea>
                                </Card>
                            ) : (
                                <Card sx={cardSx}>{inner}</Card>
                            )}
                        </Grid>
                    );
                })}
            </Grid>

            <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid size={{ xs: 12, md: 4 }}>
                    {renderChartPanel(
                        'Garantía',
                        sumExpiryStats(summary?.warranty) > 0,
                        <PieChart
                            height={240}
                            series={[
                                {
                                    data: warrantyPieData,
                                    innerRadius: 60,
                                    outerRadius: 100,
                                    paddingAngle: 2,
                                    cornerRadius: 4,
                                },
                            ]}
                            onItemClick={handleWarrantyPieClick}
                            slotProps={chartLegendSlotProps}
                            sx={{ width: '100%' }}
                        />
                    )}
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                    {renderChartPanel(
                        'Soporte de firmware',
                        sumExpiryStats(summary?.firmware) > 0,
                        <PieChart
                            height={240}
                            series={[
                                {
                                    data: firmwarePieData,
                                    innerRadius: 60,
                                    outerRadius: 100,
                                    paddingAngle: 2,
                                    cornerRadius: 4,
                                },
                            ]}
                            onItemClick={handleFirmwarePieClick}
                            slotProps={chartLegendSlotProps}
                            sx={{ width: '100%' }}
                        />
                    )}
                </Grid>
                <Grid size={{ xs: 12, md: 4 }}>
                    {renderChartPanel(
                        'Activos por tipo',
                        typeTotals.some((value) => value > 0),
                        <BarChart
                            height={240}
                            layout="horizontal"
                            yAxis={[
                                {
                                    scaleType: 'band',
                                    data: typeNames,
                                    width: yAxisWidth,
                                    tickLabelStyle: { fill: theme.palette.text.primary, fontSize: 12 },
                                },
                            ]}
                            xAxis={[
                                {
                                    scaleType: 'linear',
                                    tickLabelStyle: { fill: theme.palette.text.primary, fontSize: 12 },
                                },
                            ]}
                            series={[
                                {
                                    data: typeTotals,
                                    color: theme.palette.primary.main,
                                },
                            ]}
                            sx={{ width: '100%' }}
                        />
                    )}
                </Grid>
            </Grid>

            {category ? (
                <Box>
                    <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1.5 }}>
                        <Typography sx={{ fontWeight: 700, color: 'text.primary' }}>
                            {CATEGORY_TITLES[category]}
                        </Typography>
                        <Button size="small" onClick={handleCloseCategory} sx={{ textTransform: 'none', fontWeight: 700 }}>
                            Cerrar
                        </Button>
                    </Stack>
                    <TableBase
                        columns={listColumns}
                        data={rows}
                        loading={listLoading}
                        fetching={listFetching}
                        error={listError}
                        enableRowActions={false}
                        enableColumnFilters={false}
                        enableGlobalFilter={false}
                        fillToBottom={false}
                        tableOptions={{
                            manualPagination: true,
                            rowCount: totalElements,
                            onPaginationChange: setPagination,
                            state: { pagination },
                        }}
                    />
                </Box>
            ) : (
                <Typography sx={{ color: 'text.secondary', fontSize: '0.95rem' }}>
                    Seleccioná una tarjeta o una sección del gráfico para ver los activos.
                </Typography>
            )}
        </Box>
    );
}
