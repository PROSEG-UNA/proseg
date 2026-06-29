import { useState, useRef, useEffect } from 'react';
import {
    Box, Typography, Chip, IconButton, Button, Checkbox, useTheme,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import FileDownloadOutlinedIcon from '@mui/icons-material/FileDownloadOutlined';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { previewAssetsExcel, confirmAssetsImport, downloadAssetsTemplate } from '../../services/assetImportService.js';

const ALLOWED_EXTENSIONS = ['.xlsx', '.xls'];
const MAX_INLINE_ERRORS = 50;

export default function ImportAssetsModal({ open, onClose, onImported }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const [file, setFile]           = useState(null);
    const [importing, setImporting] = useState(false);
    const [preview, setPreview]     = useState(null);
    const [decisions, setDecisions] = useState({});
    const [result, setResult]       = useState(null);
    const [alert, setAlert]         = useState(null);
    const [isDragOver, setIsDragOver] = useState(false);
    const fileInputRef = useRef(null);
    const templatePromiseRef = useRef(null);

    const resetState = () => {
        setFile(null);
        setImporting(false);
        setPreview(null);
        setDecisions({});
        setResult(null);
        setAlert(null);
        setIsDragOver(false);
        templatePromiseRef.current = null;
    };

    const prefetchTemplateOnOpen = () => {
        if (!open) return;
        const promise = downloadAssetsTemplate();
        promise.catch(() => {});
        templatePromiseRef.current = promise;
    };

    useEffect(() => {
        prefetchTemplateOnOpen();
    }, [open]);

    const handleClose = () => {
        if (importing) return;
        resetState();
        onClose();
    };

    const handleFileSelect = (files) => {
        const selected = files?.[0];
        if (!selected) return;
        const isAllowed = ALLOWED_EXTENSIONS.some(ext => selected.name.toLowerCase().endsWith(ext));
        if (!isAllowed) {
            setAlert({ type: 'warning', message: 'Solo se aceptan archivos .xlsx o .xls' });
            return;
        }
        setFile(selected);
    };

    const handlePreview = async () => {
        if (!file) return;
        setImporting(true);
        try {
            const summary = await previewAssetsExcel(file);
            if ((summary.errors?.length ?? 0) > 0) {
                setResult({
                    totalRows: summary.totalRows,
                    created: 0,
                    cancelled: false,
                    errors: summary.errors,
                    warnings: [],
                });
            } else if ((summary.pendingCreations?.length ?? 0) === 0) {
                const final = await confirmAssetsImport(summary.rows, []);
                setResult(final);
                if (final?.created > 0) onImported?.();
            } else {
                const initial = {};
                summary.pendingCreations.forEach(pc => { initial[pc.key] = true; });
                setDecisions(initial);
                setPreview(summary);
            }
        } catch (e) {
            const status = e.response?.status;
            const message = (status && status < 500 && e.response?.data?.message)
                || 'No fue posible procesar el archivo';
            setAlert({ type: 'error', message });
        } finally {
            setImporting(false);
        }
    };

    const handleConfirm = async () => {
        if (!preview) return;
        setImporting(true);
        try {
            const approvedKeys = preview.pendingCreations
                .filter(pc => decisions[pc.key])
                .map(pc => pc.key);
            const final = await confirmAssetsImport(preview.rows, approvedKeys);
            setResult(final);
            if (final?.created > 0) onImported?.();
        } catch (e) {
            const status = e.response?.status;
            const message = (status && status < 500 && e.response?.data?.message)
                || 'No fue posible procesar el archivo';
            setAlert({ type: 'error', message });
        } finally {
            setImporting(false);
        }
    };

    const toggleDecision = (key) => {
        setDecisions(prev => ({ ...prev, [key]: !prev[key] }));
    };

    const handleDownloadTemplate = async () => {
        try {
            if (!templatePromiseRef.current) {
                templatePromiseRef.current = downloadAssetsTemplate();
            }
            const blob = await templatePromiseRef.current;
            const objectUrl = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = objectUrl;
            link.download = 'plantilla-activos.xlsx';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(objectUrl);
        } catch {
            templatePromiseRef.current = null;
            setAlert({ type: 'error', message: 'No fue posible descargar la plantilla' });
        }
    };

    const handleDownloadReport = () => {
        const lines = (result?.errors ?? []).map(e => {
            const row = e.row != null ? e.row : '';
            const reason = (e.reason ?? '').replace(/"/g, '""');
            return `${row};"${reason}"`;
        });
        const csv = ['Fila;Motivo', ...lines].join('\n');
        const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
        const objectUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = objectUrl;
        link.download = 'reporte-importacion-activos.csv';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(objectUrl);
    };

    const phase = result ? 'result' : (preview ? 'confirm' : 'upload');

    const subtitle = {
        upload: 'Sube un archivo Excel con los activos a registrar',
        confirm: 'Confirma los elementos nuevos que se crearán',
        result: 'Resumen de la importación',
    }[phase];

    const primaryButton = {
        upload: {
            label: importing ? 'Procesando…' : 'Importar',
            onClick: handlePreview,
            disabled: importing || !file,
            startIcon: <FileDownloadOutlinedIcon />,
        },
        confirm: {
            label: importing ? 'Importando…' : 'Confirmar e importar',
            onClick: handleConfirm,
            disabled: importing,
        },
        result: {
            label: 'Cerrar',
            onClick: handleClose,
            disabled: false,
        },
    }[phase];

    const secondaryButton = phase === 'result'
        ? null
        : { label: 'Cancelar', onClick: handleClose, disabled: importing };

    const summaryChip = (label, value, color) => (
        <Chip
            label={`${label}: ${value}`}
            size="small"
            color={color}
            sx={{ fontWeight: 600, fontSize: 12 }}
        />
    );

    const messageList = (title, messages, color) => (
        messages?.length > 0 && (
            <Box sx={{ mt: 2 }}>
                <Typography sx={{
                    fontSize: 11, fontWeight: 600, letterSpacing: '0.06em',
                    textTransform: 'uppercase', color: 'text.secondary', mb: 1,
                }}>
                    {title}
                </Typography>
                <Box sx={{
                    maxHeight: 180, overflowY: 'auto',
                    border: '1px solid', borderColor: 'divider', borderRadius: '10px',
                    px: 1.5, py: 1,
                }}>
                    {messages.map((msg, index) => (
                        <Typography key={index} sx={{ fontSize: 12.5, color, py: 0.25 }}>
                            {msg.row != null ? `Fila ${msg.row}: ${msg.reason}` : msg.reason}
                        </Typography>
                    ))}
                </Box>
            </Box>
        )
    );

    const renderUpload = () => (
        <Box>
            <Box sx={{
                mb: 2, display: 'flex', alignItems: 'center', gap: 1.5,
                border: '1px solid', borderColor: 'divider', borderRadius: '12px',
                px: 2, py: 1.5,
                bgcolor: `color-mix(in srgb, ${accentColor} 6%, transparent)`,
            }}>
                <Box sx={{ flex: 1 }}>
                    <Typography sx={{ fontSize: 13, fontWeight: 600 }}>
                        Plantilla de activos
                    </Typography>
                    <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>
                        Punto de partida para asegurar el formato correcto.
                    </Typography>
                </Box>
                <Button
                    variant="outlined"
                    size="small"
                    startIcon={<DownloadOutlinedIcon />}
                    onClick={handleDownloadTemplate}
                    disabled={importing}
                    sx={{
                        textTransform: 'none', fontWeight: 600, fontSize: 12.5,
                        color: accentColor, borderColor: accentColor, whiteSpace: 'nowrap',
                        '&:hover': {
                            borderColor: accentColor,
                            bgcolor: `color-mix(in srgb, ${accentColor} 8%, transparent)`,
                        },
                    }}
                >
                    Descargar
                </Button>
            </Box>
            <Box
                onDragOver={e => { e.preventDefault(); setIsDragOver(true); }}
                onDragLeave={() => setIsDragOver(false)}
                onDrop={e => { e.preventDefault(); setIsDragOver(false); handleFileSelect(e.dataTransfer.files); }}
                onClick={() => fileInputRef.current?.click()}
                sx={{
                    border: '2px dashed',
                    borderColor: isDragOver ? accentColor : 'divider',
                    borderRadius: '12px',
                    p: 4,
                    display: 'flex', flexDirection: 'column',
                    alignItems: 'center', justifyContent: 'center',
                    gap: 1, cursor: 'pointer',
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
                    XLSX, XLS
                </Typography>
            </Box>
            <input
                ref={fileInputRef}
                type="file"
                accept=".xlsx,.xls"
                style={{ display: 'none' }}
                onChange={e => { handleFileSelect(e.target.files); e.target.value = ''; }}
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
                    <IconButton size="small" onClick={() => setFile(null)} disabled={importing}>
                        <CloseIcon sx={{ fontSize: 14 }} />
                    </IconButton>
                </Box>
            )}
        </Box>
    );

    const renderMessageWithCode = (message) => message.split(/'([^']*)'/).map((part, i) => (
        i % 2 === 1
            ? (
                <Box
                    key={i}
                    component="span"
                    sx={{
                        fontFamily: '"Roboto Mono", monospace',
                        fontSize: '0.85em',
                        bgcolor: 'action.hover',
                        px: '4px',
                        py: '1px',
                        borderRadius: '4px',
                        color: 'text.primary',
                    }}
                >
                    {part}
                </Box>
            )
            : part
    ));

    const renderConfirm = () => {
        const anyRejected = preview.pendingCreations.some(pc => !decisions[pc.key]);
        return (
            <Box>
                <Typography sx={{ fontSize: 13.5, color: 'text.secondary' }}>
                    Para importar estos activos se crearán los siguientes elementos nuevos. Revisa y
                    confirma cuáles aceptas.
                </Typography>
                <Box sx={{
                    mt: 2, maxHeight: 280, overflowY: 'auto',
                    border: '1px solid', borderColor: 'divider', borderRadius: '10px',
                }}>
                    {preview.pendingCreations.map((pc) => (
                        <Box
                            key={pc.key}
                            sx={{
                                display: 'flex', alignItems: 'center', gap: 1,
                                px: 1, py: 0.75,
                                borderBottom: '1px solid', borderColor: 'divider',
                                '&:last-of-type': { borderBottom: 'none' },
                            }}
                        >
                            <Checkbox
                                size="small"
                                checked={!!decisions[pc.key]}
                                onChange={() => toggleDecision(pc.key)}
                                disabled={importing}
                            />
                            {pc.label && (
                                <Chip
                                    label={pc.label}
                                    size="small"
                                    sx={{ fontWeight: 600, fontSize: 11 }}
                                />
                            )}
                            <Box sx={{ flex: 1, minWidth: 0 }}>
                                <Typography sx={{ fontSize: 13, color: 'text.primary' }}>
                                    {renderMessageWithCode(pc.message)}
                                </Typography>
                            </Box>
                        </Box>
                    ))}
                </Box>
                {anyRejected && (
                    <Typography sx={{ mt: 1.5, fontSize: 12.5, color: 'warning.main' }}>
                        Rechazaste al menos un elemento: si confirmas, no se importará ningún activo.
                    </Typography>
                )}
            </Box>
        );
    };

    const renderResult = () => (
        <Box>
            {result.cancelled && (
                <Typography sx={{ mb: 2, fontSize: 13.5, color: 'warning.main' }}>
                    La importación se canceló porque se rechazó la creación de uno o más elementos.
                    No se insertó ningún activo.
                </Typography>
            )}
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {summaryChip('Total', result.totalRows ?? 0, 'default')}
                {summaryChip('Creados', result.created ?? 0, 'success')}
                {summaryChip('Errores', result.errors?.length ?? 0, 'error')}
                {summaryChip('Avisos', result.warnings?.length ?? 0, 'warning')}
            </Box>
            {(result.errors?.length ?? 0) > MAX_INLINE_ERRORS ? (
                <Box sx={{ mt: 2 }}>
                    <Typography sx={{ fontSize: 13, color: 'error.main' }}>
                        Se encontraron {result.errors.length} errores. No se importó ningún activo.
                        Descarga el reporte para ver el detalle.
                    </Typography>
                    <Button
                        size="small"
                        startIcon={<DownloadOutlinedIcon />}
                        onClick={handleDownloadReport}
                        sx={{ mt: 1, textTransform: 'none', fontWeight: 600, fontSize: 12.5 }}
                    >
                        Descargar reporte
                    </Button>
                </Box>
            ) : (
                messageList('Errores', result.errors, 'error.main')
            )}
            {messageList('Avisos', result.warnings, 'warning.main')}
            {!result.cancelled && (result.errors?.length ?? 0) === 0 && (result.warnings?.length ?? 0) === 0 && (
                <Typography sx={{ mt: 2, fontSize: 13.5, color: 'text.secondary' }}>
                    Todos los activos se importaron correctamente.
                </Typography>
            )}
        </Box>
    );

    return (
        <>
            <GeneralModal
                open={open}
                onClose={handleClose}
                maxWidth="sm"
                icon={FileDownloadOutlinedIcon}
                title="Importar Activos"
                subtitle={subtitle}
                loading={importing}
                contentSx={{ p: 3 }}
                secondaryButton={secondaryButton}
                primaryButton={primaryButton}
            >
                {phase === 'upload' && renderUpload()}
                {phase === 'confirm' && renderConfirm()}
                {phase === 'result' && renderResult()}
            </GeneralModal>

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}
