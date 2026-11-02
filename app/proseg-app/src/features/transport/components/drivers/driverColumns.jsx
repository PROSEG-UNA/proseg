import Chip from '@mui/material/Chip';
import TableSelectFilter from '../../../../common/components/TableSelectFilter.jsx';
import { DRIVER_STATUS_OPTIONS, DRIVER_UNIT_OPTIONS, formatHours, formatPercent, getStatusChipColor } from '../../transportUtils';

export function getDriverColumns({ showMonthColumns = false } = {}) {
    const baseColumns = [
        { accessorKey: 'name', header: 'Nombre', size: 240, grow: true },
        {
            accessorKey: 'organizationalUnit',
            header: 'Unidad',
            size: 180,
            grow: false,
            filterVariant: 'select',
            filterSelectOptions: DRIVER_UNIT_OPTIONS,
            Filter: ({ column }) => (
                <TableSelectFilter column={column} label="Unidad" options={DRIVER_UNIT_OPTIONS} clearLabel="Todas" />
            ),
        },
        {
            accessorKey: 'status',
            header: 'Estado',
            size: 120,
            grow: false,
            filterVariant: 'select',
            filterSelectOptions: DRIVER_STATUS_OPTIONS,
            Filter: ({ column }) => (
                <TableSelectFilter column={column} label="Estado" options={DRIVER_STATUS_OPTIONS} clearLabel="Todos" />
            ),
            Cell: ({ row, cell }) => (
                <Chip
                    label={cell.getValue() ?? '—'}
                    size="small"
                    color={getStatusChipColor(row.original.statusRaw)}
                    variant="outlined"
                />
            ),
        },
        {
            accessorKey: 'ordinarySchedule',
            header: 'Horario ordinario',
            size: 160,
            grow: false,
            enableColumnFilter: false,
            enableSorting: false,
        },
    ];

    if (!showMonthColumns) return baseColumns;

    return [
        ...baseColumns,
        {
            accessorKey: 'ordinaryHoursAccrued',
            header: 'H. Ordinarias',
            size: 150,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatHours(cell.getValue()),
        },
        {
            accessorKey: 'surplusExecutionRatio',
            header: '% Sobresueldo',
            size: 165,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatPercent(cell.getValue()),
        },
        {
            accessorKey: 'surplusHoursAccrued',
            header: 'H. Sobresueldo',
            size: 165,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatHours(cell.getValue()),
        },
        {
            accessorKey: 'overtimeHoursAccrued',
            header: 'H. Extra',
            size: 120,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatHours(cell.getValue()),
        },
        {
            accessorKey: 'weekendsWorked',
            header: 'Fines de semana',
            size: 175,
            grow: false,
            enableColumnFilter: false,
        },
    ];
}
