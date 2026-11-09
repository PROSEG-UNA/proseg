import { useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
    Autocomplete,
    Box,
    Button,
    Chip,
    Divider,
    IconButton,
    TextField,
    ToggleButton,
    ToggleButtonGroup,
    Typography,
    useTheme,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { TimePicker } from '@mui/x-date-pickers/TimePicker';
import dayjs from 'dayjs';
import AltRouteOutlinedIcon from '@mui/icons-material/AltRouteOutlined';
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';
import { formatDate, formatHours } from '../../transportUtils.js';
import { useBorrowedVehicleOptions, useVehicleOptions } from '../../hooks/useTransportOptions.js';
import { invalidateTourProcessingDraft, replaceCachedDraftRow } from '../../hooks/useTourProcessingDraftsData.js';
import {
    createTourProcessingDraftRow,
    deleteTourProcessingDraftRow,
    updateTourProcessingDraftRow,
    updateTourProcessingDraftRowDay,
} from '../../services/tourProcessing/tourProcessingService.js';
import {
    APPROVAL_STATUS,
    approvalStatusColor,
    approvalStatusLabel,
    draftRowDuplicateLabel,
    toggledApprovalStatus,
} from './tourProcessingDraftLabels.js';
import TourProcessingCatalogField from './TourProcessingCatalogField.jsx';
import DuplicateChip from './DuplicateChip.jsx';
import { describeDraftRowChanges } from './draftRowChanges.js';
import TourProcessingDraftDistributionEditor from './TourProcessingDraftDistributionEditor.jsx';
import { minimumTrips } from './tripDistribution.js';

const RESOURCE_TYPE_OPTIONS = [
    { value: 'INTERNAL', label: 'Interno' },
    { value: 'EXTERNAL', label: 'Contratación externa' },
    { value: 'BORROWED_VEHICLE', label: 'Préstamo de vehículo' },
];

const FORM_ID = 'tour-processing-draft-row-form';
const EMPTY_ROW = {};
const MODAL_TITLES = { view: 'Ver fila', edit: 'Editar gira', create: 'Crear gira' };
const SOURCE_ROW_LABEL = "Fila en archivo 'Previo de comisión'";
const REQUIRED_MESSAGE = 'Este campo es requerido';
const REQUIRED_TEXT_KEYS = ['number', 'executingUnit', 'responsible', 'destination'];
const POSITIVE_INTEGER_KEYS = ['passengers', 'durationDays', 'priority'];
const DATE_TIME_KEYS = ['departureDate', 'departureTime', 'returnDate', 'returnTime'];
const MAX_LENGTHS = {
    driver: 200,
    number: 50,
    vehicle: 100,
    executingUnit: 255,
    responsible: 255,
    destination: 255,
    modality: 50,
};
const DAY_HOURS_KEYS = ['ordinaryHours', 'surplusHours', 'overtimeHours'];
const MAX_ORDINARY_HOURS_PER_DAY = 9;
const OVERTIME_NEEDS_CONFIRMATION_CODE = 'TOUR_PROCESSING_DRAFT_ROW_DAY_OVERTIME_NEEDS_CONFIRMATION';
const DRIVER_UNAVAILABLE_CODE = 'TOUR_PROCESSING_DRAFT_ROW_DRIVER_UNAVAILABLE';
const OPTION_BUTTON_SX = {
    textTransform: 'none',
    fontWeight: 700,
    fontSize: 15,
    py: 1.5,
    borderRadius: '10px',
};

function approvalToggleLabel(approvalStatus) {
    return approvalStatus === APPROVAL_STATUS.NOT_APPROVED ? 'Aprobar' : 'No aprobar';
}

function errorCodeOf(error) {
    return error?.response?.data?.errors?.[0];
}

function gridSx(columns) {
    return {
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', sm: columns },
        gap: 2,
    };
}

function fieldStyles(accentColor) {
    return {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': { borderColor: 'divider' },
            '&:hover fieldset': { borderColor: `color-mix(in srgb, ${accentColor} 50%, transparent)` },
            '&.Mui-focused fieldset': { borderColor: accentColor },
        },
        '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
        '& .MuiInputBase-input': { color: 'text.primary' },
        '[data-mui-color-scheme="dark"] &': {
            '& .MuiFormHelperText-root.Mui-error': { color: 'hsl(220, 20%, 65%)' },
            '& .MuiInputLabel-root.Mui-error': { color: 'hsl(220, 20%, 65%)' },
            '& .MuiFormLabel-asterisk.Mui-error': { color: 'hsl(220, 20%, 65%)' },
        },
    };
}

function renderSectionLabel(text, accentColor) {
    return (
        <Typography sx={{
            fontSize: 11.5, fontWeight: 700, letterSpacing: '0.08em',
            textTransform: 'uppercase', color: accentColor, mb: 1.5,
        }}>
            {text}
        </Typography>
    );
}

function renderReadOnlyField(label, value, fieldSx, disabled = false) {
    return (
        <TextField
            label={label}
            value={value || '—'}
            disabled={disabled}
            fullWidth
            size="small"
            slotProps={{ input: { readOnly: true } }}
            sx={fieldSx}
        />
    );
}

function capitalize(text) {
    if (!text) return '';
    return text.charAt(0).toUpperCase() + text.slice(1);
}

