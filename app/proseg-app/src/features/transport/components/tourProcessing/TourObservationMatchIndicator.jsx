import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Box, Button, IconButton, Popover, Tooltip, Typography } from '@mui/material';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
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
    const [anchor, setAnchor] = useState(null);
    const queryClient = useQueryClient();
    const mutation = useMutation({
        mutationFn: (nextReviewed) => updateTourObservationReviewed(draftId, row.id, nextReviewed),
        onSuccess: (updated) => {
            replaceCachedDraftRow(queryClient, draftId, updated);
            onReviewed?.(updated);
            setAnchor(null);
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
        <Box sx={{ maxWidth: 320 }}>
            <Typography variant="caption" sx={{ display: 'block', fontWeight: 700 }}>
                {`Coincidencias: ${matches.join(', ')}`}
            </Typography>
            <Typography variant="body2" sx={{ mt: 0.5 }}>
                <ObservationText observation={row.observations} keywords={matches} />
            </Typography>
        </Box>
    );

    return (
        <>
            <Tooltip title={tooltip} disableHoverListener={Boolean(anchor)}>
                <IconButton
                    type="button"
                    size="small"
                    aria-label={`Coincidencias en la observación: ${matches.join(', ')}`}
                    onClick={(event) => {
                        event.stopPropagation();
                        setAnchor(event.currentTarget);
                    }}
                    sx={{ color: 'info.main' }}
                >
                    <InfoOutlinedIcon fontSize="small" />
                </IconButton>
            </Tooltip>
            <Popover
                open={Boolean(anchor)}
                anchorEl={anchor}
                onClose={() => setAnchor(null)}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
                onClick={(event) => event.stopPropagation()}
            >
                <Box sx={{ p: 1.5, display: 'flex', flexDirection: 'column', gap: 1, maxWidth: 280 }}>
                    <Button
                        type="button"
                        variant="contained"
                        size="small"
                        disabled={!canReview || mutation.isPending}
                        onClick={() => mark(true)}
                    >
                        Marcar como revisada
                    </Button>
                    {mutation.isError && (
                        <Typography variant="caption" color="error">
                            {getFriendlyApiErrorMessage(mutation.error, 'No se pudo marcar la observación como revisada')}
                        </Typography>
                    )}
                </Box>
            </Popover>
        </>
    );
}
