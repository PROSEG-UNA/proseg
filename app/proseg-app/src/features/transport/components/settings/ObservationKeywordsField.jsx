import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Alert, Box, Button, Chip, CircularProgress, Typography } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { queryKeys } from '../../../../common/query';
import { deleteObservationKeyword } from '../../services/observationKeywords/observationKeywordsService';
import { useObservationKeywordsData } from '../../hooks/useObservationKeywordsData';

export default function ObservationKeywordsField({ onEditKeyword }) {
    const [alert, setAlert] = useState(null);
    const [keywordToDelete, setKeywordToDelete] = useState(null);
    const queryClient = useQueryClient();
    const { hasPermission } = usePermissions();
    const canEdit = hasPermission(PERMISSIONS.TRANSPORT.TOURS.UPDATE);
    const canDelete = hasPermission(PERMISSIONS.TRANSPORT.TOURS.DELETE);
    const { rows, loading, fetching, error, refetch } = useObservationKeywordsData();

    const deleteMutation = useMutation({
        mutationFn: (keywordId) => deleteObservationKeyword(keywordId),
        onSuccess: () => {
            setKeywordToDelete(null);
            setAlert({ type: 'success', message: 'Palabra clave eliminada correctamente' });
            void queryClient.invalidateQueries({ queryKey: queryKeys.transport.observationKeywords() });
            void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessing() });
        },
        onError: (deleteError) => {
            setAlert({ type: 'error', message: deleteError?.response?.data?.message ?? deleteError?.message ?? 'No se pudo eliminar la palabra clave' });
        },
    });

    return (
        <>
            <Box
                role="group"
                aria-label="Palabras clave en observaciones"
                sx={{
                    minHeight: 104,
                    maxHeight: 240,
                    overflowY: 'auto',
                    p: 2,
                    border: '1px solid',
                    borderColor: 'divider',
                    borderRadius: 2,
                    bgcolor: 'background.paper',
                    display: 'flex',
                    alignContent: 'flex-start',
                    alignItems: 'flex-start',
                    flexWrap: 'wrap',
                    gap: 1,
                }}
            >
                {loading ? (
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <CircularProgress size={18} />
                        <Typography color="text.secondary">Cargando palabras clave...</Typography>
                    </Box>
                ) : error ? (
                    <Alert severity="error" action={<Button color="inherit" size="small" onClick={() => void refetch()}>Reintentar</Button>}>
                        {error}
                    </Alert>
                ) : rows.length === 0 ? (
                    <Typography color="text.secondary">Todavía no hay palabras clave. Usa Agregar para crear la primera.</Typography>
                ) : rows.map((keyword) => (
                    <Chip
                        key={keyword.id}
                        label={keyword.text}
                        onClick={canEdit ? () => onEditKeyword(keyword) : undefined}
                        onDelete={canDelete ? () => setKeywordToDelete(keyword) : undefined}
                        deleteIcon={canDelete ? <CloseIcon aria-label={`Eliminar ${keyword.text}`} /> : undefined}
                        sx={(theme) => ({
                            maxWidth: '100%',
                            bgcolor: '#d1d5db',
                            borderColor: '#9ca3af',
                            '& .MuiChip-label, & .MuiChip-deleteIcon': { color: '#374151' },
                            '&:hover': { bgcolor: '#c0c4cb' },
                            ...theme.applyStyles('dark', {
                                bgcolor: '#374151',
                                borderColor: '#6b7280',
                                '& .MuiChip-label, & .MuiChip-deleteIcon': { color: '#f3f4f6' },
                                '&:hover': { bgcolor: '#4b5563' },
                            }),
                        })}
                    />
                ))}
            </Box>
            {fetching && !loading ? <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.75 }}>Actualizando palabras clave...</Typography> : null}
            <DialogModal
                type="delete"
                open={Boolean(keywordToDelete)}
                title="Eliminar palabra clave"
                message={keywordToDelete ? `¿Seguro que deseas eliminar la palabra "${keywordToDelete.text}"?` : ''}
                onClose={() => { if (!deleteMutation.isPending) setKeywordToDelete(null); }}
                onConfirm={() => { if (keywordToDelete && !deleteMutation.isPending) deleteMutation.mutate(keywordToDelete.id); }}
                confirmLabel="Eliminar"
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