function toDayFormValues(day) {
    return {
        startTime: day.startTime ?? '',
        endTime: day.endTime ?? '',
        ordinaryHours: String(day.ordinaryHours ?? ''),
        surplusHours: String(day.surplusHours ?? ''),
        overtimeHours: String(day.overtimeHours ?? ''),
    };
}

function validateDay(values) {
    const errors = {};
    if (!values.startTime) errors.startTime = REQUIRED_MESSAGE;
    if (!values.endTime) errors.endTime = REQUIRED_MESSAGE;
    DAY_HOURS_KEYS.forEach((key) => {
        if (values[key] === '') errors[key] = REQUIRED_MESSAGE;
        else if (Number(values[key]) < 0) errors[key] = 'Debe ser mayor o igual a 0';
    });
    if (!errors.ordinaryHours && Number(values.ordinaryHours) > MAX_ORDINARY_HOURS_PER_DAY) {
        errors.ordinaryHours = `No puede superar las ${MAX_ORDINARY_HOURS_PER_DAY} horas`;
    }
    if (!errors.startTime && !errors.endTime && values.endTime <= values.startTime) {
        errors.endTime = 'La hora de fin debe ser posterior a la hora de inicio';
    }
    return errors;
}

function dayToPayload(values) {
    return {
        startTime: values.startTime,
        endTime: values.endTime,
        ordinaryHours: Number(values.ordinaryHours),
        surplusHours: Number(values.surplusHours),
        overtimeHours: Number(values.overtimeHours),
    };
}

function TourProcessingDraftRowDayCard({ day, draftId, rowId, canEdit, onSaved }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const queryClient = useQueryClient();
    const fieldSx = fieldStyles(accentColor);
    const [editing, setEditing] = useState(false);
    const [values, setValues] = useState(() => toDayFormValues(day));
    const [submitted, setSubmitted] = useState(false);
    const [needsOvertimeConfirm, setNeedsOvertimeConfirm] = useState(false);
    const errors = useMemo(() => validateDay(values), [values]);

    const mutation = useMutation({
        mutationFn: (payload) => updateTourProcessingDraftRowDay(draftId, rowId, day.id, payload),
        onSuccess: (updatedDay) => {
            onSaved(updatedDay);
            setEditing(false);
            setSubmitted(false);
            setNeedsOvertimeConfirm(false);
            void invalidateTourProcessingDraft(queryClient, draftId);
        },
        onError: (error) => {
            if (errorCodeOf(error) === OVERTIME_NEEDS_CONFIRMATION_CODE) setNeedsOvertimeConfirm(true);
        },
    });

    const saving = mutation.isPending;

    const startEditing = () => {
        setValues(toDayFormValues(day));
        setSubmitted(false);
        mutation.reset();
        setEditing(true);
    };

    const cancelEditing = () => {
        setValues(toDayFormValues(day));
        setSubmitted(false);
        setNeedsOvertimeConfirm(false);
        setEditing(false);
    };

    const handleTimeChange = (key, value) => setValues((current) => ({ ...current, [key]: value }));

    const handleHoursChange = (key, raw) => setValues((current) => ({ ...current, [key]: raw.replace(/[^0-9.]/g, '') }));

    const handleSave = () => {
        setSubmitted(true);
        if (Object.keys(errors).length > 0) return;
        mutation.mutate(dayToPayload(values));
    };

    const confirmOvertimeAnyway = () => {
        setNeedsOvertimeConfirm(false);
        mutation.mutate({ ...dayToPayload(values), confirmOvertimeBelowSurplusThreshold: true });
    };

    const visibleError = (key) => (submitted ? errors[key] : undefined);

    const renderTimeField = (key, label) => (
        <TimePicker
            label={label}
            ampm={false}
            format="HH:mm"
            value={values[key] ? dayjs(`2000-01-01T${values[key]}`) : null}
            onChange={(value) => handleTimeChange(key, value?.isValid() ? value.format('HH:mm') : '')}
            disabled={saving}
            slotProps={{
                textField: {
                    size: 'small',
                    fullWidth: true,
                    error: Boolean(visibleError(key)),
                    helperText: visibleError(key) || ' ',
                    sx: fieldSx,
                },
            }}
        />
    );

    const renderHoursField = (key, label) => (
        <TextField
            label={label}
            value={values[key]}
            onChange={(event) => handleHoursChange(key, event.target.value)}
            error={Boolean(visibleError(key))}
            helperText={visibleError(key) || ' '}
            disabled={saving}
            fullWidth
            size="small"
            slotProps={{ htmlInput: { inputMode: 'decimal' } }}
            sx={fieldSx}
        />
    );

    return (
        <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: '10px', p: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Typography sx={{ fontSize: 13.5, fontWeight: 700, color: 'text.primary' }}>
                        {formatDate(day.date)} · {capitalize(day.dayOfWeek)}
                    </Typography>
                    {day.manuallyEdited && (
                        <Chip label="Editado manualmente" size="small" color="warning" variant="outlined" />
                    )}
                </Box>
                {canEdit && !editing && (
                    <IconButton aria-label="Editar día" size="small" onClick={startEditing} sx={{ color: accentColor }}>
                        <EditOutlinedIcon fontSize="small" />
                    </IconButton>
                )}
                {editing && (
                    <Box sx={{ display: 'flex', gap: 0.5 }}>
                        <IconButton aria-label="Guardar día" size="small" onClick={handleSave} disabled={saving} sx={{ color: accentColor }}>
                            <SaveOutlinedIcon fontSize="small" />
                        </IconButton>
                        <IconButton aria-label="Cancelar edición del día" size="small" onClick={cancelEditing} disabled={saving}>
                            <CloseOutlinedIcon fontSize="small" />
                        </IconButton>
                    </Box>
                )}
            </Box>
            <Box sx={gridSx('repeat(5, 1fr)')}>
                {editing ? renderTimeField('startTime', 'Hora inicio') : renderReadOnlyField('Hora inicio', day.startTime, fieldSx)}
                {editing ? renderTimeField('endTime', 'Hora fin') : renderReadOnlyField('Hora fin', day.endTime, fieldSx)}
                {editing ? renderHoursField('ordinaryHours', 'Horas ordinarias') : renderReadOnlyField('Horas ordinarias', formatHours(day.ordinaryHours), fieldSx)}
                {editing ? renderHoursField('surplusHours', 'Horas sobresueldo') : renderReadOnlyField('Horas sobresueldo', formatHours(day.surplusHours), fieldSx)}
                {editing ? renderHoursField('overtimeHours', 'Horas extra') : renderReadOnlyField('Horas extra', formatHours(day.overtimeHours), fieldSx)}
            </Box>
            {mutation.isError && !needsOvertimeConfirm && (
                <Typography sx={{ fontSize: 12.5, color: 'error.main', mt: 1 }}>
                    {getFriendlyApiErrorMessage(mutation.error, 'No se pudo guardar el día')}
                </Typography>
            )}
            <DialogModal
                open={needsOvertimeConfirm}
                type="warning"
                title="Horas extra sin llegar al 95% de sobresueldo"
                message="Este chofer todavía no alcanza el 95% de su tope de sobresueldo del mes. ¿Confirmás asignar horas extra de todas formas?"
                confirmLabel={saving ? 'Guardando...' : 'Sí, asignar de todas formas'}
                onConfirm={confirmOvertimeAnyway}
                onClose={() => setNeedsOvertimeConfirm(false)}
            />
        </Box>
    );
}

