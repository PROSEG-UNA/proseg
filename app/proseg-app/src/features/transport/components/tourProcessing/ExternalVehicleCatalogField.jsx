import { useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Autocomplete, Box, Button, Chip, TextField, Typography, createFilterOptions, useTheme } from '@mui/material';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import { queryKeys } from '../../../../common/query';
import { toExternalVehicleOption } from '../../hooks/useTransportOptions.js';
import ExternalVehicleFormModal from '../externalHiring/ExternalVehicleFormModal.jsx';
import {
    buildExternalVehicleCatalogEntry,
    catalogEntryLabel,
    catalogEntryText,
    looksLikePlate,
    resolveExternalVehicle,
} from './tourProcessingCatalogMatch.js';

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
        </Box>
    );
}

export default function ExternalVehicleCatalogField({
    provider = '', plate = '', onChange, options, loading, loaded, isEdit, disabled, errorText, sx,
}) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const queryClient = useQueryClient();
    const { hasPermission } = usePermissions();
    const [addOpen, setAddOpen] = useState(false);
    const [popupOpen, setPopupOpen] = useState(false);
    const canAdd = hasPermission(PERMISSIONS.TRANSPORT.VEHICLES.MANAGE);

    const entries = useMemo(
        () => options.filter((option) => option.available !== false).map(buildExternalVehicleCatalogEntry),
        [options],
    );
    const selected = resolveExternalVehicle(plate, options);
    const unavailable = selected?.available === false;
    const missing = Boolean(loaded) && !loading && plate.trim() !== '' && !selected;
    const addable = missing && looksLikePlate(plate);
    const providerText = provider ? `Proveedor: ${provider}` : ' ';

    if (!isEdit) {
        return (
            <Box sx={{ gridColumn: '1 / -1', display: 'flex', alignItems: 'flex-start', gap: 1 }}>
                <TextField
                    label="Placa"
                    value={plate || '—'}
                    helperText={provider ? providerText : undefined}
                    fullWidth
                    size="small"
                    slotProps={{ input: { readOnly: true } }}
                    sx={{ ...sx, flex: 1 }}
                />
                {unavailable && <Chip label="No disponible" size="small" color="error" variant="outlined" sx={{ mt: 1, flexShrink: 0 }} />}
            </Box>
        );
    }

    const handlePick = (next) => {
        const text = catalogEntryLabel(next).trim();
        const match = resolveExternalVehicle(text, options);
        if (match) {
            onChange({ driver: match.provider, vehicle: match.label });
            return;
        }
        onChange({ driver: '', vehicle: text });
    };

    const handleAdded = (saved) => {
        if (saved?.id) {
            queryClient.setQueryData(queryKeys.transport.externalVehicleOptions(), (current) => (current
                ? [...current.filter((option) => option.id !== saved.id), toExternalVehicleOption(saved)]
                : current));
            onChange({ driver: saved.provider ?? '', vehicle: saved.plate ?? plate });
        }
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.externalVehicles() });
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessingDrafts() });
    };

    return (
        <Box sx={{ gridColumn: '1 / -1' }}>
            <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Autocomplete
                        freeSolo
                        autoSelect
                        openOnFocus
                        forcePopupIcon
                        size="small"
                        disabled={disabled}
                        loading={loading}
                        options={entries}
                        value={plate || null}
                        getOptionLabel={catalogEntryLabel}
                        isOptionEqualToValue={isSameEntry}
                        filterOptions={filterCatalogEntries}
                        renderOption={renderCatalogOption}
                        slotProps={{ listbox: { sx: { maxHeight: 320 } } }}
                        open={popupOpen}
                        onOpen={() => setPopupOpen(true)}
                        onClose={() => setPopupOpen(false)}
                        onChange={(_, next) => {
                            setPopupOpen(false);
                            handlePick(next);
                        }}
                        renderInput={(params) => (
                            <TextField
                                {...params}
                                label="Placa"
                                error={missing || Boolean(errorText)}
                                helperText={errorText || providerText}
                                sx={sx}
                            />
                        )}
                    />
                </Box>
                {unavailable && <Chip label="No disponible" size="small" color="error" variant="outlined" sx={{ mt: 1, flexShrink: 0 }} />}
            </Box>
            {missing && (
                <Box
                    role="group"
                    aria-label="Opciones para la placa que no está en Contratación Externa"
                    sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 0.25 }}
                >
                    <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1, mb: 1 }}>
                        <WarningAmberOutlinedIcon color="warning" fontSize="small" />
                        <Typography sx={{ fontSize: 13, fontWeight: 600 }}>
                            {addable
                                ? 'Esta placa no se encuentra en Contratación Externa, agrégala antes de continuar'
                                : 'Este valor no es una placa, selecciona un vehículo de Contratación Externa o deja el campo vacío'}
                        </Typography>
                    </Box>
                    {addable && canAdd && (
                        <Button
                            size="small"
                            variant="contained"
                            disableElevation
                            disabled={disabled}
                            startIcon={<AddCircleOutlineOutlinedIcon />}
                            onClick={() => setAddOpen(true)}
                            sx={{
                                textTransform: 'none',
                                fontWeight: 700,
                                fontSize: 12.5,
                                borderRadius: '8px',
                                px: 1.5,
                                bgcolor: accentColor,
                                color: '#fff',
                                boxShadow: 'none',
                                '&:hover': { bgcolor: accentColor, filter: 'brightness(0.92)', boxShadow: 'none' },
                            }}
                        >
                            Agregar vehículo externo al sistema
                        </Button>
                    )}
                </Box>
            )}
            {addOpen && (
                <ExternalVehicleFormModal
                    open
                    initialValues={{ plate: plate.trim() }}
                    onClose={() => setAddOpen(false)}
                    onSaved={handleAdded}
                />
            )}
        </Box>
    );
}
