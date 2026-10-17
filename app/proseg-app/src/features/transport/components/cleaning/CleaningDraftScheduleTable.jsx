import { Fragment } from 'react';
import {
    Alert,
    Box,
    LinearProgress,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography,
    useTheme,
} from '@mui/material';
import { headerSurfaceSx } from '../../../../common/theme/sxStyles.js';
import { formatDate, formatHourOfDay, formatHours } from '../../transportUtils.js';

const HEADER_CELL_SX = (t) => ({
    ...headerSurfaceSx(t),
    fontWeight: 600,
    fontSize: '0.9rem',
    border: 'none',
});

const GROUP_DIVIDER_SX = {
    borderRight: '1px solid',
    borderRightColor: 'divider',
};

const DAY_COLUMN_WIDTH = 100;
const COLUMN_WIDTHS = [170, 90, 90, 220, 170, 100, DAY_COLUMN_WIDTH, DAY_COLUMN_WIDTH, DAY_COLUMN_WIDTH, DAY_COLUMN_WIDTH, DAY_COLUMN_WIDTH];

function countDriverDays(driver) {
    return driver.tours.reduce((total, tour) => total + tour.days.length, 0);
}

export default function CleaningDraftScheduleTable({ schedule, loading, error, onOpenTour }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const zebraColor = theme.palette.mode === 'dark' ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.015)';

    if (loading) return <LinearProgress sx={{ mb: 2 }} />;
    if (error) {
        return (
            <Alert severity="error" variant="outlined">
                {error}
            </Alert>
        );
    }

    const drivers = (schedule?.drivers ?? []).filter((driver) => driver.tours.length > 0);
    if (drivers.length === 0) {
        return (
            <Box sx={{ py: 6, textAlign: 'center' }}>
                <Typography sx={{ fontSize: 13.5, color: 'text.secondary' }}>
                    No hay giras con chofer asignado todavía
                </Typography>
            </Box>
        );
    }

    return (
        <TableContainer
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: `${theme.shape.borderRadius}px`,
                backgroundColor: 'background.paperWarm',
            }}
        >
            <Table size="small" stickyHeader sx={{ tableLayout: 'fixed' }}>
                <colgroup>
                    {COLUMN_WIDTHS.map((width, index) => (
                        <col key={index} style={{ width }} />
                    ))}
                </colgroup>
                <TableHead>
                    <TableRow>
                        <TableCell sx={HEADER_CELL_SX}>Chofer</TableCell>
                        <TableCell sx={HEADER_CELL_SX}>Número</TableCell>
                        <TableCell sx={HEADER_CELL_SX}>Prioridad</TableCell>
                        <TableCell sx={HEADER_CELL_SX}>Unidad ejecutora</TableCell>
                        <TableCell sx={HEADER_CELL_SX}>Responsable</TableCell>
                        <TableCell sx={HEADER_CELL_SX}>Fecha</TableCell>
                        <TableCell sx={HEADER_CELL_SX}>Hora inicio</TableCell>
                        <TableCell sx={HEADER_CELL_SX}>Hora fin</TableCell>
                        <TableCell sx={HEADER_CELL_SX}>Ordinarias</TableCell>
                        <TableCell sx={HEADER_CELL_SX}>Sobresueldo</TableCell>
                        <TableCell sx={HEADER_CELL_SX}>Extra</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    {drivers.map((driver, driverIndex) => {
                        const driverRowSpan = countDriverDays(driver);
                        const rowBackgroundColor = driverIndex % 2 === 1 ? zebraColor : 'background.paperWarm';
                        const isLastDriver = driverIndex === drivers.length - 1;
                        const cellSx = (extra = {}) => ({
                            backgroundColor: rowBackgroundColor,
                            fontSize: '0.9rem',
                            borderTop: '1px solid',
                            borderTopColor: 'divider',
                            ...extra,
                        });
                        let driverCellRendered = false;
                        return (
                            <Fragment key={driver.name}>
                                {driver.tours.map((tour, tourIndex) => {
                                    const tourRowSpan = tour.days.length;
                                    const isLastTour = tourIndex === driver.tours.length - 1;
                                    return tour.days.map((day, dayIndex) => {
                                        const showDriverCell = !driverCellRendered;
                                        const showTourCells = dayIndex === 0;
                                        const isLastDayRow = isLastDriver && isLastTour && dayIndex === tour.days.length - 1;
                                        driverCellRendered = true;
                                        return (
                                            <TableRow
                                                key={`${tour.id}-${day.date}`}
                                                onClick={() => onOpenTour(tour.id)}
                                                sx={{
                                                    cursor: 'pointer',
                                                    '&:hover td': { backgroundColor: 'action.hover' },
                                                    ...(isLastDayRow ? { '& td': { borderBottom: '1px solid', borderBottomColor: 'divider' } } : {}),
                                                }}
                                            >
                                                {showDriverCell && (
                                                    <TableCell
                                                        rowSpan={driverRowSpan}
                                                        sx={cellSx({ fontWeight: 600, verticalAlign: 'top', color: accentColor, ...GROUP_DIVIDER_SX })}
                                                    >
                                                        {driver.name}
                                                    </TableCell>
                                                )}
                                                {showTourCells && (
                                                    <>
                                                        <TableCell rowSpan={tourRowSpan} sx={cellSx({ fontWeight: 600, verticalAlign: 'top' })}>{tour.number}</TableCell>
                                                        <TableCell rowSpan={tourRowSpan} sx={cellSx({ fontWeight: 600, verticalAlign: 'top' })}>{tour.priority}</TableCell>
                                                        <TableCell rowSpan={tourRowSpan} sx={cellSx({ fontWeight: 600, verticalAlign: 'top' })}>{tour.executingUnit}</TableCell>
                                                        <TableCell rowSpan={tourRowSpan} sx={cellSx({ fontWeight: 600, verticalAlign: 'top', ...GROUP_DIVIDER_SX })}>{tour.responsible}</TableCell>
                                                    </>
                                                )}
                                                <TableCell sx={cellSx()}>{formatDate(day.date)}</TableCell>
                                                <TableCell sx={cellSx()}>{formatHourOfDay(day.start)}</TableCell>
                                                <TableCell sx={cellSx()}>{formatHourOfDay(day.end)}</TableCell>
                                                <TableCell sx={cellSx()}>{formatHours(day.ordinaryHours)}</TableCell>
                                                <TableCell sx={cellSx()}>{formatHours(day.surplusHours)}</TableCell>
                                                <TableCell sx={cellSx()}>{formatHours(day.overtimeHours)}</TableCell>
                                            </TableRow>
                                        );
                                    });
                                })}
                            </Fragment>
                        );
                    })}
                </TableBody>
            </Table>
        </TableContainer>
    );
}
