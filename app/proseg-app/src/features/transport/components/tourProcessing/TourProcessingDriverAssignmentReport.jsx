import { useMemo, useState } from 'react';
import {
    Box,
    InputAdornment,
    Paper,
    Stack,
    Tab,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Tabs,
    TextField,
    Typography,
} from '@mui/material';
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined';
import SearchOutlinedIcon from '@mui/icons-material/SearchOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';

const hours = (value) => Number(value).toLocaleString('es-CR', { maximumFractionDigits: 3 });

const normalizeText = (value) => String(value).normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

function StatTile({ label, value, color = 'text.primary' }) {
    return (
        <Box sx={{ flex: 1, minWidth: 110, px: 2, py: 1.25, border: '1px solid', borderColor: 'divider', borderRadius: '10px', bgcolor: 'background.paper' }}>
            <Typography sx={{ fontSize: 24, fontWeight: 800, lineHeight: 1.15, color }}>{value}</Typography>
            <Typography variant="caption" color="text.secondary">{label}</Typography>
        </Box>
    );
}

function TourIssuesTable({ items }) {
    const [query, setQuery] = useState('');
    const visibleItems = useMemo(() => {
        const needle = normalizeText(query.trim());
        if (!needle) return items;
        return items.filter((item) => normalizeText(`${item.number} ${item.reasons.join(' ')}`).includes(needle));
    }, [items, query]);

    return (
        <Stack spacing={1.5}>
            <TextField
                size="small"
                placeholder="Buscar por número de gira o motivo"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                slotProps={{
                    input: {
                        startAdornment: <InputAdornment position="start"><SearchOutlinedIcon fontSize="small" /></InputAdornment>,
                    },
                }}
            />
            <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell sx={{ width: 110, fontWeight: 700 }}>Gira</TableCell>
                            <TableCell sx={{ fontWeight: 700 }}>Motivo</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {visibleItems.map((item) => (
                            <TableRow key={item.rowId}>
                                <TableCell sx={{ verticalAlign: 'top', fontWeight: 700 }}>{item.number}</TableCell>
                                <TableCell>
                                    <Box component="ul" sx={{ pl: 2, my: 0 }}>
                                        {item.reasons.map((reason, index) => <Typography component="li" variant="body2" key={`${index}-${reason}`}>{reason}</Typography>)}
                                    </Box>
                                </TableCell>
                            </TableRow>
                        ))}
                        {visibleItems.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={2} align="center">
                                    <Typography variant="body2" color="text.secondary">Ninguna gira coincide con la búsqueda.</Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>
        </Stack>
    );
}

function SurplusDeficitsTable({ deficits, targetHours }) {
    return (
        <Stack spacing={1.5}>
            <Typography variant="body2" color="text.secondary">
                Sobresueldo pendiente para alcanzar el 95% ({hours(targetHours)} h)
            </Typography>
            <TableContainer component={Paper} variant="outlined">
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell sx={{ fontWeight: 700 }}>Chofer</TableCell>
                            <TableCell align="right" sx={{ fontWeight: 700 }}>Horas faltantes</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {deficits.map(([name, missing]) => (
                            <TableRow key={name}>
                                <TableCell>{name}</TableCell>
                                <TableCell align="right">{hours(missing)} h</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Stack>
    );
}

function ReportBody({ result }) {
    const pending = result.unassignedDetails ?? [];
    const warnings = result.preservedAssignmentWarnings ?? [];
    const deficits = Object.entries(result.surplusDeficits ?? {});
    const [activeKey, setActiveKey] = useState(null);

    const sections = [
        { key: 'pending', label: 'Giras pendientes', count: pending.length },
        { key: 'warnings', label: 'Requieren revisión', count: warnings.length },
        { key: 'deficits', label: 'Sobresueldo', count: deficits.length },
    ].filter((section) => section.count > 0);
    const current = sections.find((section) => section.key === activeKey) ?? sections[0];

    return (
        <Box>
            <Stack direction="row" spacing={1.5} useFlexGap flexWrap="wrap" sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 2 }}>
                <StatTile label="Asignadas" value={result.assignedTours ?? 0} color="success.main" />
                <StatTile label="Pendientes" value={result.unassignedTours ?? 0} />
                <StatTile label="Requieren revisión" value={warnings.length} />
            </Stack>

            {result.optimizationLimitReached && (
                <Typography variant="body2" color="text.secondary" sx={{ px: { xs: 2.5, sm: 3 }, pb: 2 }}>
                    Se alcanzó el límite de búsqueda de mejoras. Las asignaciones generadas cumplen las reglas, pero podrían existir otras distribuciones.
                </Typography>
            )}

            {!current && (
                <Typography variant="body2" color="text.secondary" sx={{ px: { xs: 2.5, sm: 3 }, pb: 3 }}>
                    Todas las giras se asignaron sin observaciones.
                </Typography>
            )}

            {current && (
                <>
                    <Tabs
                        value={current.key}
                        onChange={(_, key) => setActiveKey(key)}
                        variant="scrollable"
                        scrollButtons="auto"
                        sx={{ position: 'sticky', top: 0, zIndex: 1, px: { xs: 1.5, sm: 2 }, bgcolor: 'background.paperWarm', borderBottom: '1px solid', borderColor: 'divider' }}
                    >
                        {sections.map((section) => (
                            <Tab key={section.key} value={section.key} label={`${section.label} (${section.count})`} sx={{ textTransform: 'none', fontWeight: 700 }} />
                        ))}
                    </Tabs>
                    <Box sx={{ px: { xs: 2.5, sm: 3 }, py: 2 }}>
                        {current.key === 'pending' && <TourIssuesTable key="pending" items={pending} />}
                        {current.key === 'warnings' && <TourIssuesTable key="warnings" items={warnings} />}
                        {current.key === 'deficits' && <SurplusDeficitsTable deficits={deficits} targetHours={result.surplusTargetHours} />}
                    </Box>
                </>
            )}
        </Box>
    );
}

export default function TourProcessingDriverAssignmentReport({ open, result, subtitle, onClose }) {
    if (!result) return null;

    return (
        <GeneralModal
            open={open}
            onClose={onClose}
            icon={FactCheckOutlinedIcon}
            title="Resultado de la autoasignación"
            subtitle={subtitle}
            maxWidth="md"
            footerLeft={(
                <Typography variant="caption" color="text.secondary" sx={{ pr: 2 }}>
                    Corresponde a la ejecución realizada; las ediciones posteriores pueden cambiarlo.
                </Typography>
            )}
            primaryButton={{ label: 'Cerrar', onClick: onClose }}
        >
            <ReportBody result={result} />
        </GeneralModal>
    );
}
