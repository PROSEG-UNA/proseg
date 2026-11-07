import { useEffect, useState } from 'react';
import { Box, Button, Checkbox, FormControlLabel, ListItemText, MenuItem, Switch, TextField } from '@mui/material';
import dayjs from 'dayjs';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import DeleteIcon from '@mui/icons-material/Delete';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { createDriver, deleteDriver, fetchDriverById, updateDriver, updateDriverMonthBusinessDays } from '../../services/drivers/driversService';
import {
    DRIVER_STATUS_OPTIONS,
    DRIVER_UNIT_OPTIONS,
    ORDINARY_SCHEDULE_OPTIONS,
    ULLOA_SHIFT_VALUES,
    VEHICLE_TYPE_OPTIONS,
    ordinaryScheduleValueOf,
    vehicleTypeLabel,
} from '../../transportUtils';

const ALL_VEHICLE_TYPES = VEHICLE_TYPE_OPTIONS.map((option) => option.value);

const INITIAL_VALUES = {
    name: '',
    status: 'ACTIVE',
    organizationalUnit: 'GENERAL_SERVICES',
    allowedVehicleTypes: ALL_VEHICLE_TYPES,
    ordinarySchedule: ORDINARY_SCHEDULE_OPTIONS[0].value,
    ulloaEnabled: false,
};

const FIELDS_TO_VALIDATE = ['name'];

function getFieldError(key, values) {
    if (key === 'name' && !values.name?.trim()) return 'Este campo es requerido';
    return '';
}

function getBusinessDaysError(value, period) {
    if (value === '') return 'Este campo es requerido';
    const daysInMonth = dayjs(`${period.year}-${String(period.month).padStart(2, '0')}-01`).daysInMonth();
    return Number(value) > daysInMonth ? `Debe estar entre 0 y ${daysInMonth}` : '';
}

function getBusinessDaysHelper({ reset, customized, defaultBusinessDays }) {
    const monthValue = defaultBusinessDays === null ? '' : ` (${defaultBusinessDays})`;
    if (reset) return `Se usará el valor del mes${monthValue} al guardar.`;
    if (customized) return `Personalizado para este chofer${defaultBusinessDays === null ? '.' : `. El valor del mes es ${defaultBusinessDays}.`}`;
    return 'Valor del mes. Puedes cambiarlo solo para este chofer.';
}

