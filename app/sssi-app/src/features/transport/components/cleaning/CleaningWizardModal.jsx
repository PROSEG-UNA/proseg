import { useMemo, useRef, useState } from 'react';
import {
    Alert,
    Box,
    Button,
    Checkbox,
    Chip,
    FormControlLabel,
    Paper,
    Tab,
    Tabs,
    Stack,
    TextField,
    Typography,
    useTheme,
} from '@mui/material';
import AutoFixHighOutlinedIcon from '@mui/icons-material/AutoFixHighOutlined';
import ArrowForwardOutlinedIcon from '@mui/icons-material/ArrowForwardOutlined';
import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import ErrorOutlinedIcon from '@mui/icons-material/ErrorOutlined';
import CloseIcon from '@mui/icons-material/Close';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import EditIcon from '@mui/icons-material/Edit';
import RestartAltOutlinedIcon from '@mui/icons-material/RestartAltOutlined';
import SaveAltOutlinedIcon from '@mui/icons-material/SaveAltOutlined';
import TableBase from '../../../../common/components/TablaBase.jsx';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import {
    finalizeCleaning,
    previewCleaning,
    registerCleaningRows,
} from '../../services/cleaning/cleaningService';

const CLEANING_TYPE = 'Depuración de giras desde archivo';

const EDITABLE_FIELDS = [
    { key: 'number', label: 'Numero' },
    { key: 'vehicle', label: 'Vehiculo' },
    { key: 'vehicleType', label: 'Tipo de vehiculo' },
    { key: 'driver', label: 'Chofer' },
    { key: 'passengers', label: 'Pasajeros' },
    { key: 'executingUnit', label: 'Unidad ejecutora' },
    { key: 'responsible', label: 'Responsable' },
    { key: 'destination', label: 'Destino' },
    { key: 'durationDays', label: 'Duracion (dias)' },
    { key: 'priority', label: 'Prioridad' },
    { key: 'modality', label: 'Modalidad' },
    { key: 'departureTime', label: 'Hora salida' },
    { key: 'returnTime', label: 'Hora regreso' },
    { key: 'departureDate', label: 'Fecha salida' },
    { key: 'returnDate', label: 'Fecha regreso' },
    { key: 'observations', label: 'Observaciones' },
];

const TEMPLATE_HEADERS = [
    'Numero',
    'Vehiculo',
    'Tipo de vehiculo',
    'Chofer',
    'Cantidad de pasajeros',
    'Unidad Ejecutora',
    'Responsable',
    'Destino(s)',
    'Duracion (dias)',
    'Prioridad',
    'Modalidad',
    'Hora de salida',
    'Hora de regreso',
    'Fecha de salida',
    'Fecha de regreso',
    'Observaciones',
];

const WIZARD_STEPS = [
    { label: 'Archivo y parámetros', icon: CloudUploadOutlinedIcon },
    { label: 'Vista previa', icon: DescriptionOutlinedIcon },
    { label: 'Confirmación', icon: SaveAltOutlinedIcon },
    { label: 'Resultado', icon: CheckCircleOutlinedIcon },
];

function toTableRows(rows = []) {
    return rows.map((row) => ({
        id: String(row.rowIndex),
        rowIndex: row.rowIndex,
        values: { ...row.values },
    }));
}

function toBackendRows(rows = []) {
    return rows.map((row) => ({
        rowIndex: Number(row.rowIndex),
        values: { ...row.values },
    }));
}

function buildTabLabel(Icon, label) {
    return (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
            <Icon sx={{ fontSize: 16 }} />
            <span>{label}</span>
        </Box>
    );
}


function MetricCard({ label, value }) {
    return (
        <Paper variant="outlined" sx={{ p: 1.5, borderRadius: 2, bgcolor: 'background.paper' }}>
            <Typography sx={{ fontSize: 18, fontWeight: 800, lineHeight: 1.1, color: 'text.primary' }}>
                {value}
            </Typography>
            <Typography sx={{ fontSize: 12.5, color: 'text.secondary', mt: 0.5 }}>
                {label}
            </Typography>
        </Paper>
    );
}

