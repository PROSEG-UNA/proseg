import { useEffect, useState } from 'react';
import { Box, MenuItem, TextField } from '@mui/material';
import AltRouteOutlinedIcon from '@mui/icons-material/AltRouteOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { createTour, fetchTourById, updateTour } from '../../services/tours/toursService';
import { TOUR_STATUS_OPTIONS } from '../../transportUtils';
import { useDriverOptions, useVehicleOptions } from '../../hooks/useTransportOptions';
import { useQueryAlert } from '../../../../common/hooks/index.js';

const INITIAL_VALUES = {
    name: '',
    origin: '',
    destination: '',
    driverId: '',
    vehicleId: '',
    startDate: '',
    endDate: '',
    status: 'PLANNED',
};

export default function TourFormModal({ open, onClose, onSaved, tourId = null }) {
    const isEdit = !!tourId;
    const [formValues, setFormValues] = useState(INITIAL_VALUES);
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [loadingTour, setLoadingTour] = useState(false);
    const drivers = useDriverOptions(open);
    const vehicles = useVehicleOptions(open);

    const driverOptions = drivers.options;
    const vehicleOptions = vehicles.options;
    const loadingReferences = drivers.loading || vehicles.loading;

    const referencesError = drivers.error ?? vehicles.error;
    const { alert, setAlert, closeAlert } = useQueryAlert(
        referencesError ? getFriendlyApiErrorMessage(referencesError, 'No se pudieron cargar choferes y vehículos') : null
    );

    useEffect(() => {
        if (!open || !tourId) return undefined;

        let cancelled = false;
        const loadTour = async () => {
            setLoadingTour(true);
            try {
                const tour = await fetchTourById(tourId);
                if (cancelled || !tour) return;
                setFormValues({
                    name: tour.name ?? '',
                    origin: tour.origin ?? '',
                    destination: tour.destination ?? '',
                    driverId: tour.driver?.id ?? tour.driverId ?? '',
                    vehicleId: tour.vehicle?.id ?? tour.vehicleId ?? '',
                    startDate: tour.startDate ?? '',
                    endDate: tour.endDate ?? '',
                    status: tour.status ?? 'PLANNED',
                });
            } catch (error) {
                if (!cancelled) {
                    setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo cargar la gira') });
                }
            } finally {
                if (!cancelled) setLoadingTour(false);
            }
        };

        void loadTour();

        return () => {
            cancelled = true;
        };
    }, [open, tourId, setAlert]);

    const validateField = (key, value) => {
        const trimmedValue = typeof value === 'string' ? value.trim() : value;
        let error = '';

        if ((key === 'name' || key === 'origin' || key === 'destination') && !trimmedValue) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'endDate' && trimmedValue && formValues.startDate) {
            if (new Date(trimmedValue) < new Date(formValues.startDate)) {
                error = 'La fecha final no puede ser menor que la inicial';
            }
        }

        setErrors((previousValue) => ({ ...previousValue, [key]: error }));
        return !error;
    };

    const handleChange = (key, value) => {
        setFormValues((previousValue) => ({ ...previousValue, [key]: value }));
        if (touched[key]) validateField(key, value);
    };

    const handleBlur = (key) => {
        setTouched((previousValue) => ({ ...previousValue, [key]: true }));
        validateField(key, formValues[key]);
    };

    const handleSave = async () => {
        const keysToValidate = ['name', 'origin', 'destination', 'endDate'];
        const nextTouched = {};
        let hasError = false;

        keysToValidate.forEach((key) => {
            nextTouched[key] = true;
            if (!validateField(key, formValues[key])) {
                hasError = true;
            }
        });

        setTouched((previousValue) => ({ ...previousValue, ...nextTouched }));

        if (hasError) {
            setAlert({ type: 'warning', message: 'Revisa los datos antes de continuar' });
            return;
        }

        setSaving(true);
        try {
            const payload = {
                name: formValues.name.trim(),
                origin: formValues.origin.trim(),
                destination: formValues.destination.trim(),
                driverId: formValues.driverId.trim() || null,
                vehicleId: formValues.vehicleId.trim() || null,
                startDate: formValues.startDate || null,
                endDate: formValues.endDate || null,
                status: formValues.status,
            };

            if (isEdit) {
                await updateTour(tourId, payload);
            } else {
                await createTour(payload);
            }

            onSaved?.();
            onClose?.();
        } catch (error) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo guardar la gira') });
        } finally {
            setSaving(false);
        }
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                icon={AltRouteOutlinedIcon}
                title={isEdit ? 'Editar gira' : 'Crear gira'}
                subtitle="Completa la información principal de la gira."
                loading={loadingTour}
                primaryButton={{
                    label: isEdit ? 'Guardar cambios' : 'Crear',
                    onClick: handleSave,
                    loading: saving,
                    disabled: saving || loadingTour || loadingReferences,
                }}
                secondaryButton={{
                    label: 'Cancelar',
                    onClick: onClose,
                    disabled: saving,
                }}
            >
                <Box sx={{ p: 3, display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' } }}>
                    <TextField
                        label="Nombre"
                        value={formValues.name}
                        onChange={(event) => handleChange('name', event.target.value)}
                        onBlur={() => handleBlur('name')}
                        error={!!errors.name}
                        helperText={errors.name}
                        required
                        fullWidth
                        sx={{ gridColumn: { md: '1 / -1' } }}
                    />
                    <TextField
                        label="Origen"
                        value={formValues.origin}
                        onChange={(event) => handleChange('origin', event.target.value)}
                        onBlur={() => handleBlur('origin')}
                        error={!!errors.origin}
                        helperText={errors.origin}
                        required
                        fullWidth
                    />
                    <TextField
                        label="Destino"
                        value={formValues.destination}
                        onChange={(event) => handleChange('destination', event.target.value)}
                        onBlur={() => handleBlur('destination')}
                        error={!!errors.destination}
                        helperText={errors.destination}
                        required
                        fullWidth
                    />
                    <TextField
                        select
                        label="Chofer"
                        value={formValues.driverId}
                        onChange={(event) => handleChange('driverId', event.target.value)}
                        fullWidth
                        disabled={loadingReferences}
                    >
                        <MenuItem value="">Sin asignar</MenuItem>
                        {driverOptions.map((option) => (
                            <MenuItem key={option.id} value={option.id}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        select
                        label="Vehículo"
                        value={formValues.vehicleId}
                        onChange={(event) => handleChange('vehicleId', event.target.value)}
                        fullWidth
                        disabled={loadingReferences}
                    >
                        <MenuItem value="">Sin asignar</MenuItem>
                        {vehicleOptions.map((option) => (
                            <MenuItem key={option.id} value={option.id}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        label="Inicio"
                        type="datetime-local"
                        value={formValues.startDate}
                        onChange={(event) => handleChange('startDate', event.target.value)}
                        fullWidth
                        InputLabelProps={{ shrink: true }}
                    />
                    <TextField
                        label="Fin"
                        type="datetime-local"
                        value={formValues.endDate}
                        onChange={(event) => handleChange('endDate', event.target.value)}
                        onBlur={() => handleBlur('endDate')}
                        error={!!errors.endDate}
                        helperText={errors.endDate}
                        fullWidth
                        InputLabelProps={{ shrink: true }}
                    />
                    <TextField
                        select
                        label="Estado"
                        value={formValues.status}
                        onChange={(event) => handleChange('status', event.target.value)}
                        fullWidth
                    >
                        {TOUR_STATUS_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                </Box>
            </GeneralModal>

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={closeAlert}
            />
        </>
    );
}
