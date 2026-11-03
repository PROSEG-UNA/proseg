import { useMemo, useState } from 'react';
import dayjs from 'dayjs';
import { Box, IconButton, Stack, Tooltip } from '@mui/material';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { esES } from '@mui/x-date-pickers/locales';
import 'dayjs/locale/es';
import TableBase from '../../../../common/components/TablaBase.jsx';
import { useTourProcessingDraftDriverSummaries, useTourProcessingDraftSchedule } from '../../hooks/useTourProcessingDraftsData.js';
import { usePersistedPagination } from '../../hooks/usePersistedPagination.js';
import { usePersistedExpansion } from '../../hooks/usePersistedExpansion.js';
import { formatDate, formatHours, matchesDriverFilters } from '../../transportUtils.js';
import TourProcessingDraftTourControlDetail from './TourProcessingDraftTourControlDetail.jsx';
import DriverSurplusSummary from '../drivers/DriverSurplusSummary.jsx';
import TourProcessingDriverFilters from './TourProcessingDriverFilters.jsx';

const DEFAULT_UNIT_FILTER = 'GENERAL_SERVICES';
const DEFAULT_STATUS_FILTER = 'ACTIVE';

const COLUMNS = [
    {
        accessorKey: 'date',
        header: 'Fecha',
        size: 120,
        grow: false,
        Cell: ({ cell }) => formatDate(cell.getValue()),
    },
    { accessorKey: 'name', header: 'Chofer', size: 240 },
    {
        accessorKey: 'surplusHours',
        header: 'Sobresueldo',
        size: 150,
        grow: false,
        Cell: ({ cell }) => formatHours(cell.getValue()),
    },
    {
        accessorKey: 'overtimeHours',
        header: 'Extra',
        size: 110,
        grow: false,
        Cell: ({ cell }) => formatHours(cell.getValue()),
    },
];

export default function TourProcessingDraftTourControlView({ draftId, year, month, canEdit, onOpenRowById }) {
    const firstDate = dayjs(`${year}-${String(month).padStart(2, '0')}-01`);
    const lastDate = firstDate.endOf('month');
    const [selectedDate, setSelectedDate] = useState(firstDate);
    const { tableOptions: expansionOptions, resetExpanded } = usePersistedExpansion('tour-processing-draft-tour-control-expand-all');
    const [pagination, setPagination] = usePersistedPagination('tour-processing-draft-tour-control-page-size', 20);
    const date = selectedDate.format('YYYY-MM-DD');
    const { drivers, loading, fetching, error } = useTourProcessingDraftSchedule(draftId, date);
    const { surplusCapPerDriver, drivers: driverSummaries, loading: loadingSummaries } = useTourProcessingDraftDriverSummaries(draftId);
    const [unitFilter, setUnitFilter] = useState(DEFAULT_UNIT_FILTER);
    const [statusFilter, setStatusFilter] = useState(DEFAULT_STATUS_FILTER);

    const rows = useMemo(() => {
        const summaryById = new Map(driverSummaries.map((summary) => [summary.id, summary]));
        return drivers
            .map((driver) => ({
                ...driver,
                organizationalUnit: summaryById.get(driver.id)?.organizationalUnit,
                status: summaryById.get(driver.id)?.status,
                date,
                tours: (driver.tours ?? []).flatMap((tour) => {
                    const day = tour.days.find((tourDay) => tourDay.date === date);
                    return day ? [{ ...tour, day }] : [];
                }),
            }))
            .filter((driver) => matchesDriverFilters(driver, unitFilter, statusFilter));
    }, [drivers, driverSummaries, date, unitFilter, statusFilter]);

    const changeFilter = (setFilter) => (value) => {
        setFilter(value);
        resetExpanded();
    };

    const changeDate = (nextDate) => {
        if (!nextDate?.isValid() || nextDate.isBefore(firstDate, 'day') || nextDate.isAfter(lastDate, 'day')) return;
        setSelectedDate(nextDate);
        resetExpanded();
    };

    const openTour = (tour) => onOpenRowById(canEdit ? 'edit' : 'view', tour.id);

    return (
        <Box>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mb: 2, alignItems: { xs: 'stretch', sm: 'center' } }}>
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexShrink: 0 }}>
                    <Tooltip title="Día anterior">
                        <span>
                            <IconButton aria-label="Día anterior" disabled={selectedDate.isSame(firstDate, 'day')} onClick={() => changeDate(selectedDate.subtract(1, 'day'))}>
                                <ChevronLeftIcon />
                            </IconButton>
                        </span>
                    </Tooltip>
                    <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="es" localeText={esES.components.MuiLocalizationProvider.defaultProps.localeText}>
                        <DatePicker
                            label="Día"
                            format="DD/MM/YYYY"
                            value={selectedDate}
                            minDate={firstDate}
                            maxDate={lastDate}
                            views={['day']}
                            onChange={changeDate}
                            slotProps={{ textField: { size: 'small', sx: { width: 220 } } }}
                        />
                    </LocalizationProvider>
                    <Tooltip title="Día siguiente">
                        <span>
                            <IconButton aria-label="Día siguiente" disabled={selectedDate.isSame(lastDate, 'day')} onClick={() => changeDate(selectedDate.add(1, 'day'))}>
                                <ChevronRightIcon />
                            </IconButton>
                        </span>
                    </Tooltip>
                </Stack>
                <TourProcessingDriverFilters unit={unitFilter} status={statusFilter} onUnitChange={changeFilter(setUnitFilter)} onStatusChange={changeFilter(setStatusFilter)} />
                <DriverSurplusSummary surplusCapPerDriver={surplusCapPerDriver} />
            </Stack>
            <TableBase
                columns={COLUMNS}
                data={rows}
                loading={loading || loadingSummaries}
                fetching={fetching}
                error={error}
                enableColumnFilters={false}
                enableDensityToggle={false}
                onRowClick={(row) => row.original.tours.length > 0 && row.toggleExpanded()}
                renderDetailPanel={({ row }) => row.original.tours.length > 0
                    ? <TourProcessingDraftTourControlDetail tours={row.original.tours} date={date} driverName={row.original.name} onOpenTour={openTour} />
                    : null}
                tableOptions={{
                    ...expansionOptions,
                    getRowId: (row) => row.id,
                    getRowCanExpand: (row) => row.original.tours.length > 0,
                    state: { ...expansionOptions.state, pagination, density: 'compact' },
                    onPaginationChange: setPagination,
                    paginateExpandedRows: false,
                }}
            />
        </Box>
    );
}
