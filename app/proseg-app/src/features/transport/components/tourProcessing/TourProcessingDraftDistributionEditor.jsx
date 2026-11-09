import { useState } from 'react';
import { Autocomplete, Box, Button, Chip, TextField, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import TourProcessingCatalogField from './TourProcessingCatalogField.jsx';
import {
    TRIP_MODES, TRIP_MODE_HINTS, TRIP_MODE_LABELS, buildTrips, detectTripMode, minimumTrips,
} from './tripDistribution.js';

const RESOURCE_TYPES = [
    { value: 'INTERNAL', label: 'Interno' },
    { value: 'BORROWED_VEHICLE', label: 'Prestado' },
    { value: 'EXTERNAL', label: 'Externo' },
];

export default function TourProcessingDraftDistributionEditor({
    assignments, totalPassengers, onChange, isEdit, saving, fieldSx, borrowedVehicles, error,
    vehicleOptions,
}) {
    const [mode, setMode] = useState(() => detectTripMode(assignments));
    const [tripCount, setTripCount] = useState(() => String(assignments.length));
    const [driverCount, setDriverCount] = useState(() => String(Math.max(2, new Set(assignments.map((assignment) => assignment.driver?.trim().toLowerCase()).filter(Boolean)).size)));
    const totalAssigned = assignments.reduce((sum, assignment) => sum + Number(assignment.passengers || 0), 0);
    const remaining = Number(totalPassengers || 0) - totalAssigned;

    const capacityOfResource = (resource) => {
        if (resource.resourceType === 'BORROWED_VEHICLE') {
            return borrowedVehicles.options.find((option) => option.id === resource.borrowedVehicleId)?.capacity;
        }
        if (resource.resourceType === 'INTERNAL' || !resource.resourceType) {
            return vehicleOptions.find((option) => option.label.toUpperCase() === resource.vehicle?.trim().toUpperCase())?.capacity;
        }
        return null;
    };
    const capacities = assignments.map(capacityOfResource);
    const totalCapacity = capacities.every((capacity) => capacity != null) ? capacities.reduce((sum, capacity) => sum + capacity, 0) : null;
    const requiredTrips = minimumTrips(totalPassengers, capacityOfResource(assignments[0] ?? {}));

    const distribute = () => onChange(buildTrips({
        mode, passengers: totalPassengers, tripCount, driverCount, assignments, capacityOf: capacityOfResource,
    }));

    const changeAssignment = (index, key, value) => onChange(assignments.map((assignment, currentIndex) => (
        currentIndex === index ? { ...assignment, [key]: value } : assignment
    )));

    const changeResourceType = (index, type) => onChange(assignments.map((assignment, currentIndex) => (
        currentIndex === index ? { ...assignment, resourceType: type, vehicle: '', borrowedVehicleId: '' } : assignment
    )));

    const removeAssignment = (index) => {
        setTripCount(String(assignments.length - 1));
        onChange(assignments.filter((_, currentIndex) => currentIndex !== index));
    };

    const addAssignment = () => {
        setTripCount(String(assignments.length + 1));
        onChange([...assignments, {
            passengers: '', driver: '', vehicle: '', resourceType: 'INTERNAL', borrowedVehicleId: '',
        }]);
    };

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1, flexWrap: 'wrap' }}>
                <Typography sx={{ fontSize: 13.5, fontWeight: 700 }}>Distribución de pasajeros</Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                    {totalCapacity != null && (
                        <Chip size="small" variant="outlined" label={`${assignments.length} viajes · ${totalCapacity} espacios`} />
                    )}
                    <Chip
                        size="small"
                        color={remaining === 0 ? 'success' : 'warning'}
                        variant="outlined"
                        label={`${totalAssigned} de ${totalPassengers || 0} asignados`}
                    />
                </Box>
            </Box>
            {isEdit && (
                <Box sx={{ border: '1px dashed', borderColor: 'divider', borderRadius: 2, p: 2, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                    <Typography sx={{ fontSize: 13, fontWeight: 700 }}>Cómo repartir la gira</Typography>
                    <ToggleButtonGroup
                        value={mode}
                        exclusive
                        size="small"
                        disabled={saving}
                        onChange={(_, nextMode) => nextMode && setMode(nextMode)}
                        aria-label="Manera de repartir la gira"
                    >
                        {Object.values(TRIP_MODES).map((value) => (
                            <ToggleButton key={value} value={value} sx={{ textTransform: 'none', fontSize: 12 }}>
                                {TRIP_MODE_LABELS[value]}
                            </ToggleButton>
                        ))}
                    </ToggleButtonGroup>
                    <Typography sx={{ fontSize: 12.5, color: 'text.secondary' }}>{TRIP_MODE_HINTS[mode]}</Typography>
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: mode === TRIP_MODES.SHARED ? '180px 180px auto' : '180px auto' }, gap: 1.5, alignItems: 'start' }}>
                        <TextField
                            label="Cantidad de viajes"
                            value={tripCount}
                            onChange={(event) => setTripCount(event.target.value.replace(/[^0-9]/g, ''))}
                            disabled={saving}
                            size="small"
                            slotProps={{ htmlInput: { inputMode: 'numeric' } }}
                            helperText={requiredTrips != null && mode !== TRIP_MODES.DISTINCT ? `Mínimo ${requiredTrips} con ${capacityOfResource(assignments[0])} espacios` : ' '}
                            sx={fieldSx}
                        />
                        {mode === TRIP_MODES.SHARED && (
                            <TextField
                                label="Cantidad de choferes"
                                value={driverCount}
                                onChange={(event) => setDriverCount(event.target.value.replace(/[^0-9]/g, ''))}
                                disabled={saving}
                                size="small"
                                slotProps={{ htmlInput: { inputMode: 'numeric' } }}
                                helperText=" "
                                sx={fieldSx}
                            />
                        )}
                        <Button onClick={distribute} disabled={saving} variant="outlined" size="small" sx={{ textTransform: 'none', fontWeight: 700, mt: 0.25 }}>
                            Repartir pasajeros
                        </Button>
                    </Box>
                </Box>
            )}
            {assignments.map((assignment, index) => {
                const capacity = capacityOfResource(assignment);
                return (
                <Box key={index} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, p: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography sx={{ fontSize: 13, fontWeight: 700 }}>Viaje {index + 1}</Typography>
                            {capacity != null && (
                                <Chip
                                    size="small"
                                    variant="outlined"
                                    color={Number(assignment.passengers) > capacity ? 'error' : 'default'}
                                    label={`${capacity} espacios`}
                                />
                            )}
                        </Box>
                        {isEdit && assignments.length > 2 && (
                            <Button
                                size="small"
                                color="error"
                                disabled={saving}
                                startIcon={<DeleteOutlineOutlinedIcon />}
                                onClick={() => removeAssignment(index)}
                                sx={{ textTransform: 'none' }}
                            >
                                Quitar
                            </Button>
                        )}
                    </Box>
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '180px 1fr' }, gap: 1.5, mb: 1.5 }}>
                        <TextField
                            label="Pasajeros"
                            value={assignment.passengers}
                            onChange={(event) => changeAssignment(index, 'passengers', event.target.value.replace(/[^0-9]/g, ''))}
                            disabled={saving}
                            size="small"
                            fullWidth
                            slotProps={{ input: { readOnly: !isEdit }, htmlInput: { inputMode: 'numeric' } }}
                            sx={fieldSx}
                        />
                        {isEdit ? (
                            <ToggleButtonGroup
                                value={assignment.resourceType || 'INTERNAL'}
                                exclusive
                                size="small"
                                disabled={saving}
                                onChange={(_, type) => type && changeResourceType(index, type)}
                            >
                                {RESOURCE_TYPES.map((type) => (
                                    <ToggleButton key={type.value} value={type.value} sx={{ textTransform: 'none', fontSize: 12 }}>
                                        {type.label}
                                    </ToggleButton>
                                ))}
                            </ToggleButtonGroup>
                        ) : (
                            <Chip
                                size="small"
                                variant="outlined"
                                label={RESOURCE_TYPES.find((type) => type.value === assignment.resourceType)?.label || 'Interno'}
                            />
                        )}
                    </Box>
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1.5 }}>
                        {assignment.resourceType === 'EXTERNAL' ? (
                            <TextField
                                label="Chofer / Empresa"
                                value={assignment.driver || ''}
                                onChange={(event) => changeAssignment(index, 'driver', event.target.value)}
                                disabled={saving}
                                size="small"
                                fullWidth
                                slotProps={{ input: { readOnly: !isEdit } }}
                                sx={fieldSx}
                            />
                        ) : (
                            <TourProcessingCatalogField
                                kind="driver"
                                generalServicesOnly={assignment.resourceType !== 'BORROWED_VEHICLE'}
                                otherUnitOnly={assignment.resourceType === 'BORROWED_VEHICLE'}
                                value={assignment.driver || ''}
                                onChange={(value) => changeAssignment(index, 'driver', value)}
                                isEdit={isEdit}
                                disabled={saving}
                                sx={fieldSx}
                            />
                        )}
                        {assignment.resourceType === 'BORROWED_VEHICLE' ? (isEdit ? (
                            <Autocomplete
                                options={borrowedVehicles.options}
                                loading={borrowedVehicles.loading}
                                value={borrowedVehicles.options.find((option) => option.id === assignment.borrowedVehicleId) ?? null}
                                onChange={(_, option) => changeAssignment(index, 'borrowedVehicleId', option?.id ?? '')}
                                getOptionLabel={(option) => option.label ?? ''}
                                isOptionEqualToValue={(option, value) => option.id === value.id}
                                disabled={saving}
                                size="small"
                                renderInput={(params) => <TextField {...params} label="Vehículo prestado" sx={fieldSx} />}
                            />
                        ) : (
                            <TextField label="Vehículo prestado" value={assignment.borrowedVehiclePlate || ''} size="small" fullWidth slotProps={{ input: { readOnly: true } }} sx={fieldSx} />
                        )) : assignment.resourceType === 'EXTERNAL' ? (
                            <TextField
                                label="Placa"
                                value={assignment.vehicle || ''}
                                onChange={(event) => changeAssignment(index, 'vehicle', event.target.value)}
                                disabled={saving}
                                size="small"
                                fullWidth
                                slotProps={{ input: { readOnly: !isEdit } }}
                                sx={fieldSx}
                            />
                        ) : (
                            <TourProcessingCatalogField
                                kind="vehicle"
                                value={assignment.vehicle || ''}
                                onChange={(value) => changeAssignment(index, 'vehicle', value)}
                                isEdit={isEdit}
                                disabled={saving}
                                sx={fieldSx}
                            />
                        )}
                    </Box>
                </Box>
                );
            })}
            {isEdit && (
                <Button startIcon={<AddOutlinedIcon />} onClick={addAssignment} disabled={saving} sx={{ alignSelf: 'flex-start', textTransform: 'none' }}>
                    Agregar viaje
                </Button>
            )}
            {error && (
                <Typography role="alert" sx={{ color: 'error.main', fontSize: 12.5 }}>{error}</Typography>
            )}
            {!error && remaining > 0 && (
                <Typography sx={{ color: 'warning.main', fontSize: 12.5 }}>
                    {`Faltan ${remaining} pasajeros por cubrir.`}
                </Typography>
            )}
        </Box>
    );
}
