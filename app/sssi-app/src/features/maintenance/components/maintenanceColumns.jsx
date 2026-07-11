import Chip from '@mui/material/Chip';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import RowActionsMenu from "../../../common/components/RowActionsMenu.jsx";
import { formatDate } from '../../../common/utils/formatters.js';

const STATUS_FILTER_OPTIONS = [
    { value: 'PENDING', label: 'Pendiente' },
    { value: 'IN_PROGRESS', label: 'En progreso' },
    { value: 'COMPLETED', label: 'Completado' },
    { value: 'CANCELLED', label: 'Cancelado' },
];

const PRIORITY_FILTER_OPTIONS = [
    { value: 'LOW', label: 'Baja' },
    { value: 'MEDIUM', label: 'Media' },
    { value: 'HIGH', label: 'Alta' },
    { value: 'URGENT', label: 'Urgente' },
];

function statusChipColor(statusRaw) {
    if (statusRaw === 'COMPLETED') return 'success';
    if (statusRaw === 'IN_PROGRESS') return 'info';
    if (statusRaw === 'PENDING') return 'warning';
    if (statusRaw === 'CANCELLED') return 'error';
    return 'default';
}

function priorityChipColor(priorityRaw) {
    if (priorityRaw === 'URGENT') return 'error';
    if (priorityRaw === 'HIGH') return 'warning';
    if (priorityRaw === 'MEDIUM') return 'info';
    if (priorityRaw === 'LOW') return 'default';
    return 'default';
}

export function getMaintenanceRequestColumns() {
    return [
        {
            accessorKey: 'title',
            header: 'Título',
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
            accessorKey: 'assetId',
            header: 'ID del Activo',
            size: 150,
            grow: false,
        },
        {
            accessorKey: 'company',
            header: 'Compañía',
            size: 160,
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
            accessorKey: 'priority',
            header: 'Prioridad',
            size: 130,
            grow: false,
            filterVariant: 'select',
            filterSelectOptions: PRIORITY_FILTER_OPTIONS,
            muiTableHeadCellProps: { align: 'center' },
            muiTableBodyCellProps: { align: 'center' },
            Cell: ({ row, cell }) => (
                <Chip
                    label={cell.getValue()}
                    color={priorityChipColor(row.original.priorityRaw)}
                    size="small"
                    variant="outlined"
                />
            ),
        },
        {
            accessorKey: 'scheduledDate',
            header: 'Fecha Programada',
            size: 150,
            grow: false,
            Cell: ({ cell }) => formatDate(cell.getValue()),
        },
        {
            accessorKey: 'observations',
            header: 'Observaciones',
            size: 200,
            grow: true,
        },
    ];
}

export function renderMaintenanceRequestActions({ onEdit, onDelete, canEdit, canDelete }) {
    return ({ row }) => (
        <RowActionsMenu
            actions={[
                ...(canEdit ? [{ icon: EditIcon, label: 'Editar', onClick: () => onEdit(row.original) }] : []),
                ...(canDelete ? [{ icon: DeleteIcon, label: 'Eliminar', onClick: () => onDelete(row.original) }] : []),
            ]}
        />
    );
}

