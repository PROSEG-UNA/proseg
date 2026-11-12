import { Button, Dialog, DialogActions, DialogContent, DialogTitle, Typography } from '@mui/material';

export default function TourProcessingSplitModeDialog({ open, oversizedRows, pending, onConfirm, onClose }) {
    return (
        <Dialog open={open} onClose={pending ? undefined : onClose} maxWidth="sm" fullWidth aria-labelledby="split-mode-title">
            <DialogTitle id="split-mode-title">Dividir giras automáticamente</DialogTitle>
            <DialogContent>
                <Typography sx={{ mb: 1.5, fontSize: 14 }}>
                    {`Se dividirán ${oversizedRows} giras cuyos pasajeros superan la capacidad de su vehículo. Los pasajeros se reparten de forma equitativa sin superar esa capacidad.`}
                </Typography>
                <Typography sx={{ fontSize: 14 }}>
                    Cada gira se divide según su destino: si está en el banco de choferes que repiten viajes, el chofer y el vehículo se repiten en todos los viajes; si está en el banco de choferes distintos, los demás viajes quedan sin asignar. Los destinos que no están en ningún banco siguen el interruptor activo en Configuraciones. Después puedes editar el resultado en cada gira.
                </Typography>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} disabled={pending} sx={{ textTransform: 'none' }}>Cancelar</Button>
                <Button onClick={onConfirm} disabled={pending} variant="contained" sx={{ textTransform: 'none' }}>
                    {pending ? 'Dividiendo...' : 'Sí, dividir giras'}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
