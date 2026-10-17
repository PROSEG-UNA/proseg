import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
    Alert, Box, Button, Checkbox, CircularProgress, FormControlLabel, IconButton, TextField, Typography, useTheme,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import MonthPicker from '../../../../common/components/MonthPicker.jsx';
import { queryKeys } from '../../../../common/query';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { downloadCleaningTemplate, importCleaningFile } from '../../services/cleaning/cleaningService';
import { downloadBlob } from '../../transportUtils.js';
import { draftPeriodLabel } from './cleaningDraftLabels.js';

const MONTH_BLOCKS = {
    CLEANING_DRAFT_MONTH_EXISTS: {
        title: (period) => `Ya existe un borrador de ${period}`,
        message: [
            'Solo puede haber un borrador pendiente por mes. Así se evita que las mismas giras se registren dos veces.',
            'Para importar este archivo, abre el borrador existente y regístralo, o elimínalo si ya no lo necesitas. También puedes elegir otro mes.',
        ].join('\n\n'),
    },
    CLEANING_MONTH_ALREADY_REGISTERED: {
        title: (period) => `${period} ya fue registrado`,
        message: [
            'La depuración de este mes ya se registró. Cada mes se registra una sola vez para que las giras no queden duplicadas.',
            'Elige otro mes para continuar. Puedes consultar la depuración registrada en la lista de borradores.',
        ].join('\n\n'),
    },
};
const ACCEPTED_EXTENSIONS = ['.xlsx', '.xls', '.xlsm', '.csv'];
const MAX_INLINE_ISSUES = 50;
const MIN_BUSINESS_DAYS = 1;
const MAX_BUSINESS_DAYS = 31;

function issueText(issue) {
    return issue.row != null ? `Fila ${issue.row}: ${issue.reason}` : issue.reason;
}