function toFormValues(row) {
    return {
        driver: row.driver ?? '',
        number: row.number ?? '',
        vehicle: row.vehicle ?? '',
        resourceType: row.resourceType ?? 'INTERNAL',
        borrowedVehicleId: row.borrowedVehicleId ?? '',
        passengers: String(row.passengers ?? ''),
        assignments: row.assignments?.map((assignment) => ({
            driver: assignment.driver ?? '',
            vehicle: assignment.vehicle ?? '',
            resourceType: assignment.resourceType ?? 'INTERNAL',
            borrowedVehicleId: assignment.borrowedVehicleId ?? '',
            borrowedVehiclePlate: assignment.borrowedVehiclePlate ?? '',
            passengers: String(assignment.passengers ?? ''),
        })) ?? null,
        executingUnit: row.executingUnit ?? '',
        responsible: row.responsible ?? '',
        destination: row.destination ?? '',
        durationDays: String(row.durationDays ?? ''),
        priority: String(row.priority ?? ''),
        modality: row.modality ?? '',
        departureDate: row.departureDate ?? '',
        departureTime: row.departureTime ?? '',
        returnDate: row.returnDate ?? '',
        returnTime: row.returnTime ?? '',
        observations: row.observations ?? '',
    };
}

function validate(values, vehicleOptions, borrowedVehicleOptions) {
    const errors = {};

    REQUIRED_TEXT_KEYS.forEach((key) => {
        if (!values[key].trim()) errors[key] = REQUIRED_MESSAGE;
    });
    Object.entries(MAX_LENGTHS).forEach(([key, maxLength]) => {
        if (!errors[key] && values[key].length > maxLength) errors[key] = `No puede superar los ${maxLength} caracteres`;
    });
    POSITIVE_INTEGER_KEYS.forEach((key) => {
        if (!values[key]) errors[key] = REQUIRED_MESSAGE;
        else if (Number(values[key]) < 1) errors[key] = 'Debe ser mayor o igual a 1';
    });
    DATE_TIME_KEYS.forEach((key) => {
        if (!values[key]) errors[key] = REQUIRED_MESSAGE;
    });
    if (values.resourceType === 'BORROWED_VEHICLE' && !values.borrowedVehicleId) {
        if (!values.assignments) errors.borrowedVehicleId = 'Selecciona el vehículo prestado';
    }

    if (values.assignments) {
        values.assignments.forEach((assignment) => {
            const passengers = Number(assignment.passengers);
            if (!Number.isInteger(passengers) || passengers < 1) errors.assignments = 'Cada viaje debe tener al menos un pasajero';
            const type = assignment.resourceType || 'INTERNAL';
            const vehicle = type === 'BORROWED_VEHICLE' ? assignment.borrowedVehicleId : assignment.vehicle?.trim().toUpperCase();
            if (type === 'BORROWED_VEHICLE' && !assignment.borrowedVehicleId) errors.assignments = 'Selecciona cada vehículo prestado';
            const capacity = type === 'BORROWED_VEHICLE'
                ? borrowedVehicleOptions.find((option) => option.id === assignment.borrowedVehicleId)?.capacity
                : type === 'INTERNAL'
                    ? vehicleOptions.find((option) => option.label.toUpperCase() === vehicle)?.capacity
                    : null;
            if (capacity != null && passengers > capacity) errors.assignments = `Un vehículo de ${capacity} espacios no puede llevar ${passengers} pasajeros`;
        });
    }

    return { ...errors, ...validateReturnRange(values) };
}

