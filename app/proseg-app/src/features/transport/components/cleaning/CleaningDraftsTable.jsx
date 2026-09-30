import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Chip } from '@mui/material';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import TableBase from '../../../../common/components/TablaBase.jsx';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { queryKeys } from '../../../../common/query';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';
import { formatDateTime } from '../../transportUtils.js';
import { deleteCleaningDraft } from '../../services/cleaning/cleaningService.js';
import { useCleaningDraftsData } from '../../hooks/useCleaningDraftsData.js';
import { useServerTableState } from '../../hooks/useServerTableState.js';
import { draftStatusColor, draftStatusLabel } from './cleaningDraftLabels.js';

const INITIAL_SORTING = [{ id: 'importedAt', desc: true }];
const FILTERABLE_COLUMNS = new Set(['fileName', 'fileType', 'status', 'importedBy']);
const STATUS_FILTER_OPTIONS = [
    { label: 'Borrador', value: 'DRAFT' },
    { label: 'Registrado', value: 'REGISTERED' },
];

export default function CleaningDraftsTable() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const [draftToDelete, setDraftToDelete] = useState(null);
    const [alert, setAlert] = useState(null);

    const { requestParams, tableOptions } = useServerTableState({
        initialSorting: INITIAL_SORTING,
        filterableColumns: FILTERABLE_COLUMNS,
    });
    const { rows, loading, fetching, error, totalElements } = useCleaningDraftsData(requestParams);

    const deleteMutation = useMutation({
        mutationFn: (draftId) => deleteCleaningDraft(draftId),
        onSuccess: async () => {
            setDraftToDelete(null);
            setAlert({ type: 'success', message: 'Borrador eliminado correctamente' });
            await queryClient.invalidateQueries({ queryKey: queryKeys.transport.cleaningDrafts() });
        },
        onError: (deleteError) => {
            setDraftToDelete(null);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(deleteError, 'No se pudo eliminar el borrador') });
        },
    });

    const columns = useMemo(() => [
        {
            accessorKey: 'importedAt',
            header: 'Importado',
            size: 160,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
        { accessorKey: 'fileName', header: 'Archivo', size: 240, grow: true },
        { accessorKey: 'fileType', header: 'Formato', size: 110, grow: false },
        { accessorKey: 'totalRows', header: 'Filas', size: 90, grow: false, enableColumnFilter: false },
        { accessorKey: 'duplicateRows', header: 'Repetidas', size: 110, grow: false, enableColumnFilter: false },
        {
            accessorKey: 'status',
            header: 'Estado',
            size: 130,
            grow: false,
            filterVariant: 'select',
            filterSelectOptions: STATUS_FILTER_OPTIONS,
            Cell: ({ cell }) => (
                <Chip
                    label={draftStatusLabel(cell.getValue())}
                    color={draftStatusColor(cell.getValue())}
                    size="small"
                    variant="outlined"
                />
            ),
        },
        { accessorKey: 'importedBy', header: 'Importado por', size: 170, grow: true },
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
                renderRowActions={({ row }) => (
                    <RowActionsMenu
                        tooltip="Acciones"
                        actions={[
                            {
                                key: 'open',
                                label: 'Abrir borrador',
                                icon: <OpenInNewOutlinedIcon fontSize="small" />,
                                onClick: () => navigate(`/transporte/depuracion/borradores/${row.original.id}`),
                            },
                            {
                                key: 'delete',
                                label: 'Eliminar',
                                color: 'error',
                                icon: <DeleteOutlineOutlinedIcon fontSize="small" />,
                                disabled: row.original.status !== 'DRAFT',
                                onClick: () => setDraftToDelete(row.original),
                            },
                        ]}
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
