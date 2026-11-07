import { memo, useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
    Alert,
    Box,
    Button,
    Chip,
    Container,
    IconButton,
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
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined';
import CallSplitOutlinedIcon from '@mui/icons-material/CallSplitOutlined';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import CloseIcon from '@mui/icons-material/Close';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import DirectionsCarFilledOutlinedIcon from '@mui/icons-material/DirectionsCarFilledOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import AutoFixHighOutlinedIcon from '@mui/icons-material/AutoFixHighOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import RouteOutlinedIcon from '@mui/icons-material/RouteOutlined';
import TuneOutlinedIcon from '@mui/icons-material/TuneOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import PrimaryButton from '../../../../common/components/PrimaryButton.jsx';
import NestedMenuItem from '../../../../common/components/NestedMenuItem.jsx';
import TableBase from '../../../../common/components/TablaBase.jsx';
import TableSelectFilter from '../../../../common/components/TableSelectFilter.jsx';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import { queryKeys } from '../../../../common/query';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { downloadBlob, formatDate } from '../../transportUtils.js';
import {
    autoAssignTourProcessingDraftDrivers,
    autoAssignTourProcessingDraftUlloaDrivers,
    autoAssignTourProcessingDraftVehicles,
    checkTourProcessingDraftFinalCommission,
    clearTourProcessingDraftAssignments,
    deleteTourProcessingDraftDuplicates,
    deleteTourProcessingDraftRow,
    exportTourProcessingDraft,
    exportTourProcessingDraftFinalCommission,
    exportTourProcessingDraftTourControl,
    finishTourProcessingDraft,
    rejectTourProcessingDraftIncompleteTours,
    reopenTourProcessingDraft,
    splitTourProcessingDraftOversizedRows,
    updateTourProcessingDraftRowApprovalStatus,
} from '../../services/tourProcessing/tourProcessingService';
import { exportDriverOvertimeRequest } from '../../services/drivers/driversService';
import { useTourProcessingCatalogMatch } from '../../hooks/useTourProcessingCatalogMatch.js';
import { useBorrowedVehicleOptions } from '../../hooks/useTransportOptions.js';
import {
    invalidateTourProcessingDraft,
    loadTourProcessingDraftRow,
    replaceCachedDraftRow,
    useTourProcessingDraft,
    useTourProcessingDraftRows,
} from '../../hooks/useTourProcessingDraftsData.js';
import { useServerTableState } from '../../hooks/useServerTableState.js';
import { usePersistedExpansion } from '../../hooks/usePersistedExpansion.js';
import CatalogValueCell from '../../components/tourProcessing/CatalogValueCell.jsx';
import TourProcessingAssignmentsCell from '../../components/tourProcessing/TourProcessingAssignmentsCell.jsx';
import TourProcessingDraftRowModal from '../../components/tourProcessing/TourProcessingDraftRowModal.jsx';
import TourProcessingDriverAssignmentReport from '../../components/tourProcessing/TourProcessingDriverAssignmentReport.jsx';
import TourProcessingAutoAssignmentInfoModal from '../../components/tourProcessing/TourProcessingAutoAssignmentInfoModal.jsx';
import TourProcessingFinalCommissionDialog from '../../components/tourProcessing/TourProcessingFinalCommissionDialog.jsx';
import { hasFinalCommissionFindings } from '../../components/tourProcessing/finalCommissionIssues.js';
import TourProcessingDraftDaysDetailPanel from '../../components/tourProcessing/TourProcessingDraftDaysDetailPanel.jsx';
import TourProcessingDraftDriverScheduleView from '../../components/tourProcessing/TourProcessingDraftDriverScheduleView.jsx';
import TourProcessingDraftTourControlView from '../../components/tourProcessing/TourProcessingDraftTourControlView.jsx';
import {
    APPROVAL_STATUS,
    APPROVAL_STATUS_FILTER_OPTIONS,
    approvalStatusColor,
    approvalStatusLabel,
    draftPeriodLabel,
    shortPeriodLabel,
    toggledApprovalStatus,
} from '../../components/tourProcessing/tourProcessingDraftLabels.js';
import DuplicateChip from '../../components/tourProcessing/DuplicateChip.jsx';
import { vehicleDistributionWarning } from '../../components/tourProcessing/vehicleDistributionWarning.js';

const HUB_PATH = '/transporte/depuracion';
const INITIAL_SORTING = [{ id: 'rowNumber', desc: false }];
const INITIAL_PAGE_SIZE = 50;
const TABLE_STORAGE_KEY = 'tour-processing-draft-rows-table-state-v3';
const INITIAL_COLUMN_VISIBILITY = { rowNumber: false };
const FILTERABLE_COLUMNS = new Set([
    'rowNumber', 'number', 'approvalStatus', 'driver', 'vehicle', 'passengers', 'executingUnit', 'responsible',
    'destination', 'durationDays', 'priority', 'modality', 'observations',
]);

const AUTO_ASSIGN_DRIVERS_MESSAGE = 'Las giras con varios viajes se completan en conjunto, asignando choferes y vehículos compatibles. Si no se puede completar una gira, se conservan sus asignaciones existentes y no se agregan partes sueltas.\n\nSi hay choferes asignados que no existen en el catálogo del sistema (marcados con el ícono de advertencia), esas filas quedarán sin chofer y la autoasignación intentará completarlas.\n\nLas giras que ya tienen un chofer del catálogo se respetan y no se reasignan.';
const AUTO_ASSIGN_ULLOA_DRIVERS_MESSAGE = 'Solo se asignan las giras Periféricas de Programa De Servicios Generales que no tienen chofer, usando los choferes marcados como conductores de giras Periféricas según su horario ordinario.\n\nSi alguna gira Periférica tiene un chofer que no existe en el catálogo del sistema (marcado con el ícono de advertencia), esa fila quedará sin chofer y se intentará completar. Las demás giras no se tocan.';
const AUTO_ASSIGN_DRIVERS_AND_VEHICLES_MESSAGE = 'Primero se autoasignan los choferes y después los vehículos.\n\nSi hay choferes o vehículos asignados que no existen en el catálogo del sistema (marcados con el ícono de advertencia), esas filas quedarán vacías y la autoasignación intentará completarlas.\n\nLas giras que ya tienen chofer o vehículo del catálogo se respetan y no se reasignan.';
const AUTO_ASSIGN_VEHICLES_MESSAGE = 'Las giras que ya tienen chofer del catálogo reciben prioridad para asignarles vehículo. En giras con varios viajes también se buscan los choferes faltantes para completar la gira en conjunto. Las asignaciones existentes se conservan.\n\nSi hay vehículos asignados que no existen en el catálogo del sistema o que no son una placa (marcados con el ícono de advertencia), esas filas quedarán sin vehículo y la autoasignación intentará completarlas.\n\nLas giras que ya tienen un vehículo del catálogo se respetan y no se reasignan.';

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
    canEdit,
    canDelete,
    hasDuplicates,
    onOpenRow,
    onDeleteRow,
    onToggleApproval,
}) {
    const [duplicateColumnHidden, setDuplicateColumnHidden] = useState(false);
    const { tableOptions: expansionOptions } = usePersistedExpansion('tour-processing-draft-rows-expand-all');

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
                    disabled: !canEdit,
                    onClick: () => onOpenRow('edit', row.original),
                },
                row.original.approvalStatus === APPROVAL_STATUS.NOT_APPROVED
                    ? {
                        key: 'approve',
                        label: 'Aprobar',
                        icon: <CheckCircleOutlineOutlinedIcon fontSize="small" />,
                        disabled: !canEdit,
                        onClick: () => onToggleApproval(row.original),
                    }
                    : {
                        key: 'disapprove',
                        label: 'No aprobar',
                        icon: <BlockOutlinedIcon fontSize="small" />,
                        disabled: !canEdit,
                        onClick: () => onToggleApproval(row.original),
                    },
                {
                    key: 'delete',
                    label: 'Eliminar',
                    color: 'error',
                    icon: <DeleteOutlineOutlinedIcon fontSize="small" />,
                    disabled: !canDelete,
                    onClick: () => onDeleteRow(row.original),
                },
            ]}
        />
    ), [canEdit, canDelete, onOpenRow, onDeleteRow, onToggleApproval]);

    const handleRowClick = useCallback(
        (row) => onOpenRow(canEdit ? 'edit' : 'view', row.original),
        [canEdit, onOpenRow],
    );

    const mergedTableOptions = useMemo(() => ({
        ...tableOptions,
        ...expansionOptions,
        onColumnVisibilityChange: handleColumnVisibilityChange,
        state: { ...tableOptions.state, ...expansionOptions.state, columnVisibility, density: 'compact' },
        rowCount: totalElements,
        muiPaginationProps: { rowsPerPageOptions: [10, 20, 50, 100] },
    }), [tableOptions, expansionOptions, handleColumnVisibilityChange, columnVisibility, totalElements]);

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
            renderDetailPanel={({ row }) => row.original.days?.length
                ? <TourProcessingDraftDaysDetailPanel days={row.original.days} assignments={row.original.assignments} />
                : null}
            tableOptions={mergedTableOptions}
        />
    );
});