function downloadIssuesReport(issues) {
    const lines = issues.map((issue) => `${issue.row ?? ''};"${(issue.reason ?? '').replace(/"/g, '""')}"`);
    const csv = ['Fila;Motivo', ...lines].join('\n');
    downloadBlob(new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' }), 'reporte-importacion-giras.csv');
}

export default function CleaningImportModal({ open, onClose }) {
    const theme = useTheme();
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const fileInputRef = useRef(null);

    const [file, setFile] = useState(null);
    const [period, setPeriod] = useState(null);
    const [businessDays, setBusinessDays] = useState('');
    const [businessDaysTouched, setBusinessDaysTouched] = useState(false);
    const [keepDrivers, setKeepDrivers] = useState(true);
    const [keepVehicles, setKeepVehicles] = useState(true);
    const [isDragOver, setIsDragOver] = useState(false);
    const [notice, setNotice] = useState(null);
    const [issues, setIssues] = useState([]);
    const [monthBlock, setMonthBlock] = useState(null);

    const resetState = () => {
        setFile(null);
        setPeriod(null);
        setBusinessDays('');
        setBusinessDaysTouched(false);
        setMonthBlock(null);
        setKeepDrivers(true);
        setKeepVehicles(true);
        setIsDragOver(false);
        setNotice(null);
        setIssues([]);
    };

    const importMutation = useMutation({
        mutationFn: importCleaningFile,
        onSuccess: async (summary) => {
            if (summary?.draftId) {
                await queryClient.invalidateQueries({ queryKey: queryKeys.transport.cleaningDrafts() });
                resetState();
                onClose();
                navigate(`/transporte/depuracion/borradores/${summary.draftId}`);
                return;
            }
            const summaryIssues = summary?.errors ?? [];
            setIssues(summaryIssues);
            setNotice({
                type: 'error',
                message: `El archivo tiene ${summaryIssues.length} error${summaryIssues.length === 1 ? '' : 'es'}. No se guardó ninguna fila: corrígelo e impórtalo de nuevo.`,
            });
        },
        onError: (error, variables) => {
            setIssues([]);
            const errorCode = error?.response?.data?.errors?.[0];
            if (MONTH_BLOCKS[errorCode]) {
                setNotice(null);
                setMonthBlock({ errorCode, period: draftPeriodLabel(variables) });
                return;
            }
            setNotice({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo procesar el archivo') });
        },
    });

    const templateMutation = useMutation({
        mutationFn: downloadCleaningTemplate,
        onSuccess: (blob) => downloadBlob(blob, 'plantilla-giras.xlsx'),
        onError: (error) => {
            setNotice({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo descargar la plantilla') });
        },
    });

    const importing = importMutation.isPending;
    const downloadingTemplate = templateMutation.isPending;

    const businessDaysValue = Number(businessDays);
    const businessDaysValid = businessDays !== ''
        && businessDaysValue >= MIN_BUSINESS_DAYS
        && businessDaysValue <= MAX_BUSINESS_DAYS;
    const businessDaysInvalid = (businessDaysTouched || businessDays !== '') && !businessDaysValid;

    const clearResult = () => {
        setNotice(null);
        setIssues([]);
    };

    const handleClose = () => {
        if (importing) return;
        resetState();
        onClose();
    };

    const handleFileSelect = (files) => {
        const selected = files?.[0];
        if (!selected) return;
        const isAccepted = ACCEPTED_EXTENSIONS.some((extension) => selected.name.toLowerCase().endsWith(extension));
        if (!isAccepted) {
            setIssues([]);
            setNotice({ type: 'warning', message: 'Solo se aceptan archivos .xlsx, .xls, .xlsm o .csv.' });
            return;
        }
        clearResult();
        setFile(selected);
    };

    const handleRemoveFile = () => {
        clearResult();
        setFile(null);
    };

    const handleImport = () => {
        if (!file || !period || !businessDaysValid || importing) return;
        clearResult();
        importMutation.mutate({
            file,
            year: period.year(),
            month: period.month() + 1,
            businessDays: businessDaysValue,
            keepDrivers,
            keepVehicles,
        });
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={handleClose}
                maxWidth="sm"
                icon={FileDownloadOutlinedIcon}
                title="Nueva depuración"
                subtitle="Sube el previo de comisión para crear un borrador"
                loading={importing}
                contentSx={{ p: 3 }}
                secondaryButton={{ label: 'Cancelar', onClick: handleClose, disabled: importing }}
                primaryButton={{
                    label: importing ? 'Importando…' : 'Importar',
                    onClick: handleImport,
                    disabled: importing || !file || !period || !businessDaysValid,
                    startIcon: <FileDownloadOutlinedIcon />,
                }}
            >
                <Box>
                    <Box sx={{
                        mb: 2, display: 'flex', alignItems: 'center', gap: 1.5,
                        border: '1px solid', borderColor: 'divider', borderRadius: '12px',
                        px: 2, py: 1.5,
                        bgcolor: `color-mix(in srgb, ${accentColor} 6%, transparent)`,
                    }}>
                        <Box sx={{ flex: 1 }}>
                            <Typography sx={{ fontSize: 13, fontWeight: 600 }}>
                                Plantilla de giras
                            </Typography>
                            <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>
                                Punto de partida para asegurar el formato correcto.
                            </Typography>
                        </Box>
                        <Button
                            variant="outlined"
                            size="small"
                            startIcon={downloadingTemplate
                                ? <CircularProgress size={14} sx={{ color: accentColor }} />
                                : <DownloadOutlinedIcon />}
                            onClick={() => templateMutation.mutate()}
                            disabled={importing || downloadingTemplate}
                            sx={{
                                textTransform: 'none', fontWeight: 600, fontSize: 12.5,
                                color: accentColor, borderColor: accentColor, whiteSpace: 'nowrap',
                                '&:hover': {
                                    borderColor: accentColor,
                                    bgcolor: `color-mix(in srgb, ${accentColor} 8%, transparent)`,
                                },
                            }}
                        >
                            {downloadingTemplate ? 'Descargando' : 'Descargar'}
                        </Button>
                    </Box>

                    <Box sx={{ mb: 2 }}>
                        <MonthPicker
                            label="Mes de la depuración"
                            ariaLabel="Elegir el mes de la depuración"
                            value={period}
                            onChange={setPeriod}
                            disabled={importing}
                            required
                        />
                    </Box>

                    <Box sx={{ mb: 2 }}>
                        <TextField
                            label="Días hábiles del mes"
                            value={businessDays}
                            onChange={(event) => setBusinessDays(event.target.value.replace(/\D/g, '').slice(0, 2))}
                            onBlur={() => setBusinessDaysTouched(true)}
                            required
                            fullWidth
                            size="small"
                            disabled={importing}
                            error={businessDaysInvalid}
                            slotProps={{ htmlInput: { inputMode: 'numeric', maxLength: 2 } }}
                        />
                    </Box>

                    <Box
                        onDragOver={(event) => { event.preventDefault(); setIsDragOver(true); }}
                        onDragLeave={() => setIsDragOver(false)}
                        onDrop={(event) => { event.preventDefault(); setIsDragOver(false); handleFileSelect(event.dataTransfer.files); }}
                        onClick={() => { if (!importing) fileInputRef.current?.click(); }}
                        sx={{
                            border: '2px dashed',
                            borderColor: isDragOver ? accentColor : 'divider',
                            borderRadius: '12px',
                            p: 4,
                            display: 'flex', flexDirection: 'column',
                            alignItems: 'center', justifyContent: 'center',
                            gap: 1, cursor: importing ? 'default' : 'pointer',
                            bgcolor: isDragOver ? `color-mix(in srgb, ${accentColor} 5%, transparent)` : 'transparent',
                            transition: 'all 0.2s',
                            '&:hover': {
                                borderColor: accentColor,
                                bgcolor: `color-mix(in srgb, ${accentColor} 5%, transparent)`,
                            },
                        }}
                    >
                        <CloudUploadIcon sx={{ fontSize: 32, color: isDragOver ? accentColor : 'text.secondary' }} />
                        <Typography sx={{ fontSize: 13.5, color: 'text.secondary', textAlign: 'center' }}>
                            Arrastra el archivo aquí o{' '}
                            <Typography component="span" sx={{ color: accentColor, fontWeight: 600 }}>
                                selecciónalo
                            </Typography>
                        </Typography>
                        <Typography sx={{ fontSize: 11.5, color: 'text.disabled' }}>
                            XLSX, XLS, XLSM, CSV
                        </Typography>
                    </Box>
                    <input
                        ref={fileInputRef}
                        type="file"
                        accept={ACCEPTED_EXTENSIONS.join(',')}
                        style={{ display: 'none' }}
                        onChange={(event) => { handleFileSelect(event.target.files); event.target.value = ''; }}
                    />

                    {file && (
                        <Box sx={{
                            mt: 2, display: 'flex', alignItems: 'center', gap: 1,
                            border: '1px solid', borderColor: 'divider', borderRadius: '10px',
                            px: 1.5, py: 1,
                        }}>
                            <InsertDriveFileOutlinedIcon sx={{ fontSize: 18, color: accentColor }} />
                            <Typography sx={{ fontSize: 13, flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                {file.name}
                            </Typography>
                            <IconButton size="small" onClick={handleRemoveFile} disabled={importing}>
                                <CloseIcon sx={{ fontSize: 14 }} />
                            </IconButton>
                        </Box>
                    )}

                    <Box sx={{ mt: 1.5, display: 'flex', flexWrap: 'wrap', columnGap: 3 }}>
                        <FormControlLabel
                            control={(
                                <Checkbox
                                    size="small"
                                    checked={keepDrivers}
                                    onChange={(event) => setKeepDrivers(event.target.checked)}
                                    disabled={importing}
                                />
                            )}
                            label={<Typography sx={{ fontSize: 13.5 }}>Mantener choferes</Typography>}
                        />
                        <FormControlLabel
                            control={(
                                <Checkbox
                                    size="small"
                                    checked={keepVehicles}
                                    onChange={(event) => setKeepVehicles(event.target.checked)}
                                    disabled={importing}
                                />
                            )}
                            label={<Typography sx={{ fontSize: 13.5 }}>Mantener vehículos</Typography>}
                        />
                    </Box>

                    {notice && (
                        <Alert severity={notice.type} variant="outlined" onClose={clearResult} sx={{ mt: 2 }}>
                            {notice.message}
                        </Alert>
                    )}

                    {issues.length > 0 && (
                        <Box sx={{ mt: 2 }}>
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1, mb: 1 }}>
                                <Typography sx={{
                                    fontSize: 11, fontWeight: 600, letterSpacing: '0.06em',
                                    textTransform: 'uppercase', color: 'text.secondary',
                                }}>
                                    Errores encontrados
                                </Typography>
                                <Button
                                    size="small"
                                    startIcon={<DownloadOutlinedIcon />}
                                    onClick={() => downloadIssuesReport(issues)}
                                    sx={{ textTransform: 'none', fontWeight: 600, fontSize: 12.5 }}
                                >
                                    Descargar reporte
                                </Button>
                            </Box>
                            {issues.length > MAX_INLINE_ISSUES ? (
                                <Typography sx={{ fontSize: 13, color: 'error.main' }}>
                                    Hay {issues.length} errores. Descarga el reporte para ver el detalle por fila.
                                </Typography>
                            ) : (
                                <Box sx={{
                                    maxHeight: 180, overflowY: 'auto',
                                    border: '1px solid', borderColor: 'divider', borderRadius: '10px',
                                    px: 1.5, py: 1,
                                }}>
                                    {issues.map((issue, index) => (
                                        <Typography key={`${issue.row ?? 'general'}-${index}`} sx={{ fontSize: 12.5, color: 'error.main', py: 0.25 }}>
                                            {issueText(issue)}
                                        </Typography>
                                    ))}
                                </Box>
                            )}
                        </Box>
                    )}

                    <Typography sx={{ mt: 2, fontSize: 12, color: 'text.secondary' }}>
                        Se leen todos los bloques diarios del archivo. Si alguna fila tiene errores no se guarda nada y verás el detalle por fila.
                        Si todo es válido se crea un borrador: ninguna gira se registra hasta que lo confirmes.
                    </Typography>
                </Box>
            </GeneralModal>
            <DialogModal
                type="warning"
                open={!!monthBlock}
                title={monthBlock ? MONTH_BLOCKS[monthBlock.errorCode].title(monthBlock.period) : undefined}
                message={monthBlock ? MONTH_BLOCKS[monthBlock.errorCode].message : undefined}
                confirmLabel="Ver borradores"
                cancelLabel="Elegir otro mes"
                onConfirm={handleClose}
                onClose={() => setMonthBlock(null)}
            />
        </>
    );
}
