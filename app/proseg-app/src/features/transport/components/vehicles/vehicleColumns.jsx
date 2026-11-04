import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import { VEHICLE_TYPE_OPTIONS, vehicleTypeLabel } from '../../transportUtils';

export function getVehicleColumns() {
    return [
        { accessorKey: 'plate', header: 'Placa', size: 150, grow: false },
        {
            accessorKey: 'type',
            header: 'Tipo',
            size: 170,
            grow: false,
            filterVariant: 'select',
            filterSelectOptions: VEHICLE_TYPE_OPTIONS,
            Cell: ({ cell }) => (cell.getValue() ? vehicleTypeLabel(cell.getValue()) : '—'),
        },
        { accessorKey: 'capacity', header: 'Capacidad', size: 120, grow: false },
        {
            accessorKey: 'available',
            header: 'Estado',
            size: 150,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => (
                <Chip
                    label={cell.getValue() ? 'Disponible' : 'No disponible'}
                    size="small"
                    color={cell.getValue() ? 'success' : 'error'}
                    variant="outlined"
                />
            ),
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
