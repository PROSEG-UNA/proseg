import { useMemo, useState, useCallback } from 'react';
import TableBase from '../../../common/components/TablaBase.jsx';
import AlertModal from '../../../common/components/AlertModal.jsx';
import { useUsersData } from '../hooks/useUsersData';
import { updateUserApproval } from '../services/usersService';
import { getUsersColumns, renderUsersActions } from './usersColumns.jsx';
import AssignUserRolesModal from './AssignUserRolesModal.jsx';

export default function UsersTable({ refreshKey = 0 }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [selectedUser, setSelectedUser] = useState(null);
    const [localRefreshKey, setLocalRefreshKey] = useState(0);
    const [alert, setAlert] = useState(null);

    const triggerRefresh = useCallback(() => {
        setLocalRefreshKey((k) => k + 1);
    }, []);

    const { rows, loading, error, totalElements } = useUsersData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        refreshKey: refreshKey + localRefreshKey,
    });

    const handleApprove = async (user) => {
        try {
            await updateUserApproval(user.id, 'APPROVED');
            const message = user.statusRaw === 'REJECTED'
                ? 'Usuario activado correctamente.'
                : 'Usuario aprobado correctamente.';
            setAlert({ type: 'success', message });
            triggerRefresh();
        } catch (err) {
            setAlert({ type: 'error', message: err?.message || 'Error al aprobar usuario.' });
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
            setAlert({ type: 'error', message: err?.message || 'Error al rechazar usuario.' });
        }
    };

    const columns = useMemo(() => getUsersColumns(), []);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                error={error}
                enableRowActions
                renderRowActions={renderUsersActions({
                    onAssignRoles: setSelectedUser,
                    onApprove: handleApprove,
                    onReject: handleReject,
                })}
                tableOptions={{
                    positionActionsColumn: 'last',
                    manualPagination: true,
                    rowCount: totalElements,
                    onPaginationChange: setPagination,
                    state: { pagination },
                }}
                enableColumnFilters={false}
                enableGlobalFilter
            />

            <AssignUserRolesModal
                open={!!selectedUser}
                user={selectedUser}
                onClose={() => setSelectedUser(null)}
                onSaved={triggerRefresh}
            />

            <AlertModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}
