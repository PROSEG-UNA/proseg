import { useCallback, useEffect, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { deleteCompany } from '../../services/company/companiesService';
import { useMaintenanceCompaniesData } from '../../hooks/company/useMaintenanceCompaniesData';
import { getCompanyColumns, renderCompanyActions } from './companyColumns.jsx';
import CompanyDetailPanel from './CompanyDetailPanel.jsx';
import CompanyDetailModal from './CompanyDetailModal.jsx';
import { queryKeys } from '../../../../common/query';

const COLUMN_TO_BACKEND_KEY = {
    name: 'name',
    legalId: 'legalId',
    contactEmail: 'contactEmail',
    contactPhone: 'contactPhone',
    address: 'address',
};

export default function CompanyTable({ onEditCompany, onManageUsers }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [alert, setAlert] = useState(null);
    const [companyToDelete, setCompanyToDelete] = useState(null);
    const queryClient = useQueryClient();
    const [detailModalOpen, setDetailModalOpen] = useState(false);
    const [detailCompanyId, setDetailCompanyId] = useState(null);
    const { hasPermission } = usePermissions();

    const canEdit = hasPermission(PERMISSIONS.MAINTENANCE.COMPANIES.MANAGE);
    const canDelete = hasPermission(PERMISSIONS.MAINTENANCE.COMPANIES.DELETE);
    const canManageUsers = hasPermission(PERMISSIONS.MAINTENANCE.COMPANY_USERS.MANAGE);

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

    const { rows, loading, fetching, error, totalElements } = useMaintenanceCompaniesData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
    });

    const columns = useMemo(() => getCompanyColumns(), []);

    const deleteCompanyMutation = useMutation({
        mutationFn: (companyId) => deleteCompany(companyId),
        onSuccess: async () => {
            setCompanyToDelete(null);
            setAlert({ type: 'success', message: 'Empresa eliminada correctamente' });
            await queryClient.invalidateQueries({ queryKey: queryKeys.maintenance.companies() });
        },
        onError: (deleteError) => {
            setAlert({ type: 'error', message: deleteError?.response?.data?.message ?? deleteError?.message ?? 'No se pudo eliminar la empresa' });
        },
    });

    const deleting = deleteCompanyMutation.isPending;

    const handleDelete = useCallback((row) => setCompanyToDelete(row), []);
    const handleDeleteCancel = useCallback(() => {
        if (deleting) return;
        setCompanyToDelete(null);
    }, [deleting]);

    const handleViewDetail = useCallback((company) => {
        setDetailCompanyId(company.id);
        setDetailModalOpen(true);
    }, []);

    const handleCloseDetailModal = useCallback(() => {
        setDetailModalOpen(false);
        setDetailCompanyId(null);
    }, []);

    const handleDeleteConfirm = useCallback(() => {
        if (!companyToDelete) return;
        deleteCompanyMutation.mutate(companyToDelete.id);
    }, [companyToDelete, deleteCompanyMutation]);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                fetching={fetching}
                error={error}
                enableRowActions={canEdit || canDelete || canManageUsers}
                renderRowActions={renderCompanyActions({
                    onEdit: onEditCompany,
                    onDelete: handleDelete,
                    onManageUsers,
                    onViewDetail: handleViewDetail,
                    canEdit,
                    canDelete,
                    canManageUsers,
                })}
                renderDetailPanel={({ row }) => (
                    <CompanyDetailPanel
                        companyId={row.original.id}
                        listRow={row.original}
                        onManageUsers={onManageUsers}
                        canManageUsers={canManageUsers}
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
                            address: false,
                            updatedAt: false,
                            legalId: false,
                            contactPhone: false,
                        },
                    },
                }}
                enableGlobalFilter
            />

            <DialogModal
                type="delete"
                open={!!companyToDelete}
                title="Eliminar empresa"
                message={`¿Seguro que deseas eliminar la empresa "${companyToDelete?.name}"?\nEsta acción no se puede deshacer.`}
                onClose={handleDeleteCancel}
                onConfirm={handleDeleteConfirm}
                confirmLabel="Eliminar"
            />

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />

            <CompanyDetailModal
                open={detailModalOpen}
                onClose={handleCloseDetailModal}
                companyId={detailCompanyId}
                onEdit={onEditCompany}
                onManageUsers={onManageUsers}
                canEdit={canEdit}
                canManageUsers={canManageUsers}
            />
        </>
    );
}

