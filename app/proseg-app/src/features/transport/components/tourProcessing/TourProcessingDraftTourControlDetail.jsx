import { Box, Typography } from '@mui/material';
import { formatDate, formatHours } from '../../transportUtils.js';

const FIELDS = [
    { key: 'departureDate', label: 'Fecha salida', format: formatDate },
    { key: 'returnDate', label: 'Fecha regreso', format: formatDate },
    { key: 'destination', label: 'Destino' },
    { key: 'vehicle', label: 'Placa' },
    { key: 'responsible', label: 'Solicitante' },
    { key: 'passengers', label: 'Pasajeros' },
    { key: 'priority', label: 'Prioridad' },
];

export default function TourProcessingDraftTourControlDetail({ tours, date, driverName, onOpenTour }) {
    return (
        <Box sx={{ px: { xs: 2, sm: 3 }, py: 1.5, bgcolor: 'background.paper' }}>
            <Typography component="h2" sx={{ mb: 1, fontSize: 13, fontWeight: 600 }}>
                Giras del {formatDate(date)}
                <Box component="span" sx={{ ml: 1, fontSize: 12, fontWeight: 400, color: 'text.secondary' }}>
                    {`${tours.length} ${tours.length === 1 ? 'gira' : 'giras'}`}
                </Box>
            </Typography>
            <Box component="ul" aria-label={`Detalle de giras de ${driverName}`} sx={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr)', gap: 1, m: 0, p: 0, listStyle: 'none' }}>
                {tours.map((tour) => (
                    <Box component="li" key={tour.id}>
                        <Box
                            role="button"
                            aria-label={`Abrir gira ${tour.number}`}
                            tabIndex={0}
                            onClick={(event) => {
                                event.stopPropagation();
                                onOpenTour(tour);
                            }}
                            onKeyDown={(event) => {
                                if (event.key !== 'Enter' && event.key !== ' ') return;
                                event.preventDefault();
                                event.stopPropagation();
                                onOpenTour(tour);
                            }}
                            sx={{
                                px: 2,
                                py: 1.25,
                                bgcolor: 'background.paperWarm',
                                border: '1px solid',
                                borderColor: 'divider',
                                borderRadius: 1,
                                cursor: 'pointer',
                                fontVariantNumeric: 'tabular-nums',
                                '&:hover': { bgcolor: 'action.hover' },
                                '&:focus-visible': { outline: '2px solid', outlineColor: 'primary.main', outlineOffset: 2 },
                                '& dt': { fontSize: 12, color: 'text.secondary' },
                                '& dd': { m: 0, fontSize: 13, lineHeight: 1.5, overflowWrap: 'anywhere' },
                            }}
                        >
                            <Box component="dl" sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(104px, 1fr))', columnGap: 2, rowGap: 1.5, m: 0, textAlign: 'left' }}>
                                <Box>
                                    <Typography component="dt">Número</Typography>
                                    <Typography component="dd" sx={{ fontWeight: 600 }}>{tour.number}</Typography>
                                </Box>
                                {[
                                    ['Ordinarias', tour.day.ordinaryHours],
                                    ['Sobresueldo', tour.day.surplusHours],
                                    ['Extra', tour.day.overtimeHours],
                                ].map(([label, hours]) => (
                                    <Box key={label}>
                                        <Typography component="dt">{label}</Typography>
                                        <Typography component="dd" sx={{ color: hours ? 'text.primary' : 'text.secondary', fontWeight: hours ? 600 : 400 }}>
                                            {`${formatHours(hours)} h`}
                                        </Typography>
                                    </Box>
                                ))}
                                {FIELDS.map((field) => (
                                    <Box key={field.key}>
                                        <Typography component="dt">{field.label}</Typography>
                                        <Typography component="dd">{field.format ? field.format(tour[field.key]) : (tour[field.key] ?? '-')}</Typography>
                                    </Box>
                                ))}
                            </Box>
                        </Box>
                    </Box>
                ))}
            </Box>
        </Box>
    );
}
