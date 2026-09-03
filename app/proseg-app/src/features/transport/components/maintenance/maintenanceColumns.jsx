import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import { formatCurrency, formatDate, formatDateTime, getStatusChipColor } from '../../transportUtils';

export function getTransportMaintenanceColumns() {
    return [
        { accessorKey: 'vehiclePlate', header: 'Vehículo', size: 150, grow: false },
        { accessorKey: 'title', header: 'Actividad', size: 220, grow: true },
        { accessorKey: 'type', header: 'Tipo', size: 160, grow: false },
        {
            accessorKey: 'scheduledDate',
            header: 'Fecha programada',
            size: 150,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDate(cell.getValue()),
        },
        {
            accessorKey: 'cost',
            header: 'Costo',
            size: 140,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatCurrency(cell.getValue()),
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

export function renderTransportMaintenanceActions({ onEdit, onDelete, canEdit, canDelete }) {
    return ({ row }) => {
        const actions = [
            {
                key: 'edit',
                label: 'Editar mantenimiento',
                icon: <EditIcon fontSize="small" />,
                hidden: !canEdit,
                onClick: () => onEdit(row.original),
            },
            {
                key: 'delete',
                label: 'Eliminar mantenimiento',
                icon: <DeleteIcon fontSize="small" />,
                hidden: !canDelete,
                onClick: () => onDelete(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}
