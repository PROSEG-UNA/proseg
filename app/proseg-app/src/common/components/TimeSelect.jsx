import { useState, useRef, useEffect } from 'react';
import { Box, Button, IconButton, InputAdornment, Popover, TextField, Typography, useTheme } from '@mui/material';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import dayjs from 'dayjs';

const HOURS = Array.from({ length: 24 }, (_, i) => i);
const pad = (n) => String(n).padStart(2, '0');

export default function TimeSelect({
    value = null,
    onChange,
    onClose,
    label,
    disabled = false,
    error = false,
    helperText,
    fullWidth = false,
    size = 'small',
    sx,
    minuteStep = 5,
}) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const selectedBg = theme.vars.palette.tones.rose.hoverBg;
    const focusRing = theme.vars.palette.tones.rose.ring;

    const anchorRef = useRef(null);
    const hourColRef = useRef(null);
    const minuteColRef = useRef(null);
    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);

    const selectedHour = value ? value.hour() : null;
    const selectedMinute = value ? value.minute() : null;

    const minutes = [];
    for (let m = 0; m < 60; m += minuteStep) minutes.push(m);
    if (selectedMinute != null && !minutes.includes(selectedMinute)) {
        minutes.push(selectedMinute);
        minutes.sort((a, b) => a - b);
    }

    const scrollSelectedIntoView = () => {
        hourColRef.current?.querySelector('[data-selected="true"]')?.scrollIntoView({ block: 'center' });
        minuteColRef.current?.querySelector('[data-selected="true"]')?.scrollIntoView({ block: 'center' });
    };

    useEffect(() => {
        if (!open) return undefined;
        const frame = requestAnimationFrame(scrollSelectedIntoView);
        return () => cancelAnimationFrame(frame);
    }, [open]);

    const handleOpen = () => {
        if (!disabled) setAnchorEl(anchorRef.current);
    };

    const handleClose = () => {
        setAnchorEl(null);
        onClose?.();
    };

    const emit = (hour, minute) => {
        const base = value ?? dayjs('2000-01-01T00:00:00');
        onChange?.(base.hour(hour).minute(minute).second(0).millisecond(0));
    };

    const handleSelectHour = (hour) => emit(hour, selectedMinute ?? 0);
    const handleSelectMinute = (minute) => emit(selectedHour ?? 0, minute);

    const handleNow = () => {
        const now = dayjs();
        const rounded = (Math.round(now.minute() / minuteStep) * minuteStep) % 60;
        emit(now.hour(), rounded);
    };

    const displayValue = value ? value.format('HH:mm') : '';

    const columnSx = {
        flex: 1,
        maxHeight: 220,
        overflowY: 'auto',
        px: 0.75,
        py: 0.75,
        display: 'flex',
        flexDirection: 'column',
        gap: 0.25,
        '&::-webkit-scrollbar': { width: 6 },
        '&::-webkit-scrollbar-thumb': { backgroundColor: 'divider', borderRadius: 3 },
    };

    const cellSx = (active) => ({
        flexShrink: 0,
        textAlign: 'center',
        cursor: 'pointer',
        userSelect: 'none',
        fontSize: 13.5,
        fontWeight: active ? 700 : 500,
        color: active ? '#fff' : 'text.secondary',
        backgroundColor: active ? selectedBg : 'transparent',
        borderRadius: '8px',
        py: 0.75,
        transition: 'background-color 120ms ease, color 120ms ease',
        '&:hover': { backgroundColor: active ? selectedBg : 'action.hover' },
        '&:focus-visible': {
            outline: `3px solid ${focusRing}`,
            outlineOffset: '-2px',
        },
    });

    const columnLabelSx = {
        flex: 1,
        fontSize: 11.5,
        fontWeight: 700,
        letterSpacing: '0.08em',
        textTransform: 'uppercase',
        color: accentColor,
        textAlign: 'center',
        py: 1,
    };

    const renderCell = (label2, active, onSelect) => (
        <Box
            data-selected={active ? 'true' : undefined}
            role="button"
            tabIndex={0}
            onClick={onSelect}
            onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    onSelect();
                }
            }}
            sx={cellSx(active)}
        >
            {label2}
        </Box>
    );

    return (
        <>
            <TextField
                ref={anchorRef}
                label={label}
                value={displayValue}
                placeholder="--:--"
                onClick={handleOpen}
                disabled={disabled}
                error={error}
                helperText={helperText}
                fullWidth={fullWidth}
                size={size}
                sx={sx}
                slotProps={{
                    input: {
                        readOnly: true,
                        sx: { cursor: disabled ? 'default' : 'pointer' },
                        endAdornment: (
                            <InputAdornment position="end">
                                <IconButton
                                    edge="end"
                                    size="small"
                                    disabled={disabled}
                                    onClick={(e) => { e.stopPropagation(); handleOpen(); }}
                                >
                                    <AccessTimeIcon sx={{ fontSize: 18, color: open ? accentColor : 'text.secondary' }} />
                                </IconButton>
                            </InputAdornment>
                        ),
                    },
                }}
            />

            <Popover
                open={open}
                anchorEl={anchorEl}
                onClose={handleClose}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
                transformOrigin={{ vertical: 'top', horizontal: 'left' }}
                slotProps={{
                    paper: {
                        sx: {
                            mt: 0.5,
                            width: 200,
                            borderRadius: '12px',
                            border: '1px solid',
                            borderColor: 'divider',
                            boxShadow: theme.vars.palette.tones.rose.shadowResting,
                            overflow: 'hidden',
                            bgcolor: 'background.paper',
                        },
                    },
                }}
            >
                <Box sx={{ display: 'flex', borderBottom: '1px solid', borderColor: 'divider' }}>
                    <Typography sx={columnLabelSx}>Hora</Typography>
                    <Typography sx={columnLabelSx}>Min</Typography>
                </Box>

                <Box sx={{ display: 'flex' }}>
                    <Box ref={hourColRef} sx={{ ...columnSx, borderRight: '1px solid', borderColor: 'divider' }}>
                        {HOURS.map((h) => renderCell(
                            pad(h),
                            h === selectedHour,
                            () => handleSelectHour(h),
                        ))}
                    </Box>
                    <Box ref={minuteColRef} sx={columnSx}>
                        {minutes.map((m) => renderCell(
                            pad(m),
                            m === selectedMinute,
                            () => handleSelectMinute(m),
                        ))}
                    </Box>
                </Box>

                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 1, px: 1, py: 0.75, borderTop: '1px solid', borderColor: 'divider' }}>
                    <Button
                        size="small"
                        onClick={handleNow}
                        sx={{ textTransform: 'none', fontWeight: 600, fontSize: 12.5, borderRadius: '8px', color: accentColor }}
                    >
                        Ahora
                    </Button>
                    <Button
                        size="small"
                        variant="contained"
                        onClick={handleClose}
                        sx={{
                            textTransform: 'none',
                            fontWeight: 600,
                            fontSize: 12.5,
                            borderRadius: '8px',
                            bgcolor: selectedBg,
                            boxShadow: 'none',
                            '&:hover': { bgcolor: selectedBg, boxShadow: 'none' },
                        }}
                    >
                        Listo
                    </Button>
                </Box>
            </Popover>
        </>
    );
}
