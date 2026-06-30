import { useCallback, useEffect, useMemo, useState } from 'react';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import CancelWithReasonModal from '../../../../common/components/CancelWithReasonModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import {
    acceptMaintenanceRequest,
    cancelMaintenanceRequest,
    deleteMaintenanceRequest,
} from '../../services/request/requestsService';
import { useMaintenanceRequestsData } from '../../hooks/request/useMaintenanceRequestsData';
import { formatDate } from '../../maintenanceUtils';
import { getMaintenanceRequestColumns, renderMaintenanceRequestActions } from './requestColumns.jsx';
import MaintenanceRequestDetailPanel from './MaintenanceRequestDetailPanel.jsx';

const COLUMN_TO_BACKEND_KEY = {
    companyName: 'company.name',
    companyLegalId: 'company.legalId',
    statusRaw: 'status',
    startDate: 'startDate',
};

export default function MaintenanceRequestTable({ refreshKey = 0, onRefresh, onEditRequest }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [alert, setAlert] = useState(null);
    const [requestToDelete, setRequestToDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [requestToAccept, setRequestToAccept] = useState(null);
    const [accepting, setAccepting] = useState(false);
    const [requestToCancel, setRequestToCancel] = useState(null);
    const [cancelling, setCancelling] = useState(false);
    const { hasPermission } = usePermissions();

    const canEdit = hasPermission(PERMISSIONS.MAINTENANCE.REQUESTS.UPDATE);
    const canDelete = hasPermission(PERMISSIONS.MAINTENANCE.REQUESTS.DELETE);
    const canAccept = hasPermission(PERMISSIONS.MAINTENANCE.REQUESTS.ACCEPT);
    const canCancel = hasPermission(PERMISSIONS.MAINTENANCE.REQUESTS.CANCEL);

    const debouncedGlobalFilter = useDebounce(globalFilter, 350);
    const debouncedColumnFilters = useDebounce(columnFilters, 350);

    const backendFilters = useMemo(() => {
        const out = {};
        debouncedColumnFilters.forEach(({ id, value }) => {
            const key = COLUMN_TO_BACKEND_KEY[id];
            if (!key) return;
            if (value === null || value === undefined || value === '') return;
            out[key] = value;
        });
        return out;
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

    const { rows, loading, error, totalElements } = useMaintenanceRequestsData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
        refreshKey,
    });

    const columns = useMemo(() => getMaintenanceRequestColumns(), []);

    const handleDelete = useCallback((row) => setRequestToDelete(row), []);
    const handleDeleteCancel = useCallback(() => {
        if (deleting) return;
        setRequestToDelete(null);
    }, [deleting]);

    const handleDeleteConfirm = useCallback(async () => {
        if (!requestToDelete) return;
        setDeleting(true);
        try {
            await deleteMaintenanceRequest(requestToDelete.id);
            setRequestToDelete(null);
            setAlert({ type: 'success', message: 'Solicitud eliminada correctamente' });
            onRefresh?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo eliminar la solicitud' });
        } finally {
            setDeleting(false);
        }
    }, [requestToDelete, onRefresh]);

    const handleAccept = useCallback((row) => setRequestToAccept(row), []);
    const handleAcceptClose = useCallback(() => {
        if (accepting) return;
        setRequestToAccept(null);
    }, [accepting]);

    const handleAcceptConfirm = useCallback(async () => {
        if (!requestToAccept) return;
        setAccepting(true);
        try {
            await acceptMaintenanceRequest(requestToAccept.id);
            setRequestToAccept(null);
            setAlert({ type: 'success', message: 'Solicitud aceptada correctamente' });
            onRefresh?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo aceptar la solicitud' });
        } finally {
            setAccepting(false);
        }
    }, [requestToAccept, onRefresh]);

    const handleCancel = useCallback((row) => setRequestToCancel(row), []);
    const handleCancelClose = useCallback(() => {
        if (cancelling) return;
        setRequestToCancel(null);
    }, [cancelling]);

    const handleCancelConfirm = useCallback(async (reason) => {
        if (!requestToCancel) return;
        setCancelling(true);
        try {
            await cancelMaintenanceRequest(requestToCancel.id, reason);
            setRequestToCancel(null);
            setAlert({ type: 'success', message: 'Solicitud cancelada correctamente' });
            onRefresh?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo cancelar la solicitud' });
        } finally {
            setCancelling(false);
        }
    }, [requestToCancel, onRefresh]);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                error={error}
                enableRowActions={canEdit || canDelete || canAccept || canCancel}
                renderRowActions={renderMaintenanceRequestActions({
                    onEdit: onEditRequest,
                    onDelete: handleDelete,
                    onAccept: handleAccept,
                    onCancel: handleCancel,
                    canEdit,
                    canDelete,
                    canAccept,
                    canCancel,
                })}
                renderDetailPanel={({ row }) => (
                    <MaintenanceRequestDetailPanel
                        requestId={row.original.id}
                    />
                )}
                tableOptions={{
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
                            companyLegalId: false,
                            createdAt: false,
                        },
                    },
                }}
                enableGlobalFilter
            />

            <DialogModal
                type="delete"
                open={!!requestToDelete}
                title="Eliminar solicitud"
                message={`¿Seguro que deseas eliminar esta solicitud de ${requestToDelete?.companyName ?? 'la empresa'}?\nEsta acción no se puede deshacer.`}
                onClose={handleDeleteCancel}
                onConfirm={handleDeleteConfirm}
                confirmLabel="Eliminar"
            />

            <DialogModal
                type="success"
                open={!!requestToAccept}
                title="Aceptar solicitud"
                message={`¿Deseas marcar como aceptada la solicitud de ${requestToAccept?.companyName ?? 'la empresa'}?`}
                onClose={handleAcceptClose}
                onConfirm={handleAcceptConfirm}
                confirmLabel="Aceptar"
            />

            <CancelWithReasonModal
                open={!!requestToCancel}
                onClose={handleCancelClose}
                onConfirm={handleCancelConfirm}
                loading={cancelling}
                title="Cancelar solicitud"
                subtitle={requestToCancel?.companyName}
                details={[
                    { label: 'Empresa', value: requestToCancel?.companyName },
                    { label: 'Estado actual', value: requestToCancel?.status },
                    { label: 'Inicio', value: formatDate(requestToCancel?.startDate) },
                    { label: 'Fin', value: formatDate(requestToCancel?.endDate) },
                ]}
                confirmLabel="Cancelar solicitud"
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


