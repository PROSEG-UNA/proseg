import { MenuItem, TextField } from '@mui/material';
import { ALL_FILTER_VALUE, DRIVER_STATUS_OPTIONS, DRIVER_UNIT_OPTIONS } from '../../transportUtils.js';

export default function TourProcessingDriverFilters({ unit, status, onUnitChange, onStatusChange }) {
    return (
        <>
            <TextField
                select
                size="small"
                margin="none"
                label="Unidad"
                value={unit}
                onChange={(event) => onUnitChange(event.target.value)}
                sx={{ width: { xs: '100%', sm: 200 }, flexShrink: 0 }}
            >
                {DRIVER_UNIT_OPTIONS.map((option) => (
                    <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                ))}
                <MenuItem value={ALL_FILTER_VALUE}>Todas</MenuItem>
            </TextField>
            <TextField
                select
                size="small"
                margin="none"
                label="Estado"
                value={status}
                onChange={(event) => onStatusChange(event.target.value)}
                sx={{ width: { xs: '100%', sm: 140 }, flexShrink: 0 }}
            >
                {DRIVER_STATUS_OPTIONS.map((option) => (
                    <MenuItem key={option.value} value={option.value}>{option.label}</MenuItem>
                ))}
                <MenuItem value={ALL_FILTER_VALUE}>Todos</MenuItem>
            </TextField>
        </>
    );
}
