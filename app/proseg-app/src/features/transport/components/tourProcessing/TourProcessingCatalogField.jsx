import { useMemo, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Autocomplete, Box, Button, Chip, InputAdornment, Paper, Tab, Tabs, TextField, Typography, createFilterOptions, useTheme } from '@mui/material';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';
import { queryKeys } from '../../../../common/query';
import { useTourProcessingCatalogMatch } from '../../hooks/useTourProcessingCatalogMatch.js';
import { toDriverOption, toVehicleOption } from '../../hooks/useTransportOptions.js';
import DriverFormModal from '../drivers/DriverFormModal.jsx';
import VehicleFormModal from '../vehicles/VehicleFormModal.jsx';
import {
    GENERAL_SERVICES_UNIT,
    canAddToCatalog,
    catalogEntryLabel,
    catalogEntryText,
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

const OTHER_UNIT_TAB = 'OTHER';

const filterCatalogEntries = createFilterOptions({ stringify: catalogEntryText });

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

function UnitTabsPaper({ children, unitTab, onUnitTabChange, ...paperProps }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const tabSx = {
        textTransform: 'none',
        fontWeight: 700,
        minHeight: 44,
        fontSize: 14,
        color: 'text.primary',
        opacity: 0.75,
        '&.Mui-selected': { color: 'text.primary', opacity: 1 },
    };
    return (
        <Paper {...paperProps}>
            <Tabs
                value={unitTab}
                onChange={(_, next) => onUnitTabChange(next)}
                onMouseDown={(event) => event.preventDefault()}
                variant="fullWidth"
                aria-label="Unidad del chofer"
                sx={{
                    minHeight: 44,
                    bgcolor: 'background.paper',
                    borderBottom: 1,
                    borderColor: 'divider',
                    boxShadow: '0 2px 6px rgba(0, 0, 0, 0.08)',
                    '& .MuiTabs-indicator': { bgcolor: accentColor },
                    position: 'relative',
                    zIndex: 1,
                }}
            >
                <Tab value={GENERAL_SERVICES_UNIT} label="Servicios Generales" sx={tabSx} />
                <Tab value={OTHER_UNIT_TAB} label="Otra Unidad" sx={tabSx} />
            </Tabs>
            {children}
        </Paper>
    );
}

export default function TourProcessingCatalogField({ kind, value, onChange, onBlur, isEdit, disabled, errorText, generalServicesOnly = false, unitTabs = false, sx }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const queryClient = useQueryClient();
    const catalog = useTourProcessingCatalogMatch()[kind];
    const config = KIND_CONFIG[kind];
    const inputRef = useRef(null);
    const [addOpen, setAddOpen] = useState(false);

    const missing = catalog.isMissing(value);
    const [unitTab, setUnitTab] = useState(GENERAL_SERVICES_UNIT);
    const [popupOpen, setPopupOpen] = useState(false);
    const options = useMemo(() => {
        const isGeneralServices = (option) => (option.unit ?? GENERAL_SERVICES_UNIT) === GENERAL_SERVICES_UNIT;
        if (generalServicesOnly) return catalog.options.filter(isGeneralServices);
        if (unitTabs) {
            return catalog.options.filter((option) => isGeneralServices(option) === (unitTab === GENERAL_SERVICES_UNIT));
        }
        return catalog.options;
    }, [catalog.options, generalServicesOnly, unitTabs, unitTab]);
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
        onChange(config.savedValue(saved) || value);
        void queryClient.invalidateQueries({ queryKey: config.cacheKey() });
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessingDrafts() });
    };

    const actionSx = {
        textTransform: 'none',
        fontWeight: 700,
        fontSize: 12.5,
        borderRadius: '8px',
        px: 1.5,
        boxShadow: 'none',
    };
    const primaryActionSx = {
        ...actionSx,
        bgcolor: accentColor,
        color: '#fff',
        '&:hover': { bgcolor: accentColor, filter: 'brightness(0.92)', boxShadow: 'none' },
    };
    const secondaryActionSx = {
        ...actionSx,
        color: accentColor,
        borderColor: `color-mix(in srgb, ${accentColor} 45%, transparent)`,
        '&:hover': { borderColor: accentColor, bgcolor: `color-mix(in srgb, ${accentColor} 8%, transparent)` },
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
                options={options}
                value={value || null}
                getOptionLabel={catalogEntryLabel}
                isOptionEqualToValue={isSameEntry}
                filterOptions={filterCatalogEntries}
                renderOption={renderCatalogOption}
                slots={unitTabs ? { paper: UnitTabsPaper } : undefined}
                slotProps={{
                    listbox: { sx: { maxHeight: 320 } },
                    ...(unitTabs ? { paper: { unitTab, onUnitTabChange: setUnitTab } } : {}),
                }}
                open={popupOpen}
                onOpen={() => setPopupOpen(true)}
                onClose={() => setPopupOpen(false)}
                onChange={(_, next) => {
                    setPopupOpen(false);
                    onChange(catalogEntryLabel(next));
                }}
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
                    <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, mb: 1 }}>
                        <WarningAmberOutlinedIcon color="warning" fontSize="small" />
                        <Typography sx={{ fontSize: 13, fontWeight: 600 }}>
                            {missingCatalogMessage(kind, value)}
                        </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                        {catalog.canAdd && canAddToCatalog(kind, value) && (
                            <Button
                                size="small"
                                variant="contained"
                                disableElevation
                                disabled={disabled}
                                startIcon={<AddCircleOutlineOutlinedIcon />}
                                onClick={() => setAddOpen(true)}
                                sx={primaryActionSx}
                            >
                                {`Agregar ${config.entity} al sistema`}
                            </Button>
                        )}
                        <Button
                            size="small"
                            variant="outlined"
                            disabled={disabled}
                            startIcon={<SwapHorizOutlinedIcon />}
                            onClick={() => inputRef.current?.focus()}
                            sx={secondaryActionSx}
                        >
                            {`Seleccionar otro ${config.entity}`}
                        </Button>
                    </Box>
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
