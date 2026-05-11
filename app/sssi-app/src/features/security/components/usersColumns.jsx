import Chip from '@mui/material/Chip';
import AddIcon from '@mui/icons-material/Add';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import RowActionsMenu from '../../../common/components/RowActionsMenu.jsx';

function statusChipColor(statusRaw) {
    if (statusRaw === 'APPROVED') return 'success';
    if (statusRaw === 'REJECTED') return 'error';
    if (statusRaw === 'INVITED') return 'secondary';
    return 'warning';
}

export function getUsersColumns() {
    return [
        {
            accessorKey: 'username',
            header: 'Usuario',
            size: 160,
            grow: true,
        },
        {
            accessorKey: 'fullName',
            header: 'Nombre',
            size: 220,
            grow: true,
        },
        {
            accessorKey: 'email',
            header: 'Correo',
            size: 260,
            grow: 2,
        },
        {
            accessorKey: 'status',
            header: 'Estado',
            size: 140,
            grow: false,
            muiTableHeadCellProps: { align: 'center' },
            muiTableBodyCellProps: { align: 'center' },
            Cell: ({ row, cell }) => (
                <Chip
                    label={cell.getValue()}
                    color={statusChipColor(row.original.statusRaw)}
                    size="small"
                    variant="outlined"
                />
            ),
        },
    ];
}

export function renderUsersActions({ onAssignRoles, onApprove, onReject }) {
    return ({ row }) => {
        const status = row.original.statusRaw;
        const showApprove = status !== 'APPROVED';
        const showReject = status !== 'REJECTED';
        const showAssignRoles = status !== 'REJECTED';

        const approveTitle = status === 'REJECTED' ? 'Activar usuario' : 'Aprobar usuario';
        const rejectTitle = status === 'APPROVED' ? 'Desactivar usuario' : 'Rechazar usuario';

        const actions = [
            {
                key: 'assign-role',
                label: 'Asignar roles',
                icon: <AddIcon fontSize="small" />,
                hidden: !showAssignRoles,
                onClick: () => onAssignRoles(row.original),
            },
            {
                key: 'approve',
                label: approveTitle,
                icon: <CheckIcon fontSize="small" />,
                hidden: !showApprove,
                onClick: () => onApprove(row.original),
            },
            {
                key: 'reject',
                label: rejectTitle,
                icon: <CloseIcon fontSize="small" />,
                hidden: !showReject,
                onClick: () => onReject(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}
