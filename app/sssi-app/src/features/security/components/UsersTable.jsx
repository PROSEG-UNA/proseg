import { useMemo, useState, useCallback, useEffect } from 'react';
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

const ALL_TAB_VALUE = 'ALL';

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

export default function UsersTable({ refreshKey = 0 }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [selectedUser, setSelectedUser] = useState(null);
    const [localRefreshKey, setLocalRefreshKey] = useState(0);
    const [alert, setAlert] = useState(null);
    const [statusTab, setStatusTab] = useState(ALL_TAB_VALUE);
    const [availableStatuses, setAvailableStatuses] = useState(['PENDING', 'APPROVED', 'REJECTED', 'INVITED']);
    const { hasPermission } = usePermissions();
    const canAssignRoles = hasPermission(PERMISSIONS.USERS.ASSIGN_ROLE);
    const canApproveUsers = hasPermission(PERMISSIONS.USERS.APPROVE);
    const canUseActions = canAssignRoles || canApproveUsers;

    const triggerRefresh = useCallback(() => {
        setLocalRefreshKey((k) => k + 1);
    }, []);

    const { rows, loading, error, totalElements } = useUsersData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        refreshKey: refreshKey + localRefreshKey,
    });

    useEffect(() => {
        let ignore = false;

        const loadStatuses = async () => {
            try {
                const statuses = await fetchUserStatuses();
                if (!ignore && statuses.length > 0) {
                    setAvailableStatuses(statuses);
                }
            } catch {
                // Si falla endpoint, mantenemos fallback local.
            }
        };

        void loadStatuses();

        return () => {
            ignore = true;
        };
    }, []);

    const handleApprove = async (user) => {
        try {
            await updateUserApproval(user.id, 'APPROVED');
            const message = user.statusRaw === 'REJECTED'
                ? 'Usuario activado correctamente.'
                : 'Usuario aprobado correctamente.';
            setAlert({ type: 'success', message });
            triggerRefresh();
        } catch (err) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(err, 'Error al aprobar usuario.') });
        }
    };

    const handleReject = async (user) => {
        try {
            await updateUserApproval(user.id, 'REJECTED');
            const message = user.statusRaw === 'APPROVED'
                ? 'Usuario desactivado correctamente.'
                : 'Usuario rechazado correctamente.';
            setAlert({ type: 'success', message });
            triggerRefresh();
        } catch (err) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(err, 'Error al rechazar usuario.') });
        }
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
            <Box sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}>
                <Tabs
                    value={statusTab}
                    onChange={(_, newValue) => {
                        setStatusTab(newValue);
                        setPagination((prev) => ({ ...prev, pageIndex: 0 }));
                    }}
                    variant="scrollable"
                    scrollButtons="auto"
                    aria-label="Filtros por estado de usuario"
                >
                    {tabItems.map((status) => (
                        <Tab
                            key={status}
                            value={status}
                            label={status === ALL_TAB_VALUE ? 'Todos' : toStatusLabel(status)}
                        />
                    ))}
                </Tabs>
            </Box>

            <TableBase
                columns={columns}
                data={filteredRows}
                loading={loading}
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
                    state: { pagination },
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
