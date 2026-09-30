import { memo, useCallback, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
    Alert,
    Box,
    Button,
    Checkbox,
    Container,
    FormControlLabel,
    LinearProgress,
    Stack,
    Tooltip,
    Typography,
    useTheme,
} from '@mui/material';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import SaveAltOutlinedIcon from '@mui/icons-material/SaveAltOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import PrimaryButton from '../../../../common/components/PrimaryButton.jsx';
import TableBase from '../../../../common/components/TablaBase.jsx';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import { queryKeys } from '../../../../common/query';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { formatDate } from '../../transportUtils.js';
import {
    deleteCleaningDraftDuplicates,
    deleteCleaningDraftRow,
    registerCleaningDraft,
} from '../../services/cleaning/cleaningService';
import { useCleaningDraft, useCleaningDraftRows } from '../../hooks/useCleaningDraftsData.js';
import { useServerTableState } from '../../hooks/useServerTableState.js';
import CleaningDraftRowModal from '../../components/cleaning/CleaningDraftRowModal.jsx';
import DuplicateChip from '../../components/cleaning/DuplicateChip.jsx';

const HUB_PATH = '/transporte/depuracion';
const INITIAL_SORTING = [{ id: 'rowNumber', desc: false }];
const INITIAL_PAGE_SIZE = 100;
const TABLE_STORAGE_KEY = 'cleaning-draft-rows-table-state';
const FILTERABLE_COLUMNS = new Set([
    'rowNumber', 'number', 'driver', 'vehicle', 'passengers', 'executingUnit', 'responsible',
    'destination', 'durationDays', 'priority', 'modality', 'observations',
]);

const DraftRowsTable = memo(function DraftRowsTable({
    columns,
    rows,
    loading,
    fetching,
    error,
    totalElements,
    tableOptions,
    isDraft,
    onOpenRow,
    onDeleteRow,
}) {
    const renderRowActions = useCallback(({ row }) => (
        <RowActionsMenu
            tooltip="Acciones"
            actions={[
                {
                    key: 'view',
                    label: 'Ver',
                    icon: <VisibilityOutlinedIcon fontSize="small" />,
                    onClick: () => onOpenRow('view', row.original),
                },
                {
                    key: 'edit',
                    label: 'Editar',
                    icon: <EditOutlinedIcon fontSize="small" />,
                    disabled: !isDraft,
                    onClick: () => onOpenRow('edit', row.original),
                },
                {
                    key: 'delete',
                    label: 'Eliminar',
                    color: 'error',
                    icon: <DeleteOutlineOutlinedIcon fontSize="small" />,
                    disabled: !isDraft,
                    onClick: () => onDeleteRow(row.original),
                },
            ]}
        />
    ), [isDraft, onOpenRow, onDeleteRow]);

    const handleRowClick = useCallback(
        (row) => onOpenRow(isDraft ? 'edit' : 'view', row.original),
        [isDraft, onOpenRow],
    );

    const mergedTableOptions = useMemo(() => ({
        ...tableOptions,
        state: { ...tableOptions.state, density: 'compact' },
        rowCount: totalElements,
        muiPaginationProps: { rowsPerPageOptions: [10, 20, 50, 100] },
    }), [tableOptions, totalElements]);

    return (
        <TableBase
            columns={columns}
            data={rows}
            loading={loading}
            fetching={fetching}
            error={error}
            enableDensityToggle={false}
            enableRowActions
            onRowClick={handleRowClick}
            renderRowActions={renderRowActions}
            tableOptions={mergedTableOptions}
        />
    );
});

function registerResultMessage(result) {
    const parts = [`Se registraron ${result?.importedRows ?? 0} giras.`];
    if (result?.replacedRows) parts.push(`Se reemplazaron ${result.replacedRows} giras existentes.`);
    parts.push(`Choferes: ${result?.createdDrivers ?? 0} creados, ${result?.updatedDrivers ?? 0} actualizados.`);
    parts.push(`Vehículos: ${result?.createdVehicles ?? 0} creados, ${result?.updatedVehicles ?? 0} actualizados.`);
    return parts.join('\n');
}

