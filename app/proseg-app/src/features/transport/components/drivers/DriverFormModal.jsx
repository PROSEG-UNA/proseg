import { useEffect, useState } from 'react';
import { Alert, Box, Button, Checkbox, FormControlLabel, ListItemText, MenuItem, Switch, TextField } from '@mui/material';
import { TimePicker } from '@mui/x-date-pickers/TimePicker';
import dayjs from 'dayjs';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import DeleteIcon from '@mui/icons-material/Delete';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { createDriver, deleteDriver, fetchDriverById, updateDriver } from '../../services/drivers/driversService';
import {
    AFTERNOON_ORDINARY_END,
    AFTERNOON_ORDINARY_START,
    DEFAULT_ORDINARY_END,
    DEFAULT_ORDINARY_START,
    DRIVER_STATUS_OPTIONS,
    DRIVER_UNIT_OPTIONS,
    MAX_ORDINARY_SCHEDULE_HOURS,
    ULLOA_SHIFT_OPTIONS,
    VEHICLE_TYPE_OPTIONS,
    timeToMinutes,
    toTimeInput,
    ulloaShiftLabel,
    vehicleTypeLabel,
} from '../../transportUtils';

const ALL_VEHICLE_TYPES = VEHICLE_TYPE_OPTIONS.map((option) => option.value);

const INITIAL_VALUES = {
    name: '',
    status: 'ACTIVE',
    organizationalUnit: 'GENERAL_SERVICES',
    allowedVehicleTypes: ALL_VEHICLE_TYPES,
    ordinaryStart: DEFAULT_ORDINARY_START,
    ordinaryEnd: DEFAULT_ORDINARY_END,
    ulloaEnabled: false,
    ulloaShifts: [],
};

const FIELDS_TO_VALIDATE = ['name', 'ordinaryStart', 'ordinaryEnd', 'ulloaShifts'];

function getFieldError(key, values) {
    if (key === 'name' && !values.name?.trim()) return 'Este campo es requerido';
    if (key === 'ordinaryStart' && !values.ordinaryStart) return 'Este campo es requerido';
    if (key === 'ordinaryEnd') {
        if (!values.ordinaryEnd) return 'Este campo es requerido';
        const start = timeToMinutes(values.ordinaryStart);
        const end = timeToMinutes(values.ordinaryEnd);
        if (start !== null && end !== null && end <= start) return 'Debe terminar después del inicio';
        if (start !== null && end !== null && (end - start) / 60 > MAX_ORDINARY_SCHEDULE_HOURS) {
            return `No puede superar las ${MAX_ORDINARY_SCHEDULE_HOURS} horas`;
        }
    }
    if (key === 'ulloaShifts' && values.ulloaEnabled && values.ulloaShifts.length === 0) {
        return 'Selecciona al menos un turno';
    }
    return '';
}

function toPickerValue(time) {
    return time ? dayjs(`2000-01-01T${time}`) : null;
}

