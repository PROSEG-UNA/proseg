import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import { formatDateTime } from '../../maintenanceUtils';

function statusColor(status) {
    if (status === 'RESOLVED') return 'success';
    if (status === 'IN_PROGRESS') return 'primary';
    if (status === 'REOPENED') return 'warning';
    if (status === 'CANCELLED') return 'error';
    return 'warning';
}

function priorityColor(priority) {
    if (priority === 'CRITICAL') return 'error';
    if (priority === 'HIGH') return 'warning';
    if (priority === 'MEDIUM') return 'info';
    return 'default';
}

export function getMaintenanceTicketColumns() {
    return [
        {
            accessorKey: 'description',
            header: 'Descripción',
            size: 280,
            grow: true,
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
            accessorKey: 'siteName',
            header: 'Sede',
            size: 180,
            grow: true,
        },
        {
            accessorKey: 'buildingName',
            header: 'Edificio',
            size: 180,
            grow: true,
        },
        {
            accessorKey: 'floorName',
            header: 'Piso',
            size: 140,
            grow: false,
        },
        {
            accessorKey: 'locationDescription',
            header: 'Detalle de Ubicación',
            size: 220,
            grow: true,
        },
        {
            accessorKey: 'assignedRole',
            header: 'Rol asignado',
            size: 180,
            grow: true,
            Cell: ({ cell }) => cell.getValue() || '—',
        },
        {
            accessorKey: 'createdBy',
            header: 'Creado por',
            size: 180,
            grow: false,
        },
        {
            accessorKey: 'assetsCount',
            header: 'Activos',
            size: 100,
            grow: false,
            enableColumnFilter: false,
            enableSorting: false,
            muiTableHeadCellProps: { align: 'center' },
            muiTableBodyCellProps: { align: 'center' },
            Cell: ({ cell }) => <Chip label={cell.getValue() ?? 0} size="small" variant="outlined" color="secondary" />,
        },
        {
            accessorKey: 'photosCount',
            header: 'Fotos',
            size: 100,
            grow: false,
            enableColumnFilter: false,
            enableSorting: false,
            muiTableHeadCellProps: { align: 'center' },
            muiTableBodyCellProps: { align: 'center' },
            Cell: ({ cell }) => <Chip label={cell.getValue() ?? 0} size="small" variant="outlined" color="info" />,
        },
        {
            accessorKey: 'createdAt',
            header: 'Creado',
            size: 170,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
    ];
}

export function renderMaintenanceTicketActions({ onEdit, onResolve, canEdit, canResolve }) {
    return ({ row }) => {
        const actions = [
            {
                key: 'edit',
                label: 'Editar ticket',
                icon: <EditIcon fontSize="small" />,
                hidden: !canEdit,
                onClick: () => onEdit(row.original),
            },
            {
                key: 'resolve',
                label: 'Resolver ticket',
                icon: <DoneAllIcon fontSize="small" />,
                hidden: !canResolve || row.original.statusRaw === 'RESOLVED',
                onClick: () => onResolve(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}