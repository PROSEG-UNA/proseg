import { useState } from 'react';
import { Box, IconButton, useTheme } from '@mui/material';
import ClearIcon from '@mui/icons-material/Clear';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { esES } from '@mui/x-date-pickers/locales';
import 'dayjs/locale/es';

const PICKER_LOCALE_TEXT = esES.components.MuiLocalizationProvider.defaultProps.localeText;

export default function MonthPicker({
    label = 'Mes',
    ariaLabel = 'Elegir el mes',
    value,
    onChange,
    disabled = false,
    clearable = false,
    required = false,
}) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const [pickerOpen, setPickerOpen] = useState(false);

    const openPicker = (event) => {
        if (disabled || !event.currentTarget.contains(event.target)) return;
        setPickerOpen(true);
    };

    return (
        <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="es" localeText={PICKER_LOCALE_TEXT}>
            <Box
                role="button"
                tabIndex={disabled ? -1 : 0}
                aria-haspopup="dialog"
                aria-expanded={pickerOpen}
                aria-label={ariaLabel}
                aria-disabled={disabled}
                onClick={openPicker}
                onKeyDown={(event) => {
                    if (event.key !== 'Enter' && event.key !== ' ') return;
                    if (!event.currentTarget.contains(event.target)) return;
                    event.preventDefault();
                    openPicker(event);
                }}
                sx={{
                    position: 'relative',
                    borderRadius: 1,
                    cursor: disabled ? 'default' : 'pointer',
                    outline: 'none',
                    '&:focus-visible': { boxShadow: `0 0 0 3px color-mix(in srgb, ${accentColor} 30%, transparent)` },
                    '&:hover .MuiPickersOutlinedInput-notchedOutline': disabled ? {} : { borderColor: accentColor },
                    '& .MuiPickersOutlinedInput-notchedOutline': pickerOpen ? { borderColor: accentColor, borderWidth: 2 } : {},
                }}
            >
                <Box inert sx={{ pointerEvents: 'none', userSelect: 'none' }}>
                    <DatePicker
                        label={label}
                        views={['year', 'month']}
                        openTo="month"
                        monthsPerRow={4}
                        format="MMMM YYYY"
                        value={value}
                        onChange={(nextValue) => onChange(nextValue?.isValid() ? nextValue : null)}
                        open={pickerOpen}
                        onOpen={() => setPickerOpen(true)}
                        onClose={() => setPickerOpen(false)}
                        disabled={disabled}
                        slotProps={{
                            field: { readOnly: true },
                            textField: {
                                size: 'small',
                                fullWidth: true,
                                required,
                                sx: clearable && value ? { '& .MuiOutlinedInput-input, & .MuiPickersOutlinedInput-input': { pr: '30px' } } : undefined,
                            },
                            popper: { placement: 'bottom' },
                            layout: {
                                sx: {
                                    '& .MuiDateCalendar-root': { height: 'auto' },
                                    '& .MuiMonthCalendar-root': { paddingBottom: '17px' },
                                },
                            },
                        }}
                    />
                </Box>
                {clearable && value && !disabled && (
                    <IconButton
                        size="small"
                        aria-label="Quitar mes seleccionado"
                        onClick={(event) => {
                            event.stopPropagation();
                            onChange(null);
                        }}
                        sx={{ position: 'absolute', top: '50%', right: 34, transform: 'translateY(-50%)', p: 0.5 }}
                    >
                        <ClearIcon sx={{ fontSize: 16 }} />
                    </IconButton>
                )}
            </Box>
        </LocalizationProvider>
    );
}
