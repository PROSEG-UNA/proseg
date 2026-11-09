import { useEffect, useMemo, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Autocomplete, Box, Button, Chip, InputAdornment, TextField, Typography, createFilterOptions, useTheme } from '@mui/material';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';
import { queryKeys } from '../../../../common/query';
import { useTourProcessingCatalogMatch } from '../../hooks/useTourProcessingCatalogMatch.js';
import { toDriverOption, toVehicleOption } from '../../hooks/useTransportOptions.js';
import DriverFormModal from '../drivers/DriverFormModal.jsx';
import VehicleFormModal from '../vehicles/VehicleFormModal.jsx';
import {
    GENERAL_SERVICES_UNIT,
    OTHER_UNIT,
    canAddToCatalog,
    catalogEntryLabel,
    catalogEntryText,
    driverMatchKey,
    missingCatalogMessage,
} from './tourProcessingCatalogMatch.js';
import MissingCatalogIcon from './MissingCatalogIcon.jsx';

const KIND_CONFIG = {
    driver: {
        label: 'Chofer',
        entity: 'chofer',
        FormModal: DriverFormModal,
        toFormValues: (value) => ({ name: value }),
        savedValue: (saved) => saved?.name,
        cacheKey: queryKeys.transport.drivers,
        optionsKey: queryKeys.transport.driverOptions,
        toOption: toDriverOption,
    },
    vehicle: {
        label: 'Vehículo',
        entity: 'vehículo',
        FormModal: VehicleFormModal,
        toFormValues: (value) => ({ plate: value }),
        savedValue: (saved) => saved?.plate,
        cacheKey: queryKeys.transport.vehicles,
        optionsKey: queryKeys.transport.vehicleOptions,
        toOption: toVehicleOption,
    },
};

const filterCatalogEntries = createFilterOptions({ stringify: catalogEntryText });
const UNLISTED_DRIVER_MESSAGE = 'El chofer no existe en el sistema. Lo puede dejar así o lo puede ingresar al sistema tocando este botón.';

function hasCatalogMatch(value, options) {
    const key = driverMatchKey(value);
    return key !== '' && options.some((option) => driverMatchKey(option.label) === key);
}

function isSameEntry(option, value) {
    return catalogEntryLabel(option) === catalogEntryLabel(value);
}

function renderCatalogOption({ key, ...props }, option) {
    return (
        <Box component="li" key={key} {...props} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography sx={{ fontSize: 14, fontWeight: 600, lineHeight: 1.3 }}>{catalogEntryLabel(option)}</Typography>
                {option.secondary && (
                    <Typography sx={{ fontSize: 12, color: 'text.secondary', lineHeight: 1.3 }}>{option.secondary}</Typography>
                )}
            </Box>
            {option.badge && <Chip size="small" variant="outlined" color={option.badge.color} label={option.badge.label} />}
        </Box>
    );
}

export default function TourProcessingCatalogField({ kind, value, onChange, onBlur, isEdit, disabled, errorText, generalServicesOnly = false, otherUnitOnly = false, sx }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const queryClient = useQueryClient();
    const catalog = useTourProcessingCatalogMatch()[kind];
    const config = KIND_CONFIG[kind];
    const inputRef = useRef(null);
    const inputValueRef = useRef(value || '');
    const pickedRef = useRef(false);
    const [addOpen, setAddOpen] = useState(false);
    const [revising, setRevising] = useState(false);

    useEffect(() => {
        inputValueRef.current = value || '';
    }, [value]);

    const missing = !otherUnitOnly && catalog.isMissing(value);
    const options = useMemo(() => {
        if (generalServicesOnly) {
            return catalog.options.filter((option) => (option.unit ?? GENERAL_SERVICES_UNIT) === GENERAL_SERVICES_UNIT);
        }
        if (otherUnitOnly) {
            return catalog.options.filter((option) => option.unit === OTHER_UNIT);
        }
        return catalog.options;
    }, [catalog.options, generalServicesOnly, otherUnitOnly]);
    const unlisted = otherUnitOnly && !revising && Boolean(String(value || '').trim()) && !hasCatalogMatch(value, options);
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

    const handleAdded = (saved) => {
        if (saved?.id) {
            queryClient.setQueryData(config.optionsKey(), (current) => (current
                ? [...current.filter((option) => option.id !== saved.id), config.toOption(saved)]
                : current));
        }
        const savedName = config.savedValue(saved) || value;
        inputValueRef.current = savedName;
        setRevising(false);
        onChange(savedName);
        void queryClient.invalidateQueries({ queryKey: config.cacheKey() });
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessingDrafts() });
    };

    const handleChange = (_, next, reason) => {
        if (otherUnitOnly && reason === 'blur') return;
        const label = catalogEntryLabel(next);
        if (otherUnitOnly && reason === 'selectOption') {
            pickedRef.current = true;
            setRevising(false);
        }
        inputValueRef.current = label;
        onChange(label);
    };

    const handleInputChange = (_, next, reason) => {
        if (!otherUnitOnly) return;
        if (reason === 'input' || reason === 'clear') {
            inputValueRef.current = next;
        }
    };

    const handleBlur = (event) => {
        if (otherUnitOnly && !pickedRef.current) {
            setRevising(false);
            onChange(inputValueRef.current);
        }
        pickedRef.current = false;
        onBlur?.(event);
    };

    const actionSx = {
        textTransform: 'none',
        fontWeight: 600,
        fontSize: 12.5,
        color: 'text.secondary',
        px: 0.75,
        '&:hover': { color: accentColor, bgcolor: 'action.hover' },
    };

    const formInitialValues = otherUnitOnly
        ? { ...config.toFormValues(value), organizationalUnit: OTHER_UNIT }
        : config.toFormValues(value);

    return (
        <Box>
            {unlisted ? (
                <Box>
                    <TextField
                        label={config.label}
                        value={value || ''}
                        fullWidth
                        size="small"
                        slotProps={{ input: { readOnly: true } }}
                        sx={sx}
                    />
                    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 0.25, mt: 0.5 }}>
                        <Typography sx={{ fontSize: 12.5, fontWeight: 600, lineHeight: 1.4 }}>
                            {UNLISTED_DRIVER_MESSAGE}
                        </Typography>
                        {catalog.canAdd && (
                            <Button
                                size="small"
                                disabled={disabled}
                                startIcon={<AddCircleOutlineOutlinedIcon />}
                                onClick={() => setAddOpen(true)}
                                sx={actionSx}
                            >
                                Ingresar al sistema
                            </Button>
                        )}
                        <Button
                            size="small"
                            disabled={disabled}
                            onClick={() => {
                                setRevising(true);
                                inputValueRef.current = value || '';
                            }}
                            sx={actionSx}
                        >
                            Corregir el nombre
                        </Button>
                    </Box>
                </Box>
            ) : (
            <Autocomplete
                freeSolo
                autoSelect={!otherUnitOnly}
                autoHighlight={false}
                openOnFocus
                forcePopupIcon
                size="small"
                disabled={disabled}
                options={options}
                value={value || null}
                getOptionLabel={catalogEntryLabel}
                isOptionEqualToValue={isSameEntry}
                filterOptions={filterCatalogEntries}
                renderOption={renderCatalogOption}
                slotProps={{ listbox: { sx: { maxHeight: 320 } } }}
                onChange={handleChange}
                onInputChange={handleInputChange}
                onBlur={handleBlur}
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
            )}

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
                    initialValues={formInitialValues}
                    onClose={() => setAddOpen(false)}
                    onSaved={handleAdded}
                />
            )}
        </Box>
    );
}