export default function CleaningDraftPage() {
    const theme = useTheme();
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const { draftId } = useParams();
    const { hasAnyPermission } = usePermissions();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const [registerOpen, setRegisterOpen] = useState(false);
    const [replaceExisting, setReplaceExisting] = useState(false);
    const [deleteDuplicatesOpen, setDeleteDuplicatesOpen] = useState(false);
    const [rowToDelete, setRowToDelete] = useState(null);
    const [rowModal, setRowModal] = useState({ open: false, mode: 'view', row: null });
    const [alert, setAlert] = useState(null);

    const canExecuteCleaning = hasAnyPermission([
        PERMISSIONS.TRANSPORT.ASSIGNMENT.GENERATE,
        PERMISSIONS.TRANSPORT.ASSIGNMENT.UPDATE,
    ]);

    const { draft, loading: loadingDraft, error: draftError } = useCleaningDraft(draftId);
    const { requestParams, tableOptions } = useServerTableState({
        initialSorting: INITIAL_SORTING,
        filterableColumns: FILTERABLE_COLUMNS,
        initialPageSize: INITIAL_PAGE_SIZE,
        storageKey: TABLE_STORAGE_KEY,
    });
    const {
        rows,
        loading: loadingRows,
        fetching: fetchingRows,
        error: rowsError,
        totalElements,
    } = useCleaningDraftRows(draftId, requestParams);

    const registerMutation = useMutation({
        mutationFn: (replaceExistingInRange) => registerCleaningDraft(draftId, replaceExistingInRange),
        onSuccess: async (result) => {
            setRegisterOpen(false);
            setAlert({ type: 'success', message: registerResultMessage(result) });
            await Promise.all([
                queryClient.invalidateQueries({ queryKey: queryKeys.transport.cleaning() }),
                queryClient.invalidateQueries({ queryKey: queryKeys.transport.tours() }),
                queryClient.invalidateQueries({ queryKey: queryKeys.transport.assignment() }),
            ]);
        },
        onError: (error) => {
            setRegisterOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron registrar las giras') });
        },
    });

    const deleteDuplicatesMutation = useMutation({
        mutationFn: () => deleteCleaningDraftDuplicates(draftId),
        onSuccess: async (deletedRows) => {
            setDeleteDuplicatesOpen(false);
            tableOptions.onPaginationChange((current) => ({ ...current, pageIndex: 0 }));
            setAlert({ type: 'success', message: `Se eliminaron ${deletedRows} filas repetidas del borrador.` });
            await queryClient.invalidateQueries({ queryKey: queryKeys.transport.cleaningDrafts() });
        },
        onError: (error) => {
            setDeleteDuplicatesOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron eliminar las filas repetidas') });
        },
    });

    const deleteRowMutation = useMutation({
        mutationFn: (row) => deleteCleaningDraftRow(draftId, row.id),
        onSuccess: async () => {
            setRowToDelete(null);
            setAlert({ type: 'success', message: 'Gira eliminada correctamente' });
            await queryClient.invalidateQueries({ queryKey: queryKeys.transport.cleaningDrafts() });
        },
        onError: (error) => {
            setRowToDelete(null);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo eliminar la gira') });
        },
    });

    const columns = useMemo(() => [
        { accessorKey: 'rowNumber', header: 'Fila', size: 90, grow: false },
        { accessorKey: 'number', header: 'Numero', size: 120, grow: false },
        {
            id: 'duplicate',
            header: 'Repetición',
            size: 140,
            grow: false,
            enableSorting: false,
            enableColumnFilter: false,
            accessorFn: (row) => row.duplicateRank,
            Cell: ({ row }) => <DuplicateChip row={row.original} />,
        },
        { accessorKey: 'driver', header: 'Chofer', size: 180, grow: false, Cell: ({ cell }) => cell.getValue() ?? '—' },
        { accessorKey: 'vehicle', header: 'Vehículo', size: 140, grow: false, Cell: ({ cell }) => cell.getValue() ?? '—' },
        { accessorKey: 'passengers', header: 'Pasajeros', size: 120, grow: false },
        { accessorKey: 'executingUnit', header: 'Unidad ejecutora', size: 240, grow: false },
        { accessorKey: 'responsible', header: 'Responsable', size: 190, grow: false },
        { accessorKey: 'destination', header: 'Destinos', size: 180, grow: false },
        { accessorKey: 'durationDays', header: 'Duración', size: 110, grow: false },
        { accessorKey: 'priority', header: 'Prioridad', size: 110, grow: false },
        { accessorKey: 'modality', header: 'Modalidad', size: 120, grow: false, Cell: ({ cell }) => cell.getValue() ?? '—' },
        {
            accessorKey: 'departureDate',
            header: 'Fecha salida',
            size: 140,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDate(cell.getValue()),
        },
        { accessorKey: 'departureTime', header: 'Hora salida', size: 120, grow: false, enableColumnFilter: false },
        {
            accessorKey: 'returnDate',
            header: 'Fecha regreso',
            size: 140,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDate(cell.getValue()),
        },
        { accessorKey: 'returnTime', header: 'Hora regreso', size: 130, grow: false, enableColumnFilter: false },
        {
            accessorKey: 'observations',
            header: 'Observaciones',
            size: 320,
            grow: false,
            Cell: ({ cell }) => {
                const value = cell.getValue();
                if (!value) return '—';
                return (
                    <Tooltip title={value} placement="top-start">
                        <Typography noWrap sx={{ fontSize: 'inherit', maxWidth: 300 }}>{value}</Typography>
                    </Tooltip>
                );
            },
        },
    ], []);

    const openRowModal = useCallback((mode, row) => setRowModal({ open: true, mode, row }), []);
    const openDeleteRow = useCallback((row) => setRowToDelete(row), []);

    if (!canExecuteCleaning) return <AccessDeniedState />;

    const isDraft = draft?.status === 'DRAFT';
    const conflictingRows = draft?.conflictingRows ?? 0;
    const duplicateRows = draft?.duplicateRows ?? 0;
    const registering = registerMutation.isPending;
    const busy = registering || deleteDuplicatesMutation.isPending;
    const draftRange = `${formatDate(draft?.minDepartureDate)} a ${formatDate(draft?.maxReturnDate)}`;

    const openRegister = () => {
        setReplaceExisting(false);
        setRegisterOpen(true);
    };

    const confirmRegister = () => {
        if (!registerMutation.isPending) registerMutation.mutate(replaceExisting);
    };

    const confirmDeleteRow = () => {
        if (!deleteRowMutation.isPending) deleteRowMutation.mutate(rowToDelete);
    };

    const confirmDeleteDuplicates = () => {
        if (!deleteDuplicatesMutation.isPending) deleteDuplicatesMutation.mutate();
    };

    const closeRowModal = () => setRowModal((current) => ({ ...current, open: false }));

    const secondaryButtonSx = {
        borderColor: 'divider', color: 'text.secondary',
        textTransform: 'none', fontWeight: 700, fontSize: 12.5, borderRadius: '8px',
        '&:hover': { borderColor: accentColor, color: accentColor, bgcolor: 'action.hover' },
    };

    return (
        <Box className="transport-cleaning-draft-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Borrador de depuración"
                    action={(
                        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                            <Button
                                variant="outlined"
                                color="inherit"
                                onClick={() => navigate(HUB_PATH)}
                                disabled={busy}
                                startIcon={<ArrowBackOutlinedIcon />}
                                sx={secondaryButtonSx}
                            >
                                Volver
                            </Button>
                            {isDraft && (
                                <PrimaryButton
                                    onClick={() => openRowModal('create', null)}
                                    disabled={busy}
                                    startIcon={<AddOutlinedIcon />}
                                >
                                    Crear gira
                                </PrimaryButton>
                            )}
                            {isDraft && (
                                <PrimaryButton
                                    onClick={openRegister}
                                    disabled={busy || conflictingRows > 0}
                                    startIcon={<SaveAltOutlinedIcon />}
                                >
                                    Registrar giras
                                </PrimaryButton>
                            )}
                        </Stack>
                    )}
                />

                {loadingDraft && <LinearProgress sx={{ mb: 2 }} />}

                {draftError && (
                    <Alert severity="error" variant="outlined" sx={{ mb: 2 }}>
                        {draftError}
                    </Alert>
                )}

                {draft && (
                    <Stack spacing={1.5} sx={{ mb: 2 }}>
                        {isDraft && conflictingRows > 0 && (
                            <Alert severity="error" variant="outlined">
                                Hay {conflictingRows} filas con un Numero repetido pero con datos distintos. No se puede registrar hasta resolverlas.
                            </Alert>
                        )}
                        {isDraft && conflictingRows === 0 && duplicateRows > 0 && (
                            <Alert
                                severity="info"
                                variant="outlined"
                                action={(
                                    <Button
                                        color="inherit"
                                        size="small"
                                        onClick={() => setDeleteDuplicatesOpen(true)}
                                        disabled={busy}
                                        startIcon={<DeleteOutlineOutlinedIcon />}
                                        sx={{ textTransform: 'none', fontWeight: 700, fontSize: 12.5, whiteSpace: 'nowrap' }}
                                    >
                                        Eliminar todos los duplicados
                                    </Button>
                                )}
                            >
                                Hay {duplicateRows} filas repetidas
                            </Alert>
                        )}
                    </Stack>
                )}

                <DraftRowsTable
                    columns={columns}
                    rows={rows}
                    loading={loadingRows}
                    fetching={fetchingRows}
                    error={rowsError}
                    totalElements={totalElements}
                    tableOptions={tableOptions}
                    isDraft={isDraft}
                    onOpenRow={openRowModal}
                    onDeleteRow={openDeleteRow}
                />
            </Container>

            <CleaningDraftRowModal
                open={rowModal.open}
                mode={rowModal.mode}
                row={rowModal.row}
                draftId={draftId}
                onClose={closeRowModal}
                onEdit={isDraft ? () => openRowModal('edit', rowModal.row) : undefined}
                onDeleted={() => setAlert({ type: 'success', message: 'Gira eliminada correctamente' })}
                onSaved={() => setAlert({
                    type: 'success',
                    message: rowModal.mode === 'create' ? 'Gira creada correctamente' : 'Fila actualizada correctamente',
                })}
            />

            <GeneralModal
                open={registerOpen}
                onClose={registering ? undefined : () => setRegisterOpen(false)}
                icon={SaveAltOutlinedIcon}
                title="Registrar giras"
                subtitle={draft?.fileName ?? ''}
                maxWidth="sm"
                loading={registering}
                secondaryButton={{ label: 'Cancelar', onClick: () => setRegisterOpen(false), disabled: registering }}
                primaryButton={{
                    label: registering ? 'Registrando...' : 'Registrar giras',
                    onClick: confirmRegister,
                    disabled: registering,
                }}
            >
                <Stack spacing={2} sx={{ p: 3 }}>
                    <Typography sx={{ fontSize: 14 }}>
                        Se registrarán <strong>{(draft?.totalRows ?? 0) - duplicateRows}</strong> giras, una por cada Numero del borrador.
                    </Typography>
                    {duplicateRows > 0 && (
                        <Typography sx={{ fontSize: 13.5, color: 'text.secondary' }}>
                            Se omitirán {duplicateRows} filas repetidas: son la misma gira listada en cada día que abarca.
                        </Typography>
                    )}
                    <Box sx={{ p: 1.5, borderRadius: '8px', border: '1px solid', borderColor: replaceExisting ? 'warning.main' : 'divider' }}>
                        <FormControlLabel
                            control={(
                                <Checkbox
                                    size="small"
                                    checked={replaceExisting}
                                    onChange={(event) => setReplaceExisting(event.target.checked)}
                                    disabled={registering}
                                />
                            )}
                            label={<Typography sx={{ fontSize: 13.5, fontWeight: 600 }}>Reemplazar giras existentes en el rango</Typography>}
                        />
                        <Typography sx={{ color: 'text.secondary', fontSize: 12.5, pl: 3.8 }}>
                            Rango del archivo: {draftRange}
                        </Typography>
                    </Box>
                    {replaceExisting && (
                        <Alert severity="warning" variant="outlined">
                            Se eliminarán todas las giras que empiecen entre el {formatDate(draft?.minDepartureDate)} y el {formatDate(draft?.maxReturnDate)}, aunque no vengan en este archivo. Sus asignaciones quedarán sin gira asociada.
                        </Alert>
                    )}
                </Stack>
            </GeneralModal>

            <DialogModal
                open={Boolean(rowToDelete)}
                type="delete"
                title="Eliminar gira"
                message={rowToDelete ? `Se eliminará la fila ${rowToDelete.rowNumber} (Número ${rowToDelete.number}) del borrador.` : ''}
                confirmLabel={deleteRowMutation.isPending ? 'Eliminando...' : 'Sí, eliminar'}
                onConfirm={confirmDeleteRow}
                onClose={() => setRowToDelete(null)}
            />

            <DialogModal
                open={deleteDuplicatesOpen}
                type="delete"
                title="Eliminar todos los duplicados"
                message={`Se eliminarán ${duplicateRows} filas repetidas del borrador. Se conservará solo la primera aparición de cada Numero, por lo que quedarán ${(draft?.totalRows ?? 0) - duplicateRows} giras.\n\nEsta acción no se puede deshacer.`}
                confirmLabel={deleteDuplicatesMutation.isPending ? 'Eliminando...' : 'Sí, eliminar duplicados'}
                onConfirm={confirmDeleteDuplicates}
                onClose={() => setDeleteDuplicatesOpen(false)}
            />

            <DialogModal
                open={Boolean(alert)}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </Box>
    );
}
