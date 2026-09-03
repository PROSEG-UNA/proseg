import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import RowActionsMenu from '../../../common/components/RowActionsMenu.jsx';
import { formatRoleName } from '../../../common/utils/index.js';

export function getRolesColumns() {
    return [
        {
            accessorKey: 'name',
            header: 'Nombre',
            size: 140,
            grow: true,
            Cell: ({ cell }) => formatRoleName(cell.getValue()),
        },
        {
            accessorKey: 'description',
            header: 'Descripción',
            size: 160,
            grow: 2,
        },
    ];
}

export function renderRolesActions({ onEdit, onDelete, canEdit, canDelete }) {
    return ({ row }) => {
        const actions = [
            {
                key: 'edit',
                label: 'Editar',
                icon: <EditIcon fontSize="small" />,
                hidden: !canEdit,
                onClick: () => onEdit(row.original),
            },
            {
                key: 'delete',
                label: 'Eliminar',
                icon: <DeleteIcon fontSize="small" />,
                color: 'error',
                hidden: !canDelete,
                onClick: () => onDelete(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}