function autoAssignDriversResultMessage(result) {
    const parts = [`Se asignó chofer a ${result?.assignedTours ?? 0} giras.`];
    if (result?.unassignedTours) parts.push(`${result.unassignedTours} giras quedaron sin chofer.`);
    if (result?.clearedTours) parts.push(`Se vaciaron ${result.clearedTours} giras que tenían un chofer fuera del catálogo.`);
    if (result?.mandatoryCoverage?.withWarning) parts.push(`${result.mandatoryCoverage.withWarning} giras obligatorias quedaron asignadas con advertencia de sobresueldo.`);
    if (result?.mandatoryCoverage?.unattended) parts.push(`${result.mandatoryCoverage.unattended} giras obligatorias quedaron sin atender.`);
    return parts.join('\n');
}

function autoAssignUlloaDriversResultMessage(result) {
    const parts = [`Se asignó chofer a ${result?.assignedTours ?? 0} giras Periféricas.`];
    if (result?.unassignedTours) parts.push(`${result.unassignedTours} giras Periféricas quedaron sin chofer.`);
    if (result?.clearedTours) parts.push(`Se vaciaron ${result.clearedTours} giras Periféricas que tenían un chofer fuera del catálogo.`);
    return parts.join('\n');
}

function finalCommissionResultMessage(rejectedTours) {
    return `Se descargó la Comisión Final. ${rejectedTours} giras sin chofer o vehículo cambiaron a estado No aprobada.`;
}

function autoAssignDriversAndVehiclesResultMessage({ drivers, vehicles }) {
    return `${autoAssignDriversResultMessage(drivers)}\n${autoAssignVehiclesResultMessage(vehicles)}`;
}

function autoAssignVehiclesResultMessage(result) {
    const parts = [`Se asignó vehículo a ${result?.assignedTours ?? 0} giras.`];
    if (result?.unassignedTours) parts.push(`${result.unassignedTours} giras o viajes quedaron sin vehículo. Revisá los motivos en el resultado de la autoasignación.`);
    if (result?.clearedTours) parts.push(`Se vaciaron ${result.clearedTours} giras que tenían un vehículo fuera del catálogo.`);
    return parts.join('\n');
}