function validateReturnRange(values) {
    if (!values.departureDate || !values.returnDate) return {};
    if (values.returnDate < values.departureDate) {
        return { returnDate: 'La fecha de regreso es anterior a la fecha de salida' };
    }
    const sameDay = values.returnDate === values.departureDate;
    if (sameDay && values.departureTime && values.returnTime && values.returnTime < values.departureTime) {
        return { returnTime: 'La hora de regreso es anterior a la hora de salida' };
    }
    return {};
}

function returnDateForDuration(departureDate, durationDays) {
    const days = Number(durationDays);
    if (!departureDate || !Number.isInteger(days) || days < 1) return null;
    return dayjs(departureDate).add(days - 1, 'day').format('YYYY-MM-DD');
}

function toNullable(value) {
    return value.trim() || null;
}

function toPayload(values, rowNumber, wasDistributed) {
    const assignments = values.assignments ?? (wasDistributed ? [{
        driver: values.driver,
        vehicle: values.vehicle,
        resourceType: values.resourceType,
        borrowedVehicleId: values.borrowedVehicleId,
        passengers: values.passengers,
    }] : null);
    const firstAssignment = assignments?.[0];
    const resourceType = firstAssignment?.resourceType || values.resourceType;
    return {
        rowNumber,
        driver: toNullable(firstAssignment?.driver ?? values.driver),
        number: values.number.trim(),
        vehicle: resourceType === 'BORROWED_VEHICLE' ? null : toNullable(firstAssignment?.vehicle ?? values.vehicle),
        resourceType,
        borrowedVehicleId: resourceType === 'BORROWED_VEHICLE' ? (firstAssignment?.borrowedVehicleId || values.borrowedVehicleId || null) : null,
        approvalStatus: values.approvalStatus,
        passengers: Number(values.passengers),
        assignments: assignments?.map((assignment) => ({
            driver: toNullable(assignment.driver || ''),
            vehicle: toNullable(assignment.vehicle || ''),
            resourceType: assignment.resourceType || 'INTERNAL',
            borrowedVehicleId: assignment.resourceType === 'BORROWED_VEHICLE' ? (assignment.borrowedVehicleId || null) : null,
            passengers: Number(assignment.passengers),
        })) ?? null,
        executingUnit: values.executingUnit.trim(),
        responsible: values.responsible.trim(),
        destination: values.destination.trim(),
        durationDays: Number(values.durationDays),
        priority: Number(values.priority),
        modality: toNullable(values.modality),
        departureDate: values.departureDate,
        departureTime: values.departureTime,
        returnDate: values.returnDate,
        returnTime: values.returnTime,
        observations: toNullable(values.observations),
    };
}

