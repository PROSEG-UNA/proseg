import { useState } from 'react';
import {
    Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, ToggleButton, ToggleButtonGroup, Typography,
} from '@mui/material';
import { EDITOR_TRIP_MODES, TRIP_MODES, TRIP_MODE_LABELS } from './tripDistribution.js';

const MODE_DETAILS = {
    [TRIP_MODES.DISTINCT]: {
        summary: 'Cada viaje lleva su propio chofer y su propio vehículo.',
        points: [
            'El Viaje 1 conserva el chofer y el vehículo actuales de la gira.',
            'Los demás viajes quedan sin chofer ni vehículo, para que los asignes después.',
            'Las horas se cuentan por separado en cada chofer.',
        ],
        useWhen: 'Úsalo cuando varios vehículos salen al mismo tiempo, cada uno con un chofer distinto.',
    },
    [TRIP_MODES.SHARED]: {
        summary: 'Uno o varios choferes hacen todos los viajes, repitiendo su vehículo.',
        points: [
            'Con 1 chofer, él hace todos los viajes uno tras otro con el mismo vehículo.',
            'Si después agregas otros choferes, los viajes se alternan entre ellos. El primero es el chofer actual.',
            'Las horas se cuentan una sola vez por chofer, no por cada viaje.',
        ],
        useWhen: 'Úsalo cuando un mismo bus o microbús lleva a los pasajeros en varias vueltas, solo o con relevos.',
    },
};

export default function TourProcessingTripSplitDialog({ open, onConfirm, onClose }) {
    const [mode, setMode] = useState(TRIP_MODES.DISTINCT);
    const details = MODE_DETAILS[mode];

    return (
        <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth aria-labelledby="trip-split-title">
            <DialogTitle id="trip-split-title" sx={{ fontSize: 17, fontWeight: 700 }}>Dividir en varios viajes</DialogTitle>
            <DialogContent>
                <Typography sx={{ fontSize: 13, fontWeight: 700, mb: 1 }}>Cómo repartir la gira</Typography>
                <ToggleButtonGroup
                    value={mode}
                    exclusive
                    size="small"
                    fullWidth
                    onChange={(_, nextMode) => nextMode && setMode(nextMode)}
                    aria-label="Manera de repartir la gira"
                >
                    {EDITOR_TRIP_MODES.map((value) => (
                        <ToggleButton key={value} value={value} sx={{ textTransform: 'none', fontSize: 12 }}>
                            {TRIP_MODE_LABELS[value]}
                        </ToggleButton>
                    ))}
                </ToggleButtonGroup>
                <Box sx={{ mt: 2, p: 2, borderRadius: 2, bgcolor: 'action.hover' }}>
                    <Typography sx={{ fontSize: 14, fontWeight: 700, mb: 1 }}>{details.summary}</Typography>
                    <Box component="ul" sx={{ m: 0, pl: 2.5, display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                        {details.points.map((point) => (
                            <Typography key={point} component="li" sx={{ fontSize: 13, color: 'text.secondary' }}>{point}</Typography>
                        ))}
                    </Box>
                    <Typography sx={{ fontSize: 13, mt: 1.5 }}>{details.useWhen}</Typography>
                </Box>
                <Typography sx={{ fontSize: 12.5, color: 'text.secondary', mt: 1.5 }}>
                    Después podrás ajustar la cantidad de viajes y los pasajeros de cada uno.
                </Typography>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} sx={{ textTransform: 'none' }}>Cancelar</Button>
                <Button onClick={() => onConfirm(mode)} variant="contained" sx={{ textTransform: 'none' }}>Dividir</Button>
            </DialogActions>
        </Dialog>
    );
}
