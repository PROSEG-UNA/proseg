import { useCallback, useEffect, useMemo, useState } from 'react';
import EditIcon from '@mui/icons-material/Edit';
import VisibilityIcon from '@mui/icons-material/Visibility';
import FormatListBulletedIcon from '@mui/icons-material/FormatListBulleted';
import NewReleasesOutlinedIcon from '@mui/icons-material/NewReleasesOutlined';
import AutorenewIcon from '@mui/icons-material/Autorenew';
import CheckCircleOutlinedIcon from "@mui/icons-material/CheckCircleOutlined";
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import { Box, Chip, Tab, Tabs } from '@mui/material';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { useMaintenanceTicketsData } from '../../hooks/useMaintenanceTicketsData';
import { MAINTENANCE_PRIORITY_OPTIONS, MAINTENANCE_TICKET_STATUS_OPTIONS, formatDateHourMinute } from '../../maintenanceUtils';

const ALL_TAB_VALUE = 'ALL';

const STATUS_TAB_ICONS = {
    ALL: <FormatListBulletedIcon sx={{ fontSize: 18 }} />,
    OPEN: <NewReleasesOutlinedIcon sx={{ fontSize: 18 }} />,
    IN_PROGRESS: <AutorenewIcon sx={{ fontSize: 18 }} />,
    RESOLVED: <CheckCircleOutlinedIcon sx={{ fontSize: 18 }} />,
    REOPENED: <AutorenewIcon sx={{ fontSize: 18 }} />,
    CANCELLED: <CancelOutlinedIcon sx={{ fontSize: 18 }} />,
};

const STATUS_TAB_LABELS = {
    ALL: 'Todos',
    OPEN: 'Abiertos',
    IN_PROGRESS: 'En Progreso',
    RESOLVED: 'Resueltos',
    REOPENED: 'Reabiertos',
    CANCELLED: 'Cancelados',
};

const STATUS_TABS = [ALL_TAB_VALUE, ...MAINTENANCE_TICKET_STATUS_OPTIONS.map((o) => o.value)];

const COLUMN_TO_BACKEND_KEY = {
    title: 'title',
    description: 'description',
    statusRaw: 'status',
    createdByName: 'createdBy',
    updatedAt: 'updatedAt',
};

function statusColor(status) {
    if (status === 'OPEN') return 'info';
    if (status === 'IN_PROGRESS') return 'warning';
    if (status === 'RESOLVED') return 'success';
    if (status === 'REOPENED') return 'warning';
    if (status === 'CANCELLED') return 'error';
    return 'default';
}

const PRIORITY_LABELS = Object.fromEntries(
    MAINTENANCE_PRIORITY_OPTIONS.map((option) => [option.value, option.label])
);

const STATUS_LABELS = Object.fromEntries(
    MAINTENANCE_TICKET_STATUS_OPTIONS.map((option) => [option.value, option.label])
);