export default function DriverFormModal({
    open,
    onClose,
    onSaved,
    onDeleted,
    onBusinessDaysSaved,
    driverId = null,
    initialValues = null,
    canEdit = true,
    canDelete = false,
    period = null,
    monthBusinessDays = null,
    defaultBusinessDays = null,
    businessDaysCustomized = false,
}) {
    const isEdit = !!driverId;
    const showBusinessDays = isEdit && Boolean(period) && monthBusinessDays !== null;
    const [businessDaysDraft, setBusinessDaysDraft] = useState(() => (monthBusinessDays === null ? '' : String(monthBusinessDays)));
    const [businessDaysReset, setBusinessDaysReset] = useState(false);
    const [businessDaysError, setBusinessDaysError] = useState('');
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
                setFormValues({
                    name: driver.name ?? '',
                    status: driver.status ?? 'ACTIVE',
                    organizationalUnit: driver.organizationalUnit ?? 'GENERAL_SERVICES',
                    allowedVehicleTypes: driver.allowedVehicleTypes ?? ALL_VEHICLE_TYPES,
                    ordinarySchedule: ordinaryScheduleValueOf(driver.ordinaryStart),
                    ulloaEnabled: (driver.ulloaShifts ?? []).length > 0,
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

    const handleBusinessDaysChange = (value) => {
        const digits = value.replace(/\D/g, '').slice(0, 2);
        setBusinessDaysDraft(digits);
        setBusinessDaysReset(false);
        setBusinessDaysError(getBusinessDaysError(digits, period));
    };

    const handleBusinessDaysReset = () => {
        setBusinessDaysDraft(defaultBusinessDays === null ? '' : String(defaultBusinessDays));
        setBusinessDaysReset(true);
        setBusinessDaysError('');
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

        if (showBusinessDays && !businessDaysReset) {
            const error = getBusinessDaysError(businessDaysDraft, period);
            setBusinessDaysError(error);
            if (error) hasError = true;
        }

        setTouched((previousValue) => ({ ...previousValue, ...nextTouched }));

        if (hasError) {
            setAlert({ type: 'warning', message: 'Revisa los datos antes de continuar' });
            return;
        }

        setSaving(true);
        try {
            const schedule = ORDINARY_SCHEDULE_OPTIONS.find((option) => option.value === formValues.ordinarySchedule)
                ?? ORDINARY_SCHEDULE_OPTIONS[0];
            const payload = {
                name: formValues.name.trim(),
                status: formValues.status,
                organizationalUnit: formValues.organizationalUnit,
                allowedVehicleTypes: formValues.allowedVehicleTypes,
                ordinaryStart: schedule.start,
                ordinaryEnd: schedule.end,
                ulloaShifts: formValues.ulloaEnabled ? ULLOA_SHIFT_VALUES : [],
            };

            const saved = isEdit
                ? await updateDriver(driverId, payload)
                : await createDriver(payload);

            const businessDaysChanged = showBusinessDays
                && (businessDaysReset ? businessDaysCustomized : Number(businessDaysDraft) !== monthBusinessDays);
            if (businessDaysChanged) {
                try {
                    await updateDriverMonthBusinessDays(
                        driverId,
                        period.year,
                        period.month,
                        businessDaysReset ? null : Number(businessDaysDraft),
                    );
                    onBusinessDaysSaved?.();
                } catch (error) {
                    onSaved?.(saved);
                    setAlert({
                        type: 'error',
                        message: `El chofer se guardó, pero no se pudieron cambiar los días hábiles. ${getFriendlyApiErrorMessage(error, '')}`.trim(),
                    });
                    return;
                }
            }

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
                    <TextField
                        select
                        label="Horario ordinario"
                        value={formValues.ordinarySchedule}
                        disabled={!canEdit}
                        onChange={(event) => handleChange('ordinarySchedule', event.target.value)}
                        helperText="Define también los turnos de giras Periféricas que puede cubrir."
                        sx={{ gridColumn: { md: '1 / -1' } }}
                        fullWidth
                    >
                        {ORDINARY_SCHEDULE_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <FormControlLabel
                        control={(
                            <Switch
                                checked={formValues.ulloaEnabled}
                                disabled={!canEdit}
                                onChange={(event) => handleChange('ulloaEnabled', event.target.checked)}
                            />
                        )}
                        label="Es conductor de giras Periféricas - Campus Benjamín Núñez"
                        sx={{ gridColumn: { md: '1 / -1' }, m: 0 }}
                    />
                    {showBusinessDays && (
                        <TextField
                            label={`Días hábiles de ${period.label}`}
                            value={businessDaysDraft}
                            disabled={!canEdit}
                            onChange={(event) => handleBusinessDaysChange(event.target.value)}
                            error={!!businessDaysError}
                            helperText={businessDaysError || getBusinessDaysHelper({
                                reset: businessDaysReset,
                                customized: businessDaysCustomized,
                                defaultBusinessDays,
                            })}
                            required
                            fullWidth
                            slotProps={{ htmlInput: { inputMode: 'numeric', maxLength: 2 } }}
                            sx={{ gridColumn: { md: '1 / -1' } }}
                        />
                    )}
                    {showBusinessDays && canEdit && businessDaysCustomized && !businessDaysReset && defaultBusinessDays !== null && (
                        <Button
                            size="small"
                            onClick={handleBusinessDaysReset}
                            sx={{ justifySelf: 'start', gridColumn: { md: '1 / -1' }, mt: -1 }}
                        >
                            {`Usar el valor del mes (${defaultBusinessDays})`}
                        </Button>
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
