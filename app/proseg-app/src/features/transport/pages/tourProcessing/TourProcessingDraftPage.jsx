import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
    Alert,
    Box,
    Button,
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
    TextField,
    useTheme,
} from '@mui/material';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
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
import TuneOutlinedIcon from '@mui/icons-material/TuneOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import PrimaryButton from '../../../../common/components/PrimaryButton.jsx';
import NestedMenuItem from '../../../../common/components/NestedMenuItem.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import { queryKeys } from '../../../../common/query';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { downloadBlob } from '../../transportUtils.js';
import {
    autoAssignTourProcessingDraftDrivers,
    autoAssignTourProcessingDraftVehicles,
    checkTourProcessingDraftFinalCommission,
    clearTourProcessingDraftAssignments,
    deleteTourProcessingDraftDuplicates,
    exportTourProcessingDraft,
    exportTourProcessingDraftFinalCommission,
    exportTourProcessingDraftTourControl,
    finishTourProcessingDraft,
    rejectTourProcessingDraftIncompleteTours,
    reopenTourProcessingDraft,
    splitTourProcessingDraftOversizedRows,
    startAutoAssignWizard,
} from '../../services/tourProcessing/tourProcessingService';
import { exportDriverOvertimeRequest } from '../../services/drivers/driversService';
import {
    invalidateTourProcessingDraft,
    loadTourProcessingDraftRow,
    useTourProcessingAutoAssignWizard,
    useTourProcessingDraft,
    useTourProcessingDraftRowProblems,
    useTourProcessingDraftRows,
} from '../../hooks/useTourProcessingDraftsData.js';
import { useServerTableState } from '../../hooks/useServerTableState.js';
import DraftRowsTable from '../../components/tourProcessing/DraftRowsTable.jsx';
import TourProcessingAutoAssignWizard from '../../components/tourProcessing/TourProcessingAutoAssignWizard.jsx';
import { DRAFT_ROWS_FILTERABLE_COLUMNS, useDraftRowColumns } from '../../components/tourProcessing/useDraftRowColumns.jsx';
import TourProcessingDraftRowModal from '../../components/tourProcessing/TourProcessingDraftRowModal.jsx';
import TourProcessingDriverAssignmentReport from '../../components/tourProcessing/TourProcessingDriverAssignmentReport.jsx';
import TourProcessingAutoAssignmentInfoModal from '../../components/tourProcessing/TourProcessingAutoAssignmentInfoModal.jsx';
import TourProcessingFinalCommissionDialog from '../../components/tourProcessing/TourProcessingFinalCommissionDialog.jsx';
import TourProcessingSplitModeDialog from '../../components/tourProcessing/TourProcessingSplitModeDialog.jsx';
import { TOUR_RULE_FILTERS } from '../../components/tourProcessing/tourRuleFilters.js';
import TourProcessingDraftDriverScheduleView from '../../components/tourProcessing/TourProcessingDraftDriverScheduleView.jsx';
import TourProcessingDraftTourControlView from '../../components/tourProcessing/TourProcessingDraftTourControlView.jsx';
import {
    draftPeriodLabel,
    shortPeriodLabel,
} from '../../components/tourProcessing/tourProcessingDraftLabels.js';

const HUB_PATH = '/transporte/depuracion';
const alertActionButtonSx = (theme) => ({
    textTransform: 'none',
    fontWeight: 700,
    fontSize: 12.5,
    whiteSpace: 'nowrap',
    color: 'text.primary',
    backgroundColor: 'hsl(45, 92%, 86%)',
    boxShadow: '0 3px 8px rgba(98, 73, 12, 0.18)',
    transition: 'transform 160ms cubic-bezier(0.23, 1, 0.32, 1), box-shadow 160ms cubic-bezier(0.23, 1, 0.32, 1)',
    '&:hover': { backgroundColor: 'hsl(45, 92%, 86%)' },
    '@media (hover: hover) and (pointer: fine)': {
        '&:hover': {
            boxShadow: '0 4px 10px rgba(98, 73, 12, 0.21)',
            transform: 'translateY(-2px)',
        },
    },
    '&:active': { transform: 'translateY(0)' },
    '@media (prefers-reduced-motion: reduce)': {
        transition: 'box-shadow 160ms ease',
        '@media (hover: hover) and (pointer: fine)': {
            '&:hover': { transform: 'none' },
        },
    },
    ...theme.applyStyles('dark', {
        color: 'hsl(45, 92%, 90%)',
        backgroundColor: 'hsl(45, 45%, 18%)',
        '&:hover': { backgroundColor: 'hsl(45, 45%, 18%)' },
    }),
});
const INITIAL_SORTING = [{ id: 'rowNumber', desc: false }];
const INITIAL_PAGE_SIZE = 50;
const TABLE_STORAGE_KEY = 'tour-processing-draft-rows-table-state-v3';
const RULE_STORAGE_KEY = 'tour-processing-draft-rows-rule';
const INITIAL_COLUMN_VISIBILITY = { rowNumber: false };

