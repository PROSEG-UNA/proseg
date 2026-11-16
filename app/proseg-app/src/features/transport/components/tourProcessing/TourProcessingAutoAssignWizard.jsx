import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
    Alert,
    Box,
    Button,
    Chip,
    Select,
    Stack,
    Typography,
    useTheme,
} from '@mui/material';
import AutoFixHighOutlinedIcon from '@mui/icons-material/AutoFixHighOutlined';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import { queryKeys } from '../../../../common/query';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import {
    discardAutoAssignWizard,
    finishAutoAssignWizard,
    recalculateAutoAssignWizard,
    saveAutoAssignWizard,
    setAutoAssignWizardStep,
} from '../../services/tourProcessing/tourProcessingService';
import {
    invalidateTourProcessingDraft,
    useTourProcessingDraftRowProblems,
    useTourProcessingDraftRows,
} from '../../hooks/useTourProcessingDraftsData.js';
import { useServerTableState } from '../../hooks/useServerTableState.js';
import DraftRowsTable from './DraftRowsTable.jsx';
import TourProcessingDriverAssignmentReport from './TourProcessingDriverAssignmentReport.jsx';
import { DRAFT_ROWS_FILTERABLE_COLUMNS, useDraftRowColumns } from './useDraftRowColumns.jsx';

const INITIAL_SORTING = [{ id: 'rowNumber', desc: false }];
const INITIAL_COLUMN_VISIBILITY = { rowNumber: false };
const TABLE_STORAGE_KEY = 'tour-processing-wizard-rows-table-state-v1';
const EXPANSION_STORAGE_KEY = 'tour-processing-wizard-rows-expand-all';

function StepNavigator({ steps, activeIndex, disabled, onSelect }) {
    const activeStep = steps[activeIndex];
    if (!activeStep) return null;

    return (
        <Box sx={{ px: { xs: 2, sm: 3 }, pt: 2, flexShrink: 0 }}>
            <Stack
                direction="row"
                alignItems="center"
                spacing={1.5}
                sx={(theme) => ({
                    p: { xs: 1.5, sm: 2 },
                    borderLeft: '4px solid',
                    borderColor: 'primary.main',
                    borderRadius: 1,
                    bgcolor: `color-mix(in srgb, ${theme.vars.palette.primary.main} 5%, ${theme.vars.palette.background.paper})`,
                })}
            >
                <Box sx={{ width: 36, height: 36, borderRadius: '50%', bgcolor: 'primary.main', color: 'primary.contrastText', display: 'grid', placeItems: 'center', flexShrink: 0, fontWeight: 700 }}>
                    {activeIndex + 1}
                </Box>
                <Box sx={{ minWidth: 0 }}>
                    <Typography sx={{ color: 'text.secondary', fontSize: 12, letterSpacing: '0.06em', textTransform: 'uppercase' }}>
                        Paso {activeIndex + 1} de {steps.length} - {activeStep.tours} giras
                    </Typography>
                    <Typography component="h3" sx={{ fontSize: { xs: 16, sm: 18 }, fontWeight: 750, lineHeight: 1.3 }}>
                        {activeStep.label}
                    </Typography>
                </Box>
            </Stack>
            <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ xs: 'stretch', sm: 'center' }} justifyContent="space-between" spacing={1.5} sx={{ mt: 1.5 }}>
                <Select
                    native
                    size="small"
                    value=""
                    onChange={(event) => onSelect(event.target.value)}
                    disabled={disabled}
                    inputProps={{ 'aria-label': 'Ir a otro paso' }}
                    sx={{ width: { xs: '100%', sm: 320 }, maxWidth: '100%', borderRadius: 1 }}
                >
                    <option value="">Ir a otro paso...</option>
                    {steps.map((step, index) => (
                        <option key={step.code} value={step.code}>
                            {index + 1}. {step.label} - {step.tours} giras
                        </option>
                    ))}
                </Select>
                <Stack direction="row" spacing={1} justifyContent={{ xs: 'flex-end', sm: 'initial' }}>
                    <Button size="small" variant="outlined" color="inherit" disabled={disabled || activeIndex === 0} onClick={() => onSelect(steps[activeIndex - 1].code)} sx={{ textTransform: 'none' }}>
                        Anterior
                    </Button>
                    <Button size="small" variant="outlined" color="inherit" disabled={disabled || activeIndex === steps.length - 1} onClick={() => onSelect(steps[activeIndex + 1].code)} sx={{ textTransform: 'none' }}>
                        Siguiente
                    </Button>
                </Stack>
            </Stack>
        </Box>
    );
}

