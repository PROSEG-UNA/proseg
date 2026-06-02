import { useCallback, useEffect, useMemo, useState } from 'react';
import EditIcon from '@mui/icons-material/Edit';
import VisibilityIcon from '@mui/icons-material/Visibility';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { resolveMaintenanceTicket } from '../../services/ticketsService';
import { useMaintenanceTicketsData } from '../../hooks/useMaintenanceTicketsData';

const COLUMN_TO_BACKEND_KEY = {
    title: 'title',
    description: 'description',
    statusRaw: 'status',
    priorityRaw: 'priority',
    siteName: 'siteName',
    buildingName: 'buildingName',
    floorName: 'floorName',
    locationDescription: 'locationDescription',
    assignedRole: 'assignedRole',
    createdBy: 'createdBy',
};

export default function MaintenanceTicketTable({ refreshKey = 0, onRefresh, onEdit, onView }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [ticketToResolve, setTicketToResolve] = useState(null);
    const [alert, setAlert] = useState(null);
    const [resolving, setResolving] = useState(false);
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
        return output;
    }, [debouncedColumnFilters]);

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
    }, [debouncedGlobalFilter, backendFilters, backendSort]);

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
            accessorKey: 'priorityRaw',
            header: 'Prioridad',
            enableColumnFilter: true,
            Cell: ({ row }) => row.original.priority,
        },
        {
            accessorKey: 'createdAt',
            header: 'Creado',
            enableColumnFilter: true,
            Cell: ({ row }) => new Date(row.original.createdAt).toLocaleDateString('es-CR'),
        },
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
            {
                key: 'resolve-ticket',
                label: 'Resolver ticket',
                icon: <DoneAllIcon fontSize="small" />,
                hidden: !canEditTickets,
                onClick: () => handleResolve(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Acciones del ticket" />;
    };

    const handleView = useCallback((row) => {
        onView?.(row);
    }, [onView]);

    const handleEdit = useCallback((row) => {
        onEdit?.(row);
    }, [onEdit]);

    const handleResolve = useCallback((row) => setTicketToResolve(row), []);

    const handleResolveCancel = useCallback(() => {
        if (resolving) return;
        setTicketToResolve(null);
    }, [resolving]);

    const handleResolveConfirm = useCallback(async () => {
        if (!ticketToResolve) return;
        setResolving(true);
        try {
            await resolveMaintenanceTicket(ticketToResolve.id);
            setTicketToResolve(null);
            setAlert({ type: 'success', message: 'Ticket resuelto correctamente' });
            onRefresh?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo resolver el ticket' });
        } finally {
            setResolving(false);
        }
    }, [ticketToResolve, onRefresh]);

    return (
        <>
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
                type="warning"
                open={!!ticketToResolve}
                title="Resolver ticket"
                message={`¿Seguro que deseas resolver el ticket "${ticketToResolve?.title ?? ticketToResolve?.description}"?\nEsta acción cambiará su estado a RESUELTO.`}
                onClose={handleResolveCancel}
                onConfirm={handleResolveConfirm}
                confirmLabel={resolving ? 'Resolviendo...' : 'Resolver'}
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