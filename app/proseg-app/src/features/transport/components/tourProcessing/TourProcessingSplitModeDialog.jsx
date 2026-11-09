import { useState } from 'react';
import {
    Button, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, FormControlLabel, Radio, RadioGroup, Typography,
} from '@mui/material';
import { TRIP_MODES } from './tripDistribution.js';

const OPTIONS = [
    {
        value: TRIP_MODES.DISTINCT,
        label: 'Choferes distintos',
        description: 'Cada gira queda repartida en tantos viajes como haga falta. El primer viaje conserva el chofer y el vehículo actuales; los demás quedan sin asignar.',
    },
    {
        value: TRIP_MODES.SAME,
        label: 'Un chofer y vehículo con varios viajes',
        description: 'Todos los viajes repiten el chofer y el vehículo actuales de la gira. Las horas se cuentan una sola vez.',
    },
];

export default function TourProcessingSplitModeDialog({ open, oversizedRows, pending, onConfirm, onClose }) {
    const [mode, setMode] = useState(TRIP_MODES.DISTINCT);

    return (
        <Dialog open={open} onClose={pending ? undefined : onClose} maxWidth="sm" fullWidth aria-labelledby="split-mode-title">
            <DialogTitle id="split-mode-title">Dividir giras automáticamente</DialogTitle>
            <DialogContent>
                <Typography sx={{ mb: 1.5, fontSize: 14 }}>
                    {`Se dividirán ${oversizedRows} giras cuyos pasajeros superan la capacidad de su vehículo. Los pasajeros se reparten de forma equitativa sin superar esa capacidad. Después puedes editar el resultado en cada gira.`}
                </Typography>
                <FormControl>
                    <RadioGroup aria-label="Manera de dividir" value={mode} onChange={(event) => setMode(event.target.value)}>
                        {OPTIONS.map((option) => (
                            <FormControlLabel
                                key={option.value}
                                value={option.value}
                                control={<Radio size="small" />}
                                sx={{ alignItems: 'flex-start', mb: 1 }}
                                label={(
                                    <>
                                        <Typography sx={{ fontSize: 14, fontWeight: 700 }}>{option.label}</Typography>
                                        <Typography sx={{ fontSize: 12.5, color: 'text.secondary' }}>{option.description}</Typography>
                                    </>
                                )}
                            />
                        ))}
                    </RadioGroup>
                </FormControl>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={pending} sx={{ textTransform: 'none' }}>Cancelar</Button>
                <Button onClick={() => onConfirm(mode)} disabled={pending} variant="contained" sx={{ textTransform: 'none' }}>
                    {pending ? 'Dividiendo...' : 'Sí, dividir giras'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
