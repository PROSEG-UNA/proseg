import { useMemo, useState } from 'react';
import { Autocomplete, Box, IconButton, Stack, TextField, Tooltip } from '@mui/material';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import TableBase from '../../../../common/components/TablaBase.jsx';
import { useTourProcessingDraftDriverSchedule, useTourProcessingDraftDriverSummaries } from '../../hooks/useTourProcessingDraftsData.js';
import { formatDate, formatHourOfDay, formatHours, matchesDriverFilters } from '../../transportUtils.js';
import { usePersistedColumnVisibility } from '../../hooks/usePersistedColumnVisibility.js';
import { usePersistedPagination } from '../../hooks/usePersistedPagination.js';
import { usePersistedExpansion } from '../../hooks/usePersistedExpansion.js';
import TourProcessingDraftDaysDetailPanel from './TourProcessingDraftDaysDetailPanel.jsx';
import DriverSurplusSummary from '../drivers/DriverSurplusSummary.jsx';
import TourProcessingDriverFilters from './TourProcessingDriverFilters.jsx';

const COLUMN_VISIBILITY_STORAGE_KEY = 'tour-processing-draft-driver-schedule-column-visibility';
const INITIAL_COLUMN_VISIBILITY = { startHour: false, endHour: false };
const DEFAULT_UNIT_FILTER = 'GENERAL_SERVICES';
const DEFAULT_STATUS_FILTER = 'ACTIVE';

function sumDays(days, key) {
    return days.reduce((total, day) => total + (day[key] ?? 0), 0);
}

function toTableRow(tour) {
    const firstDay = tour.days[0];
    const lastDay = tour.days[tour.days.length - 1];
    return {
        id: tour.id,
        number: tour.number,
        priority: tour.priority,
        executingUnit: tour.executingUnit,
        responsible: tour.responsible,
        departureDate: firstDay?.date ?? null,
        startHour: firstDay?.start ?? null,
        endHour: lastDay?.end ?? null,
        ordinaryHours: sumDays(tour.days, 'ordinaryHours'),
        surplusHours: sumDays(tour.days, 'surplusHours'),
        overtimeHours: sumDays(tour.days, 'overtimeHours'),
        days: tour.days,
    };
}

const COLUMNS = [
    { accessorKey: 'number', header: 'Número', size: 120, grow: false },
    { accessorKey: 'priority', header: 'Prioridad', size: 110, grow: false },
    { accessorKey: 'executingUnit', header: 'Unidad ejecutora', size: 240, grow: false },
    { accessorKey: 'responsible', header: 'Responsable', size: 190, grow: false },
    {
        accessorKey: 'departureDate',
        header: 'Fecha',
        size: 120,
        grow: false,
        enableColumnFilter: false,
        Cell: ({ cell }) => formatDate(cell.getValue()),
    },
    {
        accessorKey: 'startHour',
        header: 'Hora inicio',
        size: 110,
        grow: false,
        enableColumnFilter: false,
        Cell: ({ cell }) => formatHourOfDay(cell.getValue()),
    },
    {
        accessorKey: 'endHour',
        header: 'Hora fin',
        size: 110,
        grow: false,
        enableColumnFilter: false,
        Cell: ({ cell }) => formatHourOfDay(cell.getValue()),
    },
    {
        accessorKey: 'ordinaryHours',
        header: 'Ordinarias',
        size: 110,
        grow: false,
        enableColumnFilter: false,
        Cell: ({ cell }) => formatHours(cell.getValue()),
    },
    {
        accessorKey: 'surplusHours',
        header: 'Sobresueldo',
        size: 120,
        grow: false,
        enableColumnFilter: false,
        Cell: ({ cell }) => formatHours(cell.getValue()),
    },
    {
        accessorKey: 'overtimeHours',
        header: 'Extra',
        size: 100,
        grow: false,
        enableColumnFilter: false,
        Cell: ({ cell }) => formatHours(cell.getValue()),
    },
];

