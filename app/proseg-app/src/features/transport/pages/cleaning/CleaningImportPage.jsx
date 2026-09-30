import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
    Alert,
    Box,
    Button,
    Chip,
    Container,
    LinearProgress,
    Paper,
    Stack,
    Typography,
    useTheme,
} from '@mui/material';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import RestartAltOutlinedIcon from '@mui/icons-material/RestartAltOutlined';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import PrimaryButton from '../../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import { queryKeys } from '../../../../common/query';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { downloadCleaningTemplate, importCleaningFile } from '../../services/cleaning/cleaningService';

const ACCEPTED_EXTENSIONS = ['.xlsx', '.xls', '.xlsm', '.csv'];
const MAX_INLINE_ISSUES = 50;

function issueText(issue) {
    return issue.row != null ? `Fila ${issue.row}: ${issue.reason}` : issue.reason;
}

function downloadBlob(blob, fileName) {
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(objectUrl);
}

function downloadIssuesReport(issues) {
    const lines = issues.map((issue) => `${issue.row ?? ''};"${(issue.reason ?? '').replace(/"/g, '""')}"`);
    const csv = ['Fila;Motivo', ...lines].join('\n');
    downloadBlob(new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' }), 'reporte-importacion-giras.csv');
}

export default function CleaningImportPage() {
    const theme = useTheme();
    const navigate = useNavigate();
    const location = useLocation();
    const queryClient = useQueryClient();
    const { hasAnyPermission } = usePermissions();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const [file, setFile] = useState(null);
    const [isDragOver, setIsDragOver] = useState(false);
    const [notice, setNotice] = useState(null);
    const [issues, setIssues] = useState([]);

    const canExecuteCleaning = hasAnyPermission([
        PERMISSIONS.TRANSPORT.ASSIGNMENT.GENERATE,
        PERMISSIONS.TRANSPORT.ASSIGNMENT.UPDATE,
    ]);

    const importMutation = useMutation({
        mutationFn: importCleaningFile,
        onSuccess: async (summary) => {
            if (summary?.draftId) {
                await queryClient.invalidateQueries({ queryKey: queryKeys.transport.cleaningDrafts() });
                navigate(`/transporte/depuracion/borradores/${summary.draftId}`, { replace: true });
                return;
            }
            const summaryIssues = summary?.errors ?? [];
            setIssues(summaryIssues);
            setNotice({
                type: 'error',
                message: `El archivo tiene ${summaryIssues.length} error${summaryIssues.length === 1 ? '' : 'es'}. No se guardó ninguna fila: corrígelo e impórtalo de nuevo.`,
            });
        },
        onError: (error) => {
            setIssues([]);
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

    if (!canExecuteCleaning) return <AccessDeniedState />;

    const importing = importMutation.isPending;

    const clearResult = () => {
        setNotice(null);
        setIssues([]);
    };

    const selectFile = (files) => {
        const nextFile = files?.[0];
        if (!nextFile) return;
        const isAccepted = ACCEPTED_EXTENSIONS.some((extension) => nextFile.name.toLowerCase().endsWith(extension));
        if (!isAccepted) {
            setIssues([]);
            setNotice({ type: 'warning', message: 'Solo se aceptan archivos .xlsx, .xls, .xlsm o .csv.' });
            return;
        }
        clearResult();
        setFile(nextFile);
    };

    const resetFile = () => {
        clearResult();
        setFile(null);
    };

    const goBackToHub = () => {
        if (location.key !== 'default') {
            navigate(-1);
            return;
        }
        navigate('/transporte/depuracion');
    };

    const startImport = () => {
        if (!file || importing) return;
        clearResult();
        importMutation.mutate(file);
    };

    return (
        <Box className="transport-cleaning-import-page">
            <Container maxWidth="xl" sx={{ pt: 3, pb: 3 }}>
                <PageHeader
                    title="Nueva depuración"
                    description="Importa el previo de comisión. Si el archivo es válido se guarda como borrador para revisarlo antes de registrar las giras."
                    action={(
                        <Button
                            variant="outlined"
                            color="inherit"
                            onClick={goBackToHub}
                            disabled={importing}
                            startIcon={<ArrowBackOutlinedIcon />}
                            sx={{
                                borderColor: 'divider', color: 'text.secondary',
                                textTransform: 'none', fontWeight: 700, fontSize: 12.5, borderRadius: '8px',
                                '&:hover': { borderColor: accentColor, color: accentColor, bgcolor: 'action.hover' },
                            }}
                        >
                            Volver
                        </Button>
                    )}
                />

                <Paper
                    variant="outlined"
                    sx={{
                        borderRadius: '16px',
                        overflow: 'hidden',
                        bgcolor: 'background.paperWarm',
                        boxShadow: theme.vars.palette.tones.rose.shadowResting,
                    }}
                >
                    {importing && (
                        <LinearProgress
                            sx={{
                                height: 2,
                                bgcolor: `color-mix(in srgb, ${accentColor} 15%, transparent)`,
                                '& .MuiLinearProgress-bar': { bgcolor: accentColor },
                            }}
                        />
                    )}

                    <Stack spacing={2} sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3 }}>
                        {notice && (
                            <Alert severity={notice.type} variant="outlined" onClose={clearResult}>
                                {notice.message}
                            </Alert>
                        )}

                        {issues.length > 0 && (
                            <Paper variant="outlined" sx={{ p: 2, borderRadius: '10px', bgcolor: 'background.paper' }}>
                                <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={1} sx={{ mb: 1 }}>
                                    <Typography sx={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.06em', textTransform: 'uppercase', color: 'text.secondary' }}>
                                        Errores encontrados
                                    </Typography>
                                    <Button
                                        size="small"
                                        startIcon={<DownloadOutlinedIcon />}
                                        onClick={() => downloadIssuesReport(issues)}
                                        sx={{ textTransform: 'none', fontWeight: 700, fontSize: 12.5 }}
                                    >
                                        Descargar reporte
                                    </Button>
                                </Stack>
                                {issues.length > MAX_INLINE_ISSUES ? (
                                    <Typography sx={{ fontSize: 13, color: 'error.main' }}>
                                        Hay {issues.length} errores. Descarga el reporte para ver el detalle por fila.
                                    </Typography>
                                ) : (
                                    <Box sx={{ maxHeight: 260, overflowY: 'auto' }}>
                                        {issues.map((issue, index) => (
                                            <Typography key={`${issue.row ?? 'general'}-${index}`} sx={{ fontSize: 12.5, color: 'error.main', py: 0.25 }}>
                                                {issueText(issue)}
                                            </Typography>
                                        ))}
                                    </Box>
                                )}
                            </Paper>
                        )}

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
                                        sx={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: 1.5,
                                            border: '1px dashed',
                                            borderColor: file || isDragOver ? accentColor : 'divider',
                                            borderRadius: '10px',
                                            px: 1.75,
                                            py: 1.5,
                                            minHeight: 86,
                                            cursor: importing ? 'default' : 'pointer',
                                            bgcolor: isDragOver ? 'action.hover' : 'background.paper',
                                            transition: 'border-color 0.2s ease, background-color 0.2s ease',
                                            '&:hover': { borderColor: accentColor, bgcolor: 'action.hover' },
                                        }}
                                    >
                                        <Box sx={{
                                            width: 40, height: 40, borderRadius: '10px',
                                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                                            bgcolor: 'action.selected',
                                            color: accentColor,
                                            flexShrink: 0,
                                        }}>
                                            {file ? <DescriptionOutlinedIcon sx={{ fontSize: 20 }} /> : <CloudUploadOutlinedIcon sx={{ fontSize: 20 }} />}
                                        </Box>
                                        <Box sx={{ minWidth: 0, flex: 1 }}>
                                            <Typography sx={{ fontWeight: 700, fontSize: 13.5 }} noWrap>
                                                {file ? file.name : 'Arrastra tu archivo aquí'}
                                            </Typography>
                                            <Typography sx={{ color: 'text.secondary', fontSize: 12.25 }}>
                                                {file
                                                    ? `${Math.max(1, Math.round(file.size / 1024))} KB - haz clic para cambiarlo`
                                                    : 'O haz clic para abrir el explorador'}
                                            </Typography>
                                        </Box>
                                        <input
                                            hidden
                                            type="file"
                                            accept={ACCEPTED_EXTENSIONS.join(',')}
                                            disabled={importing}
                                            onChange={(event) => { selectFile(event.target.files); event.target.value = ''; }}
                                        />
                                    </Box>

                                    <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                                        <Button
                                            variant="text"
                                            startIcon={<DownloadOutlinedIcon />}
                                            onClick={() => templateMutation.mutate()}
                                            disabled={templateMutation.isPending}
                                            sx={{ textTransform: 'none', fontWeight: 700, borderRadius: '8px', fontSize: 12.75 }}
                                        >
                                            {templateMutation.isPending ? 'Descargando...' : 'Descargar plantilla'}
                                        </Button>
                                        {file && (
                                            <Button
                                                variant="text"
                                                color="inherit"
                                                startIcon={<RestartAltOutlinedIcon />}
                                                onClick={resetFile}
                                                disabled={importing}
                                                sx={{ textTransform: 'none', fontWeight: 700, borderRadius: '8px', fontSize: 12.75 }}
                                            >
                                                Quitar archivo
                                            </Button>
                                        )}
                                    </Box>
                                </Stack>
                            </Paper>

                            <Paper variant="outlined" sx={{ p: 2, borderRadius: '8px', bgcolor: 'background.paper' }}>
                                <Stack spacing={1.5}>
                                    <Typography sx={{ fontWeight: 700, fontSize: 15 }}>
                                        Qué pasa al importar
                                    </Typography>
                                    <Typography sx={{ color: 'text.secondary', fontSize: 13 }}>
                                        Se leen todos los bloques diarios del archivo. Si alguna fila tiene errores no se guarda nada y verás el detalle por fila.
                                    </Typography>
                                    <Typography sx={{ color: 'text.secondary', fontSize: 13 }}>
                                        Si todo es válido se crea un borrador con todas las filas. Ninguna gira se registra hasta que lo confirmes desde el borrador.
                                    </Typography>
                                    <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', pt: 0.5 }}>
                                        {ACCEPTED_EXTENSIONS.map((extension) => (
                                            <Chip key={extension} label={extension} size="small" variant="outlined" />
                                        ))}
                                    </Box>
                                </Stack>
                            </Paper>
                        </Box>
                    </Stack>

                    <Box
                        sx={{
                            px: { xs: 2.5, sm: 3 },
                            py: 1.75,
                            borderTop: '1px solid',
                            borderColor: 'divider',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'flex-end',
                            gap: 1,
                            bgcolor: theme.vars.palette.tones.rose.footerBg,
                        }}
                    >
                        <Button
                            onClick={goBackToHub}
                            variant="outlined"
                            disabled={importing}
                            sx={{
                                borderColor: 'divider', color: 'text.secondary',
                                textTransform: 'none', fontWeight: 600, fontSize: 12.5, borderRadius: '8px',
                                '&:hover': { borderColor: theme.vars.palette.tones.rose.secondaryHoverBg, color: accentColor, bgcolor: theme.vars.palette.tones.rose.secondaryHoverBg },
                            }}
                        >
                            Cancelar
                        </Button>
                        <PrimaryButton
                            onClick={startImport}
                            startIcon={<CloudUploadOutlinedIcon />}
                            disabled={!file || importing}
                        >
                            {importing ? 'Importando...' : 'Importar archivo'}
                        </PrimaryButton>
                    </Box>
                </Paper>
            </Container>
        </Box>
    );
}
