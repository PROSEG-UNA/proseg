import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import PeopleIcon from '@mui/icons-material/People';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import { formatDateTime } from '../../maintenanceUtils';

export function getCompanyColumns() {
    return [
        { accessorKey: 'name', header: 'Nombre', size: 220, grow: true },
        { accessorKey: 'legalId', header: 'Cédula Jurídica', size: 180, grow: false },
        { accessorKey: 'contactEmail', header: 'Correo', size: 220, grow: true },
        { accessorKey: 'contactPhone', header: 'Teléfono', size: 160, grow: false },
        { accessorKey: 'address', header: 'Dirección', size: 260, grow: true },
        {
            accessorKey: 'usersCount',
            header: 'Usuarios',
            size: 120,
            grow: false,
            enableColumnFilter: false,
            enableSorting: false,
            muiTableHeadCellProps: { align: 'center' },
            muiTableBodyCellProps: { align: 'center' },
            Cell: ({ cell }) => (
                <Chip
                    label={cell.getValue() ?? 0}
                    size="small"
                    variant="outlined"
                    color="primary"
                />
            ),
        },
        {
            accessorKey: 'createdAt',
            header: 'Creada',
            size: 170,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
        {
            accessorKey: 'updatedAt',
            header: 'Actualizada',
            size: 170,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
    ];
}

export function renderCompanyActions({ onEdit, onDelete, onManageUsers, canEdit, canDelete, canManageUsers }) {
    return ({ row }) => {
        const actions = [
            {
                key: 'users',
                label: 'Gestionar usuarios',
                icon: <PeopleIcon fontSize="small" />,
                hidden: !canManageUsers,
                onClick: () => onManageUsers(row.original),
            },
            {
                key: 'edit',
                label: 'Editar empresa',
                icon: <EditIcon fontSize="small" />,
                hidden: !canEdit,
                onClick: () => onEdit(row.original),
            },
            {
                key: 'delete',
                label: 'Eliminar empresa',
                icon: <DeleteIcon fontSize="small" />,
                hidden: !canDelete,
                onClick: () => onDelete(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}

