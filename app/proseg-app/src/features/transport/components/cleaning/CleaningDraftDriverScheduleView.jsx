import { useMemo, useState } from 'react';
import { Autocomplete, Box, Stack, TextField } from '@mui/material';
import TableBase from '../../../../common/components/TablaBase.jsx';
import { useCleaningDraftAllRows, useCleaningDraftDriverSchedule, useCleaningDraftDriverSummaries } from '../../hooks/useCleaningDraftsData.js';
import { formatDate, formatHourOfDay, formatHours } from '../../transportUtils.js';
import { usePersistedColumnVisibility } from '../../hooks/usePersistedColumnVisibility.js';
import CleaningDraftDaysDetailPanel from './CleaningDraftDaysDetailPanel.jsx';
import DriverSurplusSummary from '../drivers/DriverSurplusSummary.jsx';

const COLUMN_VISIBILITY_STORAGE_KEY = 'cleaning-draft-driver-schedule-column-visibility';
const INITIAL_COLUMN_VISIBILITY = { startHour: false, endHour: false };

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

export default function CleaningDraftDriverScheduleView({ draftId, isDraft, onOpenRow }) {
    const [selectedDriverId, setSelectedDriverId] = useState(null);
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [columnVisibility, setColumnVisibility] = usePersistedColumnVisibility(COLUMN_VISIBILITY_STORAGE_KEY);

    const { surplusCapPerDriver, drivers, loading: loadingDrivers, error: driversError } = useCleaningDraftDriverSummaries(draftId);

    const effectiveDriverId = selectedDriverId && drivers.some((driver) => driver.id === selectedDriverId)
        ? selectedDriverId
        : (drivers[0]?.id ?? null);

    const handleDriverChange = (nextDriverId) => {
        setSelectedDriverId(nextDriverId);
        setPagination((current) => (current.pageIndex === 0 ? current : { ...current, pageIndex: 0 }));
    };

    const { tours, loading: loadingTours, fetching: fetchingTours, error: toursError, totalElements } = useCleaningDraftDriverSchedule(
        draftId,
        effectiveDriverId,
        pagination,
    );

    const { rows: allRows } = useCleaningDraftAllRows(draftId);
    const rowsById = useMemo(() => new Map(allRows.map((row) => [row.id, row])), [allRows]);

    const tableRows = useMemo(() => tours.map(toTableRow), [tours]);
    const selectedDriver = drivers.find((driver) => driver.id === effectiveDriverId) ?? null;

    const handleRowClick = (row) => {
        const fullRow = rowsById.get(row.original.id);
        if (fullRow) onOpenRow(isDraft ? 'edit' : 'view', fullRow);
    };

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
                <Box sx={{ width: { xs: '100%', sm: 220 }, flexShrink: 0 }}>
                    <Autocomplete
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
                </Box>
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
                renderDetailPanel={({ row }) => <CleaningDraftDaysDetailPanel days={row.original.days} />}
                tableOptions={{
                    manualPagination: true,
                    rowCount: totalElements,
                    onPaginationChange: setPagination,
                    onColumnVisibilityChange: setColumnVisibility,
                    state: { pagination, columnVisibility: { ...INITIAL_COLUMN_VISIBILITY, ...columnVisibility } },
                    muiPaginationProps: { rowsPerPageOptions: [10, 20, 50] },
                }}
            />
        </Box>
    );
}
