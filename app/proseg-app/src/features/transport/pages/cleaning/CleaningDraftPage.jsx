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
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Stack,
    Tab,
    Tabs,
    Tooltip,
    Typography,
    useTheme,
} from '@mui/material';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import DirectionsCarFilledOutlinedIcon from '@mui/icons-material/DirectionsCarFilledOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import SaveAltOutlinedIcon from '@mui/icons-material/SaveAltOutlined';
import TuneOutlinedIcon from '@mui/icons-material/TuneOutlined';
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
import { downloadBlob, formatDate } from '../../transportUtils.js';
import {
    autoAssignCleaningDraftDrivers,
    autoAssignCleaningDraftVehicles,
    deleteCleaningDraftDuplicates,
    deleteCleaningDraftRow,
    exportCleaningDraft,
    registerCleaningDraft,
} from '../../services/cleaning/cleaningService';
import { exportDriverOvertimeRequest } from '../../services/drivers/driversService';
import { useCleaningCatalogMatch } from '../../hooks/useCleaningCatalogMatch.js';
import {
    useCleaningDraft,
    useCleaningDraftAllRows,
    useCleaningDraftRows,
    useCleaningDraftSchedule,
} from '../../hooks/useCleaningDraftsData.js';
import { useServerTableState } from '../../hooks/useServerTableState.js';
import CatalogValueCell from '../../components/cleaning/CatalogValueCell.jsx';
import CleaningDraftRowModal from '../../components/cleaning/CleaningDraftRowModal.jsx';
import CleaningDraftScheduleTable from '../../components/cleaning/CleaningDraftScheduleTable.jsx';
import { draftPeriodLabel, shortPeriodLabel } from '../../components/cleaning/cleaningDraftLabels.js';
import DuplicateChip from '../../components/cleaning/DuplicateChip.jsx';

const HUB_PATH = '/transporte/depuracion';
const INITIAL_SORTING = [{ id: 'rowNumber', desc: false }];
const INITIAL_PAGE_SIZE = 100;
const TABLE_STORAGE_KEY = 'cleaning-draft-rows-table-state';
const INITIAL_COLUMN_VISIBILITY = { rowNumber: false };
const FILTERABLE_COLUMNS = new Set([
    'rowNumber', 'number', 'driver', 'vehicle', 'passengers', 'executingUnit', 'responsible',
    'destination', 'durationDays', 'priority', 'modality', 'observations',
]);

const AUTO_ASSIGN_DRIVERS_MESSAGE = 'Si hay choferes asignados que no existen en el catálogo del sistema (marcados con el ícono de advertencia), esas filas quedarán sin chofer y la autoasignación intentará completarlas.\n\nLas giras que ya tienen un chofer del catálogo se respetan y no se reasignan.';
const AUTO_ASSIGN_VEHICLES_MESSAGE = 'Si hay vehículos asignados que no existen en el catálogo del sistema o que no son una placa (marcados con el ícono de advertencia), esas filas quedarán sin vehículo y la autoasignación intentará completarlas.\n\nLas giras que ya tienen un vehículo del catálogo se respetan y no se reasignan.';

const NO_DUPLICATES_HEADER = (
    <Box component="span" sx={{ display: 'flex', flexDirection: 'column' }}>
        Repetición
        <Typography component="span" sx={{ fontSize: 11.5, color: 'text.disabled' }}>
            No hay giras duplicadas
        </Typography>
    </Box>
);

