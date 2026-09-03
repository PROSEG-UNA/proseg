import Chip from '@mui/material/Chip';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import { formatDate, formatDateTime } from '../../maintenanceUtils';

function statusColor(status) {
    if (status === 'COMPLETED') return 'success';
    if (status === 'CANCELLED') return 'error';
    return 'warning';
}

export function getRegisterColumns() {
    return [
        { accessorKey: 'companyName', header: 'Empresa', size: 210, grow: true },
        { accessorKey: 'companyLegalId', header: 'Cédula Jurídica', size: 170, grow: false },
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
            accessorKey: 'responsibleName',
            header: 'Responsable',
            size: 200,
            grow: true,
            enableColumnFilter: false,
            enableSorting: false,
        },
        {
            accessorKey: 'startDate',
            header: 'Inicio',
            size: 150,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDate(cell.getValue()),
        },
        {
            accessorKey: 'endDate',
            header: 'Fin',
            size: 150,
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

export function renderRegisterActions({ onOpen }) {
    return ({ row }) => {
        const actions = [
            {
                key: 'open',
                label: 'Abrir registro',
                icon: <OpenInNewIcon fontSize="small" />,
                onClick: () => onOpen(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}
