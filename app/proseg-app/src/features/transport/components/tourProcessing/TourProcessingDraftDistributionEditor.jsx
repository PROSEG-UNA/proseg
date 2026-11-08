import { Autocomplete, Box, Button, Chip, TextField, ToggleButton, ToggleButtonGroup, Typography } from '@mui/material';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import TourProcessingCatalogField from './TourProcessingCatalogField.jsx';

const RESOURCE_TYPES = [
    { value: 'INTERNAL', label: 'Interno' },
    { value: 'BORROWED_VEHICLE', label: 'Prestado' },
    { value: 'EXTERNAL', label: 'Externo' },
];

export default function TourProcessingDraftDistributionEditor({
    assignments, totalPassengers, onChange, isEdit, saving, fieldSx, borrowedVehicles, error,
    vehicleOptions,
}) {
    const totalAssigned = assignments.reduce((sum, assignment) => sum + Number(assignment.passengers || 0), 0);
    const remaining = Number(totalPassengers || 0) - totalAssigned;

    const changeAssignment = (index, key, value) => onChange(assignments.map((assignment, currentIndex) => (
        currentIndex === index ? { ...assignment, [key]: value } : assignment
    )));

    const changeResourceType = (index, type) => onChange(assignments.map((assignment, currentIndex) => (
        currentIndex === index ? { ...assignment, resourceType: type, vehicle: '', borrowedVehicleId: '' } : assignment
    )));

    const removeAssignment = (index) => onChange(assignments.filter((_, currentIndex) => currentIndex !== index));

    const addAssignment = () => onChange([...assignments, {
        passengers: '', driver: '', vehicle: '', resourceType: 'INTERNAL', borrowedVehicleId: '',
    }]);

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1, flexWrap: 'wrap' }}>
                <Typography sx={{ fontSize: 13.5, fontWeight: 700 }}>Distribución de pasajeros</Typography>
                <Chip
                    size="small"
                    color={remaining === 0 ? 'success' : 'warning'}
                    variant="outlined"
                    label={`${totalAssigned} de ${totalPassengers || 0} asignados`}
                />
            </Box>
            {assignments.map((assignment, index) => {
                const capacity = assignment.resourceType === 'BORROWED_VEHICLE'
                    ? borrowedVehicles.options.find((option) => option.id === assignment.borrowedVehicleId)?.capacity
                    : assignment.resourceType === 'INTERNAL'
                        ? vehicleOptions.find((option) => option.label.toUpperCase() === assignment.vehicle?.trim().toUpperCase())?.capacity
                        : null;
                return (
                <Box key={index} sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, p: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography sx={{ fontSize: 13, fontWeight: 700 }}>Vehículo {index + 1}</Typography>
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
                                generalServicesOnly
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
                    Agregar vehículo
                </Button>
            )}
            {(remaining !== 0 || error) && (
                <Typography role="alert" sx={{ color: 'error.main', fontSize: 12.5 }}>
                    {error || (remaining > 0 ? `Faltan ${remaining} pasajeros por asignar` : `Hay ${-remaining} pasajeros de más`)}
                </Typography>
            )}
        </Box>
    );
}
