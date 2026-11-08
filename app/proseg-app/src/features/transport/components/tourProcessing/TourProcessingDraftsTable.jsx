import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Chip } from '@mui/material';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import TableBase from '../../../../common/components/TablaBase.jsx';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import { queryKeys } from '../../../../common/query';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';
import { deleteTourProcessingDraft } from '../../services/tourProcessing/tourProcessingService.js';
import { useTourProcessingDraftsData } from '../../hooks/useTourProcessingDraftsData.js';
import { useServerTableState } from '../../hooks/useServerTableState.js';
import { draftPeriodLabel, draftStatusColor, draftStatusLabel } from './tourProcessingDraftLabels.js';

const INITIAL_SORTING = [];
const FILTERABLE_COLUMNS = new Set(['fileName']);

export default function TourProcessingDraftsTable({ status = 'DRAFT' }) {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [draftToDelete, setDraftToDelete] = useState(null);
    const { hasPermission } = usePermissions();
    const canDeleteDrafts = hasPermission(PERMISSIONS.TRANSPORT.TOURS.DELETE);
    const [alert, setAlert] = useState(null);

    const { requestParams, tableOptions } = useServerTableState({
        initialSorting: INITIAL_SORTING,
        filterableColumns: FILTERABLE_COLUMNS,
        storageKey: status === 'CLOSED' ? 'tour-processing-closed-table-state' : 'tour-processing-drafts-table-state',
    });
    const { rows, loading, fetching, error, totalElements } = useTourProcessingDraftsData({
        ...requestParams,
        filters: { ...requestParams.filters, status },
    });

    const deleteMutation = useMutation({
        mutationFn: (draftId) => deleteTourProcessingDraft(draftId),
        onSuccess: () => {
            setDraftToDelete(null);
            setAlert({ type: 'success', message: 'Borrador eliminado correctamente' });
            void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessingDrafts() });
        },
        onError: (deleteError) => {
            setDraftToDelete(null);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(deleteError, 'No se pudo eliminar el borrador') });
        },
    });

    const openDraft = (draftId) => navigate(`/transporte/depuracion/borradores/${draftId}`);

    const columns = useMemo(() => [
        {
            id: 'period',
            header: 'Mes',
            size: 160,
            grow: false,
            accessorFn: draftPeriodLabel,
            enableColumnFilter: false,
            enableSorting: false,
        },
        { accessorKey: 'fileName', header: 'Archivo', size: 240, grow: true },
        { accessorKey: 'totalRows', header: 'Filas', size: 90, grow: false, enableColumnFilter: false },
        { accessorKey: 'duplicateRows', header: 'Repetidas', size: 110, grow: false, enableColumnFilter: false },
        {
            accessorKey: 'status',
            header: 'Estado',
            size: 130,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => (
                <Chip
                    label={draftStatusLabel(cell.getValue())}
                    color={draftStatusColor(cell.getValue())}
                    size="small"
                    variant="outlined"
                />
            ),
        },
    ], []);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                fetching={fetching}
                error={error}
                enableRowActions
                onRowClick={(row) => openDraft(row.original.id)}
                renderRowActions={({ row }) => (
                    <RowActionsMenu
                        tooltip="Acciones"
                        actions={[
                            {
                                key: 'open',
                                label: status === 'CLOSED' ? 'Ver depuración' : 'Abrir borrador',
                                icon: <OpenInNewOutlinedIcon fontSize="small" />,
                                onClick: () => openDraft(row.original.id),
                            },
                            status === 'DRAFT' && canDeleteDrafts ? {
                                key: 'delete',
                                label: 'Eliminar',
                                color: 'error',
                                icon: <DeleteOutlineOutlinedIcon fontSize="small" />,
                                disabled: row.original.status !== 'DRAFT',
                                onClick: () => setDraftToDelete(row.original),
                            } : null,
                        ].filter(Boolean)}
                    />
                )}
                tableOptions={{ ...tableOptions, rowCount: totalElements }}
            />

            <DialogModal
                open={Boolean(draftToDelete)}
                type="delete"
                title="Eliminar borrador"
                message={draftToDelete ? `Se eliminará el borrador de "${draftToDelete.fileName}" con sus ${draftToDelete.totalRows} filas.` : ''}
                confirmLabel={deleteMutation.isPending ? 'Eliminando...' : 'Sí, eliminar'}
                onConfirm={() => {
                    if (!deleteMutation.isPending) deleteMutation.mutate(draftToDelete.id);
                }}
                onClose={() => setDraftToDelete(null)}
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