export default function TourProcessingDraftPage() {
    const theme = useTheme();
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const { draftId } = useParams();
    const [searchParams, setSearchParams] = useSearchParams();
    const { hasAnyPermission } = usePermissions();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const [optionsAnchor, setOptionsAnchor] = useState(null);
    const [finishOpen, setFinishOpen] = useState(false);
    const [reopenOpen, setReopenOpen] = useState(false);
    const [deleteDuplicatesOpen, setDeleteDuplicatesOpen] = useState(false);
    const [splitOversizedOpen, setSplitOversizedOpen] = useState(false);
    const [dismissedOversizedRows, setDismissedOversizedRows] = useState(null);
    const [autoAssignDriversOpen, setAutoAssignDriversOpen] = useState(false);
    const [autoAssignUlloaOpen, setAutoAssignUlloaOpen] = useState(false);
    const [autoAssignVehiclesOpen, setAutoAssignVehiclesOpen] = useState(false);
    const [autoAssignBothOpen, setAutoAssignBothOpen] = useState(false);
    const [autoAssignmentInfoOpen, setAutoAssignmentInfoOpen] = useState(false);
    const [clearAssignmentsOpen, setClearAssignmentsOpen] = useState(false);
    const [rowToDelete, setRowToDelete] = useState(null);
    const [rowModal, setRowModal] = useState({ open: false, mode: 'view', row: null, rowId: null });
    const [alert, setAlert] = useState(null);
    const [assignmentReport, setAssignmentReport] = useState(null);
    const [assignmentReportOpen, setAssignmentReportOpen] = useState(false);
    const [finalCommissionCheck, setFinalCommissionCheck] = useState(null);
    const requestedTab = searchParams.get('tab');
    const viewTab = ['horas-extra', 'control-giras'].includes(requestedTab) ? requestedTab : 'rows';

    const handleViewTabChange = (_, value) => {
        setSearchParams((current) => {
            const next = new URLSearchParams(current);
            if (value === 'rows') next.delete('tab');
            else next.set('tab', value);
            return next;
        }, { replace: true });
    };

    const canCreateTours = hasAnyPermission([
        PERMISSIONS.TRANSPORT.TOURS.CREATE,
    ]);
    const canEditTours = hasAnyPermission([
        PERMISSIONS.TRANSPORT.TOURS.UPDATE,
    ]);
    const canDeleteTours = hasAnyPermission([
        PERMISSIONS.TRANSPORT.TOURS.DELETE,
    ]);
    const canExportTours = hasAnyPermission([
        PERMISSIONS.TRANSPORT.TOURS.EXPORT,
    ]);
    const canReadDrivers = hasAnyPermission([
        PERMISSIONS.TRANSPORT.DRIVERS.READ,
    ]);
    const canReadTourProcessing = hasAnyPermission([
        PERMISSIONS.TRANSPORT.TOURS.READ,
        PERMISSIONS.TRANSPORT.TOURS.IMPORT,
        PERMISSIONS.TRANSPORT.TOURS.CREATE,
        PERMISSIONS.TRANSPORT.TOURS.UPDATE,
        PERMISSIONS.TRANSPORT.TOURS.DELETE,
        PERMISSIONS.TRANSPORT.TOURS.EXPORT,
    ]);
    const canReadVehicles = hasAnyPermission([
        PERMISSIONS.TRANSPORT.VEHICLES.READ,
        PERMISSIONS.TRANSPORT.VEHICLES.MANAGE,
        PERMISSIONS.TRANSPORT.VEHICLES.DELETE,
    ]);

    const {
        driver: { isMissing: isDriverMissing },
        vehicle: { isMissing: isVehicleMissing, catalog: vehicleCatalog },
    } = useTourProcessingCatalogMatch();
    const borrowedVehicles = useBorrowedVehicleOptions(canReadVehicles);
    const { draft, loading: loadingDraft, error: draftError } = useTourProcessingDraft(draftId);
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
    } = useTourProcessingDraftRows(draftId, requestParams, { enabled: viewTab === 'rows' });

    const finishMutation = useMutation({
        mutationFn: () => finishTourProcessingDraft(draftId),
        onSuccess: () => {
            setFinishOpen(false);
            void invalidateTourProcessingDraft(queryClient, draftId);
            navigate(HUB_PATH, { state: { openHistory: true } });
        },
        onError: (error) => {
            setFinishOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo terminar la depuración') });
        },
    });

    const reopenMutation = useMutation({
        mutationFn: () => reopenTourProcessingDraft(draftId),
        onSuccess: (updatedDraft) => {
            setReopenOpen(false);
            queryClient.setQueryData(queryKeys.transport.tourProcessingDraftDetail(draftId), updatedDraft);
            void invalidateTourProcessingDraft(queryClient, draftId);
            setAlert({ type: 'success', message: 'La depuración volvió a estado borrador' });
        },
        onError: (error) => {
            setReopenOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo reabrir la depuración') });
        },
    });

    const deleteDuplicatesMutation = useMutation({
        mutationFn: () => deleteTourProcessingDraftDuplicates(draftId),
        onSuccess: (deletedRows) => {
            setDeleteDuplicatesOpen(false);
            tableOptions.onPaginationChange((current) => ({ ...current, pageIndex: 0 }));
            setAlert({ type: 'success', message: `Se eliminaron ${deletedRows} filas repetidas del borrador.` });
            void invalidateTourProcessingDraft(queryClient, draftId);
        },
        onError: (error) => {
            setDeleteDuplicatesOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron eliminar las filas repetidas') });
        },
    });

    const splitOversizedMutation = useMutation({
        mutationFn: () => splitTourProcessingDraftOversizedRows(draftId),
        onSuccess: (splitRows) => {
            setSplitOversizedOpen(false);
            setAlert({ type: 'success', message: `Se dividieron ${splitRows} giras por la capacidad de sus vehículos. Asigna ahora los choferes y vehículos de cada parte.` });
            void invalidateTourProcessingDraft(queryClient, draftId);
        },
        onError: (error) => {
            setSplitOversizedOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron dividir las giras') });
        },
    });

    const autoAssignDriversMutation = useMutation({
        mutationFn: () => autoAssignTourProcessingDraftDrivers(draftId),
        onSuccess: (result) => {
            setAutoAssignDriversOpen(false);
            setAssignmentReport({ draftId, result });
            setAssignmentReportOpen(true);
            setAlert({ type: 'success', message: autoAssignDriversResultMessage(result) });
            void invalidateTourProcessingDraft(queryClient, draftId);
        },
        onError: (error) => {
            setAutoAssignDriversOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron autoasignar los choferes') });
        },
    });

    const autoAssignUlloaMutation = useMutation({
        mutationFn: () => autoAssignTourProcessingDraftUlloaDrivers(draftId),
        onSuccess: (result) => {
            setAutoAssignUlloaOpen(false);
            setAssignmentReport({ draftId, result });
            setAssignmentReportOpen(true);
            setAlert({ type: 'success', message: autoAssignUlloaDriversResultMessage(result) });
            void invalidateTourProcessingDraft(queryClient, draftId);
        },
        onError: (error) => {
            setAutoAssignUlloaOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron autoasignar los choferes de Periféricas') });
        },
    });

    const autoAssignVehiclesMutation = useMutation({
        mutationFn: () => autoAssignTourProcessingDraftVehicles(draftId),
        onSuccess: (result) => {
            setAutoAssignVehiclesOpen(false);
            setAssignmentReport({ draftId, result: null, vehicleResult: result });
            setAssignmentReportOpen(true);
            setAlert({ type: 'success', message: autoAssignVehiclesResultMessage(result) });
            void invalidateTourProcessingDraft(queryClient, draftId);
        },
        onError: (error) => {
            setAutoAssignVehiclesOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron autoasignar los vehículos') });
        },
    });

    const autoAssignBothMutation = useMutation({
        mutationFn: async () => {
            const drivers = await autoAssignTourProcessingDraftDrivers(draftId);
            const vehicles = await autoAssignTourProcessingDraftVehicles(draftId);
            return { drivers, vehicles };
        },
        onSuccess: (result) => {
            setAutoAssignBothOpen(false);
            setAssignmentReport({ draftId, result: result.drivers, vehicleResult: result.vehicles });
            setAssignmentReportOpen(true);
            setAlert({ type: 'success', message: autoAssignDriversAndVehiclesResultMessage(result) });
            void invalidateTourProcessingDraft(queryClient, draftId);
        },
        onError: (error) => {
            setAutoAssignBothOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron autoasignar los choferes y vehículos') });
            void invalidateTourProcessingDraft(queryClient, draftId);
        },
    });

    const clearAssignmentsMutation = useMutation({
        mutationFn: () => clearTourProcessingDraftAssignments(draftId),
        onSuccess: () => {
            setClearAssignmentsOpen(false);
            setAlert({ type: 'success', message: 'Se limpiaron los choferes y vehículos de todas las giras del borrador.' });
            void invalidateTourProcessingDraft(queryClient, draftId);
        },
        onError: (error) => {
            setClearAssignmentsOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron limpiar las asignaciones') });
        },
    });

    const exportMutation = useMutation({
        mutationFn: () => exportTourProcessingDraft(draftId),
        onSuccess: ({ blob, fileName }) => downloadBlob(blob, fileName),
        onError: (error) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo exportar el borrador') });
        },
    });

    const tourControlMutation = useMutation({
        mutationFn: () => exportTourProcessingDraftTourControl(draftId),
        onSuccess: ({ blob, fileName }) => downloadBlob(blob, fileName),
        onError: (error) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo generar el control de giras') });
        },
    });

    const overtimeRequestMutation = useMutation({
        mutationFn: () => exportDriverOvertimeRequest(draft.year, draft.month),
        onSuccess: ({ blob, fileName }) => downloadBlob(blob, fileName),
        onError: (error) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo generar la solicitud de horas extra') });
        },
    });

    const finalCommissionMutation = useMutation({
        mutationFn: async ({ rejectIncomplete }) => {
            const rejectedTours = rejectIncomplete ? await rejectTourProcessingDraftIncompleteTours(draftId) : 0;
            const file = await exportTourProcessingDraftFinalCommission(draftId);
            return { ...file, rejectedTours };
        },
        onSuccess: ({ blob, fileName, rejectedTours }) => {
            setFinalCommissionCheck(null);
            downloadBlob(blob, fileName);
            if (rejectedTours > 0) {
                setAlert({ type: 'success', message: finalCommissionResultMessage(rejectedTours) });
                void invalidateTourProcessingDraft(queryClient, draftId);
            }
        },
        onError: (error) => {
            setFinalCommissionCheck(null);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo generar la Comisión Final') });
        },
    });

    const finalCommissionCheckMutation = useMutation({
        mutationFn: () => checkTourProcessingDraftFinalCommission(draftId),
        onSuccess: (check) => {
            if (hasFinalCommissionFindings(check)) setFinalCommissionCheck(check);
            else finalCommissionMutation.mutate({ rejectIncomplete: false });
        },
        onError: (error) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo revisar el borrador para la Comisión Final') });
        },
    });

    const deleteRowMutation = useMutation({
        mutationFn: (row) => deleteTourProcessingDraftRow(draftId, row.id),
        onSuccess: () => {
            setRowToDelete(null);
            setAlert({ type: 'success', message: 'Gira eliminada correctamente' });
            void invalidateTourProcessingDraft(queryClient, draftId);
        },
        onError: (error) => {
            setRowToDelete(null);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo eliminar la gira') });
        },
    });

    const approvalStatusMutation = useMutation({
        mutationFn: (row) => updateTourProcessingDraftRowApprovalStatus(draftId, row.id, toggledApprovalStatus(row.approvalStatus)),
        onSuccess: (updatedRow) => {
            replaceCachedDraftRow(queryClient, draftId, updatedRow);
            setAlert({
                type: 'success',
                message: updatedRow?.approvalStatus === APPROVAL_STATUS.NOT_APPROVED
                    ? 'Gira marcada como no aprobada'
                    : 'Gira aprobada correctamente',
            });
            void invalidateTourProcessingDraft(queryClient, draftId);
        },
        onError: (error) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo cambiar el estado de la gira') });
        },
    });

    const columns = useMemo(() => [
        { accessorKey: 'rowNumber', header: 'Fila', size: 90, grow: false },
        { accessorKey: 'number', header: 'Numero', size: 120, grow: false },
        {
            id: 'duplicate',
            header: 'Repetición',
            size: 120,
            grow: false,
            enableSorting: false,
            enableColumnFilter: false,
            accessorFn: (row) => row.duplicateRank,
            Cell: ({ row }) => <DuplicateChip row={row.original} />,
        },
        {
            accessorKey: 'approvalStatus',
            header: 'Estado',
            size: 120,
            grow: false,
            filterVariant: 'select',
            filterSelectOptions: APPROVAL_STATUS_FILTER_OPTIONS,
            Filter: ({ column }) => (
                <TableSelectFilter column={column} label="Estado" options={APPROVAL_STATUS_FILTER_OPTIONS} clearLabel="Todas" />
            ),
            Cell: ({ cell }) => (
                <Chip
                    label={approvalStatusLabel(cell.getValue())}
                    color={approvalStatusColor(cell.getValue())}
                    size="small"
                    variant="outlined"
                />
            ),
        },
        {
            accessorKey: 'driver',
            header: 'Chofer',
            size: 210,
            grow: false,
            Cell: ({ cell, row }) => {
                if (row.original.assignments?.length > 1) {
                    return <TourProcessingAssignmentsCell kind="driver" assignments={row.original.assignments} isMissing={isDriverMissing} />;
                }
                return <CatalogValueCell
                    kind="driver"
                    value={cell.getValue()}
                    missing={row.original.resourceType !== 'EXTERNAL' && isDriverMissing(cell.getValue())}
                />;
            },
        },
        {
            accessorKey: 'vehicle',
            header: 'Vehículo',
            size: 130,
            grow: false,
            Cell: ({ cell, row }) => {
                const warning = vehicleDistributionWarning(row.original, vehicleCatalog, borrowedVehicles.options);
                if (row.original.assignments?.length > 1) {
                    return <TourProcessingAssignmentsCell kind="vehicle" assignments={row.original.assignments} isMissing={isVehicleMissing} />;
                }
                return (
                    <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.75, maxWidth: '100%' }}>
                        <CatalogValueCell
                            kind="vehicle"
                            value={row.original.resourceType === 'BORROWED_VEHICLE' ? row.original.borrowedVehiclePlate : cell.getValue()}
                            missing={row.original.resourceType !== 'EXTERNAL' && row.original.resourceType !== 'BORROWED_VEHICLE' && isVehicleMissing(cell.getValue())}
                        />
                        {warning && (
                            <Tooltip title={warning} placement="top">
                                <WarningAmberOutlinedIcon
                                    role="img"
                                    aria-label={warning}
                                    sx={{ fontSize: 18, color: 'warning.main', flexShrink: 0, cursor: 'default' }}
                                />
                            </Tooltip>
                        )}
                    </Box>
                );
            },
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
    ], [isDriverMissing, isVehicleMissing, vehicleCatalog, borrowedVehicles.options]);

    const openRowModal = useCallback((mode, row) => setRowModal({ open: true, mode, row, rowId: row?.id ?? null }), []);
    const openRowModalById = useCallback((mode, rowId) => setRowModal({ open: true, mode, row: null, rowId }), []);
    const pendingRowId = rowModal.open && !rowModal.row ? rowModal.rowId : null;

    useEffect(() => {
        if (!pendingRowId) return undefined;
        let active = true;
        loadTourProcessingDraftRow(draftId, pendingRowId)
            .then((row) => {
                if (active) setRowModal((current) => (current.rowId === pendingRowId ? { ...current, row } : current));
            })
            .catch((error) => {
                if (!active) return;
                setRowModal((current) => ({ ...current, open: false }));
                setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo cargar la gira') });
            });
        return () => {
            active = false;
        };
    }, [draftId, pendingRowId]);
    const openDeleteRow = useCallback((row) => setRowToDelete(row), []);
    const { isPending: changingApproval, mutate: toggleApproval } = approvalStatusMutation;
    const handleToggleApproval = useCallback((row) => {
        if (!changingApproval) toggleApproval(row);
    }, [changingApproval, toggleApproval]);

    if (!canReadTourProcessing) return <AccessDeniedState />;

    const isOpenDraft = draft?.status === 'DRAFT';
    const canCreateRows = isOpenDraft && canCreateTours;
    const canEditRows = isOpenDraft && canEditTours;
    const canDeleteRows = isOpenDraft && canDeleteTours;
    const canReopen = draft?.status === 'CLOSED' && canEditTours;
    const canExportOvertime = canReadDrivers && Boolean(draft?.year && draft?.month);
    const hasOptions = canCreateRows || canEditRows || canReopen || canExportTours || canExportOvertime;
    const conflictingRows = draft?.conflictingRows ?? 0;
    const duplicateRows = draft?.duplicateRows ?? 0;
    const oversizedRows = draft?.oversizedRows ?? 0;
    const exporting = exportMutation.isPending;
    const autoAssigningDrivers = autoAssignDriversMutation.isPending;
    const autoAssigningUlloa = autoAssignUlloaMutation.isPending;
    const autoAssigningVehicles = autoAssignVehiclesMutation.isPending;
    const autoAssigningBoth = autoAssignBothMutation.isPending;
    const clearingAssignments = clearAssignmentsMutation.isPending;
    const exportingTourControl = tourControlMutation.isPending;
    const exportingOvertimeRequest = overtimeRequestMutation.isPending;
    const exportingFinalCommission = finalCommissionCheckMutation.isPending || finalCommissionMutation.isPending;
    const periodLabel = draft?.year && draft?.month ? shortPeriodLabel(draft.year, draft.month) : null;
    const exportLabel = draft?.year && draft?.month ? `Descargar '${draftPeriodLabel(draft)}'` : 'Descargar borrador';
    const hasAssignmentReport = assignmentReport?.draftId === draftId;
    const busy = finishMutation.isPending || reopenMutation.isPending || autoAssigningDrivers || autoAssigningUlloa || autoAssigningVehicles || autoAssigningBoth || clearingAssignments || deleteDuplicatesMutation.isPending || splitOversizedMutation.isPending;
    const pageTitle = draft?.year && draft?.month
        ? `Depuración de giras ${draftPeriodLabel(draft).toLowerCase()}`
        : 'Depuración de giras';

    const closeOptions = () => setOptionsAnchor(null);

    const handleFinish = () => {
        closeOptions();
        setFinishOpen(true);
    };

    const handleReopen = () => {
        closeOptions();
        setReopenOpen(true);
    };

    const handleCreateRow = () => {
        closeOptions();
        openRowModal('create', null);
    };

    const handleAutoAssignDrivers = () => {
        closeOptions();
        setAutoAssignDriversOpen(true);
    };

    const handleAutoAssignUlloa = () => {
        closeOptions();
        setAutoAssignUlloaOpen(true);
    };

    const handleOpenAssignmentReport = () => {
        closeOptions();
        setAssignmentReportOpen(true);
    };

    const handleAutoAssignVehicles = () => {
        closeOptions();
        setAutoAssignVehiclesOpen(true);
    };

    const handleAutoAssignBoth = () => {
        closeOptions();
        setAutoAssignBothOpen(true);
    };

    const handleAutoAssignmentInfo = () => {
        closeOptions();
        setAutoAssignmentInfoOpen(true);
    };

    const handleClearAssignments = () => {
        closeOptions();
        setClearAssignmentsOpen(true);
    };

    const handleExport = () => {
        closeOptions();
        if (!exportMutation.isPending) exportMutation.mutate();
    };

    const handleExportTourControl = () => {
        closeOptions();
        if (!tourControlMutation.isPending) tourControlMutation.mutate();
    };

    const handleExportOvertimeRequest = () => {
        closeOptions();
        if (!overtimeRequestMutation.isPending) overtimeRequestMutation.mutate();
    };

    const handleExportFinalCommission = () => {
        closeOptions();
        if (!exportingFinalCommission) finalCommissionCheckMutation.mutate();
    };

    const confirmFinalCommission = (options) => {
        if (!finalCommissionMutation.isPending) finalCommissionMutation.mutate(options);
    };

    const confirmAutoAssignDrivers = () => {
        if (!autoAssignDriversMutation.isPending) autoAssignDriversMutation.mutate();
    };

    const confirmAutoAssignUlloa = () => {
        if (!autoAssignUlloaMutation.isPending) autoAssignUlloaMutation.mutate();
    };

    const confirmAutoAssignVehicles = () => {
        if (!autoAssignVehiclesMutation.isPending) autoAssignVehiclesMutation.mutate();
    };

    const confirmAutoAssignBoth = () => {
        if (!autoAssignBothMutation.isPending) autoAssignBothMutation.mutate();
    };

    const confirmClearAssignments = () => {
        if (!clearAssignmentsMutation.isPending) clearAssignmentsMutation.mutate();
    };

    const confirmDeleteRow = () => {
        if (!deleteRowMutation.isPending) deleteRowMutation.mutate(rowToDelete);
    };

    const confirmDeleteDuplicates = () => {
        if (!deleteDuplicatesMutation.isPending) deleteDuplicatesMutation.mutate();
    };

    const confirmSplitOversized = () => {
        if (!splitOversizedMutation.isPending) splitOversizedMutation.mutate();
    };

    const closeRowModal = () => setRowModal((current) => ({ ...current, open: false }));

    const secondaryButtonSx = {
        borderColor: 'divider', color: 'text.secondary',
        textTransform: 'none', fontWeight: 700, fontSize: 12.5, borderRadius: '8px',
        '&:hover': { borderColor: accentColor, color: accentColor, bgcolor: 'action.hover' },
    };

    return (
        <Box className="transport-tour-processing-draft-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title={pageTitle}
                    action={(
                        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
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
                            {draft && hasOptions && (
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
                                        {canCreateRows && (
                                            <MenuItem onClick={handleCreateRow}>
                                                <ListItemIcon><AddOutlinedIcon fontSize="small" /></ListItemIcon>
                                                <ListItemText>Crear gira</ListItemText>
                                            </MenuItem>
                                        )}
                                        {canEditRows && (
                                            <NestedMenuItem label="Autoasignar" icon={<AutoFixHighOutlinedIcon fontSize="small" />}>
                                                <MenuItem onClick={handleAutoAssignDrivers}>
                                                    <ListItemIcon><BadgeOutlinedIcon fontSize="small" /></ListItemIcon>
                                                    <ListItemText>Autoasignar choferes</ListItemText>
                                                </MenuItem>
                                                <MenuItem onClick={handleAutoAssignVehicles}>
                                                    <ListItemIcon><DirectionsCarFilledOutlinedIcon fontSize="small" /></ListItemIcon>
                                                    <ListItemText>Autoasignar vehículos</ListItemText>
                                                </MenuItem>
                                                <MenuItem onClick={handleAutoAssignUlloa}>
                                                    <ListItemIcon><RouteOutlinedIcon fontSize="small" /></ListItemIcon>
                                                    <ListItemText>Autoasignar choferes - Solo Periféricas</ListItemText>
                                                </MenuItem>
                                                <MenuItem onClick={handleAutoAssignBoth}>
                                                    <ListItemIcon><GroupsOutlinedIcon fontSize="small" /></ListItemIcon>
                                                    <ListItemText>Autoasignar choferes y vehículos</ListItemText>
                                                </MenuItem>
                                                 <MenuItem onClick={handleAutoAssignmentInfo}>
                                                     <ListItemIcon><InfoOutlinedIcon fontSize="small" /></ListItemIcon>
                                                     <ListItemText>Información de la autoasignación</ListItemText>
                                                 </MenuItem>
                                            </NestedMenuItem>
                                        )}
                                        {canEditRows && hasAssignmentReport && (
                                            <MenuItem onClick={handleOpenAssignmentReport}>
                                                <ListItemIcon><FactCheckOutlinedIcon fontSize="small" /></ListItemIcon>
                                                <ListItemText>Ver resultado de la última autoasignación</ListItemText>
                                            </MenuItem>
                                        )}
                                        {canEditRows && (
                                            <MenuItem onClick={handleClearAssignments}>
                                                <ListItemIcon><DeleteOutlineOutlinedIcon fontSize="small" /></ListItemIcon>
                                                <ListItemText>Limpiar Choferes y Vehiculos</ListItemText>
                                            </MenuItem>
                                        )}
                                        {canReopen && (
                                            <MenuItem onClick={handleReopen}>
                                                <ListItemIcon><EditOutlinedIcon fontSize="small" /></ListItemIcon>
                                                <ListItemText>Devolver a borrador</ListItemText>
                                            </MenuItem>
                                        )}
                                        {(canExportTours || canExportOvertime) && (
                                            <NestedMenuItem label="Descargar" icon={<DownloadOutlinedIcon fontSize="small" />}>
                                                {canExportTours && (
                                                    <MenuItem onClick={handleExport} disabled={exporting}>
                                                        <ListItemIcon><DownloadOutlinedIcon fontSize="small" /></ListItemIcon>
                                                        <ListItemText>{exportLabel}</ListItemText>
                                                    </MenuItem>
                                                )}
                                                {canExportTours && periodLabel && (
                                                    <MenuItem onClick={handleExportTourControl} disabled={exportingTourControl}>
                                                        <ListItemIcon><DownloadOutlinedIcon fontSize="small" /></ListItemIcon>
                                                        <ListItemText>{`Descargar 'Control de Giras ${periodLabel}'`}</ListItemText>
                                                    </MenuItem>
                                                )}
                                                {canExportOvertime && (
                                                    <MenuItem onClick={handleExportOvertimeRequest} disabled={exportingOvertimeRequest}>
                                                        <ListItemIcon><DownloadOutlinedIcon fontSize="small" /></ListItemIcon>
                                                        <ListItemText>{`Descargar 'Solicitud de Horas Extra ${periodLabel}'`}</ListItemText>
                                                    </MenuItem>
                                                )}
                                                {canExportTours && periodLabel && (
                                                    <MenuItem onClick={handleExportFinalCommission} disabled={exportingFinalCommission}>
                                                        <ListItemIcon><DownloadOutlinedIcon fontSize="small" /></ListItemIcon>
                                                        <ListItemText>{`Descargar 'Comisión Final ${periodLabel}'`}</ListItemText>
                                                    </MenuItem>
                                                )}
                                            </NestedMenuItem>
                                        )}
                                        {canEditRows && (
                                            <MenuItem onClick={handleFinish}>
                                                <ListItemIcon><CheckCircleOutlineOutlinedIcon fontSize="small" /></ListItemIcon>
                                                <ListItemText>Terminar depuración</ListItemText>
                                            </MenuItem>
                                        )}
                                    </Menu>
                                </>
                            )}
                        </Stack>
                    )}
                />

                {(loadingDraft || exporting || exportingTourControl || exportingOvertimeRequest || exportingFinalCommission || autoAssigningDrivers || autoAssigningUlloa || autoAssigningVehicles || autoAssigningBoth || clearingAssignments) && <LinearProgress sx={{ mb: 2 }} />}

                {draftError && (
                    <Alert severity="error" variant="outlined" sx={{ mb: 2 }}>
                        {draftError}
                    </Alert>
                )}

                {draft && (
                    <Stack spacing={1.5} sx={{ mb: 2 }}>
                        {canEditRows && conflictingRows > 0 && (
                            <Alert severity="error" variant="outlined">
                                Hay {conflictingRows} filas con un Numero repetido pero con datos distintos. No se puede registrar hasta resolverlas.
                            </Alert>
                        )}
                        {canDeleteRows && conflictingRows === 0 && duplicateRows > 0 && (
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
                        {canEditRows && conflictingRows === 0 && duplicateRows === 0 && oversizedRows > 0 && dismissedOversizedRows !== oversizedRows && (
                            <Alert
                                severity="warning"
                                variant="outlined"
                                action={(
                                    <Stack direction="row" alignItems="center" spacing={0.5}>
                                        <Button
                                            color="inherit"
                                            size="small"
                                            onClick={() => setSplitOversizedOpen(true)}
                                            disabled={busy}
                                            startIcon={<CallSplitOutlinedIcon />}
                                            sx={{ textTransform: 'none', fontWeight: 700, fontSize: 12.5, whiteSpace: 'nowrap' }}
                                        >
                                            Dividir giras automáticamente
                                        </Button>
                                        <IconButton
                                            color="inherit"
                                            size="small"
                                            aria-label="Cerrar advertencia"
                                            onClick={() => setDismissedOversizedRows(oversizedRows)}
                                        >
                                            <CloseIcon fontSize="small" />
                                        </IconButton>
                                    </Stack>
                                )}
                            >
                                Hay {oversizedRows} giras que necesitarán más de un viaje o vehículo para completarse
                            </Alert>
                        )}
                    </Stack>
                )}

                <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
                    <Tabs value={viewTab} onChange={handleViewTabChange} variant="scrollable" scrollButtons="auto">
                        <Tab value="rows" label="Filas" sx={{ textTransform: 'none', fontWeight: 700 }} />
                        <Tab value="horas-extra" label="Solicitud de horas extra" sx={{ textTransform: 'none', fontWeight: 700 }} />
                        <Tab value="control-giras" label="Control de giras" sx={{ textTransform: 'none', fontWeight: 700 }} />
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
                        canEdit={canEditRows}
                        canDelete={canDeleteRows}
                        hasDuplicates={duplicateRows > 0}
                        onOpenRow={openRowModal}
                        onDeleteRow={openDeleteRow}
                        onToggleApproval={handleToggleApproval}
                    />
                )}

                {viewTab === 'horas-extra' && (
                    <TourProcessingDraftDriverScheduleView draftId={draftId} canEdit={canEditRows} onOpenRowById={openRowModalById} />
                )}
                {viewTab === 'control-giras' && draft && (
                    <TourProcessingDraftTourControlView key={`${draftId}-${draft.year}-${draft.month}`} draftId={draftId} year={draft.year} month={draft.month} canEdit={canEditRows} onOpenRowById={openRowModalById} />
                )}
            </Container>

            <TourProcessingDraftRowModal
                open={rowModal.open}
                mode={rowModal.mode}
                row={rowModal.row}
                loadingRow={Boolean(pendingRowId)}
                draftId={draftId}
                canDelete={canDeleteRows}
                onClose={closeRowModal}
                onEdit={canEditRows ? () => openRowModal('edit', rowModal.row) : undefined}
                onDeleted={() => setAlert({ type: 'success', message: 'Gira eliminada correctamente' })}
                onSaved={() => setAlert({
                    type: 'success',
                    message: rowModal.mode === 'create' ? 'Gira creada correctamente' : 'Fila actualizada correctamente',
                })}
            />

            <DialogModal
                open={finishOpen}
                type="warning"
                title="Terminar depuración"
                message="La depuración aparecerá en el historial y no se podrá editar hasta que se devuelva a borrador. Las horas del mes seguirán contando para los choferes."
                confirmLabel={finishMutation.isPending ? 'Terminando...' : 'Terminar depuración'}
                onConfirm={() => { if (!finishMutation.isPending) finishMutation.mutate(); }}
                onClose={finishMutation.isPending ? undefined : () => setFinishOpen(false)}
            />

            <DialogModal
                open={reopenOpen}
                type="warning"
                title="Devolver a borrador"
                message="La depuración volverá a ser editable y aparecerá en la lista de borradores."
                confirmLabel={reopenMutation.isPending ? 'Reabriendo...' : 'Devolver a borrador'}
                onConfirm={() => { if (!reopenMutation.isPending) reopenMutation.mutate(); }}
                onClose={reopenMutation.isPending ? undefined : () => setReopenOpen(false)}
            />

            <DialogModal
                open={Boolean(rowToDelete)}
                type="delete"
                title="Eliminar gira"
                message={rowToDelete ? `Se eliminará la fila ${rowToDelete.rowNumber} (Número ${rowToDelete.number}) del borrador.` : ''}
                confirmLabel={deleteRowMutation.isPending ? 'Eliminando...' : 'Sí, eliminar'}
                onConfirm={confirmDeleteRow}
                onClose={() => setRowToDelete(null)}
            />

            {hasAssignmentReport && (
                <TourProcessingDriverAssignmentReport
                    open={assignmentReportOpen}
                    result={assignmentReport.result}
                    vehicleResult={assignmentReport.vehicleResult}
                    subtitle={draftPeriodLabel(draft)}
                    onClose={() => setAssignmentReportOpen(false)}
                />
            )}

            {finalCommissionCheck && (
                <TourProcessingFinalCommissionDialog
                    open
                    check={finalCommissionCheck}
                    subtitle={periodLabel ? `Comisión Final ${periodLabel}` : ''}
                    downloading={finalCommissionMutation.isPending}
                    canRejectIncomplete={canEditTours}
                    onClose={() => setFinalCommissionCheck(null)}
                    onConfirm={confirmFinalCommission}
                />
            )}

            <DialogModal
                open={autoAssignDriversOpen}
                type="warning"
                title="Autoasignar choferes"
                message={AUTO_ASSIGN_DRIVERS_MESSAGE}
                confirmLabel={autoAssigningDrivers ? 'Asignando...' : 'Sí, autoasignar'}
                onConfirm={confirmAutoAssignDrivers}
                onClose={autoAssigningDrivers ? undefined : () => setAutoAssignDriversOpen(false)}
            />

            <TourProcessingAutoAssignmentInfoModal
                open={autoAssignmentInfoOpen}
                onClose={() => setAutoAssignmentInfoOpen(false)}
            />

            <DialogModal
                open={autoAssignUlloaOpen}
                type="warning"
                title="Autoasignar choferes - Solo Periféricas"
                message={AUTO_ASSIGN_ULLOA_DRIVERS_MESSAGE}
                confirmLabel={autoAssigningUlloa ? 'Asignando...' : 'Sí, autoasignar'}
                onConfirm={confirmAutoAssignUlloa}
                onClose={autoAssigningUlloa ? undefined : () => setAutoAssignUlloaOpen(false)}
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
                open={autoAssignBothOpen}
                type="warning"
                title="Autoasignar choferes y vehículos"
                message={AUTO_ASSIGN_DRIVERS_AND_VEHICLES_MESSAGE}
                confirmLabel={autoAssigningBoth ? 'Asignando...' : 'Sí, autoasignar'}
                onConfirm={confirmAutoAssignBoth}
                onClose={autoAssigningBoth ? undefined : () => setAutoAssignBothOpen(false)}
            />

            <DialogModal
                open={clearAssignmentsOpen}
                type="warning"
                title="Limpiar Choferes y Vehiculos"
                message="Se eliminarán las asignaciones de todos los choferes y vehículos de este borrador, incluidos los vehículos prestados. Las giras permanecerán."
                confirmLabel={clearingAssignments ? 'Limpiando...' : 'Sí, limpiar'}
                onConfirm={confirmClearAssignments}
                onClose={clearingAssignments ? undefined : () => setClearAssignmentsOpen(false)}
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
                open={splitOversizedOpen}
                type="warning"
                title="Dividir giras automáticamente"
                message={`Se dividirán ${oversizedRows} giras cuyos pasajeros superan la capacidad de su vehículo. Cada una quedará repartida en tantos vehículos como haga falta, con los pasajeros distribuidos de forma equitativa.\n\nLa primera parte conserva el chofer y el vehículo actuales; las demás quedan sin asignar.`}
                confirmLabel={splitOversizedMutation.isPending ? 'Dividiendo...' : 'Sí, dividir giras'}
                onConfirm={confirmSplitOversized}
                onClose={splitOversizedMutation.isPending ? undefined : () => setSplitOversizedOpen(false)}
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
