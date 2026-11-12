import { Box, Typography } from '@mui/material';
import { formatDate } from '../../transportUtils.js';

const SECTIONS = [
    { key: 'OTHER_UNIT', title: 'Préstamo de vehículo', driverLabel: 'Chofer', emptyDriver: '-' },
    { key: 'EXTERNAL', title: 'Contratación externa', driverLabel: 'Proveedor', emptyDriver: 'Por contratar' },
];

export default function TourProcessingDraftTourControlOtherTours({ entries, date }) {
    const groups = SECTIONS
        .map((section) => ({ ...section, entries: entries.filter((entry) => entry.section === section.key) }))
        .filter((group) => group.entries.length > 0);

    if (groups.length === 0) return null;

    return (
        <Box component="section" aria-label="Giras con otros recursos" sx={{ mt: 3 }}>
            <Typography component="h2" sx={{ mb: 1, fontSize: 14, fontWeight: 600 }}>
                {`Giras con otros recursos del ${formatDate(date)}`}
            </Typography>
            {groups.map((group) => (
                <Box key={group.key} sx={{ mb: 2 }}>
                    <Typography component="h3" sx={{ mb: 1, fontSize: 13, fontWeight: 600 }}>
                        {group.title}
                        <Box component="span" sx={{ ml: 1, fontSize: 12, fontWeight: 400, color: 'text.secondary' }}>
                            {`${group.entries.length} ${group.entries.length === 1 ? 'gira' : 'giras'}`}
                        </Box>
                    </Typography>
                    <Box component="ul" aria-label={`Giras de ${group.title.toLowerCase()}`} sx={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr)', gap: 1, m: 0, p: 0, listStyle: 'none' }}>
                        {group.entries.map((entry, index) => (
                            <Box
                                component="li"
                                key={`${entry.number}-${index}`}
                                sx={{
                                    px: 2,
                                    py: 1.25,
                                    bgcolor: 'background.paperWarm',
                                    border: '1px solid',
                                    borderColor: 'divider',
                                    borderRadius: 1,
                                    fontVariantNumeric: 'tabular-nums',
                                    '& dt': { fontSize: 12, color: 'text.secondary' },
                                    '& dd': { m: 0, fontSize: 13, lineHeight: 1.5, overflowWrap: 'anywhere' },
                                }}
                            >
                                <Box component="dl" sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(104px, 1fr))', columnGap: 2, rowGap: 1.5, m: 0, textAlign: 'left' }}>
                                    {[
                                        ['Número', entry.number, true],
                                        [group.driverLabel, entry.driver ?? group.emptyDriver],
                                        ['Placa', entry.plate ?? '-'],
                                        ['Destino', entry.destination ?? '-'],
                                        ['Solicitante', entry.responsible ?? '-'],
                                        ['Pasajeros', entry.passengers ?? '-'],
                                        ['Prioridad', entry.priority ?? '-'],
                                        ['Fecha salida', formatDate(entry.departureDate)],
                                        ['Fecha regreso', formatDate(entry.returnDate)],
                                    ].map(([label, value, strong]) => (
                                        <Box key={label}>
                                            <Typography component="dt">{label}</Typography>
                                            <Typography component="dd" sx={{ fontWeight: strong ? 600 : 400 }}>{value}</Typography>
                                        </Box>
                                    ))}
                                </Box>
                            </Box>
                        ))}
                    </Box>
                </Box>
            ))}
        </Box>
    );
}
