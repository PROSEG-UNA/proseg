import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import { formatHours, formatPercent, getStatusChipColor } from '../../transportUtils';

export function getDriverColumns({ showMonthColumns = false } = {}) {
    const baseColumns = [
        { accessorKey: 'name', header: 'Nombre', size: 280, grow: true },
        {
            accessorKey: 'status',
            header: 'Estado',
            size: 130,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ row, cell }) => (
                <Chip
                    label={cell.getValue() ?? '—'}
                    size="small"
                    color={getStatusChipColor(row.original.statusRaw)}
                    variant="outlined"
                />
            ),
        },
    ];

    if (!showMonthColumns) return baseColumns;

    return [
        ...baseColumns,
        {
            accessorKey: 'ordinaryHoursAccrued',
            header: 'Horas Ordinarias acumulado',
            size: 190,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatHours(cell.getValue()),
        },
        {
            accessorKey: 'surplusExecutionRatio',
            header: 'Porcentaje de ejecución sobresueldo',
            size: 220,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatPercent(cell.getValue()),
        },
        {
            accessorKey: 'surplusHoursAccrued',
            header: 'Horas Sobresueldo acumulado',
            size: 190,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatHours(cell.getValue()),
        },
        {
            accessorKey: 'overtimeHoursAccrued',
            header: 'Horas Extra acumulado',
            size: 190,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatHours(cell.getValue()),
        },
        {
            accessorKey: 'weekendsWorked',
            header: 'Fines de Semana laburados',
            size: 190,
            grow: false,
            enableColumnFilter: false,
        },
    ];
}

export function renderDriverActions({ onEdit, onDelete, canEdit, canDelete }) {
    return ({ row }) => {
        const actions = [
            {
                key: 'edit',
                label: 'Editar chofer',
                icon: <EditIcon fontSize="small" />,
                hidden: !canEdit,
                onClick: () => onEdit(row.original),
            },
            {
                key: 'delete',
                label: 'Eliminar chofer',
                icon: <DeleteIcon fontSize="small" />,
                hidden: !canDelete,
                onClick: () => onDelete(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}
