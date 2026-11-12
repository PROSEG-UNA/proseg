import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Autocomplete, Box, Button, TextField, Typography, useTheme } from '@mui/material';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import { queryKeys } from '../../../../common/query';
import BorrowedVehicleFormModal from '../vehicles/BorrowedVehicleFormModal.jsx';
import { plateMatchKey } from './tourProcessingCatalogMatch.js';

export default function BorrowedVehicleCatalogField({ value, plate = '', onChange, onBlur, options, loading, disabled, errorText, required = false, sx }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const queryClient = useQueryClient();
    const [text, setText] = useState(plate);
    const [addOpen, setAddOpen] = useState(false);
    const selected = options.find((option) => option.id === value) ?? null;
    const inputValue = selected?.label ?? text;
    const missing = !loading && !value && inputValue.trim() && !options.some((option) => plateMatchKey(option.label) === plateMatchKey(inputValue));

    const handleAdded = (saved) => {
        if (saved?.id) {
            queryClient.setQueryData(queryKeys.transport.borrowedVehicleOptions(), (current) => [
                ...(current ?? []).filter((option) => option.id !== saved.id),
                { id: saved.id, label: saved.plate, capacity: saved.capacity ?? null },
            ]);
            setText(saved.plate);
            onChange(saved.id);
        }
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.borrowedVehicles() });
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessingDrafts() });
    };

    return (
        <Box>
            <Autocomplete
                freeSolo
                autoSelect
                openOnFocus
                forcePopupIcon
                options={options}
                loading={loading}
                value={selected}
                inputValue={inputValue}
                onInputChange={(_, next, reason) => {
                    if (reason !== 'input' && reason !== 'clear') return;
                    setText(next);
                    if (value) onChange('');
                }}
                onChange={(_, next) => {
                    const option = typeof next === 'string' ? options.find((entry) => plateMatchKey(entry.label) === plateMatchKey(next)) : next;
                    setText(option?.label ?? (typeof next === 'string' ? next : ''));
                    onChange(option?.id ?? '');
                }}
                getOptionLabel={(option) => typeof option === 'string' ? option : option.label ?? ''}
                isOptionEqualToValue={(option, current) => option.id === current.id}
                disabled={disabled}
                size="small"
                renderInput={(params) => (
                    <TextField
                        {...params}
                        label="Vehículo prestado"
                        required={required}
                        error={Boolean(missing || errorText)}
                        helperText={errorText || ' '}
                        onBlur={onBlur}
                        sx={sx}
                    />
                )}
            />
            {missing && (
                <Box role="group" aria-label="Opciones para el vehículo prestado que no está en el sistema" sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 0.25 }}>
                    <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, mb: 1 }}>
                        <WarningAmberOutlinedIcon color="warning" fontSize="small" />
                        <Typography sx={{ fontSize: 13, fontWeight: 600 }}>
                            Este vehículo prestado no se encuentra en el sistema, agrégalo antes de continuar
                        </Typography>
                    </Box>
                    <Button
                        size="small"
                        variant="contained"
                        disableElevation
                        disabled={disabled}
                        startIcon={<AddCircleOutlineOutlinedIcon />}
                        onClick={() => setAddOpen(true)}
                        sx={{ textTransform: 'none', fontWeight: 700, fontSize: 12.5, borderRadius: '8px', px: 1.5, bgcolor: accentColor, color: '#fff', boxShadow: 'none', '&:hover': { bgcolor: accentColor, filter: 'brightness(0.92)', boxShadow: 'none' } }}
                    >
                        Agregar vehículo al sistema
                    </Button>
                </Box>
            )}
            {addOpen && (
                <BorrowedVehicleFormModal
                    open
                    initialValues={{ plate: inputValue }}
                    onClose={() => setAddOpen(false)}
                    onSaved={handleAdded}
                />
            )}
        </Box>
    );
}