function StepSummary({ step, dirty, onShowResult, hasResult }) {
    return (
        <Stack spacing={1.25} sx={{ px: { xs: 2, sm: 3 }, py: 1.5, flexShrink: 0, minWidth: 0 }}>
            <Stack direction="row" spacing={1} alignItems="center" useFlexGap flexWrap="wrap">
                <Chip size="small" label={`${step.complete} completas`} sx={{ bgcolor: 'action.hover', color: 'text.secondary' }} />
                {step.withoutDriver > 0 && <Chip size="small" color="warning" label={`${step.withoutDriver} sin chofer`} />}
                {step.withoutVehicle > 0 && <Chip size="small" color="warning" label={`${step.withoutVehicle} sin vehículo`} />}
                {hasResult && (
                    <Button size="small" onClick={onShowResult} sx={{ height: 24, minHeight: 24, py: 0, textTransform: 'none', fontWeight: 600 }}>
                        Ver resultado de la autoasignación
                    </Button>
                )}
            </Stack>
            {dirty && (
                <Alert severity="warning" variant="outlined">
                    Hiciste cambios manuales. Al aceptar este paso se recalculará la autoasignación de lo que se asignó automáticamente, respetando tus cambios.
                </Alert>
            )}
        </Stack>
    );
}

export default function TourProcessingAutoAssignWizard({
    draftId,
    wizard,
    subtitle,
    canEdit,
    onClose,
    onFinished,
    onOpenRow,
    onObservationReviewed,
    onMessage,
}) {
    const queryClient = useQueryClient();
    const theme = useTheme();
    const steps = wizard.steps ?? [];
    const [activeCode, setActiveCode] = useState(wizard.currentStep);
    const [cancelOpen, setCancelOpen] = useState(false);
    const [finishOpen, setFinishOpen] = useState(false);
    const [resultOpen, setResultOpen] = useState(false);
    const [expanded, setExpanded] = useState(false);

    const activeIndex = Math.max(0, steps.findIndex((step) => step.code === activeCode));
    const activeStep = steps[activeIndex] ?? null;
    const isLast = activeIndex >= steps.length - 1;

    const { problemsByRowId } = useTourProcessingDraftRowProblems(draftId);
    const columns = useDraftRowColumns({
        draftId,
        canReviewObservations: canEdit,
        problemsByRowId,
        onObservationReviewed,
        compact: true,
    });
    const { requestParams, tableOptions } = useServerTableState({
        initialSorting: INITIAL_SORTING,
        filterableColumns: DRAFT_ROWS_FILTERABLE_COLUMNS,
        initialPageSize: 20,
        initialColumnVisibility: INITIAL_COLUMN_VISIBILITY,
        storageKey: TABLE_STORAGE_KEY,
    });
    const { rows, loading, fetching, error, totalElements } = useTourProcessingDraftRows(
        draftId,
        { ...requestParams, rule: activeStep?.filterRule ?? '' },
        { enabled: Boolean(activeStep) },
    );

    const { onPaginationChange } = tableOptions;
    useEffect(() => {
        onPaginationChange((current) => ({ ...current, pageIndex: 0 }));
    }, [activeStep?.code, onPaginationChange]);

    const refresh = () => invalidateTourProcessingDraft(queryClient, draftId);
    const reportError = (fallback) => (failure) => onMessage('error', getFriendlyApiErrorMessage(failure, fallback));
    const storeWizard = (updated) => queryClient.setQueryData(queryKeys.transport.tourProcessingDraftAutoAssignWizard(draftId), updated);

    const recalculateMutation = useMutation({
        mutationFn: () => recalculateAutoAssignWizard(draftId),
        onSuccess: (updated) => {
            storeWizard(updated);
            void refresh();
        },
        onError: reportError('No se pudo recalcular la autoasignación'),
    });

    const advanceMutation = useMutation({
        mutationFn: async () => {
            const next = steps[activeIndex + 1];
            const current = wizard.dirty ? await recalculateAutoAssignWizard(draftId) : wizard;
            const target = current.steps.find((step) => step.code === next?.code) ?? current.steps[Math.min(activeIndex + 1, current.steps.length - 1)];
            const saved = await setAutoAssignWizardStep(draftId, target?.code ?? null);
            return { saved, target };
        },
        onSuccess: ({ saved, target }) => {
            storeWizard(saved);
            setActiveCode(target?.code ?? null);
            void refresh();
        },
        onError: reportError('No se pudo pasar al siguiente paso'),
    });

    const saveMutation = useMutation({
        mutationFn: () => saveAutoAssignWizard(draftId, activeStep?.code ?? null),
        onSuccess: (updated) => {
            storeWizard(updated);
            setCancelOpen(false);
            onClose();
            onMessage('success', 'Se guardó el progreso de la autoasignación. Puedes retomarla cuando quieras.');
        },
        onError: reportError('No se pudo guardar el progreso'),
    });

    const discardMutation = useMutation({
        mutationFn: () => discardAutoAssignWizard(draftId),
        onSuccess: () => {
            setCancelOpen(false);
            storeWizard({ active: false, currentStep: null, dirty: false, steps: [], lastResult: null });
            void refresh();
            onClose();
            onMessage('success', 'Se descartaron los cambios de la autoasignación y el borrador quedó como estaba al empezar.');
        },
        onError: reportError('No se pudieron descartar los cambios'),
    });

    const finishMutation = useMutation({
        mutationFn: async () => {
            const lastResult = wizard.lastResult;
            await finishAutoAssignWizard(draftId);
            return lastResult;
        },
        onSuccess: (lastResult) => {
            setFinishOpen(false);
            storeWizard({ active: false, currentStep: null, dirty: false, steps: [], lastResult: null });
            void refresh();
            onClose();
            onFinished(lastResult);
        },
        onError: reportError('No se pudo terminar la autoasignación'),
    });

    const busy = recalculateMutation.isPending || advanceMutation.isPending || saveMutation.isPending
        || discardMutation.isPending || finishMutation.isPending;

    const handleAccept = () => {
        if (isLast || !activeStep) setFinishOpen(true);
        else advanceMutation.mutate();
    };

    const handleStepClick = (code) => {
        setActiveCode(code);
        if (!busy) setAutoAssignWizardStep(draftId, code).then(storeWizard).catch(() => undefined);
    };

    return (
        <>
            <GeneralModal
                open
                onClose={() => setCancelOpen(true)}
                icon={AutoFixHighOutlinedIcon}
                title="Autoasignación paso a paso"
                subtitle={subtitle}
                maxWidth="xl"
                fillHeight
                expanded={expanded}
                onExpandedChange={setExpanded}
                loading={busy}
                contentSx={{ display: 'flex', flexDirection: 'column', overflowY: 'auto', overflowX: 'hidden' }}
                footerLeft={(
                    <Button
                        size="small"
                        variant="outlined"
                        color="inherit"
                        onClick={() => setCancelOpen(true)}
                        disabled={busy}
                        sx={{ textTransform: 'none', fontWeight: 600, fontSize: 12.5, borderRadius: '8px' }}
                    >
                        Cancelar
                    </Button>
                )}
                secondaryButton={{
                    label: recalculateMutation.isPending ? 'Recalculando...' : 'Recalcular',
                    onClick: () => recalculateMutation.mutate(),
                    disabled: busy,
                }}
                primaryButton={{
                    label: isLast || !activeStep ? 'Terminar autoasignación' : 'Aceptar y continuar',
                    variant: 'contained',
                    onClick: handleAccept,
                    disabled: busy,
                    loading: advanceMutation.isPending,
                    sx: {
                        background: `linear-gradient(135deg, ${theme.vars.palette.tones.rose.headerBg} 0%, ${theme.vars.palette.tones.rose.headerBgEnd} 100%)`,
                        borderColor: 'transparent',
                        color: '#fff',
                        fontWeight: 700,
                        boxShadow: theme.vars.palette.tones.rose.buttonShadow,
                        '&:hover': {
                            background: `linear-gradient(135deg, ${theme.vars.palette.tones.rose.hoverBg}, ${theme.vars.palette.tones.rose.hoverBg})`,
                            boxShadow: theme.vars.palette.tones.rose.buttonShadowHover,
                            borderColor: 'transparent',
                            color: '#fff',
                        },
                        '&.Mui-disabled': { color: 'rgba(255, 255, 255, 0.8)' },
                    },
                }}
            >
                <StepNavigator steps={steps} activeIndex={activeIndex} disabled={busy} onSelect={handleStepClick} />
                {activeStep ? (
                    <>
                        <StepSummary
                            step={activeStep}
                            dirty={wizard.dirty}
                            hasResult={Boolean(wizard.lastResult)}
                            onShowResult={() => setResultOpen(true)}
                        />
                        <Box sx={{ flex: '1 1 auto', minHeight: 360, display: 'flex', flexDirection: 'column' }}>
                            <DraftRowsTable
                                columns={columns}
                                rows={rows}
                                loading={loading}
                                fetching={fetching}
                                error={error}
                                totalElements={totalElements}
                                tableOptions={tableOptions}
                                canEdit={canEdit}
                                hasDuplicates={false}
                                onOpenRow={onOpenRow}
                                fitContainer
                                virtualizeRows={tableOptions.state.pagination.pageSize > 20}
                                expansionStorageKey={EXPANSION_STORAGE_KEY}
                            />
                        </Box>
                    </>
                ) : (
                    <Alert severity="info" variant="outlined" sx={{ m: 3 }}>
                        Ya no quedan giras pendientes en este asistente. Puedes terminar la autoasignación.
                    </Alert>
                )}
            </GeneralModal>

            <DialogModal
                open={cancelOpen}
                type="warning"
                title="Salir de la autoasignación"
                message="Puedes guardar el progreso para retomarlo después o descartar todos los cambios hechos desde que abriste el asistente, incluidas tus ediciones manuales."
                confirmLabel={saveMutation.isPending ? 'Guardando...' : 'Guardar y salir'}
                extraActions={[{ label: discardMutation.isPending ? 'Descartando...' : 'Descartar cambios', onClick: () => discardMutation.mutate() }]}
                onConfirm={() => { if (!busy) saveMutation.mutate(); }}
                onClose={busy ? undefined : () => setCancelOpen(false)}
            />

            <DialogModal
                open={finishOpen}
                type="info"
                title="Terminar autoasignación"
                message="Las asignaciones quedarán como definitivas en el borrador y ya no se recalcularán al volver a autoasignar."
                confirmLabel={finishMutation.isPending ? 'Terminando...' : 'Terminar'}
                onConfirm={() => { if (!busy) finishMutation.mutate(); }}
                onClose={busy ? undefined : () => setFinishOpen(false)}
            />

            {wizard.lastResult && (
                <TourProcessingDriverAssignmentReport
                    open={resultOpen}
                    result={wizard.lastResult.drivers}
                    vehicleResult={wizard.lastResult.vehicles}
                    subtitle={subtitle}
                    onClose={() => setResultOpen(false)}
                />
            )}
        </>
    );
}
