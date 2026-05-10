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

export function getAssetsColumns() {
    return [
        {
            accessorKey: 'name',
            header: 'Nombre',
            size: 200,
            grow: true,
        },
        {
            accessorKey: 'brand',
            header: 'Marca',
            size: 160,
            grow: true,
        },
        {
            accessorKey: 'model',
            header: 'Modelo',
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
