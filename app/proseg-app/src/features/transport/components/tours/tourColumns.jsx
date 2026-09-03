import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import { formatDateTime, getStatusChipColor } from '../../transportUtils';

export function getTourColumns() {
    return [
        { accessorKey: 'name', header: 'Gira', size: 220, grow: true },
        { accessorKey: 'origin', header: 'Origen', size: 180, grow: true },
        { accessorKey: 'destination', header: 'Destino', size: 180, grow: true },
        { accessorKey: 'driverName', header: 'Chofer', size: 200, grow: true },
        { accessorKey: 'vehiclePlate', header: 'Vehículo', size: 150, grow: false },
        {
            accessorKey: 'startDate',
            header: 'Inicio',
            size: 170,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
        {
            accessorKey: 'endDate',
            header: 'Fin',
            size: 170,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
        {
            accessorKey: 'status',
            header: 'Estado',
            size: 150,
            grow: false,
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
}

export function renderTourActions({ onEdit, onDelete, canEdit, canDelete }) {
    return ({ row }) => {
        const actions = [
            {
                key: 'edit',
                label: 'Editar gira',
                icon: <EditIcon fontSize="small" />,
                hidden: !canEdit,
                onClick: () => onEdit(row.original),
            },
            {
                key: 'delete',
                label: 'Eliminar gira',
                icon: <DeleteIcon fontSize="small" />,
                hidden: !canDelete,
                onClick: () => onDelete(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}
