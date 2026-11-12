import { useId, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Box, FormControlLabel, Switch, Typography } from '@mui/material';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { queryKeys } from '../../../../common/query';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { deleteTourSplitKeyword } from '../../services/tourSplit/tourSplitService';
import { useTourSplitKeywordsData } from '../../hooks/useTourSplitKeywordsData';
import KeywordChipWell from './KeywordChipWell.jsx';
import SettingsSectionHeader from './SettingsSectionHeader.jsx';

export default function TourSplitKeywordsSection({ mode, title, description, switchLabel, defaultActive, settingsLoading, settingsSaving, onToggleDefault, onAddKeyword, onEditKeyword }) {
    const titleId = useId();
    const [alert, setAlert] = useState(null);
    const [keywordToDelete, setKeywordToDelete] = useState(null);
    const queryClient = useQueryClient();
    const { hasPermission } = usePermissions();
    const canEdit = hasPermission(PERMISSIONS.TRANSPORT.TOURS.UPDATE);
    const canDelete = hasPermission(PERMISSIONS.TRANSPORT.TOURS.DELETE);
    const { rows, loading, fetching, error, refetch } = useTourSplitKeywordsData(mode);

    const deleteMutation = useMutation({
        mutationFn: (keywordId) => deleteTourSplitKeyword(keywordId),
        onSuccess: () => {
            setKeywordToDelete(null);
            setAlert({ type: 'success', message: 'Destino eliminado correctamente' });
            void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourSplitKeywordList(mode) });
        },
        onError: (deleteError) => {
            setKeywordToDelete(null);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(deleteError, 'No se pudo eliminar el destino') });
        },
    });

    return (
        <Box
            component="section"
            aria-labelledby={titleId}
            sx={{
                display: 'grid',
                gridTemplateColumns: 'minmax(0, 1fr)',
                gridTemplateRows: { md: 'subgrid' },
                gridRow: { md: 'span 3' },
                rowGap: 2,
                alignContent: 'start',
            }}
        >
            <SettingsSectionHeader
                titleId={titleId}
                component="h3"
                title={title}
                count={loading || error ? undefined : rows.length}
                description={description}
                onAdd={canEdit ? onAddKeyword : undefined}
            />
            <Box>
                <KeywordChipWell
                    ariaLabel={`Destinos: ${title}`}
                    rows={rows}
                    loading={loading}
                    error={error}
                    onRetry={() => void refetch()}
                    canEdit={canEdit}
                    canDelete={canDelete}
                    onEdit={onEditKeyword}
                    onDelete={setKeywordToDelete}
                    loadingLabel="Cargando destinos"
                    emptyTitle="Todavía no hay destinos"
                    emptyHint={canEdit ? 'Usa Agregar para añadir el primero.' : 'Aún no se ha añadido ninguno.'}
                />
                <Typography
                    variant="caption"
                    role="status"
                    sx={{ display: 'block', mt: 0.75, minHeight: 18, color: 'text.secondary', visibility: fetching && !loading ? 'visible' : 'hidden' }}
                >
                    Actualizando destinos...
                </Typography>
            </Box>
            <FormControlLabel
                labelPlacement="start"
                sx={{
                    m: 0,
                    px: 2,
                    py: 0.5,
                    width: '100%',
                    justifyContent: 'space-between',
                    borderRadius: 2,
                    border: '1px solid',
                    borderColor: defaultActive ? 'primary.main' : 'divider',
                    bgcolor: 'background.paperWarm',
                    transition: 'border-color 160ms ease-out',
                }}
                control={(
                    <Switch
                        checked={Boolean(defaultActive)}
                        onChange={(event) => onToggleDefault(event.target.checked)}
                        disabled={!canEdit || settingsLoading || settingsSaving}
                        slotProps={{ input: { 'aria-label': switchLabel } }}
                    />
                )}
                label={<Typography sx={{ fontSize: 14, fontWeight: 600 }}>{switchLabel}</Typography>}
            />
            <DialogModal
                type="delete"
                open={Boolean(keywordToDelete)}
                title="Eliminar destino"
                message={keywordToDelete ? `¿Seguro que deseas eliminar el destino "${keywordToDelete.text}"?` : ''}
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
