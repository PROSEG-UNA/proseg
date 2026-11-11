import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Box, Typography } from '@mui/material';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { queryKeys } from '../../../../common/query';
import { deleteObservationKeyword } from '../../services/observationKeywords/observationKeywordsService';
import { useObservationKeywordsData } from '../../hooks/useObservationKeywordsData';
import KeywordChipWell from './KeywordChipWell.jsx';
import SettingsSectionHeader from './SettingsSectionHeader.jsx';

export default function ObservationKeywordsField({ onAddKeyword, onEditKeyword }) {
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
        <Box
            component="section"
            aria-labelledby="observation-keywords-title"
            sx={{ p: { xs: 2, sm: 3 }, border: '1px solid', borderColor: 'divider', borderRadius: 3, bgcolor: 'background.paperWarm' }}
        >
            <SettingsSectionHeader
                titleId="observation-keywords-title"
                title="Palabras clave en observaciones"
                count={loading || error ? undefined : rows.length}
                description="Las coincidencias aparecen junto al número de gira. Al pasar el cursor se muestra la palabra clave y puedes marcar la observación como revisada. No se distinguen mayúsculas ni tildes."
                onAdd={canEdit ? onAddKeyword : undefined}
                sx={{ mb: 2.5 }}
            />
            <KeywordChipWell
                ariaLabel="Palabras clave en observaciones"
                rows={rows}
                loading={loading}
                error={error}
                onRetry={() => void refetch()}
                canEdit={canEdit}
                canDelete={canDelete}
                onEdit={onEditKeyword}
                onDelete={setKeywordToDelete}
                loadingLabel="Cargando palabras clave"
                emptyTitle="Todavía no hay palabras clave"
                emptyHint={canEdit ? 'Usa Agregar para crear la primera.' : 'Aún no se ha creado ninguna.'}
            />
            <Typography
                variant="caption"
                role="status"
                sx={{ display: 'block', mt: 0.75, minHeight: 18, color: 'text.secondary', visibility: fetching && !loading ? 'visible' : 'hidden' }}
            >
                Actualizando palabras clave...
            </Typography>
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
        </Box>
    );
}