export default function DriverFormModal({ open, onClose, onSaved, onDeleted, driverId = null, initialValues = null, canEdit = true, canDelete = false }) {
    const isEdit = !!driverId;
    const [formValues, setFormValues] = useState(() => ({ ...INITIAL_VALUES, ...initialValues }));
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [loadingDriver, setLoadingDriver] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
    const [alert, setAlert] = useState(null);

    useEffect(() => {
        if (!open || !driverId) return undefined;

        let cancelled = false;
        const loadDriver = async () => {
            setLoadingDriver(true);
            try {
                const driver = await fetchDriverById(driverId);
                if (cancelled || !driver) return;
                const ulloaShifts = driver.ulloaShifts ?? [];
                setFormValues({
                    name: driver.name ?? '',
                    status: driver.status ?? 'ACTIVE',
                    organizationalUnit: driver.organizationalUnit ?? 'GENERAL_SERVICES',
                    allowedVehicleTypes: driver.allowedVehicleTypes ?? ALL_VEHICLE_TYPES,
                    ordinaryStart: toTimeInput(driver.ordinaryStart, DEFAULT_ORDINARY_START),
                    ordinaryEnd: toTimeInput(driver.ordinaryEnd, DEFAULT_ORDINARY_END),
                    ulloaEnabled: ulloaShifts.length > 0,
                    ulloaShifts,
                });
            } catch (error) {
                if (!cancelled) {
                    setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo cargar el chofer') });
                }
            } finally {
                if (!cancelled) setLoadingDriver(false);
            }
        };

        void loadDriver();

        return () => {
            cancelled = true;
        };
    }, [open, driverId]);

    const validateField = (key, values) => {
        const error = getFieldError(key, values);
        setErrors((previousValue) => ({ ...previousValue, [key]: error }));
        return !error;
    };

    const applyChanges = (changes) => {
        const nextValues = { ...formValues, ...changes };
        setFormValues(nextValues);
        FIELDS_TO_VALIDATE.forEach((key) => {
            if (touched[key]) validateField(key, nextValues);
        });
    };

    const handleChange = (key, value) => applyChanges({ [key]: value });

    const handleBlur = (key) => {
        setTouched((previousValue) => ({ ...previousValue, [key]: true }));
        validateField(key, formValues);
    };

    const handleUlloaToggle = (enabled) => {
        applyChanges({ ulloaEnabled: enabled, ulloaShifts: enabled ? formValues.ulloaShifts : [] });
    };

    const handleUseAfternoonSchedule = () => {
        applyChanges({ ordinaryStart: AFTERNOON_ORDINARY_START, ordinaryEnd: AFTERNOON_ORDINARY_END });
    };

    const handleSave = async () => {
        if (!canEdit) return;
        const nextTouched = {};
        let hasError = false;

        FIELDS_TO_VALIDATE.forEach((key) => {
            nextTouched[key] = true;
            if (!validateField(key, formValues)) {
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
                status: formValues.status,
                organizationalUnit: formValues.organizationalUnit,
                allowedVehicleTypes: formValues.allowedVehicleTypes,
                ordinaryStart: formValues.ordinaryStart,
                ordinaryEnd: formValues.ordinaryEnd,
                ulloaShifts: formValues.ulloaEnabled ? formValues.ulloaShifts : [],
            };

            const saved = isEdit
                ? await updateDriver(driverId, payload)
                : await createDriver(payload);

            onSaved?.(saved);
            onClose?.();
        } catch (error) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo guardar el chofer') });
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async () => {
        if (!driverId || !canDelete || deleting) return;
        setDeleting(true);
        try {
            await deleteDriver(driverId);
            setDeleteConfirmOpen(false);
            onDeleted?.();
            onClose?.();
        } catch (error) {
            setDeleteConfirmOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo eliminar el chofer') });
        } finally {
            setDeleting(false);
        }
    };

    const afternoonSelected = formValues.ulloaEnabled && formValues.ulloaShifts.includes('AFTERNOON');
    const hasAfternoonSchedule = formValues.ordinaryStart === AFTERNOON_ORDINARY_START
        && formValues.ordinaryEnd === AFTERNOON_ORDINARY_END;

    const renderTimeField = (key, label) => (
        <TimePicker
            label={label}
            ampm={false}
            format="HH:mm"
            value={toPickerValue(formValues[key])}
            disabled={!canEdit}
            onChange={(value) => handleChange(key, value?.isValid() ? value.format('HH:mm') : '')}
            onClose={() => handleBlur(key)}
            slotProps={{
                textField: {
                    required: true,
                    fullWidth: true,
                    onBlur: () => handleBlur(key),
                    error: !!errors[key],
                    helperText: errors[key],
                },
            }}
        />
    );

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                icon={BadgeOutlinedIcon}
                title={isEdit ? (canEdit ? 'Editar chofer' : 'Ver chofer') : 'Crear chofer'}
                subtitle="Completa la información principal del chofer."
                loading={loadingDriver}
                primaryButton={canEdit ? {
                    label: isEdit ? 'Guardar cambios' : 'Crear',
                    onClick: handleSave,
                    loading: saving,
                    disabled: saving || loadingDriver || deleting,
                } : undefined}
                secondaryButton={{
                    label: canEdit ? 'Cancelar' : 'Cerrar',
                    onClick: onClose,
                    disabled: saving || deleting,
                }}
                footerLeft={isEdit && canDelete ? (
                    <Button
                        color="error"
                        size="small"
                        startIcon={<DeleteIcon />}
                        disabled={saving || loadingDriver || deleting}
                        onClick={() => setDeleteConfirmOpen(true)}
                    >
                        Eliminar chofer
                    </Button>
                ) : undefined}
            >
                <Box sx={{ p: 3, display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' } }}>
                    <TextField
                        label="Nombre"
                        value={formValues.name}
                        disabled={!canEdit}
                        onChange={(event) => handleChange('name', event.target.value)}
                        onBlur={() => handleBlur('name')}
                        error={!!errors.name}
                        helperText={errors.name}
                        required
                        fullWidth
                    />
                    <TextField
                        select
                        label="Estado"
                        value={formValues.status}
                        disabled={!canEdit}
                        onChange={(event) => handleChange('status', event.target.value)}
                        fullWidth
                    >
                        {DRIVER_STATUS_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        select
                        label="Unidad a la que pertenece"
                        value={formValues.organizationalUnit}
                        disabled={!canEdit}
                        onChange={(event) => handleChange('organizationalUnit', event.target.value)}
                        fullWidth
                    >
                        {DRIVER_UNIT_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        select
                        label="Tipos de vehículo que puede manejar"
                        value={formValues.allowedVehicleTypes}
                        disabled={!canEdit}
                        onChange={(event) => handleChange('allowedVehicleTypes', event.target.value)}
                        slotProps={{
                            select: {
                                multiple: true,
                                renderValue: (selected) => selected.map(vehicleTypeLabel).join(', '),
                            },
                        }}
                        sx={{ gridColumn: { md: '1 / -1' } }}
                        fullWidth
                    >
                        {VEHICLE_TYPE_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                <Checkbox
                                    checked={formValues.allowedVehicleTypes.includes(option.value)}
                                    tabIndex={-1}
                                    sx={{ m: 0, mr: 1.5, p: 0 }}
                                />
                                <ListItemText primary={option.label} />
                            </MenuItem>
                        ))}
                    </TextField>
                    {renderTimeField('ordinaryStart', 'Inicio del horario ordinario')}
                    {renderTimeField('ordinaryEnd', 'Fin del horario ordinario')}
                    <FormControlLabel
                        control={(
                            <Switch
                                checked={formValues.ulloaEnabled}
                                disabled={!canEdit}
                                onChange={(event) => handleUlloaToggle(event.target.checked)}
                            />
                        )}
                        label="Es conductor de giras hacia Ulloa (Barreal) - Campus Benjamín Núñez"
                        sx={{ gridColumn: { md: '1 / -1' }, m: 0 }}
                    />
                    {formValues.ulloaEnabled && (
                        <TextField
                            select
                            label="Turnos de Ulloa que cubre"
                            value={formValues.ulloaShifts}
                            disabled={!canEdit}
                            onChange={(event) => handleChange('ulloaShifts', event.target.value)}
                            onBlur={() => handleBlur('ulloaShifts')}
                            error={!!errors.ulloaShifts}
                            helperText={errors.ulloaShifts || 'Estas giras se asignan primero a los choferes con el turno configurado.'}
                            slotProps={{
                                select: {
                                    multiple: true,
                                    renderValue: (selected) => selected.map(ulloaShiftLabel).join(', '),
                                },
                            }}
                            sx={{ gridColumn: { md: '1 / -1' } }}
                            fullWidth
                        >
                            {ULLOA_SHIFT_OPTIONS.map((option) => (
                                <MenuItem key={option.value} value={option.value}>
                                    <Checkbox
                                        checked={formValues.ulloaShifts.includes(option.value)}
                                        tabIndex={-1}
                                        sx={{ m: 0, mr: 1.5, p: 0 }}
                                    />
                                    <ListItemText primary={option.label} />
                                </MenuItem>
                            ))}
                        </TextField>
                    )}
                    {canEdit && afternoonSelected && !hasAfternoonSchedule && (
                        <Alert
                            severity="info"
                            variant="outlined"
                            sx={{ gridColumn: { md: '1 / -1' } }}
                            action={(
                                <Button color="inherit" size="small" onClick={handleUseAfternoonSchedule}>
                                    Usar 11:00 a 20:00
                                </Button>
                            )}
                        >
                            El turno de tarde cuenta 8 horas ordinarias y 2 de sobresueldo cuando el horario ordinario del chofer es de 11:00 a 20:00.
                        </Alert>
                    )}
                </Box>
            </GeneralModal>

            <DialogModal
                type="delete"
                open={deleteConfirmOpen}
                title="Eliminar chofer"
                message={`¿Seguro que deseas eliminar al chofer "${formValues.name}"?\nEsta acción no se puede deshacer.`}
                onClose={() => { if (!deleting) setDeleteConfirmOpen(false); }}
                onConfirm={handleDelete}
                confirmLabel={deleting ? 'Eliminando...' : 'Eliminar'}
            />

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}