export default function CleaningWizardModal({ open, onClose, onImported }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const [activeStep, setActiveStep] = useState(0);
    const [file, setFile] = useState(null);
    const [isDragOver, setIsDragOver] = useState(false);
    const [loadingPreview, setLoadingPreview] = useState(false);
    const [loadingFinalize, setLoadingFinalize] = useState(false);
    const [loadingRegister, setLoadingRegister] = useState(false);
    const [previewRows, setPreviewRows] = useState([]);
    const [cleanedRows, setCleanedRows] = useState([]);
    const [suggestedRemovals, setSuggestedRemovals] = useState([]);
    const [duplicateGroups, setDuplicateGroups] = useState([]);
    const [rowSelection, setRowSelection] = useState({});
    const [keepDuplicates, setKeepDuplicates] = useState(false);
    const [replaceExisting, setReplaceExisting] = useState(false);
    const [editingRow, setEditingRow] = useState(null);
    const [notice, setNotice] = useState(null);
    const [registerSummary, setRegisterSummary] = useState(null);
    const [resultState, setResultState] = useState(null);
    const fileInputRef = useRef(null);

    const loadingAny = loadingPreview || loadingFinalize || loadingRegister;
    const selectedRowIndexes = useMemo(
        () => Object.keys(rowSelection)
            .filter((key) => rowSelection[key])
            .map((key) => Number(key)),
        [rowSelection],
    );

    const selectFile = (files) => {
        const nextFile = files?.[0] ?? null;
        if (!nextFile) return;

        resetWorkflow();
        setFile(nextFile);
    };

    const columns = useMemo(() => [
        { accessorKey: 'rowIndex', header: 'Fila', size: 80, grow: false, enableColumnFilter: false },
        { accessorFn: (row) => row.values.date ?? '—', id: 'date', header: 'Fecha', size: 130, grow: false },
        { accessorFn: (row) => row.values.number ?? '—', id: 'number', header: 'Numero', size: 90, grow: false },
        { accessorFn: (row) => row.values.vehicle ?? '—', id: 'vehicle', header: 'Vehiculo', size: 120, grow: false },
        { accessorFn: (row) => row.values.driver ?? '—', id: 'driver', header: 'Chofer', size: 180, grow: true },
        { accessorFn: (row) => row.values.passengers ?? '—', id: 'passengers', header: 'Pasajeros', size: 100, grow: false },
        { accessorFn: (row) => row.values.priority ?? '—', id: 'priority', header: 'Prioridad', size: 100, grow: false },
        { accessorFn: (row) => row.values.executingUnit ?? '—', id: 'executingUnit', header: 'Unidad ejecutora', size: 180, grow: true },
        { accessorFn: (row) => row.values.responsible ?? '—', id: 'responsible', header: 'Responsable', size: 180, grow: true },
        { accessorFn: (row) => row.values.destination ?? '—', id: 'destination', header: 'Destino', size: 220, grow: true },
        { accessorFn: (row) => row.values.departureTime ?? '—', id: 'departureTime', header: 'Salida', size: 100, grow: false },
        { accessorFn: (row) => row.values.returnTime ?? '—', id: 'returnTime', header: 'Regreso', size: 100, grow: false },
    ], []);

    const clearNotice = () => setNotice(null);

    const openFilePicker = () => {
        fileInputRef.current?.click();
    };

    const handleCloseWizard = () => {
        setEditingRow(null);
        onClose();
    };

    const resetWorkflow = () => {
        setActiveStep(0);
        setFile(null);
        setPreviewRows([]);
        setCleanedRows([]);
        setSuggestedRemovals([]);
        setDuplicateGroups([]);
        setRowSelection({});
        setKeepDuplicates(false);
        setReplaceExisting(false);
        setEditingRow(null);
        setRegisterSummary(null);
        setResultState(null);
        clearNotice();
    };

    const clearOutcome = () => {
        setCleanedRows([]);
        setRegisterSummary(null);
        setResultState(null);
    };

    const invalidateOutcome = () => {
        clearOutcome();
    };

    const downloadTemplateCsv = () => {
        const csvRow = (values) => values.map((value) => `"${String(value).replace(/"/g, '""')}"`).join(',');
        const lines = [
            'Sabado 2 de Mayo de 2026 - ejemplo con duplicados sugeridos',
            TEMPLATE_HEADERS.join(','),
            csvRow(['1001', 'UNA-101', 'Microbus', 'Chofer 1', '12', 'Servicios Generales', 'Ana Soto', 'Campus Omar Dengo', '1', '1', 'TLG', '07:30', '16:30', '2026-05-02', '2026-05-02', 'Viaje base']),
            csvRow(['1001', 'UNA-101', 'Microbus', 'Chofer 1', '12', 'Servicios Generales', 'Ana Soto', 'Campus Omar Dengo', '1', '1', 'TLG', '07:30', '16:30', '2026-05-02', '2026-05-02', 'Fila repetida para conservar duplicados']),
            csvRow(['1002', 'UNA-102', 'Sedan', 'Chofer 2', '4', 'Servicios Generales', 'Luis Mora', 'Heredia', '1', '6', 'TPG', '08:00', '13:30', '2026-05-02', '2026-05-02', 'Viaje distinto en la misma fecha']),
            '',
            'Domingo 3 de Mayo de 2026 - ejemplo para reemplazar en el rango',
            TEMPLATE_HEADERS.join(','),
            csvRow(['1003', 'UNA-201', 'Microbus', 'Chofer 3', '10', 'Unidad Ejecutora 1', 'Carlos Perez', 'Alajuela', '2', '3', 'TLG', '06:45', '14:15', '2026-05-03', '2026-05-03', 'Puede reemplazar registros previos']),
            csvRow(['1004', 'UNA-202', 'Pickup', 'Chofer 4', '2', 'Unidad Ejecutora 1', 'Maria Lopez', 'San Jose', '1', '4', 'TPG', '09:00', '11:30', '2026-05-03', '2026-05-03', 'Otro ejemplo del mismo rango']),
        ];
        const csv = `\uFEFF${lines.join('\n')}`;
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'plantilla_depuracion_giras.csv';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
    };

    const previewFile = async () => {
        if (!file) {
            setNotice({ type: 'warning', message: 'Selecciona un archivo .xlsx o .csv para continuar.' });
            return;
        }
        setLoadingPreview(true);
        try {
            const result = await previewCleaning(file);
            const rows = toTableRows(result?.rows ?? []);
            setPreviewRows(rows);
            setCleanedRows([]);
            setRegisterSummary(null);
            setResultState(null);
            setDuplicateGroups(result?.duplicateGroups ?? []);
            const suggested = result?.suggestedRemovals ?? [];
            setSuggestedRemovals(suggested);
            const initialSelection = {};
            suggested.forEach((rowIndex) => {
                initialSelection[String(rowIndex)] = true;
            });
            setRowSelection(initialSelection);
            clearNotice();
            setActiveStep(1);
        } catch (error) {
            setNotice({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo procesar el archivo') });
        } finally {
            setLoadingPreview(false);
        }
    };

    const handleRowSelectionChange = (updater) => {
        setRowSelection((currentValue) => (
            typeof updater === 'function' ? updater(currentValue) : updater
        ));
        invalidateOutcome();
    };

    const handleKeepDuplicatesChange = (event) => {
        setKeepDuplicates(event.target.checked);
        invalidateOutcome();
    };

    const handleReplaceExistingChange = (event) => {
        setReplaceExisting(event.target.checked);
        invalidateOutcome();
    };

    const handleSaveEdition = () => {
        if (!editingRow) return;
        setPreviewRows((currentValue) => currentValue.map((row) => (row.id === editingRow.id ? editingRow : row)));
        setEditingRow(null);
        invalidateOutcome();
    };

    const applyCleaning = async () => {
        if (!previewRows.length) {
            setNotice({ type: 'warning', message: 'Primero analiza un archivo con registros válidos.' });
            return;
        }

        setLoadingFinalize(true);
        setLoadingRegister(true);
        setResultState(null);
        clearNotice();

        try {
            const finalized = await finalizeCleaning({
                rows: toBackendRows(previewRows),
                selectedForDeletion: selectedRowIndexes,
                manualRemovals: [],
                suggestedRemovals,
                keepDuplicates,
            });
            const finalizedRows = toTableRows(finalized?.cleanedRows ?? []);
            setCleanedRows(finalizedRows);

            const registered = await registerCleaningRows({
                rows: toBackendRows(finalizedRows),
                replaceExistingInRange: replaceExisting,
            });
            setRegisterSummary(registered);
            onImported?.();
            setResultState({
                status: 'success',
                title: 'Depuración completada',
                message: `Se registraron ${registered?.importedRows ?? 0} giras depuradas.`,
            });
        } catch (error) {
            setResultState({
                status: 'error',
                title: 'No se pudo completar la depuración',
                message: getFriendlyApiErrorMessage(error, 'No se pudo registrar la depuración'),
            });
        } finally {
            setLoadingFinalize(false);
            setLoadingRegister(false);
            setActiveStep(3);
        }
    };

    const startNewCleaning = () => {
        resetWorkflow();
    };

    const goNext = () => {
        if (activeStep === 0) {
            previewFile();
            return;
        }

        if (activeStep === 1) {
            if (!previewRows.length) {
                setNotice({ type: 'warning', message: 'No hay filas para revisar en la vista previa.' });
                return;
            }
            clearNotice();
            setActiveStep(2);
            return;
        }

        if (activeStep === 2) {
            applyCleaning();
        }
    };

    const goBack = () => {
        clearNotice();
        if (activeStep > 0 && activeStep < 3) {
            setActiveStep((currentValue) => currentValue - 1);
            return;
        }
        if (activeStep === 3 && resultState?.status === 'error') {
            setResultState(null);
            setActiveStep(2);
        }
    };

    const handleResultPrimary = () => {
        if (resultState?.status === 'error') {
            setResultState(null);
            setActiveStep(2);
            return;
        }
        handleCloseWizard();
    };

    const handleResultSecondary = () => {
        if (resultState?.status === 'error') {
            handleCloseWizard();
            return;
        }
        startNewCleaning();
    };

    const footerConfig = (() => {
        if (activeStep === 0) {
            return {
                secondaryButton: {
                    label: 'Cancelar',
                    onClick: handleCloseWizard,
                },
                primaryButton: {
                    label: 'Analizar archivo',
                    onClick: goNext,
                    startIcon: <CloudUploadOutlinedIcon />,
                    disabled: loadingPreview || !file,
                },
            };
        }

        if (activeStep === 1) {
            return {
                secondaryButton: {
                    label: 'Atrás',
                    onClick: goBack,
                },
                primaryButton: {
                    label: 'Continuar a confirmación',
                    onClick: goNext,
                    startIcon: <ArrowForwardOutlinedIcon />,
                    disabled: !previewRows.length,
                },
            };
        }

        if (activeStep === 2) {
            return {
                secondaryButton: {
                    label: 'Atrás',
                    onClick: goBack,
                },
                primaryButton: {
                    label: loadingFinalize || loadingRegister ? 'Procesando...' : 'Confirmar y registrar',
                    onClick: goNext,
                    startIcon: <SaveAltOutlinedIcon />,
                    disabled: loadingFinalize || loadingRegister,
                },
            };
        }

        return resultState?.status === 'error'
            ? {
                secondaryButton: {
                    label: 'Cerrar',
                    onClick: handleCloseWizard,
                },
                primaryButton: {
                    label: 'Volver a confirmar',
                    onClick: handleResultPrimary,
                    startIcon: <RestartAltOutlinedIcon />,
                },
            }
            : {
                secondaryButton: {
                    label: 'Nueva depuración',
                    onClick: handleResultSecondary,
                    startIcon: <RestartAltOutlinedIcon />,
                },
                primaryButton: {
                    label: 'Cerrar',
                    onClick: handleResultPrimary,
                    startIcon: <CloseIcon />,
                },
            };
    })();

    return (
        <>
            <GeneralModal
                open={open}
                onClose={handleCloseWizard}
                maxWidth="lg"
                fillHeight
                title="Depuración de giras"
                icon={AutoFixHighOutlinedIcon}
                loading={loadingAny}
                showCloseButton={!loadingAny}
                primaryButton={footerConfig.primaryButton}
                secondaryButton={footerConfig.secondaryButton}
                contentSx={{ bgcolor: 'background.paper' }}
            >
                <Box sx={{ height: '100%', minHeight: 0, display: 'flex', flexDirection: 'column' }}>
                    <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 2 }}>
                        <Typography sx={{ fontWeight: 800, fontSize: { xs: 16, sm: 17 }, color: 'text.primary', lineHeight: 1.2 }}>
                            Gestión de Transporte
                        </Typography>
                        <Typography sx={{ color: 'text.secondary', fontSize: 13.25, mt: 0.35 }}>
                            Sube el archivo, revisa la vista previa y confirma el registro en un flujo estándar.
                        </Typography>
                    </Box>

                    <Box sx={{ px: { xs: 2.5, sm: 3 }, borderTop: '1px solid', borderBottom: '1px solid', borderColor: 'divider' }}>
                        <Tabs
                            value={activeStep}
                            onChange={(_, value) => {
                                if (value <= activeStep) setActiveStep(value);
                            }}
                            variant="scrollable"
                            scrollButtons="auto"
                            allowScrollButtonsMobile
                            TabIndicatorProps={{ sx: { backgroundColor: accentColor } }}
                            sx={{
                                minHeight: 44,
                                '& .MuiTab-root': {
                                    minHeight: 44,
                                    textTransform: 'none',
                                    fontWeight: 700,
                                    fontSize: 13.25,
                                    color: 'text.secondary',
                                },
                                '& .MuiTab-root.Mui-selected': {
                                    color: accentColor,
                                },
                            }}
                        >
                            {WIZARD_STEPS.map((step, index) => (
                                <Tab
                                    key={step.label}
                                    value={index}
                                    disabled={index > activeStep}
                                    label={buildTabLabel(step.icon, step.label)}
                                />
                            ))}
                        </Tabs>
                    </Box>

                    {notice && (
                        <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.25 }}>
                            <Alert severity={notice.type} variant="outlined" onClose={clearNotice}>
                                {notice.message}
                            </Alert>
                        </Box>
                    )}

                    <Box sx={{ flex: 1, minHeight: 0, overflowY: 'auto' }}>
                        <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3 }}>
                            {activeStep === 0 && (
                                <Box
                                    sx={{
                                        display: 'grid',
                                        gap: 2,
                                        gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1.15fr) minmax(300px, 0.85fr)' },
                                        alignItems: 'start',
                                    }}
                                >
                                    <Paper variant="outlined" sx={{ p: 2, borderRadius: '8px', bgcolor: 'background.paper' }}>
                                        <Stack spacing={2}>
                                            <Box
                                                component="label"
                                                onDragOver={(event) => { event.preventDefault(); setIsDragOver(true); }}
                                                onDragLeave={() => setIsDragOver(false)}
                                                onDrop={(event) => { event.preventDefault(); setIsDragOver(false); selectFile(event.dataTransfer.files); }}
                                                onClick={openFilePicker}
                                                sx={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    gap: 1.5,
                                                    border: '1px dashed',
                                                    borderColor: file ? accentColor : isDragOver ? accentColor : 'divider',
                                                    borderRadius: '10px',
                                                    px: 1.75,
                                                    py: 1.5,
                                                    minHeight: 86,
                                                    cursor: 'pointer',
                                                    bgcolor: isDragOver ? 'action.hover' : 'background.paper',
                                                    transition: 'border-color 0.2s ease, background-color 0.2s ease',
                                                    '&:hover': {
                                                        borderColor: accentColor,
                                                        bgcolor: 'action.hover',
                                                    },
                                                }}
                                            >
                                                {file ? (
                                                    <>
                                                        <Box sx={{
                                                            width: 40, height: 40, borderRadius: '10px',
                                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                                            bgcolor: 'action.selected',
                                                            color: accentColor,
                                                            flexShrink: 0,
                                                        }}>
                                                            <DescriptionOutlinedIcon sx={{ fontSize: 20 }} />
                                                        </Box>
                                                        <Box sx={{ minWidth: 0, flex: 1 }}>
                                                            <Typography sx={{ fontWeight: 700, fontSize: 13.5 }} noWrap>
                                                                {file.name}
                                                            </Typography>
                                                            <Typography sx={{ color: 'text.secondary', fontSize: 12.25 }}>
                                                                {Math.max(1, Math.round(file.size / 1024))} KB - toca para cambiar
                                                            </Typography>
                                                        </Box>
                                                    </>
                                                ) : (
                                                    <>
                                                        <Box sx={{
                                                            width: 40, height: 40, borderRadius: '10px',
                                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                                            bgcolor: 'action.selected',
                                                            color: accentColor,
                                                            flexShrink: 0,
                                                        }}>
                                                            <CloudUploadOutlinedIcon sx={{ fontSize: 20 }} />
                                                        </Box>
                                                        <Box sx={{ minWidth: 0, flex: 1 }}>
                                                            <Typography sx={{ fontWeight: 700, fontSize: 13.5 }}>
                                                                Arrastra tu archivo aquí
                                                            </Typography>
                                                            <Typography sx={{ color: 'text.secondary', fontSize: 12.25 }}>
                                                                O haz clic para abrir el explorador
                                                            </Typography>
                                                        </Box>
                                                    </>
                                                )}
                                                <input
                                                    ref={fileInputRef}
                                                    hidden
                                                    type="file"
                                                    accept=".xlsx,.csv"
                                                    onChange={(event) => { selectFile(event.target.files); event.target.value = ''; }}
                                                />
                                            </Box>

                                            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                                                <Button
                                                    variant="text"
                                                    startIcon={<DownloadOutlinedIcon />}
                                                    onClick={downloadTemplateCsv}
                                                    sx={{ textTransform: 'none', fontWeight: 700, borderRadius: '8px', fontSize: 12.75 }}
                                                >
                                                    Descargar plantilla
                                                </Button>
                                                {file && (
                                                    <Button
                                                        variant="text"
                                                        color="inherit"
                                                        startIcon={<RestartAltOutlinedIcon />}
                                                        onClick={resetWorkflow}
                                                        disabled={loadingPreview}
                                                        sx={{ textTransform: 'none', fontWeight: 700, borderRadius: '8px', fontSize: 12.75 }}
                                                    >
                                                        Limpiar
                                                    </Button>
                                                )}
                                            </Box>
                                        </Stack>
                                    </Paper>

                                    <Paper variant="outlined" sx={{ p: 2, borderRadius: '8px', bgcolor: 'background.paper' }}>
                                        <Stack spacing={2}>
                                            <Box>
                                                <Typography sx={{ fontWeight: 700, fontSize: 15 }}>
                                                    Configuración rápida
                                                </Typography>
                                                <Typography sx={{ color: 'text.secondary', fontSize: 12.5, mt: 0.25 }}>
                                                    Define cómo tratar la carga antes de analizarla.
                                                </Typography>
                                            </Box>

                                            <Stack spacing={1}>
                                                {[
                                                    {
                                                        checked: keepDuplicates,
                                                        onChange: handleKeepDuplicatesChange,
                                                        title: 'Conservar duplicados sugeridos',
                                                        description: 'Mantén los repetidos para revisarlos manualmente.',
                                                    },
                                                    {
                                                        checked: replaceExisting,
                                                        onChange: handleReplaceExistingChange,
                                                        title: 'Reemplazar giras en el rango importado',
                                                        description: 'Sobrescribe registros previos en el mismo rango.',
                                                    },
                                                ].map((option) => (
                                                    <Box
                                                        key={option.title}
                                                        sx={{
                                                            p: 1.5,
                                                            borderRadius: '8px',
                                                            border: '1px solid',
                                                            borderColor: option.checked ? accentColor : 'divider',
                                                            bgcolor: 'background.paper',
                                                        }}
                                                    >
                                                        <FormControlLabel
                                                            control={<Checkbox checked={option.checked} onChange={option.onChange} size="small" />}
                                                            label={<Typography sx={{ fontSize: 13.25, fontWeight: 600 }}>{option.title}</Typography>}
                                                        />
                                                        <Typography sx={{ color: 'text.secondary', fontSize: 12, pl: 3.8, mt: -0.25 }}>
                                                            {option.description}
                                                        </Typography>
                                                    </Box>
                                                ))}
                                            </Stack>

                                            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                                                <Chip label=".xlsx" size="small" variant="outlined" />
                                                <Chip label=".csv" size="small" variant="outlined" />
                                                <Chip label="Vista previa" size="small" variant="outlined" />
                                            </Box>
                                        </Stack>
                                    </Paper>
                                </Box>
                            )}

                            {activeStep === 1 && (
                                <Stack spacing={1.5} sx={{ minHeight: 0 }}>
                                    <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
                                        <Chip label={`${previewRows.length} filas`} variant="outlined" size="small" sx={{ fontWeight: 700 }} />
                                        <Chip label={`${duplicateGroups.length} grupos duplicados`} variant="outlined" size="small" sx={{ fontWeight: 700 }} />
                                        <Chip label={`${suggestedRemovals.length} sugeridas eliminar`} variant="outlined" size="small" sx={{ fontWeight: 700 }} />
                                    </Stack>

                                    {duplicateGroups.length > 0 && (
                                        <Alert severity="warning" variant="outlined">
                                            {duplicateGroups.length} grupos de duplicados en esta carga.
                                        </Alert>
                                    )}

                                    <TableBase
                                        columns={columns}
                                        data={previewRows}
                                        loading={loadingPreview}
                                        enableRowSelection
                                        rowSelection={rowSelection}
                                        onRowSelectionChange={handleRowSelectionChange}
                                        enableRowActions
                                        renderRowActions={({ row }) => (
                                            <RowActionsMenu
                                                tooltip="Acciones"
                                                actions={[{
                                                    key: 'edit',
                                                    label: 'Editar fila',
                                                    icon: <EditIcon fontSize="small" />,
                                                    onClick: () => setEditingRow({ ...row.original, values: { ...row.original.values } }),
                                                }]}
                                            />
                                        )}
                                        tableOptions={{
                                            initialState: { pagination: { pageIndex: 0, pageSize: 10 } },
                                        }}
                                    />

                                    <Typography sx={{ color: 'text.secondary', fontSize: 12.5 }}>
                                        {selectedRowIndexes.length} fila{selectedRowIndexes.length !== 1 ? 's' : ''} marcada{selectedRowIndexes.length !== 1 ? 's' : ''} para eliminar
                                    </Typography>
                                </Stack>
                            )}

                            {activeStep === 2 && (
                                <Stack spacing={2}>
                                    {cleanedRows.length > 0 ? (
                                        <Alert severity="success" variant="outlined">
                                            Vista depurada lista. Puedes volver a registrar con la configuración actual.
                                        </Alert>
                                    ) : (
                                        <Alert severity="info" variant="outlined">
                                            Se aplicará la depuración sobre {previewRows.length} filas.
                                        </Alert>
                                    )}

                                    <Box sx={{ display: 'grid', gap: 1.25, gridTemplateColumns: { xs: 'repeat(2, minmax(0, 1fr))', md: 'repeat(4, minmax(0, 1fr))' } }}>
                                        <MetricCard label="Filas analizadas" value={previewRows.length} />
                                        <MetricCard label="Para eliminar" value={selectedRowIndexes.length} />
                                        <MetricCard label="Conservar duplicados" value={keepDuplicates ? 'Sí' : 'No'} />
                                        <MetricCard label="Reemplazar existentes" value={replaceExisting ? 'Sí' : 'No'} />
                                    </Box>
                                </Stack>
                            )}

                            {activeStep === 3 && (
                                <Stack spacing={2}>
                                    <Stack direction="row" spacing={1.5} alignItems="center">
                                        {resultState?.status === 'success' ? (
                                            <CheckCircleOutlinedIcon sx={{ color: 'success.main', fontSize: 28 }} />
                                        ) : (
                                            <ErrorOutlinedIcon sx={{ color: 'error.main', fontSize: 28 }} />
                                        )}
                                        <Typography sx={{ fontWeight: 700, fontSize: 15 }}>
                                            {resultState?.title ?? 'Operación finalizada'}
                                        </Typography>
                                    </Stack>

                                    <Alert severity={resultState?.status === 'success' ? 'success' : 'error'} variant="outlined">
                                        {resultState?.message ?? 'No hay información disponible.'}
                                    </Alert>

                                    {resultState?.status === 'success' && registerSummary && (
                                        <Stack spacing={1.5}>
                                            <Box sx={{ display: 'grid', gap: 1.25, gridTemplateColumns: { xs: 'repeat(2, minmax(0, 1fr))', md: 'repeat(4, minmax(0, 1fr))' } }}>
                                                <MetricCard label="Registradas" value={registerSummary.importedRows ?? 0} />
                                                <MetricCard label="Solicitadas" value={registerSummary.requestedRows ?? 0} />
                                                <MetricCard label="Reemplazadas" value={registerSummary.replacedRows ?? 0} />
                                                <MetricCard label="Giras depuradas" value={cleanedRows.length} />
                                            </Box>
                                            <Typography sx={{ color: 'text.secondary', fontSize: 13 }}>
                                                Choferes: {registerSummary.createdDrivers ?? 0} creados, {registerSummary.updatedDrivers ?? 0} actualizados.
                                                {' '}Vehículos: {registerSummary.createdVehicles ?? 0} creados, {registerSummary.updatedVehicles ?? 0} actualizados.
                                            </Typography>
                                        </Stack>
                                    )}
                                </Stack>
                            )}
                        </Box>
                    </Box>
                </Box>
            </GeneralModal>

            <GeneralModal
                open={!!editingRow}
                onClose={() => setEditingRow(null)}
                title="Editar fila"
                subtitle={editingRow ? `Fila ${editingRow.rowIndex}` : ''}
                maxWidth="md"
                primaryButton={{ label: 'Guardar cambios', onClick: handleSaveEdition }}
                secondaryButton={{ label: 'Cancelar', onClick: () => setEditingRow(null) }}
            >
                <Box sx={{ p: 3, display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' } }}>
                    {EDITABLE_FIELDS.map((field) => (
                        <TextField
                            key={field.key}
                            label={field.label}
                            value={editingRow?.values?.[field.key] ?? ''}
                            onChange={(event) => setEditingRow((currentValue) => ({
                                ...currentValue,
                                values: { ...currentValue.values, [field.key]: event.target.value },
                            }))}
                            fullWidth
                            sx={field.key === 'observations' ? { gridColumn: { md: '1 / -1' } } : undefined}
                        />
                    ))}
                </Box>
            </GeneralModal>
        </>
    );
}
