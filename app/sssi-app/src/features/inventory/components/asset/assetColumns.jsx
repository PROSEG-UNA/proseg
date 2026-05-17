import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';

const STATUS_FILTER_OPTIONS = [
    { value: 'BUENO', label: 'Bueno' },
    { value: 'REGULAR', label: 'Regular' },
    { value: 'MALO', label: 'Malo' },
    { value: 'EN_REPARACION', label: 'En reparación' },
    { value: 'BAJA', label: 'Baja' },
];

function statusChipColor(statusRaw) {
    if (statusRaw === 'BUENO') return 'success';
    if (statusRaw === 'REGULAR') return 'warning';
    if (statusRaw === 'MALO') return 'error';
    if (statusRaw === 'EN_REPARACION') return 'info';
    return 'default';
}

function formatDate(value) {
    if (!value) return '—';
    const [year, month, day] = value.split('-');
    return `${day}/${month}/${year}`;
}

export function getAssetsColumns() {
    return [
        {
            accessorKey: 'assetNumber',
            header: 'N° Activo',
            size: 150,
            grow: false,
        },
        {
            accessorKey: 'serialNumber',
            header: 'N° Serie',
            size: 170,
            grow: false,
        },
        {
            accessorKey: 'name',
            header: 'Nombre',
            size: 200,
            grow: true,
        },
        {
            accessorKey: 'description',
            header: 'Descripción',
            size: 220,
            grow: true,
        },
        {
            accessorKey: 'type',
            header: 'Tipo',
            size: 140,
            grow: true,
        },
        {
            accessorKey: 'brand',
            header: 'Marca',
            size: 140,
            grow: true,
        },
        {
            accessorKey: 'model',
            header: 'Modelo',
            size: 160,
            grow: true,
        },
        {
            accessorKey: 'site',
            header: 'Sede',
            size: 160,
            grow: true,
        },
        {
            accessorKey: 'location',
            header: 'Ubicación',
            size: 180,
            grow: true,
        },
        {
            accessorKey: 'status',
            header: 'Estado',
            size: 140,
            grow: false,
            filterVariant: 'select',
            filterSelectOptions: STATUS_FILTER_OPTIONS,
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
        {
            accessorKey: 'acquisitionDate',
            header: 'Fecha de Adquisición',
            size: 180,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDate(cell.getValue()),
        },
        {
            accessorKey: 'warrantyEndDate',
            header: 'Fecha de fin Garantía',
            size: 180,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDate(cell.getValue()),
        },
        {
            accessorKey: 'firmwareSupportEndDate',
            header: 'Fecha de fin Firmware',
            size: 180,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDate(cell.getValue()),
        },
        {
            accessorKey: 'latitude',
            header: 'Latitud',
            size: 130,
            grow: false,
            enableColumnFilter: false,
            enableSorting: false,
            Cell: ({ cell }) => cell.getValue() != null ? cell.getValue() : '—',
        },
        {
            accessorKey: 'longitude',
            header: 'Longitud',
            size: 130,
            grow: false,
            enableColumnFilter: false,
            enableSorting: false,
            Cell: ({ cell }) => cell.getValue() != null ? cell.getValue() : '—',
        },
    ];
}

export function renderAssetActions({ onEdit, onDelete }) {
    return ({ row }) => {
        const actions = [
            {
                key: 'edit',
                label: 'Editar activo',
                icon: <EditIcon fontSize="small" />,
                onClick: () => onEdit(row.original),
            },
            {
                key: 'delete',
                label: 'Eliminar activo',
                icon: <DeleteIcon fontSize="small" />,
                onClick: () => onDelete(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}
