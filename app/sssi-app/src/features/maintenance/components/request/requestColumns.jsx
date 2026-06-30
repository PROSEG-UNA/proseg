import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutlined';
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import { formatDate, formatDateTime } from '../../maintenanceUtils';

function statusColor(status) {
    if (status === 'COMPLETED') return 'success';
    if (status === 'ACCEPTED') return 'info';
    if (status === 'CANCELLED') return 'error';
    return 'warning';
}

export function getMaintenanceRequestColumns() {
    return [
        { accessorKey: 'companyName', header: 'Empresa', size: 210, grow: true },
        { accessorKey: 'companyLegalId', header: 'Cédula Jurídica', size: 170, grow: false },
        {
            accessorKey: 'email',
            header: 'Correo',
            size: 220,
            grow: true,
            enableColumnFilter: false,
            enableSorting: false,
        },
        {
            accessorKey: 'statusRaw',
            header: 'Estado',
            size: 140,
            grow: false,
            filterVariant: 'select',
            muiTableHeadCellProps: { align: 'center' },
            muiTableBodyCellProps: { align: 'center' },
            Cell: ({ row }) => (
                <Chip label={row.original.status} size="small" variant="outlined" color={statusColor(row.original.statusRaw)} />
            ),
        },
        {
            accessorKey: 'startDate',
            header: 'Inicio',
            size: 160,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDate(cell.getValue()),
        },
        {
            accessorKey: 'endDate',
            header: 'Fin',
            size: 160,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDate(cell.getValue()),
        },
        {
            accessorKey: 'createdAt',
            header: 'Creada',
            size: 170,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
    ];
}

export function renderMaintenanceRequestActions({
    onEdit,
    onDelete,
    onAccept,
    onCancel,
    canEdit,
    canDelete,
    canAccept,
    canCancel,
}) {
    return ({ row }) => {
        const status = row.original.statusRaw;
        const canAcceptRow = canAccept && (status === 'PENDING' || status === 'CANCELLED');
        const canCancelRow = canCancel && (status === 'PENDING' || status === 'ACCEPTED');

        const actions = [
            {
                key: 'accept',
                label: 'Aceptar solicitud',
                icon: <CheckCircleOutlineIcon fontSize="small" />,
                hidden: !canAcceptRow,
                onClick: () => onAccept(row.original),
            },
            {
                key: 'cancel',
                label: 'Cancelar solicitud',
                icon: <CancelOutlinedIcon fontSize="small" />,
                hidden: !canCancelRow,
                onClick: () => onCancel(row.original),
            },
            {
                key: 'edit',
                label: 'Editar solicitud',
                icon: <EditIcon fontSize="small" />,
                hidden: !canEdit,
                onClick: () => onEdit(row.original),
            },
            {
                key: 'delete',
                label: 'Eliminar solicitud',
                icon: <DeleteIcon fontSize="small" />,
                hidden: !canDelete,
                onClick: () => onDelete(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}
