import { useMemo, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Box, Button, Divider, TextField, Typography, useTheme } from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { TimePicker } from '@mui/x-date-pickers/TimePicker';
import dayjs from 'dayjs';
import AltRouteOutlinedIcon from '@mui/icons-material/AltRouteOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { queryKeys } from '../../../../common/query';
import { getFriendlyApiErrorMessage } from '../../../../common/utils';
import { formatDate } from '../../transportUtils.js';
import {
    createCleaningDraftRow,
    deleteCleaningDraftRow,
    updateCleaningDraftRow,
} from '../../services/cleaning/cleaningService.js';
import { draftRowDuplicateLabel } from './cleaningDraftLabels.js';
import DuplicateChip from './DuplicateChip.jsx';

const FORM_ID = 'cleaning-draft-row-form';
const EMPTY_ROW = {};
const MODAL_TITLES = { view: 'Ver fila', edit: 'Editar fila', create: 'Crear gira' };
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

function toFormValues(row) {
    return {
        driver: row.driver ?? '',
        number: row.number ?? '',
        vehicle: row.vehicle ?? '',
        passengers: String(row.passengers ?? ''),
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

function validate(values) {
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

    if (!errors.departureDate && !errors.returnDate) {
        const sameDay = values.returnDate === values.departureDate;
        if (values.returnDate < values.departureDate) {
            errors.returnDate = 'La fecha de regreso es anterior a la fecha de salida';
        } else if (sameDay && !errors.departureTime && !errors.returnTime && values.returnTime < values.departureTime) {
            errors.returnTime = 'La hora de regreso es anterior a la hora de salida';
        }
    }

    return errors;
}

function toNullable(value) {
    return value.trim() || null;
}

function toPayload(values, rowNumber) {
    return {
        rowNumber,
        driver: toNullable(values.driver),
        number: values.number.trim(),
        vehicle: toNullable(values.vehicle),
        passengers: Number(values.passengers),
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

function CleaningDraftRowForm({ row, isEdit, saving, onSubmit, onInvalid }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const [values, setValues] = useState(() => toFormValues(row));
    const [touched, setTouched] = useState({});
    const [submitted, setSubmitted] = useState(false);
    const errors = useMemo(() => validate(values), [values]);

    const visibleError = (key) => (submitted || touched[key] ? errors[key] : undefined);

    const handleChange = (key, value) => setValues((current) => ({ ...current, [key]: value }));

    const handleBlur = (key) => setTouched((current) => ({ ...current, [key]: true }));

    const handleSubmit = (event) => {
        event.preventDefault();
        if (!isEdit || saving) return;
        setSubmitted(true);
        if (Object.keys(errors).length > 0) {
            onInvalid();
            return;
        }
        onSubmit(toPayload(values, row.rowNumber));
    };

    const fieldSx = {
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

    const gridSx = (columns) => ({
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', sm: columns },
        gap: 2,
    });

    const sectionLabel = (text) => (
        <Typography sx={{
            fontSize: 11.5, fontWeight: 700, letterSpacing: '0.08em',
            textTransform: 'uppercase', color: accentColor, mb: 1.5,
        }}>
            {text}
        </Typography>
    );

    const pickerTextField = (key) => ({
        size: 'small',
        fullWidth: true,
        required: true,
        error: Boolean(visibleError(key)),
        helperText: visibleError(key) || ' ',
        sx: fieldSx,
        onBlur: () => handleBlur(key),
    });

    const renderReadOnly = (label, value) => (
        <TextField
            label={label}
            value={value || '—'}
            fullWidth
            size="small"
            slotProps={{ input: { readOnly: true } }}
            sx={fieldSx}
        />
    );

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

    const renderDate = (key, label) => {
        if (!isEdit) return renderReadOnly(label, formatDate(values[key]));
        return (
            <DatePicker
                label={label}
                format="DD/MM/YYYY"
                value={values[key] ? dayjs(values[key]) : null}
                onChange={(value) => handleChange(key, value?.isValid() ? value.format('YYYY-MM-DD') : '')}
                disabled={saving}
                slotProps={{ textField: pickerTextField(key) }}
            />
        );
    };

    const renderTime = (key, label) => {
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
                    {renderReadOnly('Fila', row.rowNumber ? String(row.rowNumber) : 'Se asigna al guardar')}
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
                <Box sx={gridSx('repeat(3, 1fr)')}>
                    {renderField('driver', 'Chofer')}
                    {renderField('vehicle', 'Vehículo')}
                    {renderField('passengers', 'Pasajeros', { required: true, numeric: true })}
                </Box>
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
                    {renderDate('returnDate', 'Fecha regreso')}
                    {renderTime('returnTime', 'Hora regreso')}
                </Box>
            </Box>

            <Divider />

            <Box>
                {sectionLabel('Observaciones')}
                {renderField('observations', 'Observaciones', { multiline: true })}
            </Box>
        </Box>
    );
}

export default function CleaningDraftRowModal({ open, mode, row, draftId, onClose, onSaved, onDeleted, onEdit }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const queryClient = useQueryClient();
    const [alert, setAlert] = useState(null);
    const [deleteOpen, setDeleteOpen] = useState(false);
    const isCreate = mode === 'create';
    const isEdit = mode !== 'view';
    const canDelete = mode === 'edit' && Boolean(row);

    const saveMutation = useMutation({
        mutationFn: (payload) => (isCreate
            ? createCleaningDraftRow(draftId, payload)
            : updateCleaningDraftRow(draftId, row.id, payload)),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: queryKeys.transport.cleaningDrafts() });
            onSaved?.();
            onClose();
        },
        onError: (error) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo guardar la fila') });
        },
    });

    const deleteMutation = useMutation({
        mutationFn: () => deleteCleaningDraftRow(draftId, row.id),
        onSuccess: async () => {
            setDeleteOpen(false);
            await queryClient.invalidateQueries({ queryKey: queryKeys.transport.cleaningDrafts() });
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
                subtitle={row ? `Fila ${row.rowNumber} del borrador` : 'Nueva fila del borrador'}
                loading={saving}
                footerLeft={canDelete ? (
                    <Button
                        onClick={() => setDeleteOpen(true)}
                        disabled={saving}
                        color="error"
                        variant="outlined"
                        size="small"
                        startIcon={<DeleteOutlineOutlinedIcon />}
                        sx={{ textTransform: 'none', fontWeight: 600, fontSize: 12.5, borderRadius: '8px' }}
                    >
                        Eliminar
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
                    <CleaningDraftRowForm
                        key={`${row?.id ?? 'new'}-${mode}`}
                        row={row ?? EMPTY_ROW}
                        isEdit={isEdit}
                        saving={saving}
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
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}
