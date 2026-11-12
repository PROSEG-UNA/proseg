import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Box, Button, IconButton, Tooltip, Typography } from '@mui/material';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import DoneOutlinedIcon from '@mui/icons-material/DoneOutlined';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';
import { replaceCachedDraftRow } from '../../hooks/useTourProcessingDraftsData.js';
import { updateTourObservationReviewed } from '../../services/tourProcessing/tourProcessingService.js';
import { highlightObservation } from './observationKeywordText.js';

function ObservationText({ observation, keywords }) {
    const parts = highlightObservation(observation, keywords);
    if (parts.length === 0) return 'Sin observación';
    return parts.map((part, index) => (
        part.match
            ? <Box key={`${part.text}-${index}`} component="mark" sx={{ px: 0.25, borderRadius: '2px', bgcolor: 'warning.light', color: 'inherit' }}>{part.text}</Box>
            : <span key={`${part.text}-${index}`}>{part.text}</span>
    ));
}

export default function TourObservationMatchIndicator({ draftId, row, canReview = false, allowReopen = false, onReviewed }) {
    const matches = row?.matchedObservationKeywords ?? [];
    const reviewed = Boolean(row?.observationReviewed);
    const [tooltipOpen, setTooltipOpen] = useState(false);
    const queryClient = useQueryClient();
    const mutation = useMutation({
        mutationFn: (nextReviewed) => updateTourObservationReviewed(draftId, row.id, nextReviewed),
        onSuccess: (updated) => {
            replaceCachedDraftRow(queryClient, draftId, updated);
            onReviewed?.(updated);
            setTooltipOpen(false);
        },
    });

    if (matches.length === 0) return null;
    if (reviewed && !allowReopen) return null;

    const mark = (nextReviewed) => {
        if (!canReview || mutation.isPending) return;
        mutation.mutate(nextReviewed);
    };

    if (reviewed) {
        return (
            <Button
                type="button"
                size="small"
                variant="text"
                disabled={!canReview || mutation.isPending}
                onClick={() => mark(false)}
            >
                Volver a marcar como pendiente
            </Button>
        );
    }

    const tooltip = (
        <Box onClick={(event) => event.stopPropagation()} sx={{ width: 272, maxWidth: 'calc(100vw - 32px)', p: 1.5 }}>
            <Typography variant="caption" sx={{ display: 'block', fontWeight: 700, color: 'text.secondary' }}>
                {`Coincidencias: ${matches.join(', ')}`}
            </Typography>
            <Typography variant="body2" sx={{ mt: 0.75, lineHeight: 1.5, color: 'text.primary' }}>
                <ObservationText observation={row.observations} keywords={matches} />
            </Typography>
            <Button
                type="button"
                variant="outlined"
                color="inherit"
                size="small"
                fullWidth
                startIcon={<DoneOutlinedIcon fontSize="small" />}
                disabled={!canReview || mutation.isPending}
                onClick={() => mark(true)}
                sx={{ mt: 1.5, fontWeight: 700, fontSize: 12.5 }}
            >
                Marcar como revisada
            </Button>
            {mutation.isError && (
                <Typography variant="caption" color="error" sx={{ display: 'block', mt: 1 }}>
                    {getFriendlyApiErrorMessage(mutation.error, 'No se pudo marcar la observación como revisada')}
                </Typography>
            )}
        </Box>
    );

    return (
        <Tooltip
            title={tooltip}
            placement="bottom-start"
            open={tooltipOpen}
            onOpen={() => setTooltipOpen(true)}
            onClose={() => setTooltipOpen(false)}
            leaveDelay={150}
            slotProps={{
                tooltip: {
                    sx: {
                        maxWidth: 'none',
                        p: 0,
                        bgcolor: 'background.paper',
                        color: 'text.primary',
                        border: '1px solid',
                        borderColor: 'divider',
                        borderRadius: 2,
                        boxShadow: 3,
                    },
                },
            }}
        >
            <IconButton
                type="button"
                size="small"
                aria-label={`Coincidencias en la observación: ${matches.join(', ')}`}
                onClick={(event) => {
                    event.stopPropagation();
                    setTooltipOpen(true);
                }}
                sx={{ color: 'info.main' }}
            >
                <InfoOutlinedIcon fontSize="small" />
            </IconButton>
        </Tooltip>
    );
}
