import { useMemo, useState, useCallback, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {Box, Button, Tab, Tabs} from '@mui/material';
import TableBase from '../../../common/components/TablaBase.jsx';
import DialogModal from '../../../common/components/DialogModal.jsx';
import { useUsersData } from '../hooks/useUsersData';
import {fetchUserStatuses, resendInvitation, updateUserApproval} from '../services/usersService';
import { getUsersColumns, renderUsersActions } from './usersColumns.jsx';
import AssignUserRolesModal from './AssignUserRolesModal.jsx';
import ReplayIcon from '@mui/icons-material/Replay';
import { usePermissions } from '../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../common/constants/permissions';
import GroupsIcon from '@mui/icons-material/Groups';
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import BlockIcon from '@mui/icons-material/Block';
import MailIcon from '@mui/icons-material/Mail';
import { getFriendlyApiErrorMessage } from '../../../common/utils';
import { queryKeys } from '../../../common/query';

const STORAGE_KEY = 'users-table-column-visibility';
const DEFAULT_COLUMN_VISIBILITY = {};
const ALL_TAB_VALUE = 'ALL';
const FALLBACK_USER_STATUSES = ['PENDING', 'APPROVED', 'REJECTED', 'INVITED'];

function toStatusLabel(status) {
    switch (status) {
        case 'INVITED':
            return 'Invitados';
        case 'APPROVED':
            return 'Activos';
        case 'REJECTED':
            return 'Inactivos';
        case 'PENDING':
            return 'Pendientes';
        default:
            return status;
    }
}

function getStatusIcon(status) {
    switch (status) {
        case 'ALL':
            return <GroupsIcon sx={{ fontSize: 18 }} />;
        case 'PENDING':
            return <HourglassEmptyIcon sx={{ fontSize: 18 }} />;
        case 'APPROVED':
            return <CheckCircleIcon sx={{ fontSize: 18 }} />;
        case 'REJECTED':
            return <BlockIcon sx={{ fontSize: 18 }} />;
        case 'INVITED':
            return <MailIcon sx={{ fontSize: 18 }} />;
        default:
            return null;
    }
}

export default function UsersTable() {
    const [columnVisibility, setColumnVisibility] = useState(() => {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved) {
            try { return JSON.parse(saved); } catch { return DEFAULT_COLUMN_VISIBILITY; }
        }
        return DEFAULT_COLUMN_VISIBILITY;
    });
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [selectedUser, setSelectedUser] = useState(null);
    const [alert, setAlert] = useState(null);
    const [statusTab, setStatusTab] = useState(ALL_TAB_VALUE);
    const queryClient = useQueryClient();
    const { hasPermission } = usePermissions();
    const canAssignRoles = hasPermission(PERMISSIONS.USERS.ASSIGN_ROLE);
    const canApproveUsers = hasPermission(PERMISSIONS.USERS.APPROVE);
    const canUseActions = canAssignRoles || canApproveUsers;

    const persistColumnVisibility = () => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(columnVisibility));
    };

    useEffect(persistColumnVisibility, [columnVisibility]);

    const triggerRefresh = useCallback(() => {
        void queryClient.invalidateQueries({ queryKey: queryKeys.security.users() });
    }, [queryClient]);

    const { rows, loading, fetching, error, totalElements } = useUsersData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
    });

    const { data: fetchedStatuses } = useQuery({
        queryKey: queryKeys.security.userStatuses(),
        queryFn: async () => {
            const statuses = await fetchUserStatuses();
            return statuses.length > 0 ? statuses : null;
        },
        retry: false,
    });

    const availableStatuses = fetchedStatuses ?? FALLBACK_USER_STATUSES;

    const updateApprovalMutation = useMutation({
        mutationFn: ({ userId, status }) => updateUserApproval(userId, status),
        onSuccess: (_result, variables) => {
            setAlert({ type: 'success', message: variables.successMessage });
            triggerRefresh();
        },
        onError: (approvalError, variables) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(approvalError, variables.errorMessage) });
        },
    });

    const handleApprove = (user) => {
        updateApprovalMutation.mutate({
            userId: user.id,
            status: 'APPROVED',
            successMessage: user.statusRaw === 'REJECTED'
                ? 'Usuario activado correctamente.'
                : 'Usuario aprobado correctamente.',
            errorMessage: 'Error al aprobar usuario.',
        });
    };

    const handleReject = (user) => {
        updateApprovalMutation.mutate({
            userId: user.id,
            status: 'REJECTED',
            successMessage: user.statusRaw === 'APPROVED'
                ? 'Usuario desactivado correctamente.'
                : 'Usuario rechazado correctamente.',
            errorMessage: 'Error al rechazar usuario.',
        });
    };

    const columns = useMemo(() => {
        let baseColumns = getUsersColumns().map((column) => ({
            ...column,
            muiTableBodyCellProps: {
                ...(column.muiTableBodyCellProps ?? {}),
                sx: {
                    ...(column.muiTableBodyCellProps?.sx ?? {}),
                    py: 1.15,
                },
            },
        }));

        if (statusTab === 'INVITED') {
            baseColumns = baseColumns.filter(
                (column) =>
                    column.accessorKey !== 'username' &&
                    column.accessorKey !== 'fullName'
            );
            baseColumns.push({
                id: 'resendInvitation',
                header: 'Reenviar Invitación',
                size: 180,
                Cell: ({ row }) => (
                    <Button
                        variant="contained"
                        size="small"
                        color="secondary"
                        startIcon={<ReplayIcon />}
                        onClick={async () => {
                            try {
                                await resendInvitation(row.original.id);

                                setAlert({
                                    type: 'success',
                                    message: 'Invitación reenviada correctamente.',
                                });

                                triggerRefresh();
                            } catch (err) {
                                setAlert({
                                    type: 'error',
                                    message:
                                        getFriendlyApiErrorMessage(err, 'Error al reenviar invitación.'),
                                });
                            }
                        }}
                    >
                        Reenviar
                    </Button>
                ),
            });
        }

        return baseColumns;
    }, [statusTab]);

    const filteredRows = useMemo(() => {
        if (statusTab === ALL_TAB_VALUE) {
            return rows;
        }

        return rows.filter((row) => row.statusRaw === statusTab);
    }, [rows, statusTab]);

    const tabItems = useMemo(
        () => [ALL_TAB_VALUE, ...availableStatuses],
        [availableStatuses]
    );

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
                    aria-label="Filtros por estado de usuario"
                >
                    {tabItems.map((status) => (
                        <Tab
                            key={status}
                            value={status}
                            label={status === ALL_TAB_VALUE ? 'Todos' : toStatusLabel(status)}
                            icon={getStatusIcon(status)}
                            iconPosition="start"
                            sx={{ textTransform: 'none', fontWeight: 700 }}
                        />
                    ))}
                </Tabs>
            </Box>

            <TableBase
                columns={columns}
                data={filteredRows}
                loading={loading}
                fetching={fetching}
                error={error}
                enableRowActions={canUseActions}
                renderRowActions={canUseActions ? renderUsersActions({
                    onAssignRoles: setSelectedUser,
                    onApprove: handleApprove,
                    onReject: handleReject,
                    canAssignRoles,
                    canApprove: canApproveUsers,
                    canReject: canApproveUsers,
                }) : undefined}
                tableOptions={{
                    positionActionsColumn: 'last',
                    manualPagination: true,
                    rowCount: statusTab === ALL_TAB_VALUE ? totalElements : filteredRows.length,
                    onPaginationChange: setPagination,
                    onColumnVisibilityChange: setColumnVisibility,
                    state: { pagination, columnVisibility },
                    displayColumnDefOptions: {
                        'mrt-row-actions': {
                            muiTableBodyCellProps: {
                                sx: { py: 1.15 },
                            },
                        },
                    },
                }}
                enableGlobalFilter
            />

            <AssignUserRolesModal
                open={!!selectedUser}
                user={selectedUser}
                onClose={() => setSelectedUser(null)}
                onSaved={triggerRefresh}
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