export default function TourProcessingDraftDriverScheduleView({ draftId, isDraft, onOpenRowById }) {
    const [selectedDriverId, setSelectedDriverId] = useState(null);
    const [pagination, setPagination] = usePersistedPagination('tour-processing-draft-driver-schedule-page-size');
    const { tableOptions: expansionOptions, resetExpanded } = usePersistedExpansion('tour-processing-draft-driver-schedule-expand-all');
    const [columnVisibility, setColumnVisibility] = usePersistedColumnVisibility(COLUMN_VISIBILITY_STORAGE_KEY);

    const [unitFilter, setUnitFilter] = useState(DEFAULT_UNIT_FILTER);
    const [statusFilter, setStatusFilter] = useState(DEFAULT_STATUS_FILTER);

    const { surplusCapPerDriver, drivers: allDrivers, loading: loadingDrivers, error: driversError } = useTourProcessingDraftDriverSummaries(draftId);

    const filteredDrivers = useMemo(
        () => allDrivers.filter((driver) => matchesDriverFilters(driver, unitFilter, statusFilter)),
        [allDrivers, unitFilter, statusFilter],
    );

    const effectiveDriverId = selectedDriverId && filteredDrivers.some((driver) => driver.id === selectedDriverId)
        ? selectedDriverId
        : (filteredDrivers[0]?.id ?? null);

    const resetTable = () => {
        resetExpanded();
        setPagination((current) => (current.pageIndex === 0 ? current : { ...current, pageIndex: 0 }));
    };

    const handleDriverChange = (nextDriverId) => {
        setSelectedDriverId(nextDriverId);
        resetTable();
    };

    const handleUnitChange = (nextUnit) => {
        setUnitFilter(nextUnit);
        resetTable();
    };

    const handleStatusChange = (nextStatus) => {
        setStatusFilter(nextStatus);
        resetTable();
    };

    const { tours, loading: loadingTours, fetching: fetchingTours, error: toursError, totalElements } = useTourProcessingDraftDriverSchedule(
        draftId,
        effectiveDriverId,
        pagination,
    );

    const tableRows = useMemo(() => tours.map(toTableRow), [tours]);
    const drivers = filteredDrivers;
    const selectedDriverIndex = drivers.findIndex((driver) => driver.id === effectiveDriverId);
    const selectedDriver = drivers[selectedDriverIndex] ?? null;

    const handleRowClick = (row) => onOpenRowById(isDraft ? 'edit' : 'view', row.original.id);

    return (
        <Box>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={3}
                alignItems={{ xs: 'stretch', sm: 'center' }}
                flexWrap="wrap"
                useFlexGap
                sx={{ mb: 2 }}
            >
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexShrink: 0 }}>
                    <Tooltip title="Chofer anterior">
                        <span>
                            <IconButton
                                aria-label="Chofer anterior"
                                disabled={selectedDriverIndex <= 0}
                                onClick={() => handleDriverChange(drivers[selectedDriverIndex - 1].id)}
                            >
                                <ChevronLeftIcon />
                            </IconButton>
                        </span>
                    </Tooltip>
                    <Autocomplete
                        sx={{ width: { xs: '100%', sm: 220 }, minWidth: 0 }}
                        disableClearable
                        size="small"
                        loading={loadingDrivers}
                        options={drivers}
                        getOptionLabel={(driver) => driver.name ?? ''}
                        isOptionEqualToValue={(option, value) => option.id === value.id}
                        value={selectedDriver}
                        onChange={(_, next) => next && handleDriverChange(next.id)}
                        renderInput={(params) => <TextField {...params} label="Chofer" margin="none" />}
                    />
                    <Tooltip title="Chofer siguiente">
                        <span>
                            <IconButton
                                aria-label="Chofer siguiente"
                                disabled={selectedDriverIndex < 0 || selectedDriverIndex >= drivers.length - 1}
                                onClick={() => handleDriverChange(drivers[selectedDriverIndex + 1].id)}
                            >
                                <ChevronRightIcon />
                            </IconButton>
                        </span>
                    </Tooltip>
                </Stack>
                <TourProcessingDriverFilters unit={unitFilter} status={statusFilter} onUnitChange={handleUnitChange} onStatusChange={handleStatusChange} />
                {selectedDriver && (
                    <DriverSurplusSummary surplusCapPerDriver={surplusCapPerDriver} driver={selectedDriver} />
                )}
            </Stack>

            <TableBase
                columns={COLUMNS}
                data={tableRows}
                loading={loadingDrivers || loadingTours}
                fetching={fetchingTours}
                error={driversError ?? toursError}
                onRowClick={handleRowClick}
                enableGlobalFilter={false}
                enableColumnFilters={false}
                enableDensityToggle={false}
                renderDetailPanel={({ row }) => <TourProcessingDraftDaysDetailPanel days={row.original.days} />}
                tableOptions={{
                    ...expansionOptions,
                    manualPagination: true,
                    rowCount: totalElements,
                    onPaginationChange: setPagination,
                    onColumnVisibilityChange: setColumnVisibility,
                    state: { ...expansionOptions.state, pagination, columnVisibility: { ...INITIAL_COLUMN_VISIBILITY, ...columnVisibility } },
                    muiPaginationProps: { rowsPerPageOptions: [10, 20, 50] },
                }}
            />
        </Box>
    );
}