function TourProcessingDraftRowForm({ row, draftId, isEdit, saving, showOptions, showDelete, approvalStatus, onToggleApproval, onDelete, onSubmit, onInvalid }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const [values, setValues] = useState(() => toFormValues(row));
    const [touched, setTouched] = useState({});
    const [submitted, setSubmitted] = useState(false);
    const [days, setDays] = useState(() => row.days ?? []);
    const [distributionDays, setDistributionDays] = useState(() => row.assignments?.map((assignment) => ({
        rowId: assignment.rowId,
        driver: assignment.driver,
        days: assignment.days ?? [],
    })) ?? []);
    const vehicleOptions = useVehicleOptions(isEdit);
    const borrowedVehicles = useBorrowedVehicleOptions(isEdit && (
        values.resourceType === 'BORROWED_VEHICLE'
        || values.assignments?.some((assignment) => assignment.resourceType === 'BORROWED_VEHICLE')
    ));
    const errors = useMemo(
        () => validate(values, vehicleOptions.options, borrowedVehicles.options),
        [values, vehicleOptions.options, borrowedVehicles.options],
    );
    const returnRangeErrors = useMemo(() => validateReturnRange(values), [values]);

    const updateDayInList = (updatedDay) => setDays((current) => current.map(
        (day) => (day.id === updatedDay.id ? updatedDay : day),
    ));
    const updateDistributionDay = (rowId, updatedDay) => setDistributionDays((current) => current.map((assignment) => (
        assignment.rowId === rowId
            ? { ...assignment, days: assignment.days.map((day) => (day.id === updatedDay.id ? updatedDay : day)) }
            : assignment
    )));

    const visibleError = (key) => (submitted || touched[key] || returnRangeErrors[key] ? errors[key] : undefined);

    const handleChange = (key, value) => setValues((current) => {
        const next = { ...current, [key]: value };
        if (key === 'durationDays') {
            const returnDate = returnDateForDuration(current.departureDate, value);
            if (returnDate) next.returnDate = returnDate;
        }
        return next;
    });

    const handleBlur = (key) => setTouched((current) => ({ ...current, [key]: true }));

    const handleResourceTypeChange = (_, nextResourceType) => {
        if (!nextResourceType) return;
        setValues((current) => ({
            ...current,
            resourceType: nextResourceType,
            vehicle: '',
            borrowedVehicleId: '',
        }));
    };

    const pendingContracting = values.assignments
        ? values.assignments.some((assignment) => assignment.resourceType === 'EXTERNAL'
            && (!assignment.driver.trim() || !assignment.vehicle.trim()))
        : values.resourceType === 'EXTERNAL' && (!values.driver.trim() || !values.vehicle.trim());

    const selectedVehicleCapacity = vehicleOptions.options.find((option) => option.label.toUpperCase() === values.vehicle.trim().toUpperCase())?.capacity;
    const selectedBorrowedCapacity = borrowedVehicles.options.find((option) => option.id === values.borrowedVehicleId)?.capacity;
    const maximumCapacity = Math.max(0, ...vehicleOptions.options
        .filter((option) => option.available !== false && option.capacity != null)
        .map((option) => option.capacity));
    const passengers = Number(values.passengers);
    const needsSeveralVehicles = !values.assignments && passengers > 1 && (
        (values.resourceType === 'INTERNAL' && (
            (selectedVehicleCapacity != null && passengers > selectedVehicleCapacity)
            || (selectedVehicleCapacity == null && maximumCapacity > 0 && passengers > maximumCapacity)
        ))
        || (values.resourceType === 'BORROWED_VEHICLE'
            && selectedBorrowedCapacity != null && passengers > selectedBorrowedCapacity)
    );

    const startDistribution = () => {
        const capacities = vehicleOptions.options
            .filter((option) => option.available !== false && option.capacity > 0)
            .map((option) => option.capacity)
            .sort((left, right) => right - left);
        let suggestedCount = 2;
        if (capacities.length > 0) {
            let combinedCapacity = 0;
            let count = 0;
            while (combinedCapacity < passengers && count < capacities.length) {
                combinedCapacity += capacities[count];
                count++;
            }
            suggestedCount = Math.max(2, combinedCapacity >= passengers ? count : Math.ceil(passengers / capacities[0]));
        }
        const selectedCapacity = values.resourceType === 'BORROWED_VEHICLE' ? selectedBorrowedCapacity : selectedVehicleCapacity;
        suggestedCount = Math.max(suggestedCount, minimumTrips(passengers, selectedCapacity) ?? 0);
        suggestedCount = Math.min(suggestedCount, passengers);
        setValues((current) => ({
            ...current,
            assignments: Array.from({ length: suggestedCount }, (_, index) => ({
                passengers: String(Math.floor(passengers / suggestedCount) + (index < passengers % suggestedCount ? 1 : 0)),
                driver: index === 0 ? current.driver : '',
                vehicle: index === 0 ? current.vehicle : '',
                resourceType: index === 0 ? current.resourceType : 'INTERNAL',
                borrowedVehicleId: index === 0 ? current.borrowedVehicleId : '',
            })),
        }));
    };

    const stopDistribution = () => setValues((current) => ({
        ...current,
        driver: current.assignments?.[0]?.driver ?? current.driver,
        vehicle: current.assignments?.[0]?.vehicle ?? current.vehicle,
        resourceType: current.assignments?.[0]?.resourceType ?? current.resourceType,
        borrowedVehicleId: current.assignments?.[0]?.borrowedVehicleId ?? current.borrowedVehicleId,
        assignments: null,
    }));

    const handleSubmit = (event) => {
        event.preventDefault();
        if (!isEdit || saving) return;
        setSubmitted(true);
        if (Object.keys(errors).length > 0) {
            onInvalid();
            return;
        }
        onSubmit(toPayload({ ...values, approvalStatus }, row.rowNumber, Boolean(row.assignments?.length)));
    };

    const fieldSx = fieldStyles(accentColor);

    const sectionLabel = (text) => renderSectionLabel(text, accentColor);

    const pickerTextField = (key) => ({
        size: 'small',
        fullWidth: true,
        required: true,
        error: Boolean(visibleError(key)),
        helperText: visibleError(key) || ' ',
        sx: fieldSx,
        onBlur: () => handleBlur(key),
    });

    const renderReadOnly = (label, value, disabled) => renderReadOnlyField(label, value, fieldSx, disabled);

    const renderField = (key, label, { required = false, numeric = false, multiline = false } = {}) => (
        <TextField
            label={label}
            value={isEdit ? values[key] : (values[key] || '—')}
            onChange={(event) => handleChange(key, numeric ? event.target.value.replace(/[^0-9]/g, '') : event.target.value)}
            onBlur={() => handleBlur(key)}
            required={isEdit && required}
            error={isEdit && Boolean(visibleError(key))}
            helperText={isEdit ? (visibleError(key) || ' ') : undefined}
            disabled={saving}
            multiline={multiline}
            minRows={multiline ? 3 : undefined}
            fullWidth
            size="small"
            slotProps={{
                input: { readOnly: !isEdit },
                htmlInput: numeric ? { inputMode: 'numeric' } : undefined,
            }}
            sx={fieldSx}
        />
    );

    const renderCatalogField = (key) => (
        <TourProcessingCatalogField
            kind={key}
            value={values[key]}
            onChange={(value) => handleChange(key, value)}
            onBlur={() => handleBlur(key)}
            isEdit={isEdit}
            disabled={saving}
            errorText={isEdit ? visibleError(key) : undefined}
            sx={fieldSx}
        />
    );

    const returnDateLimit = values.departureDate ? { minDate: dayjs(values.departureDate) } : {};
    const returnTimeLimit = values.departureTime && values.returnDate === values.departureDate
        ? { minTime: dayjs(`2000-01-01T${values.departureTime}`) }
        : {};

    const renderDate = (key, label, limit) => {
        if (!isEdit) return renderReadOnly(label, formatDate(values[key]));
        return (
            <DatePicker
                label={label}
                format="DD/MM/YYYY"
                value={values[key] ? dayjs(values[key]) : null}
                onChange={(value) => handleChange(key, value?.isValid() ? value.format('YYYY-MM-DD') : '')}
                disabled={saving}
                slotProps={{ textField: pickerTextField(key) }}
                {...limit}
            />
        );
    };

    const renderTime = (key, label, limit) => {
        if (!isEdit) return renderReadOnly(label, values[key]);
        return (
            <TimePicker
                label={label}
                ampm={false}
                format="HH:mm"
                value={values[key] ? dayjs(`2000-01-01T${values[key]}`) : null}
                onChange={(value) => handleChange(key, value?.isValid() ? value.format('HH:mm') : '')}
                disabled={saving}
                slotProps={{ textField: pickerTextField(key) }}
                {...limit}
            />
        );
    };

    return (
        <Box
            component="form"
            id={FORM_ID}
            noValidate
            onSubmit={handleSubmit}
            sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'flex', flexDirection: 'column', gap: 3 }}
        >
            <Box>
                {sectionLabel('Identificación')}
                <Box sx={gridSx('1fr 1fr')}>
                    {renderReadOnly(SOURCE_ROW_LABEL, row.rowNumber ? String(row.rowNumber) : 'Se asigna al guardar', true)}
                    {renderField('number', 'Número', { required: true })}
                    {!isEdit && draftRowDuplicateLabel(row) && (
                        <Box sx={{ gridColumn: '1 / -1' }}>
                            <DuplicateChip row={row} />
                        </Box>
                    )}
                </Box>
            </Box>

            <Divider />

            <Box>
                {sectionLabel('Chofer y vehículo')}
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2, flexWrap: 'wrap' }}>
                    {isEdit && !values.assignments ? (
                        <ToggleButtonGroup
                            value={values.resourceType}
                            onChange={handleResourceTypeChange}
                            exclusive
                            size="small"
                            disabled={saving}
                        >
                            {RESOURCE_TYPE_OPTIONS.map((option) => (
                                <ToggleButton key={option.value} value={option.value} sx={{ textTransform: 'none', fontSize: 12.5, fontWeight: 600, px: 1.5 }}>
                                    {option.label}
                                </ToggleButton>
                            ))}
                        </ToggleButtonGroup>
                    ) : !values.assignments ? (
                        <Chip
                            label={RESOURCE_TYPE_OPTIONS.find((option) => option.value === values.resourceType)?.label ?? 'Interno'}
                            size="small"
                            variant="outlined"
                        />
                    ) : <Chip label={`${values.assignments.length} viajes`} size="small" variant="outlined" />}
                    {pendingContracting && (
                        <Chip label="Pendiente de contratar" size="small" color="warning" variant="outlined" />
                    )}
                </Box>
                {!values.assignments && <Box sx={gridSx('repeat(2, 1fr)')}>
                    {values.resourceType === 'EXTERNAL'
                        ? renderField('driver', 'Chofer / Empresa')
                        : renderCatalogField('driver')}
                    {values.resourceType === 'EXTERNAL' && renderField('vehicle', 'Placa (contratación externa)')}
                    {values.resourceType === 'BORROWED_VEHICLE' && (isEdit ? (
                        <Autocomplete
                            options={borrowedVehicles.options}
                            loading={borrowedVehicles.loading}
                            value={borrowedVehicles.options.find((option) => option.id === values.borrowedVehicleId) ?? null}
                            onChange={(_, next) => handleChange('borrowedVehicleId', next?.id ?? '')}
                            getOptionLabel={(option) => option.label ?? ''}
                            isOptionEqualToValue={(option, value) => option.id === value.id}
                            disabled={saving}
                            size="small"
                            renderInput={(params) => (
                                <TextField
                                    {...params}
                                    label="Vehículo prestado"
                                    required
                                    error={Boolean(visibleError('borrowedVehicleId'))}
                                    helperText={visibleError('borrowedVehicleId') || ' '}
                                    onBlur={() => handleBlur('borrowedVehicleId')}
                                    sx={fieldSx}
                                />
                            )}
                        />
                    ) : renderReadOnly('Vehículo prestado', row.borrowedVehiclePlate))}
                    {values.resourceType === 'INTERNAL' && renderCatalogField('vehicle')}
                </Box>}
                {values.assignments && (
                    <TourProcessingDraftDistributionEditor
                        assignments={values.assignments}
                        totalPassengers={values.passengers}
                        onChange={(assignments) => handleChange('assignments', assignments)}
                        isEdit={isEdit}
                        saving={saving}
                        fieldSx={fieldSx}
                        borrowedVehicles={borrowedVehicles}
                        vehicleOptions={vehicleOptions.options}
                        error={submitted ? errors.assignments : undefined}
                    />
                )}
            </Box>

            <Divider />

            <Box>
                {sectionLabel('Pasajeros')}
                <Box sx={gridSx('repeat(3, 1fr)')}>
                    {renderField('passengers', 'Pasajeros', { required: true, numeric: true })}
                </Box>
                {isEdit && (
                    <Box sx={{ mt: 1 }}>
                        <Box sx={{ minHeight: { xs: 56, sm: 32 }, display: 'flex', alignItems: 'flex-start', gap: 1 }}>
                            {needsSeveralVehicles && (
                                <>
                                    <WarningAmberOutlinedIcon color="warning" fontSize="small" />
                                    <Typography sx={{ fontSize: 13, fontWeight: 600 }}>
                                        Esta gira necesita más de un viaje para {passengers} pasajeros.
                                    </Typography>
                                </>
                            )}
                        </Box>
                        {values.assignments ? (
                            <Button onClick={stopDistribution} disabled={saving} size="small" sx={{ textTransform: 'none', fontWeight: 700 }}>
                                Volver a un solo viaje
                            </Button>
                        ) : (
                            <Button onClick={startDistribution} disabled={saving || passengers < 2} size="small" sx={{ textTransform: 'none', fontWeight: 700 }}>
                                Dividir en varios viajes
                            </Button>
                        )}
                    </Box>
                )}
            </Box>

            <Divider />

            <Box>
                {sectionLabel('Gira')}
                <Box sx={gridSx('repeat(3, 1fr)')}>
                    {renderField('executingUnit', 'Unidad ejecutora', { required: true })}
                    {renderField('responsible', 'Responsable', { required: true })}
                    {renderField('destination', 'Destinos', { required: true })}
                    {renderField('durationDays', 'Duración (días)', { required: true, numeric: true })}
                    {renderField('priority', 'Prioridad', { required: true, numeric: true })}
                    {renderField('modality', 'Modalidad')}
                </Box>
            </Box>

            <Divider />

            <Box>
                {sectionLabel('Fechas y horas')}
                <Box sx={gridSx('1fr 1fr')}>
                    {renderDate('departureDate', 'Fecha salida')}
                    {renderTime('departureTime', 'Hora salida')}
                    {renderDate('returnDate', 'Fecha regreso', returnDateLimit)}
                    {renderTime('returnTime', 'Hora regreso', returnTimeLimit)}
                </Box>
            </Box>

            <Divider />

            {days.length > 0 && !values.assignments && (
                <>
                    <Box>
                        {sectionLabel('Días')}
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                            {days.map((day) => (
                                <TourProcessingDraftRowDayCard
                                    key={day.id}
                                    day={day}
                                    draftId={draftId}
                                    rowId={row.id}
                                    canEdit={isEdit}
                                    onSaved={updateDayInList}
                                />
                            ))}
                        </Box>
                    </Box>

                    <Divider />
                </>
            )}

            {values.assignments && distributionDays.some((assignment) => assignment.days.length > 0) && (
                <>
                    <Box>
                        {sectionLabel('Días por chofer')}
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                            {distributionDays.filter((assignment) => assignment.days.length > 0).map((assignment, index) => (
                                <Box key={assignment.rowId}>
                                    <Typography sx={{ fontSize: 13, fontWeight: 700, mb: 1 }}>
                                        {assignment.driver || `Vehículo ${index + 1}`}
                                    </Typography>
                                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                                        {assignment.days.map((day) => (
                                            <TourProcessingDraftRowDayCard
                                                key={day.id}
                                                day={day}
                                                draftId={draftId}
                                                rowId={assignment.rowId}
                                                canEdit={isEdit}
                                                onSaved={(updatedDay) => updateDistributionDay(assignment.rowId, updatedDay)}
                                            />
                                        ))}
                                    </Box>
                                </Box>
                            ))}
                        </Box>
                    </Box>
                    <Divider />
                </>
            )}

            <Box>
                {sectionLabel('Observaciones')}
                {renderField('observations', 'Observaciones', { multiline: true })}
            </Box>

            {showOptions && (
                <>
                    <Divider />

                    <Box>
                        {sectionLabel('Opciones')}
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                            <Chip
                                label={`Estado: ${approvalStatusLabel(approvalStatus)}`}
                                color={approvalStatusColor(approvalStatus)}
                                size="small"
                                variant="outlined"
                                sx={{ alignSelf: 'flex-start' }}
                            />
                            <Button
                                onClick={onToggleApproval}
                                disabled={saving}
                                variant="outlined"
                                size="large"
                                fullWidth
                                startIcon={approvalStatus === APPROVAL_STATUS.NOT_APPROVED ? <CheckCircleOutlineOutlinedIcon /> : <BlockOutlinedIcon />}
                                sx={OPTION_BUTTON_SX}
                            >
                                {approvalToggleLabel(approvalStatus)}
                            </Button>
                            {showDelete && (
                                <Button
                                    onClick={onDelete}
                                    disabled={saving}
                                    color="error"
                                    variant="outlined"
                                    size="large"
                                    fullWidth
                                    startIcon={<DeleteOutlineOutlinedIcon />}
                                    sx={OPTION_BUTTON_SX}
                                >
                                    Eliminar
                                </Button>
                            )}
                        </Box>
                    </Box>
                </>
            )}
        </Box>
    );
}

