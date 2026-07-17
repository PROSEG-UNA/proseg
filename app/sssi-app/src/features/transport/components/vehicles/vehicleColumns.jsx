import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import { formatDateTime, getStatusChipColor } from '../../transportUtils';

export function getVehicleColumns() {
    return [
        { accessorKey: 'plate', header: 'Placa', size: 150, grow: false },
        { accessorKey: 'brand', header: 'Marca', size: 180, grow: false },
        { accessorKey: 'model', header: 'Modelo', size: 180, grow: false },
        { accessorKey: 'year', header: 'Año', size: 110, grow: false },
        { accessorKey: 'capacity', header: 'Capacidad', size: 120, grow: false },
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
        {
            accessorKey: 'createdAt',
            header: 'Creado',
            size: 170,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
        {
            accessorKey: 'updatedAt',
            header: 'Actualizado',
            size: 170,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
    ];
}

export function renderVehicleActions({ onEdit, onDelete, canEdit, canDelete }) {
    return ({ row }) => {
        const actions = [
            {
                key: 'edit',
                label: 'Editar vehículo',
                icon: <EditIcon fontSize="small" />,
                hidden: !canEdit,
                onClick: () => onEdit(row.original),
            },
            {
                key: 'delete',
                label: 'Eliminar vehículo',
                icon: <DeleteIcon fontSize="small" />,
                hidden: !canDelete,
                onClick: () => onDelete(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}
