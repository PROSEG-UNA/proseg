import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import MiscellaneousServicesIcon from '@mui/icons-material/MiscellaneousServices';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import { formatDate, formatDateTime } from '../../maintenanceUtils';

function statusColor(status) {
    if (status === 'COMPLETED') return 'success';
    if (status === 'IN_PROGRESS') return 'primary';
    if (status === 'CANCELLED') return 'error';
    return 'warning';
}

function priorityColor(priority) {
    if (priority === 'CRITICAL') return 'error';
    if (priority === 'HIGH') return 'warning';
    if (priority === 'MEDIUM') return 'info';
    return 'default';
}

export function getMaintenanceRequestColumns() {
    return [
        { accessorKey: 'companyName', header: 'Empresa', size: 210, grow: true },
        { accessorKey: 'companyLegalId', header: 'Cédula Jurídica', size: 170, grow: false },
        { accessorKey: 'assetId', header: 'Activo', size: 240, grow: true },
        { accessorKey: 'title', header: 'Título', size: 220, grow: true },
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
            accessorKey: 'priorityRaw',
            header: 'Prioridad',
            size: 140,
            grow: false,
            filterVariant: 'select',
            muiTableHeadCellProps: { align: 'center' },
            muiTableBodyCellProps: { align: 'center' },
            Cell: ({ row }) => (
                <Chip label={row.original.priority} size="small" variant="outlined" color={priorityColor(row.original.priorityRaw)} />
            ),
        },
        {
            accessorKey: 'scheduledDate',
            header: 'Programada',
            size: 160,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDate(cell.getValue()),
        },
        {
            accessorKey: 'techniciansCount',
            header: 'Técnicos',
            size: 120,
            grow: false,
            enableColumnFilter: false,
            enableSorting: false,
            muiTableHeadCellProps: { align: 'center' },
            muiTableBodyCellProps: { align: 'center' },
            Cell: ({ cell }) => (
                <Chip label={cell.getValue() ?? 0} size="small" variant="outlined" color="secondary" />
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
    ];
}

export function renderMaintenanceRequestActions({ onEdit, onDelete, onAddTechnician, canEdit, canDelete, canAddTechnician }) {
    return ({ row }) => {
        const actions = [
            {
                key: 'technician',
                label: 'Agregar técnico',
                icon: <MiscellaneousServicesIcon fontSize="small" />,
                hidden: !canAddTechnician,
                onClick: () => onAddTechnician(row.original),
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

