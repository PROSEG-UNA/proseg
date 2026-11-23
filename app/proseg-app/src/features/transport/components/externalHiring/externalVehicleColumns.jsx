import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';

export function getExternalVehicleColumns() {
    return [
        { accessorKey: 'provider', header: 'Proveedor', size: 280 },
        { accessorKey: 'plate', header: 'Placa', size: 150, grow: false },
        { accessorKey: 'capacity', header: 'Capacidad', size: 120, grow: false },
        {
            accessorKey: 'available',
            header: 'Estado',
            size: 170,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => (
                <Chip
                    label={cell.getValue() ? 'Disponible' : 'No disponible'}
                    size="small"
                    color={cell.getValue() ? 'success' : 'error'}
                    variant="outlined"
                    sx={{ width: 104, justifyContent: 'center' }}
                />
            ),
        },
    ];
}

export function renderExternalVehicleActions({ onEdit, onDelete, canEdit, canDelete }) {
    return ({ row }) => {
        const actions = [
            {
                key: 'edit',
                label: 'Editar vehículo externo',
                icon: <EditIcon fontSize="small" />,
                hidden: !canEdit,
                onClick: () => onEdit(row.original),
            },
            {
                key: 'delete',
                label: 'Eliminar vehículo externo',
                icon: <DeleteIcon fontSize="small" />,
                hidden: !canDelete,
                onClick: () => onDelete(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}