export default function MaintenanceTicketTable({ refreshKey = 0, onRefresh, onEdit, onView }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [statusTab, setStatusTab] = useState(ALL_TAB_VALUE);
    const [alert, setAlert] = useState(null);
    const { hasPermission } = usePermissions();

    const canViewTickets = hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.READ)
        || hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.CREATE)
        || hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.EDIT)
        || hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.DELETE);
    const canEditTickets = hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.EDIT);

    const debouncedGlobalFilter = useDebounce(globalFilter, 350);
    const debouncedColumnFilters = useDebounce(columnFilters, 350);

    const backendFilters = useMemo(() => {
        const output = {};
        debouncedColumnFilters.forEach(({ id, value }) => {
            const key = COLUMN_TO_BACKEND_KEY[id];
            if (!key) return;
            if (value === null || value === undefined || value === '') return;
            output[key] = value;
        });
        if (statusTab !== ALL_TAB_VALUE) {
            output.status = statusTab;
        }
        return output;
    }, [debouncedColumnFilters, statusTab]);

    const backendSort = useMemo(
        () => sorting
            .map(({ id, desc }) => {
                const key = COLUMN_TO_BACKEND_KEY[id];
                if (!key) return null;
                return `${key},${desc ? 'desc' : 'asc'}`;
            })
            .filter(Boolean),
        [sorting]
    );

    useEffect(() => {
        setPagination((prev) => (prev.pageIndex === 0 ? prev : { ...prev, pageIndex: 0 }));
    }, [debouncedGlobalFilter, backendFilters, backendSort, statusTab]);

    const { rows, loading, error, totalElements } = useMaintenanceTicketsData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
        refreshKey,
    });

    const columns = useMemo(() => [
        {
            accessorKey: 'title',
            header: 'Titulo',
            enableColumnFilter: true,
        },
        {
            accessorKey: 'description',
            header: 'Descripcion',
            enableColumnFilter: true,
        },
        {
            accessorKey: 'statusRaw',
            header: 'Estado',
            enableColumnFilter: true,
            Cell: ({ row }) => (
                <Chip
                    label={STATUS_LABELS[row.original.statusRaw] ?? row.original.statusRaw}
                    size="small"
                    variant="outlined"
                    color={statusColor(row.original.statusRaw)}
                />
            ),
        },
        {
            accessorKey: 'createdByName',
            header: 'Creado por',
            enableColumnFilter: true,
            Cell: ({ row }) => row.original.createdByName || row.original.createdBy || '—',
        },
        {
            accessorKey: 'updatedAt',
            header: 'Modificado',
            enableColumnFilter: true,
            Cell: ({ row }) => formatDateHourMinute(row.original.updatedAt) || '—',
        }
    ], []);

    const renderRowActions = ({ row }) => {
        if (!canViewTickets) {
            return null;
        }

        const actions = [
            {
                key: 'view-detail',
                label: 'Ver detalle',
                icon: <VisibilityIcon fontSize="small" />,
                onClick: () => handleView(row.original),
            },
            {
                key: 'edit-ticket',
                label: 'Editar ticket',
                icon: <EditIcon fontSize="small" />,
                hidden: !canEditTickets,
                onClick: () => handleEdit(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Acciones" />;
    };

    const handleView = useCallback((row) => {
        onView?.(row);
    }, [onView]);

    const handleEdit = useCallback((row) => {
        onEdit?.(row);
    }, [onEdit]);

    return (
        <>
            <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 1 }}>
                <Tabs
                    value={statusTab}
                    onChange={(_, newValue) => {
                        setStatusTab(newValue);
                        setPagination((prev) => ({ ...prev, pageIndex: 0 }));
                    }}
                    variant="scrollable"
                    scrollButtons="auto"
                    allowScrollButtonsMobile
                    aria-label="Filtros por estado de ticket"
                >
                    {STATUS_TABS.map((status) => (
                        <Tab
                            key={status}
                            value={status}
                            label={STATUS_TAB_LABELS[status]}
                            icon={STATUS_TAB_ICONS[status]}
                            iconPosition="start"
                            sx={{ textTransform: 'none', fontWeight: 700 }}
                        />
                    ))}
                </Tabs>
            </Box>

            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                error={error}
                enableRowActions={canViewTickets}
                renderRowActions={renderRowActions}
                tableOptions={{
                    enableExpanding: false,
                    positionActionsColumn: 'last',
                    manualPagination: true,
                    manualFiltering: true,
                    manualSorting: true,
                    rowCount: totalElements,
                    onPaginationChange: setPagination,
                    onGlobalFilterChange: setGlobalFilter,
                    onColumnFiltersChange: setColumnFilters,
                    onSortingChange: setSorting,
                    state: { pagination, globalFilter, columnFilters, sorting },
                    initialState: {
                        columnVisibility: {
                            assignedRole: true,
                            assetsCount: false,
                            photosCount: false,
                        },
                    },
                }}
                enableGlobalFilter
            />

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}