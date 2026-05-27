import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';

export function getMaintenanceTechnicianColumns() {
    return [
        { accessorKey: 'fullName', header: 'Nombre completo', size: 220, grow: true },
        { accessorKey: 'position', header: 'Puesto', size: 180, grow: true },
        { accessorKey: 'email', header: 'Correo', size: 220, grow: true },
        { accessorKey: 'phone', header: 'Teléfono', size: 160, grow: false },
        {
            accessorKey: 'leader',
            header: 'Líder',
            size: 120,
            grow: false,
            enableColumnFilter: false,
            enableSorting: false,
            muiTableHeadCellProps: { align: 'center' },
            muiTableBodyCellProps: { align: 'center' },
            Cell: ({ cell }) => (
                <Chip label={cell.getValue() ? 'Sí' : 'No'} size="small" variant="outlined" color={cell.getValue() ? 'success' : 'default'} />
            ),
        },
    ];
}

export function renderMaintenanceTechnicianActions({ onEdit, onDelete, canEdit, canDelete }) {
    return ({ row }) => {
        const actions = [
            {
                key: 'edit',
                label: 'Editar técnico',
                icon: <EditIcon fontSize="small" />,
                hidden: !canEdit,
                onClick: () => onEdit(row.original),
            },
            {
                key: 'delete',
                label: 'Eliminar técnico',
                icon: <DeleteIcon fontSize="small" />,
                hidden: !canDelete,
                onClick: () => onDelete(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}