function readStoredRule() {
    try {
        const stored = localStorage.getItem(RULE_STORAGE_KEY);
        return TOUR_RULE_FILTERS.some((option) => option.code === stored) ? stored : '';
    } catch {
        return '';
    }
}

function writeStoredRule(rule) {
    try {
        if (rule) localStorage.setItem(RULE_STORAGE_KEY, rule);
        else localStorage.removeItem(RULE_STORAGE_KEY);
    } catch {
        return;
    }
}
const AUTO_ASSIGN_DRIVERS_MESSAGE = 'Las giras con varios viajes se completan en conjunto, asignando choferes y vehículos compatibles. Si no se puede completar una gira, se conservan sus asignaciones existentes y no se agregan partes sueltas.\n\nSi una gira interna tiene un chofer que no existe en el catálogo del sistema (marcado con el ícono de advertencia), esa fila quedará sin chofer y la autoasignación intentará completarla.\n\nLas giras que ya tienen un chofer del catálogo se respetan y no se reasignan. Los préstamos de vehículo no se modifican: el chofer se deja como está. Un chofer de Servicios Generales puesto a mano en un préstamo no se considera ocupado en ese horario.';
const AUTO_ASSIGN_DRIVERS_AND_VEHICLES_MESSAGE = 'Se calculará la autoasignación completa de choferes y vehículos y se te mostrará paso a paso: primero las giras de prioridad 1 con menos de 5 pasajeros, luego las Periféricas, después las demás prioridades sin horas extra y al final las que usan fin de semana u horas extra.\n\nEn cada paso puedes revisar y editar los choferes y vehículos antes de aceptarlo. Si cambias algo a mano, al continuar se recalcula lo que se asignó automáticamente respetando tus cambios.\n\nSolo se consideran las giras aprobadas sin chofer o sin vehículo del catálogo. Las que ya tienen ambos se respetan. Podrás guardar el progreso y retomarlo después, o descartar todo y volver al estado inicial.';
const AUTO_ASSIGN_VEHICLES_MESSAGE = 'Las giras que ya tienen chofer del catálogo reciben prioridad para asignarles vehículo. En giras con varios viajes también se buscan los choferes faltantes para completar la gira en conjunto. Las asignaciones existentes se conservan.\n\nSi hay vehículos asignados que no existen en el catálogo del sistema o que no son una placa (marcados con el ícono de advertencia), esas filas quedarán sin vehículo y la autoasignación intentará completarlas.\n\nLas giras que ya tienen un vehículo del catálogo se respetan y no se reasignan.';

function autoAssignDriversResultMessage(result) {
    const parts = [`Se asignó chofer a ${result?.assignedTours ?? 0} giras.`];
    if (result?.unassignedTours) parts.push(`${result.unassignedTours} giras quedaron sin chofer.`);
    if (result?.clearedTours) parts.push(`Se vaciaron ${result.clearedTours} giras que tenían un chofer fuera del catálogo.`);
    if (result?.mandatoryCoverage?.withWarning) parts.push(`${result.mandatoryCoverage.withWarning} giras obligatorias quedaron asignadas con advertencia de sobresueldo.`);
    if (result?.mandatoryCoverage?.unattended) parts.push(`${result.mandatoryCoverage.unattended} giras obligatorias quedaron sin atender.`);
    return parts.join('\n');
}

