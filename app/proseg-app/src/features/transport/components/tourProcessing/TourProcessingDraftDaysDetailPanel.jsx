import { Box, Chip, Typography } from '@mui/material';
import { formatDate, formatHourOfDay, formatHours } from '../../transportUtils.js';

function assignmentTitle(assignment, index) {
    const plate = assignment.borrowedVehiclePlate || assignment.vehicle;
    return [`Vehículo ${index + 1}`, plate, assignment.driver].filter(Boolean).join(' · ');
}

function DaysList({ days, ariaLabel }) {
    return (
        <Box
            component="ul"
            aria-label={ariaLabel}
            sx={{
                display: 'grid',
                gridTemplateColumns: 'minmax(0, 1fr)',
                maxWidth: 520,
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
                            textAlign: 'left',
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
    );
}

export default function TourProcessingDraftDaysDetailPanel({ days, assignments }) {
    const groups = (assignments ?? []).length > 1
        ? assignments.map((assignment, index) => ({ assignment, index, days: assignment.days ?? [] })).filter((group) => group.days.length)
        : [];
    const grouped = groups.length > 1;
    const totalDays = grouped ? Math.max(...groups.map((group) => group.days.length)) : days.length;

    return (
        <Box sx={{ px: { xs: 2, sm: 3 }, py: 1.5, bgcolor: 'background.paper', cursor: 'pointer' }}>
            <Typography component="h2" sx={{ mb: 1, fontSize: 13, fontWeight: 600 }}>
                Detalle por día
                <Box component="span" sx={{ ml: 1, fontSize: 12, fontWeight: 400, color: 'text.secondary' }}>
                    {`${totalDays} ${totalDays === 1 ? 'día' : 'días'}`}
                    {grouped && ` · ${groups.length} vehículos`}
                </Box>
            </Typography>
            {grouped ? (
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    {groups.map(({ assignment, index, days: assignmentDays }) => (
                        <Box
                            component="section"
                            key={assignment.rowId ?? index}
                            aria-label={assignmentTitle(assignment, index)}
                            sx={{
                                maxWidth: 560,
                                pl: 1.5,
                                borderLeft: '3px solid',
                                borderColor: 'primary.main',
                            }}
                        >
                            <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 1, mb: 1 }}>
                                <Typography sx={{ fontSize: 13, fontWeight: 700 }}>{assignmentTitle(assignment, index)}</Typography>
                                {assignment.passengers != null && (
                                    <Chip size="small" variant="outlined" label={`${assignment.passengers} pasajeros`} />
                                )}
                            </Box>
                            <DaysList days={assignmentDays} ariaLabel={`Desglose de horario y horas por día de ${assignmentTitle(assignment, index)}`} />
                        </Box>
                    ))}
                </Box>
            ) : (
                <DaysList days={days} ariaLabel="Desglose de horario y horas por día" />
            )}
        </Box>
    );
}
