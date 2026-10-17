import { useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Autocomplete, Box, Button, InputAdornment, TextField, Typography, useTheme } from '@mui/material';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';
import { queryKeys } from '../../../../common/query';
import { useCleaningCatalogMatch } from '../../hooks/useCleaningCatalogMatch.js';
import DriverFormModal from '../drivers/DriverFormModal.jsx';
import VehicleFormModal from '../vehicles/VehicleFormModal.jsx';
import { canAddToCatalog, missingCatalogMessage } from './cleaningCatalogMatch.js';
import MissingCatalogIcon from './MissingCatalogIcon.jsx';

const KIND_CONFIG = {
    driver: {
        label: 'Chofer',
        entity: 'chofer',
        FormModal: DriverFormModal,
        toFormValues: (value) => ({ name: value }),
        savedValue: (saved) => saved?.name,
        cacheKey: queryKeys.transport.drivers,
    },
    vehicle: {
        label: 'Vehículo',
        entity: 'vehículo',
        FormModal: VehicleFormModal,
        toFormValues: (value) => ({ plate: value }),
        savedValue: (saved) => saved?.plate,
        cacheKey: queryKeys.transport.vehicles,
    },
};

export default function CleaningCatalogField({ kind, value, onChange, onBlur, isEdit, disabled, errorText, sx }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const queryClient = useQueryClient();
    const catalog = useCleaningCatalogMatch()[kind];
    const config = KIND_CONFIG[kind];
    const inputRef = useRef(null);
    const [addOpen, setAddOpen] = useState(false);

    const missing = catalog.isMissing(value);
    const FormModal = config.FormModal;

    if (!isEdit) {
        return (
            <TextField
                label={config.label}
                value={value || '—'}
                fullWidth
                size="small"
                slotProps={{
                    input: {
                        readOnly: true,
                        endAdornment: missing ? (
                            <InputAdornment position="end">
                                <MissingCatalogIcon kind={kind} value={value} />
                            </InputAdornment>
                        ) : undefined,
                    },
                }}
                sx={sx}
            />
        );
    }

    const handleAdded = async (saved) => {
        await queryClient.invalidateQueries({ queryKey: config.cacheKey() });
        onChange(config.savedValue(saved) || value);
    };

    const actionSx = {
        textTransform: 'none',
        fontWeight: 600,
        fontSize: 12.5,
        color: 'text.secondary',
        px: 0.75,
        '&:hover': { color: accentColor, bgcolor: 'action.hover' },
    };

    return (
        <Box>
            <Autocomplete
                freeSolo
                autoSelect
                openOnFocus
                forcePopupIcon
                size="small"
                disabled={disabled}
                options={catalog.options}
                value={value || null}
                onChange={(_, next) => onChange(next ?? '')}
                onBlur={onBlur}
                renderInput={(params) => (
                    <TextField
                        {...params}
                        label={config.label}
                        inputRef={inputRef}
                        error={missing || Boolean(errorText)}
                        helperText={errorText || ' '}
                        sx={sx}
                    />
                )}
            />

            {missing && (
                <Box
                    role="group"
                    aria-label={`Opciones para el ${config.entity} que no está en el sistema`}
                    sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 0.25 }}
                >
                    <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 0.75, color: 'error.main', mb: 0.5 }}>
                        <ErrorOutlineOutlinedIcon sx={{ fontSize: 16, mt: '2px', flexShrink: 0 }} />
                        <Typography sx={{ fontSize: 12.5, fontWeight: 600, lineHeight: 1.4 }}>
                            {missingCatalogMessage(kind, value)}
                        </Typography>
                    </Box>
                    {catalog.canAdd && canAddToCatalog(kind, value) && (
                        <Button
                            size="small"
                            disabled={disabled}
                            startIcon={<AddCircleOutlineOutlinedIcon />}
                            onClick={() => setAddOpen(true)}
                            sx={actionSx}
                        >
                            {`Agregar ${config.entity} al sistema`}
                        </Button>
                    )}
                    <Button
                        size="small"
                        disabled={disabled}
                        startIcon={<SwapHorizOutlinedIcon />}
                        onClick={() => inputRef.current?.focus()}
                        sx={actionSx}
                    >
                        {`Seleccionar otro ${config.entity}`}
                    </Button>
                </Box>
            )}

            {addOpen && (
                <FormModal
                    open
                    initialValues={config.toFormValues(value)}
                    onClose={() => setAddOpen(false)}
                    onSaved={handleAdded}
                />
            )}
        </Box>
    );
}
