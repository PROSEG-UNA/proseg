import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import { formatDateTime, getStatusChipColor } from '../../transportUtils';

export function getDriverColumns() {
    return [
        { accessorKey: 'fullName', header: 'Nombre', size: 220, grow: true },
        { accessorKey: 'documentId', header: 'Identificación', size: 180, grow: false },
        { accessorKey: 'licenseNumber', header: 'Licencia', size: 180, grow: false },
        { accessorKey: 'phone', header: 'Teléfono', size: 160, grow: false },
        { accessorKey: 'email', header: 'Correo', size: 220, grow: true },
        {
            accessorKey: 'status',
            header: 'Estado',
            size: 130,
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