const DraftRowsTable = memo(function DraftRowsTable({
    columns,
    rows,
    loading,
    fetching,
    error,
    totalElements,
    tableOptions,
    isDraft,
    hasDuplicates,
    onOpenRow,
    onDeleteRow,
}) {
    const [duplicateColumnHidden, setDuplicateColumnHidden] = useState(false);

    if (!hasDuplicates && duplicateColumnHidden) setDuplicateColumnHidden(false);

    const tableColumns = useMemo(() => columns.map((column) => (
        column.id === 'duplicate' && !hasDuplicates
            ? { ...column, enableHiding: false, header: NO_DUPLICATES_HEADER }
            : column
    )), [columns, hasDuplicates]);

    const columnVisibility = useMemo(() => ({
        ...tableOptions.state.columnVisibility,
        duplicate: hasDuplicates && !duplicateColumnHidden,
    }), [tableOptions.state.columnVisibility, hasDuplicates, duplicateColumnHidden]);

    const { onColumnVisibilityChange } = tableOptions;
    const handleColumnVisibilityChange = useCallback((updater) => {
        const nextVisibility = typeof updater === 'function' ? updater(columnVisibility) : updater;
        const { duplicate, ...otherColumns } = nextVisibility;
        if (hasDuplicates) setDuplicateColumnHidden(duplicate === false);
        onColumnVisibilityChange(otherColumns);
    }, [columnVisibility, hasDuplicates, onColumnVisibilityChange]);

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
        onColumnVisibilityChange: handleColumnVisibilityChange,
        state: { ...tableOptions.state, columnVisibility, density: 'compact' },
        rowCount: totalElements,
        muiPaginationProps: { rowsPerPageOptions: [10, 20, 50, 100] },
    }), [tableOptions, handleColumnVisibilityChange, columnVisibility, totalElements]);

    return (
        <TableBase
            columns={tableColumns}
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

const DraftScheduleView = memo(function DraftScheduleView({ draftId, isDraft, onOpenRow }) {
    const { schedule, loading, error } = useCleaningDraftSchedule(draftId);
    const { rows } = useCleaningDraftAllRows(draftId);
    const rowsById = useMemo(() => new Map(rows.map((row) => [row.id, row])), [rows]);

    const handleOpenTour = useCallback((tourId) => {
        const row = rowsById.get(tourId);
        if (row) onOpenRow(isDraft ? 'edit' : 'view', row);
    }, [rowsById, isDraft, onOpenRow]);

    return (
        <CleaningDraftScheduleTable
            schedule={schedule}
            loading={loading}
            error={error}
            onOpenTour={handleOpenTour}
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

function autoAssignDriversResultMessage(result) {
    const parts = [`Se asignó chofer a ${result?.assignedTours ?? 0} giras.`];
    if (result?.unassignedTours) parts.push(`${result.unassignedTours} giras quedaron sin chofer porque ningún chofer disponible cumple las reglas.`);
    if (result?.clearedTours) parts.push(`Se vaciaron ${result.clearedTours} giras que tenían un chofer fuera del catálogo.`);
    return parts.join('\n');
}

function autoAssignVehiclesResultMessage(result) {
    const parts = [`Se asignó vehículo a ${result?.assignedTours ?? 0} giras.`];
    if (result?.unassignedTours) parts.push(`${result.unassignedTours} giras quedaron sin vehículo porque ningún vehículo disponible cumple las reglas.`);
    if (result?.clearedTours) parts.push(`Se vaciaron ${result.clearedTours} giras que tenían un vehículo fuera del catálogo.`);
    return parts.join('\n');
}

export default function CleaningDraftPage() {
    const theme = useTheme();
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const { draftId } = useParams();
    const { hasAnyPermission } = usePermissions();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const [optionsAnchor, setOptionsAnchor] = useState(null);
    const [registerOpen, setRegisterOpen] = useState(false);
    const [replaceExisting, setReplaceExisting] = useState(false);
    const [deleteDuplicatesOpen, setDeleteDuplicatesOpen] = useState(false);
    const [autoAssignDriversOpen, setAutoAssignDriversOpen] = useState(false);
    const [autoAssignVehiclesOpen, setAutoAssignVehiclesOpen] = useState(false);
    const [rowToDelete, setRowToDelete] = useState(null);
    const [rowModal, setRowModal] = useState({ open: false, mode: 'view', row: null });
    const [alert, setAlert] = useState(null);
    const [viewTab, setViewTab] = useState('rows');

    const canExecuteCleaning = hasAnyPermission([
        PERMISSIONS.TRANSPORT.TOURS.MANAGE,
    ]);

    const {
        driver: { isMissing: isDriverMissing },
        vehicle: { isMissing: isVehicleMissing },
    } = useCleaningCatalogMatch();
    const { draft, loading: loadingDraft, error: draftError } = useCleaningDraft(draftId);
    const { requestParams, tableOptions } = useServerTableState({
        initialSorting: INITIAL_SORTING,
        filterableColumns: FILTERABLE_COLUMNS,
        initialPageSize: INITIAL_PAGE_SIZE,
        initialColumnVisibility: INITIAL_COLUMN_VISIBILITY,
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

    const autoAssignDriversMutation = useMutation({
        mutationFn: () => autoAssignCleaningDraftDrivers(draftId),
        onSuccess: async (result) => {
            setAutoAssignDriversOpen(false);
            setAlert({ type: 'success', message: autoAssignDriversResultMessage(result) });
            await Promise.all([
                queryClient.invalidateQueries({ queryKey: queryKeys.transport.cleaningDrafts() }),
                queryClient.invalidateQueries({ queryKey: queryKeys.transport.driverMonths(draft?.year, draft?.month) }),
            ]);
        },
        onError: (error) => {
            setAutoAssignDriversOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron autoasignar los choferes') });
        },
    });

    const autoAssignVehiclesMutation = useMutation({
        mutationFn: () => autoAssignCleaningDraftVehicles(draftId),
        onSuccess: async (result) => {
            setAutoAssignVehiclesOpen(false);
            setAlert({ type: 'success', message: autoAssignVehiclesResultMessage(result) });
            await queryClient.invalidateQueries({ queryKey: queryKeys.transport.cleaningDrafts() });
        },
        onError: (error) => {
            setAutoAssignVehiclesOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron autoasignar los vehículos') });
        },
    });

    const exportMutation = useMutation({
        mutationFn: () => exportCleaningDraft(draftId),
        onSuccess: ({ blob, fileName }) => downloadBlob(blob, fileName),
        onError: (error) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo exportar el borrador') });
        },
    });

    const overtimeRequestMutation = useMutation({
        mutationFn: () => exportDriverOvertimeRequest(draft.year, draft.month),
        onSuccess: ({ blob, fileName }) => downloadBlob(blob, fileName),
        onError: (error) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo generar la solicitud de horas extra') });
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
        {
            accessorKey: 'driver',
            header: 'Chofer',
            size: 210,
            grow: false,
            Cell: ({ cell }) => (
                <CatalogValueCell kind="driver" value={cell.getValue()} missing={isDriverMissing(cell.getValue())} />
            ),
        },
        {
            accessorKey: 'vehicle',
            header: 'Vehículo',
            size: 160,
            grow: false,
            Cell: ({ cell }) => (
                <CatalogValueCell kind="vehicle" value={cell.getValue()} missing={isVehicleMissing(cell.getValue())} />
            ),
        },
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
    ], [isDriverMissing, isVehicleMissing]);

    const openRowModal = useCallback((mode, row) => setRowModal({ open: true, mode, row }), []);
    const openDeleteRow = useCallback((row) => setRowToDelete(row), []);

    if (!canExecuteCleaning) return <AccessDeniedState />;

    const isDraft = draft?.status === 'DRAFT';
    const conflictingRows = draft?.conflictingRows ?? 0;
    const duplicateRows = draft?.duplicateRows ?? 0;
    const registering = registerMutation.isPending;
    const exporting = exportMutation.isPending;
    const autoAssigningDrivers = autoAssignDriversMutation.isPending;
    const autoAssigningVehicles = autoAssignVehiclesMutation.isPending;
    const exportingOvertimeRequest = overtimeRequestMutation.isPending;
    const periodLabel = draft?.year && draft?.month ? shortPeriodLabel(draft.year, draft.month) : null;
    const exportLabel = draft?.year && draft?.month ? `Descargar '${draftPeriodLabel(draft)}'` : 'Descargar borrador';
    const busy = registering || autoAssigningDrivers || autoAssigningVehicles || deleteDuplicatesMutation.isPending;
    const draftRange = `${formatDate(draft?.minDepartureDate)} a ${formatDate(draft?.maxReturnDate)}`;
    const pageTitle = draft?.year && draft?.month
        ? `Depuración de giras ${draftPeriodLabel(draft).toLowerCase()}`
        : 'Depuración de giras';

    const closeOptions = () => setOptionsAnchor(null);

    const handleCreateRow = () => {
        closeOptions();
        openRowModal('create', null);
    };

    const handleAutoAssignDrivers = () => {
        closeOptions();
        setAutoAssignDriversOpen(true);
    };

    const handleAutoAssignVehicles = () => {
        closeOptions();
        setAutoAssignVehiclesOpen(true);
    };

    const handleExport = () => {
        closeOptions();
        if (!exportMutation.isPending) exportMutation.mutate();
    };

    const handleExportOvertimeRequest = () => {
        closeOptions();
        if (!overtimeRequestMutation.isPending) overtimeRequestMutation.mutate();
    };

    const confirmRegister = () => {
        if (!registerMutation.isPending) registerMutation.mutate(replaceExisting);
    };

    const confirmAutoAssignDrivers = () => {
        if (!autoAssignDriversMutation.isPending) autoAssignDriversMutation.mutate();
    };

    const confirmAutoAssignVehicles = () => {
        if (!autoAssignVehiclesMutation.isPending) autoAssignVehiclesMutation.mutate();
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
                    title={pageTitle}
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
                            {draft && (
                                <>
                                    <PrimaryButton
                                        onClick={(event) => setOptionsAnchor(event.currentTarget)}
                                        disabled={busy}
                                        startIcon={<TuneOutlinedIcon />}
                                        endIcon={<KeyboardArrowDownIcon />}
                                        aria-haspopup="menu"
                                        aria-expanded={Boolean(optionsAnchor)}
                                    >
                                        Opciones
                                    </PrimaryButton>
                                    <Menu
                                        anchorEl={optionsAnchor}
                                        open={Boolean(optionsAnchor)}
                                        onClose={closeOptions}
                                        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                                        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                                    >
                                        {isDraft && (
                                            <MenuItem onClick={handleCreateRow}>
                                                <ListItemIcon><AddOutlinedIcon fontSize="small" /></ListItemIcon>
                                                <ListItemText>Crear gira</ListItemText>
                                            </MenuItem>
                                        )}
                                        {isDraft && (
                                            <MenuItem onClick={handleAutoAssignDrivers}>
                                                <ListItemIcon><BadgeOutlinedIcon fontSize="small" /></ListItemIcon>
                                                <ListItemText>Autoasignar choferes</ListItemText>
                                            </MenuItem>
                                        )}
                                        {isDraft && (
                                            <MenuItem onClick={handleAutoAssignVehicles}>
                                                <ListItemIcon><DirectionsCarFilledOutlinedIcon fontSize="small" /></ListItemIcon>
                                                <ListItemText>Autoasignar vehículos</ListItemText>
                                            </MenuItem>
                                        )}
                                        <MenuItem onClick={handleExport} disabled={exporting}>
                                            <ListItemIcon><DownloadOutlinedIcon fontSize="small" /></ListItemIcon>
                                            <ListItemText>{exportLabel}</ListItemText>
                                        </MenuItem>
                                        {periodLabel && (
                                            <MenuItem onClick={closeOptions}>
                                                <ListItemIcon><DownloadOutlinedIcon fontSize="small" /></ListItemIcon>
                                                <ListItemText>{`Descargar 'Control de Giras ${periodLabel}'`}</ListItemText>
                                            </MenuItem>
                                        )}
                                        {periodLabel && (
                                            <MenuItem onClick={handleExportOvertimeRequest} disabled={exportingOvertimeRequest}>
                                                <ListItemIcon><DownloadOutlinedIcon fontSize="small" /></ListItemIcon>
                                                <ListItemText>{`Descargar 'Solicitud de Horas Extra ${periodLabel}'`}</ListItemText>
                                            </MenuItem>
                                        )}
                                        {periodLabel && (
                                            <MenuItem onClick={closeOptions}>
                                                <ListItemIcon><DownloadOutlinedIcon fontSize="small" /></ListItemIcon>
                                                <ListItemText>{`Descargar 'Comisión Final ${periodLabel}'`}</ListItemText>
                                            </MenuItem>
                                        )}
                                    </Menu>
                                </>
                            )}
                        </Stack>
                    )}
                />

                {(loadingDraft || exporting || exportingOvertimeRequest || autoAssigningDrivers || autoAssigningVehicles) && <LinearProgress sx={{ mb: 2 }} />}

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

                <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
                    <Tabs value={viewTab} onChange={(_, value) => setViewTab(value)}>
                        <Tab value="rows" label="Filas" sx={{ textTransform: 'none', fontWeight: 700 }} />
                        <Tab value="schedule" label="Solicitud de horas extra" sx={{ textTransform: 'none', fontWeight: 700 }} />
                    </Tabs>
                </Box>

                {viewTab === 'rows' && (
                    <DraftRowsTable
                        columns={columns}
                        rows={rows}
                        loading={loadingRows}
                        fetching={fetchingRows}
                        error={rowsError}
                        totalElements={totalElements}
                        tableOptions={tableOptions}
                        isDraft={isDraft}
                        hasDuplicates={duplicateRows > 0}
                        onOpenRow={openRowModal}
                        onDeleteRow={openDeleteRow}
                    />
                )}

                {viewTab === 'schedule' && (
                    <DraftScheduleView draftId={draftId} isDraft={isDraft} onOpenRow={openRowModal} />
                )}
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
                open={autoAssignDriversOpen}
                type="warning"
                title="Autoasignar choferes"
                message={AUTO_ASSIGN_DRIVERS_MESSAGE}
                confirmLabel={autoAssigningDrivers ? 'Asignando...' : 'Sí, autoasignar'}
                onConfirm={confirmAutoAssignDrivers}
                onClose={autoAssigningDrivers ? undefined : () => setAutoAssignDriversOpen(false)}
            />

            <DialogModal
                open={autoAssignVehiclesOpen}
                type="warning"
                title="Autoasignar vehículos"
                message={AUTO_ASSIGN_VEHICLES_MESSAGE}
                confirmLabel={autoAssigningVehicles ? 'Asignando...' : 'Sí, autoasignar'}
                onConfirm={confirmAutoAssignVehicles}
                onClose={autoAssigningVehicles ? undefined : () => setAutoAssignVehiclesOpen(false)}
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
