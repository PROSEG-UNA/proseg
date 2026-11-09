import { useCallback, useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import TableBase from '../../../../common/components/TablaBase.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { queryKeys } from '../../../../common/query';
import { deleteObservationKeyword } from '../../services/observationKeywords/observationKeywordsService';
import { usePersistedPagination } from '../../hooks/usePersistedPagination.js';
import { useObservationKeywordsData } from '../../hooks/useObservationKeywordsData';

export default function ObservationKeywordsTable({ onEditKeyword }) {
    const [pagination, setPagination] = usePersistedPagination('observation-keywords-table-page-size');
    const [globalFilter, setGlobalFilter] = useState('');
    const [sorting, setSorting] = useState([]);
    const [alert, setAlert] = useState(null);
    const [keywordToDelete, setKeywordToDelete] = useState(null);
    const queryClient = useQueryClient();
    const { hasPermission } = usePermissions();
    const canEdit = hasPermission(PERMISSIONS.TRANSPORT.TOURS.UPDATE);
    const canDelete = hasPermission(PERMISSIONS.TRANSPORT.TOURS.DELETE);
    const debouncedGlobalFilter = useDebounce(globalFilter, 350);

    const backendSort = useMemo(
        () => sorting.map(({ id, desc }) => (id === 'text' ? `text,${desc ? 'desc' : 'asc'}` : null)).filter(Boolean),
        [sorting],
    );

    const { rows, loading, fetching, error, totalElements } = useObservationKeywordsData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        sort: backendSort,
    });

    const refresh = () => {
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.observationKeywords() });
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessing() });
    };

    const deleteMutation = useMutation({
        mutationFn: (keywordId) => deleteObservationKeyword(keywordId),
        onSuccess: () => {
            setKeywordToDelete(null);
            setAlert({ type: 'success', message: 'Palabra clave eliminada correctamente' });
            refresh();
        },
        onError: (deleteError) => {
            setAlert({ type: 'error', message: deleteError?.response?.data?.message ?? deleteError?.message ?? 'No se pudo eliminar la palabra clave' });
        },
    });

    const columns = useMemo(() => [
        { accessorKey: 'text', header: 'Palabra clave', size: 320, grow: true },
    ], []);

    const handleGlobalFilterChange = useCallback((updater) => {
        setGlobalFilter((current) => (typeof updater === 'function' ? updater(current) : updater));
        setPagination((previous) => (previous.pageIndex === 0 ? previous : { ...previous, pageIndex: 0 }));
    }, [setPagination]);

    const handleSortingChange = useCallback((updater) => {
        setSorting((current) => (typeof updater === 'function' ? updater(current) : updater));
        setPagination((previous) => (previous.pageIndex === 0 ? previous : { ...previous, pageIndex: 0 }));
    }, [setPagination]);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                fetching={fetching}
                error={error}
                enableRowActions={canEdit || canDelete}
                onRowClick={canEdit ? (row) => onEditKeyword(row.original) : undefined}
                renderRowActions={({ row }) => (
                    <RowActionsMenu
                        tooltip="Ver acción"
                        actions={[
                            {
                                key: 'edit',
                                label: 'Editar palabra clave',
                                icon: <EditIcon fontSize="small" />,
                                hidden: !canEdit,
                                onClick: () => onEditKeyword(row.original),
                            },
                            {
                                key: 'delete',
                                label: 'Eliminar palabra clave',
                                icon: <DeleteIcon fontSize="small" />,
                                hidden: !canDelete,
                                onClick: () => setKeywordToDelete(row.original),
                            },
                        ]}
                    />
                )}
                tableOptions={{
                    positionActionsColumn: 'last',
                    manualPagination: true,
                    manualFiltering: true,
                    manualSorting: true,
                    rowCount: totalElements,
                    onPaginationChange: setPagination,
                    onGlobalFilterChange: handleGlobalFilterChange,
                    onSortingChange: handleSortingChange,
                    state: { pagination, globalFilter, sorting },
                }}
                enableGlobalFilter
            />
            <DialogModal
                type="delete"
                open={Boolean(keywordToDelete)}
                title="Eliminar palabra clave"
                message={keywordToDelete ? `¿Seguro que deseas eliminar la palabra "${keywordToDelete.text}"?` : ''}
                onClose={() => { if (!deleteMutation.isPending) setKeywordToDelete(null); }}
                onConfirm={() => { if (keywordToDelete) deleteMutation.mutate(keywordToDelete.id); }}
                confirmLabel="Eliminar"
            />
            <DialogModal
                open={Boolean(alert)}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}
