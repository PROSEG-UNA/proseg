import IconButton from '@mui/material/IconButton';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import Tooltip from '@mui/material/Tooltip';
import Box from '@mui/material/Box';

export function getRolesColumns() {
    return [
        {
            accessorKey: 'name',
            header: 'Nombre',
            size: 140,
            grow: true,
        },
        {
            accessorKey: 'description',
            header: 'Descripción',
            size: 160,
            grow: 2,
        },
        {
            accessorKey: 'permissionCount',
            header: '# Permisos',
            size: 120,
            grow: false,
            muiTableHeadCellProps: { align: 'center' },
            muiTableBodyCellProps: { align: 'center' },
        },
    ];
}

export function renderRolesActions({ onEdit, onDelete }) {
    return ({ row }) => (
        <Box sx={{ display: 'flex', gap: '4px', justifyContent: 'center' }}>
            <Tooltip title="Editar">
                <IconButton size="small" onClick={() => onEdit(row.original)}>
                    <EditIcon fontSize="small" sx={{ color: 'primary.icon' }} />
                </IconButton>
            </Tooltip>
            <Tooltip title="Eliminar">
                <IconButton size="small" onClick={() => onDelete(row.original)}>
                    <DeleteIcon fontSize="small" sx={{ color: 'primary.icon' }} />
                </IconButton>
            </Tooltip>
        </Box>
    );
}