export default function TourProcessingDraftRowModal({ open, mode, row, loadingRow = false, draftId, canDelete = true, onClose, onSaved, onDeleted, onEdit }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const queryClient = useQueryClient();
    const [alert, setAlert] = useState(null);
    const [deleteOpen, setDeleteOpen] = useState(false);
    const isCreate = mode === 'create';
    const isEdit = mode !== 'view';
    const canChangeExistingRow = mode === 'edit' && Boolean(row);
    const savedApprovalStatus = row?.approvalStatus ?? APPROVAL_STATUS.APPROVED;
    const approvalSourceKey = `${open}-${mode}-${row?.id}-${savedApprovalStatus}`;
    const [approvalStatus, setApprovalStatus] = useState(savedApprovalStatus);
    const [approvalSource, setApprovalSource] = useState(approvalSourceKey);

    if (approvalSource !== approvalSourceKey) {
        setApprovalSource(approvalSourceKey);
        setApprovalStatus(savedApprovalStatus);
    }

    const toggleApproval = () => setApprovalStatus((current) => toggledApprovalStatus(current));

    const saveMutation = useMutation({
        mutationFn: (payload) => (isCreate
            ? createTourProcessingDraftRow(draftId, payload)
            : updateTourProcessingDraftRow(draftId, row.id, payload)),
        onSuccess: (savedRow, payload) => {
            replaceCachedDraftRow(queryClient, draftId, savedRow);
            void invalidateTourProcessingDraft(queryClient, draftId);
            onSaved?.({
                created: isCreate,
                number: payload.number,
                changes: isCreate ? [] : describeDraftRowChanges(row, payload),
            });
            onClose();
        },
        onError: (error) => {
            const message = getFriendlyApiErrorMessage(error, 'No se pudo guardar la fila');
            setAlert(errorCodeOf(error) === DRIVER_UNAVAILABLE_CODE
                ? { type: 'warning', title: 'Chofer no disponible', message }
                : { type: 'error', message });
        },
    });

    const deleteMutation = useMutation({
        mutationFn: () => deleteTourProcessingDraftRow(draftId, row.id),
        onSuccess: () => {
            setDeleteOpen(false);
            void invalidateTourProcessingDraft(queryClient, draftId);
            onDeleted?.();
            onClose();
        },
        onError: (error) => {
            setDeleteOpen(false);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo eliminar la gira') });
        },
    });

    const deleting = deleteMutation.isPending;
    const saving = saveMutation.isPending || deleting;

    const confirmDelete = () => {
        if (!deleting) deleteMutation.mutate();
    };

    const contentSx = {
        overflowY: 'auto',
        '&::-webkit-scrollbar': { width: '5px' },
        '&::-webkit-scrollbar-track': { background: 'transparent' },
        '&::-webkit-scrollbar-thumb': { background: `color-mix(in srgb, ${accentColor} 25%, transparent)`, borderRadius: '4px' },
        '&::-webkit-scrollbar-thumb:hover': { background: `color-mix(in srgb, ${accentColor} 45%, transparent)` },
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={saving ? undefined : onClose}
                maxWidth="md"
                icon={AltRouteOutlinedIcon}
                title={MODAL_TITLES[mode]}
                subtitle={row ? `Número de gira ${row.number}` : (loadingRow ? 'Cargando gira...' : 'Nueva fila del borrador')}
                loading={saving || loadingRow}
                footerLeft={canChangeExistingRow ? (
                    <Button
                        onClick={toggleApproval}
                        disabled={saving}
                        variant="outlined"
                        size="small"
                        startIcon={approvalStatus === APPROVAL_STATUS.NOT_APPROVED ? <CheckCircleOutlineOutlinedIcon /> : <BlockOutlinedIcon />}
                        sx={{ textTransform: 'none', fontWeight: 600, fontSize: 12.5, borderRadius: '8px' }}
                    >
                        {approvalToggleLabel(approvalStatus)}
                    </Button>
                ) : undefined}
                secondaryButton={{ label: isEdit ? 'Cancelar' : 'Cerrar', onClick: onClose, disabled: saving }}
                primaryButton={isEdit ? {
                    label: saveMutation.isPending ? 'Guardando...' : (isCreate ? 'Crear gira' : 'Guardar cambios'),
                    type: 'submit',
                    form: FORM_ID,
                    disabled: saving,
                    startIcon: <SaveOutlinedIcon />,
                } : (onEdit ? {
                    label: 'Editar',
                    onClick: onEdit,
                    startIcon: <EditOutlinedIcon />,
                    sx: { order: -1 },
                } : undefined)}
                contentSx={contentSx}
            >
                {(row || isCreate) && (
                    <TourProcessingDraftRowForm
                        key={`${row?.id ?? 'new'}-${mode}`}
                        row={row ?? EMPTY_ROW}
                        draftId={draftId}
                        isEdit={isEdit}
                        saving={saving}
                        showOptions={canChangeExistingRow}
                        showDelete={canDelete}
                        approvalStatus={approvalStatus}
                        onToggleApproval={toggleApproval}
                        onDelete={() => setDeleteOpen(true)}
                        onSubmit={(payload) => saveMutation.mutate(payload)}
                        onInvalid={() => setAlert({ type: 'warning', message: 'Revisa los datos antes de continuar' })}
                    />
                )}
            </GeneralModal>

            <DialogModal
                open={deleteOpen}
                type="delete"
                title="Eliminar gira"
                message={row ? `Se eliminará la fila ${row.rowNumber} (Número ${row.number}) del borrador.` : ''}
                confirmLabel={deleting ? 'Eliminando...' : 'Sí, eliminar'}
                onConfirm={confirmDelete}
                onClose={() => setDeleteOpen(false)}
            />

            <DialogModal
                open={Boolean(alert)}
                type={alert?.type}
                title={alert?.title}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}