function savedRowMessage({ created, number, changes }) {
    if (created) return `Gira ${number} creada correctamente`;
    if (!changes?.length) return `Gira ${number} guardada. No hubo cambios en sus datos.`;
    return `Gira ${number} actualizada correctamente.\n\n${changes.join('\n')}`;
}

function rejectedIncompleteMessage(rejectedTours) {
    return rejectedTours > 0
        ? `Se pasaron ${rejectedTours} giras sin chofer o vehículo a estado No aprobada.`
        : 'No había giras sin chofer o vehículo.';
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
    const [autoAssignVehiclesOpen, setAutoAssignVehiclesOpen] = useState(false);
    const [autoAssignBothOpen, setAutoAssignBothOpen] = useState(false);
    const [wizardOpen, setWizardOpen] = useState(false);
    const [wizardNoticeDismissed, setWizardNoticeDismissed] = useState(false);
    const [autoAssignmentInfoOpen, setAutoAssignmentInfoOpen] = useState(false);
    const [clearAssignmentsOpen, setClearAssignmentsOpen] = useState(false);
    const [exportHighlightOpen, setExportHighlightOpen] = useState(false);
    const [rowModal, setRowModal] = useState({ open: false, mode: 'view', row: null, rowId: null });
    const [alert, setAlert] = useState(null);
    const [assignmentReport, setAssignmentReport] = useState(null);
    const [assignmentReportOpen, setAssignmentReportOpen] = useState(false);
    const [errorsOpen, setErrorsOpen] = useState(false);
    const [rejectIncompleteOpen, setRejectIncompleteOpen] = useState(false);
    const [rule, setRule] = useState(readStoredRule);
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

    const { draft, loading: loadingDraft, error: draftError } = useTourProcessingDraft(draftId);
    const { wizard } = useTourProcessingAutoAssignWizard(draftId, { enabled: draft?.status === 'DRAFT' && canEditTours });
    const { requestParams, tableOptions } = useServerTableState({
        initialSorting: INITIAL_SORTING,
        filterableColumns: DRAFT_ROWS_FILTERABLE_COLUMNS,
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
    } = useTourProcessingDraftRows(draftId, { ...requestParams, rule }, { enabled: viewTab === 'rows' });
    const { problemsByRowId } = useTourProcessingDraftRowProblems(draftId, { enabled: viewTab === 'rows' });

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
            setAlert({ type: 'success', message: `Se dividieron ${splitRows} giras por la capacidad de sus vehículos. Revisa los viajes de cada gira y completa los choferes y vehículos que falten.` });
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

    const startWizardMutation = useMutation({
        mutationFn: () => startAutoAssignWizard(draftId),
        onSuccess: (started) => {
            setAutoAssignBothOpen(false);
            queryClient.setQueryData(queryKeys.transport.tourProcessingDraftAutoAssignWizard(draftId), started);
            setWizardOpen(true);
            void invalidateTourProcessingDraft(queryClient, draftId);
        },
        onError: (error) => {
            setAutoAssignBothOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo iniciar la autoasignación') });
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
        mutationFn: (highlightProblems) => exportTourProcessingDraft(draftId, { highlightProblems }),
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
        mutationFn: () => exportTourProcessingDraftFinalCommission(draftId),
        onSuccess: ({ blob, fileName }) => downloadBlob(blob, fileName),
        onError: (error) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo generar la Comisión Final') });
        },
    });

    const errorsQuery = useQuery({
        queryKey: queryKeys.transport.tourProcessingDraftFinalCommissionCheck(draftId),
        queryFn: () => checkTourProcessingDraftFinalCommission(draftId),
        enabled: errorsOpen && Boolean(draftId),
    });

    const rejectIncompleteMutation = useMutation({
        mutationFn: () => rejectTourProcessingDraftIncompleteTours(draftId),
        onSuccess: (rejectedTours) => {
            setRejectIncompleteOpen(false);
            setAlert({ type: 'success', message: rejectedIncompleteMessage(rejectedTours) });
            void invalidateTourProcessingDraft(queryClient, draftId);
        },
        onError: (error) => {
            setRejectIncompleteOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron pasar las giras a No aprobada') });
        },
    });

    const handleObservationReviewed = useCallback((updated) => {
        setRowModal((current) => {
            if (!current.row || current.row.id !== updated?.id) return current;
            return {
                ...current,
                row: {
                    ...current.row,
                    observationReviewed: Boolean(updated.observationReviewed),
                    matchedObservationKeywords: updated.matchedObservationKeywords ?? [],
                },
            };
        });
    }, []);

    const columns = useDraftRowColumns({
        draftId,
        canReviewObservations: draft?.status === 'DRAFT' && canEditTours,
        problemsByRowId,
        onObservationReviewed: handleObservationReviewed,
    });

    const openRowModal = useCallback((mode, row) => setRowModal({ open: true, mode, row, rowId: row?.id ?? null }), []);
    const openRowModalById = useCallback((mode, rowId) => setRowModal({ open: true, mode, row: null, rowId }), []);
    const pendingRowId = rowModal.open && !rowModal.row ? rowModal.rowId : null;

    useEffect(() => {
        if (!wizard.active) setWizardNoticeDismissed(false);
    }, [wizard.active]);

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
    const autoAssigningVehicles = autoAssignVehiclesMutation.isPending;
    const startingWizard = startWizardMutation.isPending;
    const clearingAssignments = clearAssignmentsMutation.isPending;
    const exportingTourControl = tourControlMutation.isPending;
    const exportingOvertimeRequest = overtimeRequestMutation.isPending;
    const exportingFinalCommission = finalCommissionMutation.isPending;
    const periodLabel = draft?.year && draft?.month ? shortPeriodLabel(draft.year, draft.month) : null;
    const exportLabel = draft?.year && draft?.month ? `Descargar '${draftPeriodLabel(draft)}'` : 'Descargar borrador';
    const hasAssignmentReport = assignmentReport?.draftId === draftId;
    const busy = finishMutation.isPending || reopenMutation.isPending || autoAssigningDrivers || autoAssigningVehicles || startingWizard || clearingAssignments || deleteDuplicatesMutation.isPending || splitOversizedMutation.isPending;
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
        if (wizard.active) setWizardOpen(true);
        else setAutoAssignBothOpen(true);
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
        setExportHighlightOpen(true);
    };

    const confirmExport = (highlightProblems) => {
        setExportHighlightOpen(false);
        if (!exportMutation.isPending) exportMutation.mutate(highlightProblems);
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
        if (!exportingFinalCommission) finalCommissionMutation.mutate();
    };

    const handleShowErrors = () => {
        closeOptions();
        setErrorsOpen(true);
    };

    const applyRule = (next) => {
        setRule(next);
        writeStoredRule(next);
        tableOptions.onPaginationChange((current) => (current.pageIndex === 0 ? current : { ...current, pageIndex: 0 }));
    };

    const confirmAutoAssignDrivers = () => {
        if (!autoAssignDriversMutation.isPending) autoAssignDriversMutation.mutate();
    };

    const confirmAutoAssignVehicles = () => {
        if (!autoAssignVehiclesMutation.isPending) autoAssignVehiclesMutation.mutate();
    };

    const confirmAutoAssignBoth = () => {
        if (!startWizardMutation.isPending) startWizardMutation.mutate();
    };

    const confirmClearAssignments = () => {
        if (!clearAssignmentsMutation.isPending) clearAssignmentsMutation.mutate();
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
                                                <MenuItem onClick={handleAutoAssignBoth}>
                                                    <ListItemIcon><GroupsOutlinedIcon fontSize="small" /></ListItemIcon>
                                                    <ListItemText>Autoasignar choferes y vehículos</ListItemText>
                                                </MenuItem>
                                                <MenuItem onClick={handleAutoAssignDrivers}>
                                                    <ListItemIcon><BadgeOutlinedIcon fontSize="small" /></ListItemIcon>
                                                    <ListItemText>Autoasignar choferes</ListItemText>
                                                </MenuItem>
                                                <MenuItem onClick={handleAutoAssignVehicles}>
                                                    <ListItemIcon><DirectionsCarFilledOutlinedIcon fontSize="small" /></ListItemIcon>
                                                    <ListItemText>Autoasignar vehículos</ListItemText>
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
                                        {(canExportTours || canEditTours) && periodLabel && (
                                            <MenuItem onClick={handleShowErrors}>
                                                <ListItemIcon><WarningAmberOutlinedIcon fontSize="small" /></ListItemIcon>
                                                <ListItemText>Visualizar errores</ListItemText>
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

                {(loadingDraft || exporting || exportingTourControl || exportingOvertimeRequest || exportingFinalCommission || autoAssigningDrivers || autoAssigningVehicles || startingWizard || clearingAssignments) && <LinearProgress sx={{ mb: 2 }} />}

                {draftError && (
                    <Alert severity="error" variant="outlined" sx={{ mb: 2 }}>
                        {draftError}
                    </Alert>
                )}

                {draft && (
                    <Stack spacing={1.5} sx={{ mb: 2 }}>
                        {canEditRows && wizard.active && !wizardOpen && !wizardNoticeDismissed && (
                            <Alert
                                severity="info"
                                variant="outlined"
                                action={(
                                    <Stack direction="row" alignItems="center" spacing={0.5}>
                                        <Button
                                            variant="contained"
                                            color="inherit"
                                            size="small"
                                            onClick={() => setWizardOpen(true)}
                                            disabled={busy}
                                            startIcon={<AutoFixHighOutlinedIcon />}
                                            sx={alertActionButtonSx}
                                        >
                                            Continuar autoasignación
                                        </Button>
                                        <IconButton
                                            color="inherit"
                                            size="small"
                                            aria-label="Cerrar advertencia"
                                            onClick={() => setWizardNoticeDismissed(true)}
                                        >
                                            <CloseIcon fontSize="small" />
                                        </IconButton>
                                    </Stack>
                                )}
                            >
                                Hay una autoasignación paso a paso en curso. Continúala para aceptar los pasos pendientes o descártala desde el asistente.
                            </Alert>
                        )}
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
                                        variant="contained"
                                        color="inherit"
                                        size="small"
                                        onClick={() => setDeleteDuplicatesOpen(true)}
                                        disabled={busy}
                                        startIcon={<DeleteOutlineOutlinedIcon />}
                                        sx={alertActionButtonSx}
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
                                            variant="contained"
                                            color="inherit"
                                            size="small"
                                            onClick={() => setSplitOversizedOpen(true)}
                                            disabled={busy}
                                            startIcon={<CallSplitOutlinedIcon />}
                                            sx={alertActionButtonSx}
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
                        <Tab value="control-giras" label="Control de giras" sx={{ textTransform: 'none', fontWeight: 700 }} />
                        <Tab value="horas-extra" label="Solicitud de horas extra" sx={{ textTransform: 'none', fontWeight: 700 }} />
                    </Tabs>
                </Box>

                {viewTab === 'rows' && (
                    <Stack spacing={1.25}>
                        <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap" alignItems="center">
                            <TextField
                                select
                                size="small"
                                label="Filtros al mostrar giras"
                                value={rule}
                                onChange={(event) => applyRule(event.target.value)}
                                slotProps={{ inputLabel: { shrink: true }, select: { displayEmpty: true } }}
                                sx={{ minWidth: 320 }}
                            >
                                <MenuItem value="">Mostrar todas</MenuItem>
                                {TOUR_RULE_FILTERS.map((option) => (
                                    <MenuItem key={option.code} value={option.code}>{option.label}</MenuItem>
                                ))}
                            </TextField>
                        </Stack>
                    <DraftRowsTable
                        columns={columns}
                        rows={rows}
                        loading={loadingRows}
                        fetching={fetchingRows}
                        error={rowsError}
                        totalElements={totalElements}
                        tableOptions={tableOptions}
                        canEdit={canEditRows}
                        hasDuplicates={duplicateRows > 0}
                        onOpenRow={openRowModal}
                    />
                    </Stack>
                )}

                {viewTab === 'horas-extra' && (
                    <TourProcessingDraftDriverScheduleView draftId={draftId} canEdit={canEditRows} onOpenRowById={openRowModalById} />
                )}
                {viewTab === 'control-giras' && draft && (
                    <TourProcessingDraftTourControlView key={`${draftId}-${draft.year}-${draft.month}`} draftId={draftId} year={draft.year} month={draft.month} canEdit={canEditRows} onOpenRowById={openRowModalById} />
                )}
            </Container>

            {wizardOpen && wizard.active && (
                <TourProcessingAutoAssignWizard
                    draftId={draftId}
                    wizard={wizard}
                    subtitle={draft ? draftPeriodLabel(draft) : ''}
                    canEdit={canEditRows}
                    onClose={() => setWizardOpen(false)}
                    onFinished={(lastResult) => {
                        setAssignmentReport({ draftId, result: lastResult?.drivers ?? null, vehicleResult: lastResult?.vehicles ?? null });
                        setAssignmentReportOpen(true);
                        setAlert({ type: 'success', message: 'Autoasignación terminada. Las asignaciones quedaron guardadas en el borrador.' });
                    }}
                    onOpenRow={openRowModal}
                    onObservationReviewed={handleObservationReviewed}
                    onMessage={(type, message) => setAlert({ type, message })}
                />
            )}

            <TourProcessingDraftRowModal
                open={rowModal.open}
                mode={rowModal.mode}
                row={rowModal.row}
                loadingRow={Boolean(pendingRowId)}
                draftId={draftId}
                canDelete={canDeleteRows}
                onClose={closeRowModal}
                onEdit={canEditRows ? () => openRowModal('edit', rowModal.row) : undefined}
                onObservationReviewed={handleObservationReviewed}
                onDeleted={() => setAlert({ type: 'success', message: 'Gira eliminada correctamente' })}
                onSaved={(saved) => setAlert({ type: 'success', message: savedRowMessage(saved) })}
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

            {hasAssignmentReport && (
                <TourProcessingDriverAssignmentReport
                    open={assignmentReportOpen}
                    result={assignmentReport.result}
                    vehicleResult={assignmentReport.vehicleResult}
                    subtitle={draftPeriodLabel(draft)}
                    onClose={() => setAssignmentReportOpen(false)}
                />
            )}

            <TourProcessingFinalCommissionDialog
                open={errorsOpen}
                check={errorsQuery.data}
                subtitle={periodLabel ? `Borrador ${periodLabel}` : ''}
                loading={errorsQuery.isLoading}
                fetching={errorsQuery.isFetching}
                error={errorsQuery.error ? getFriendlyApiErrorMessage(errorsQuery.error, 'No se pudieron cargar los errores') : null}
                rejecting={rejectIncompleteMutation.isPending}
                canRejectIncomplete={canEditRows}
                onClose={() => setErrorsOpen(false)}
                onOpenTour={(rowId) => openRowModalById(canEditRows ? 'edit' : 'view', rowId)}
                onRejectIncomplete={() => setRejectIncompleteOpen(true)}
            />

            <DialogModal
                open={rejectIncompleteOpen}
                type="warning"
                title="Pasar a No aprobada"
                message="Las giras aprobadas que no tienen chofer o vehículo pasarán a estado No aprobada. El cambio se guarda en el borrador."
                confirmLabel={rejectIncompleteMutation.isPending ? 'Guardando...' : 'Sí, pasar a No aprobada'}
                onConfirm={() => { if (!rejectIncompleteMutation.isPending) rejectIncompleteMutation.mutate(); }}
                onClose={rejectIncompleteMutation.isPending ? undefined : () => setRejectIncompleteOpen(false)}
            />

            <DialogModal
                open={exportHighlightOpen}
                type="info"
                title="Resaltar giras con problemas"
                message="¿Desea que las giras que presentan algún problema se resalten en amarillo, con una columna que indique el problema de cada una?"
                confirmLabel="Sí, resaltar"
                extraActions={[{ label: 'No, descargar sin resaltar', onClick: () => confirmExport(false) }]}
                onConfirm={() => confirmExport(true)}
                onClose={() => setExportHighlightOpen(false)}
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

            <TourProcessingAutoAssignmentInfoModal
                open={autoAssignmentInfoOpen}
                onClose={() => setAutoAssignmentInfoOpen(false)}
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
                confirmLabel={startingWizard ? 'Calculando...' : 'Sí, empezar'}
                onConfirm={confirmAutoAssignBoth}
                onClose={startingWizard ? undefined : () => setAutoAssignBothOpen(false)}
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

            <TourProcessingSplitModeDialog
                open={splitOversizedOpen}
                oversizedRows={oversizedRows}
                pending={splitOversizedMutation.isPending}
                onConfirm={confirmSplitOversized}
                onClose={() => setSplitOversizedOpen(false)}
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
