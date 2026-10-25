import { Box, Typography } from '@mui/material';
import { formatDate, formatHourOfDay, formatHours } from '../../transportUtils.js';

export default function CleaningDraftDaysDetailPanel({ days }) {
    return (
        <Box sx={{ px: { xs: 2, sm: 3 }, py: 1.5, bgcolor: 'background.paper', cursor: 'pointer' }}>
            <Typography component="h3" sx={{ mb: 1, fontSize: 13, fontWeight: 600 }}>
                Detalle por día
                <Box component="span" sx={{ ml: 1, fontSize: 12, fontWeight: 400, color: 'text.secondary' }}>
                    {`${days.length} ${days.length === 1 ? 'día' : 'días'}`}
                </Box>
            </Typography>
            <Box
                component="ul"
                aria-label="Desglose de horario y horas por día"
                sx={{
                    display: 'grid',
                    gridTemplateColumns: 'minmax(0, 1fr)',
                    gap: 1,
                    m: 0,
                    p: 0,
                    listStyle: 'none',
                }}
            >
                {days.map((day) => (
                    <Box
                        component="li"
                        key={day.date}
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            flexWrap: 'wrap',
                            gap: 2,
                            px: 2,
                            py: 1.25,
                            bgcolor: 'background.paperWarm',
                            border: '1px solid',
                            borderColor: 'divider',
                            borderRadius: 1,
                            cursor: 'pointer',
                            fontVariantNumeric: 'tabular-nums',
                            '&:hover': { bgcolor: 'action.hover' },
                        }}
                    >
                        <Box sx={{ minWidth: 110 }}>
                            <Typography sx={{ fontSize: 13, fontWeight: 600 }}>
                                {formatDate(day.date)}
                            </Typography>
                            <Typography sx={{ fontSize: 12, color: 'text.secondary', whiteSpace: 'nowrap' }}>
                                {day.startTime?.slice(0, 5) ?? formatHourOfDay(day.start)}
                                {' - '}
                                {day.endTime?.slice(0, 5) ?? formatHourOfDay(day.end)}
                            </Typography>
                        </Box>
                        <Box
                            component="dl"
                            sx={{
                                display: 'grid',
                                gridTemplateColumns: 'repeat(3, 1fr)',
                                flex: '1 1 230px',
                                gap: 1.5,
                                m: 0,
                                textAlign: 'right',
                                '& dt': { fontSize: 12, color: 'text.secondary', whiteSpace: 'nowrap' },
                                '& dd': { m: 0, fontSize: 13, lineHeight: 1.5 },
                            }}
                        >
                            {[
                                ['Ordinarias', day.ordinaryHours],
                                ['Sobresueldo', day.surplusHours],
                                ['Extra', day.overtimeHours],
                            ].map(([label, hours]) => (
                                <Box key={label}>
                                    <Typography component="dt">{label}</Typography>
                                    <Typography component="dd" sx={{ color: hours ? 'text.primary' : 'text.secondary', fontWeight: hours ? 600 : 400 }}>
                                        {`${formatHours(hours)} h`}
                                    </Typography>
                                </Box>
                            ))}
                        </Box>
                    </Box>
                ))}
            </Box>
        </Box>
    );
}
