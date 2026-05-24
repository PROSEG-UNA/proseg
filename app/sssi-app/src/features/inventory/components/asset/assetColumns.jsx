import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';

const STATUS_FILTER_OPTIONS = [
    { value: 'APROBADO', label: 'Aprobado' },
    { value: 'DE_BAJA', label: 'De baja' },
];

function statusChipColor(statusRaw) {
    if (statusRaw === 'APROBADO') return 'success';
    if (statusRaw === 'DE_BAJA') return 'error';
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
            accessorKey: 'executingUnit',
            header: 'Unidad Ejecutora',
            size: 180,
            grow: true,
        },
        {
            accessorKey: 'responsibleEmployee',
            header: 'Funcionario',
            size: 200,
            grow: true,
        },
        {
            accessorKey: 'responsibleEmployeeId',
            header: 'ID Funcionario',
            size: 160,
            grow: false,
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
            accessorKey: 'decommissionDate',
            header: 'Fecha de Baja',
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

export function renderAssetActions({ onEdit, onDelete, canEdit, canDelete }) {
    return ({ row }) => {
        const actions = [
            {
                key: 'edit',
                label: 'Editar activo',
                icon: <EditIcon fontSize="small" />,
                hidden: !canEdit,
                onClick: () => onEdit(row.original),
            },
            {
                key: 'delete',
                label: 'Eliminar activo',
                icon: <DeleteIcon fontSize="small" />,
                hidden: !canDelete,
                onClick: () => onDelete(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };
}
