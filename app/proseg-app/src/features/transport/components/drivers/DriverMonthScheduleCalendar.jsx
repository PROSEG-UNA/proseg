import { useEffect, useRef, useState } from 'react';
import { Box, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material';
import { alpha } from '@mui/material/styles';
import dayjs from 'dayjs';
import { ORDINARY_SCHEDULE_OPTIONS } from '../../transportUtils';

const WEEKDAY_LABELS = ['Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá', 'Do'];
const SCHEDULE_COLORS = {
    [ORDINARY_SCHEDULE_OPTIONS[0].value]: 'warning',
    [ORDINARY_SCHEDULE_OPTIONS[1].value]: 'info',
};
const SCHEDULE_NAMES = {
    [ORDINARY_SCHEDULE_OPTIONS[0].value]: 'Mañana',
    [ORDINARY_SCHEDULE_OPTIONS[1].value]: 'Tarde',
};

function scheduleCellSx(color, disabled) {
    return (theme) => ({
        height: 46,
        minWidth: 0,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: 1,
        border: '1px solid',
        borderColor: alpha(theme.palette[color].main, 0.55),
        bgcolor: alpha(theme.palette[color].main, 0.22),
        color: 'text.primary',
        font: 'inherit',
        p: 0,
        cursor: disabled ? 'not-allowed' : 'pointer',
        userSelect: 'none',
        '&:hover': disabled ? {} : { bgcolor: alpha(theme.palette[color].main, 0.34) },
        '&:focus-visible': { outline: `2px solid ${theme.palette.primary.main}`, outlineOffset: 1 },
    });
}

const WEEKEND_CELL_SX = {
    height: 46,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 1,
    bgcolor: 'action.hover',
    color: 'text.disabled',
    userSelect: 'none',
};

export default function DriverMonthScheduleCalendar({ period, baseValue, overrides, disabled = false, onSelect }) {
    const [chosenSchedule, setChosenSchedule] = useState(null);
    const selectingRef = useRef(false);
    const activeSchedule = chosenSchedule ?? ORDINARY_SCHEDULE_OPTIONS.find((option) => option.value !== baseValue).value;
    const firstDay = dayjs(`${period.year}-${String(period.month).padStart(2, '0')}-01`);
    const leadingBlanks = (firstDay.day() + 6) % 7;
    const days = Array.from({ length: firstDay.daysInMonth() }, (_, index) => index + 1);

    useEffect(() => {
        const stopSelecting = () => {
            selectingRef.current = false;
        };
        document.addEventListener('pointerup', stopSelecting);
        document.addEventListener('pointercancel', stopSelecting);
        return () => {
            document.removeEventListener('pointerup', stopSelecting);
            document.removeEventListener('pointercancel', stopSelecting);
        };
    }, []);

    const selectDay = (day) => {
        if (!disabled) onSelect(day, activeSchedule);
    };

    const handlePointerDown = (event, day) => {
        if (disabled) return;
        event.currentTarget.releasePointerCapture?.(event.pointerId);
        selectingRef.current = true;
        selectDay(day);
    };

    const handlePointerEnter = (day) => {
        if (selectingRef.current) selectDay(day);
    };

    return (
        <Box sx={{ display: 'grid', gap: 1.5, gridColumn: { md: '1 / -1' } }}>
            <Typography variant="subtitle2">{`Horario ordinario de ${period.label}`}</Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap' }}>
                <Typography variant="body2" color="text.secondary">Horario a seleccionar</Typography>
                <ToggleButtonGroup
                    exclusive
                    size="small"
                    value={activeSchedule}
                    disabled={disabled}
                    aria-label="Horario a seleccionar"
                    onChange={(_, value) => {
                        if (value) setChosenSchedule(value);
                    }}
                >
                    {ORDINARY_SCHEDULE_OPTIONS.map((option) => (
                        <ToggleButton key={option.value} value={option.value} sx={{ textTransform: 'none', px: 1.5, gap: 0.75 }}>
                            <Box
                                component="span"
                                sx={(theme) => ({
                                    width: 10,
                                    height: 10,
                                    borderRadius: '3px',
                                    bgcolor: alpha(theme.palette[SCHEDULE_COLORS[option.value]].main, 0.6),
                                })}
                            />
                            {`${SCHEDULE_NAMES[option.value]} ${option.label}`}
                        </ToggleButton>
                    ))}
                </ToggleButtonGroup>
            </Box>
            <Box
                data-testid="schedule-calendar"
                sx={{ display: 'grid', gridTemplateColumns: 'repeat(7, minmax(0, 1fr))', gap: 0.5, touchAction: 'none' }}
            >
                {WEEKDAY_LABELS.map((label) => (
                    <Typography key={label} variant="caption" color="text.secondary" sx={{ textAlign: 'center' }}>{label}</Typography>
                ))}
                {Array.from({ length: leadingBlanks }, (_, index) => <Box key={`blank-${index}`} />)}
                {days.map((day) => {
                    const weekday = dayjs(`${period.year}-${String(period.month).padStart(2, '0')}-${String(day).padStart(2, '0')}`).day();
                    if (weekday === 0 || weekday === 6) {
                        return (
                            <Box key={day} aria-label={`Día ${day}, fin de semana`} sx={WEEKEND_CELL_SX}>
                                <Typography variant="body2">{day}</Typography>
                                <Typography sx={{ fontSize: 11, lineHeight: 1.2 }}>extra</Typography>
                            </Box>
                        );
                    }
                    const value = overrides[day] ?? baseValue;
                    return (
                        <Box
                            key={day}
                            component="button"
                            type="button"
                            aria-label={`Día ${day}, ${SCHEDULE_NAMES[value].toLowerCase()}`}
                            disabled={disabled}
                            onPointerDown={(event) => handlePointerDown(event, day)}
                            onPointerEnter={() => handlePointerEnter(day)}
                            onClick={() => selectDay(day)}
                            sx={scheduleCellSx(SCHEDULE_COLORS[value], disabled)}
                        >
                            <Typography variant="body2">{day}</Typography>
                            <Typography sx={{ fontSize: 11, lineHeight: 1.2 }}>{SCHEDULE_NAMES[value]}</Typography>
                        </Box>
                    );
                })}
            </Box>
            <Typography variant="caption" color="text.secondary">
                Los días sin cambios usan el horario del chofer. Sábado y domingo no cuentan como horario ordinario: sus horas siempre son extra.
            </Typography>
        </Box>
    );
}
