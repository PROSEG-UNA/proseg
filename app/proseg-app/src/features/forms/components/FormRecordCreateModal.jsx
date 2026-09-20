import { useEffect, useMemo, useState } from 'react';
import {
    Box,
    Button,
    IconButton,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Typography,
} from '@mui/material';
import AddCircleOutlinedIcon from '@mui/icons-material/AddCircleOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import GeneralModal from '../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../common/components/DialogModal.jsx';
import { createFormRecord } from '../services/formsService';

const INITIAL_VALUES = {
    date: null,
    group: '',
    supervisor: '',
    observation: '',
    securityOfficer: '',
    vehicleAccessOperator: '',
    arrivalTime: '',
    position: '',
    reason: '',
    time: '',
    guard: '',
    shift: '',
    shiftTime: '',
    workPosition: '',
    absenceReason: '',
};

const createOvertimeRow = (index, formDate = null) => ({
    id: `row-${index}-${Date.now()}`,
    number: index + 1,
    name: '',
    cedula: '',
    date: formDate,
    entryTime: '',
    exitTime: '',
    totalHours: '',
});

function calculateTotalHours(entryTime, exitTime) {
    if (!entryTime || !exitTime) return '';
    const [entryHour, entryMinute] = entryTime.split(':').map(Number);
    const [exitHour, exitMinute] = exitTime.split(':').map(Number);
    if ([entryHour, entryMinute, exitHour, exitMinute].some(Number.isNaN)) return '';

    const entryMinutes = entryHour * 60 + entryMinute;
    const exitMinutes = exitHour * 60 + exitMinute;
    if (exitMinutes <= entryMinutes) return '';

    return ((exitMinutes - entryMinutes) / 60).toFixed(2);
}

function normalizeOvertimeRows(rows, formDate) {
    return rows.map((row, index) => ({
        ...row,
        number: index + 1,
        date: row.date ?? formDate,
        totalHours: calculateTotalHours(row.entryTime, row.exitTime),
    }));
}

function buildPayload(formType, values, overtimeRows) {
    const isoDate = values.date ? dayjs(values.date).format('YYYY-MM-DD') : null;

    switch (formType?.code) {
        case 'OVERTIME_REPORT':
            return {
                fecha: isoDate,
                grupo: values.group,
                supervisor: values.supervisor,
                detalle: overtimeRows.map((row) => ({
                    no: row.number,
                    nombre: row.name,
                    cedula: row.cedula,
                    fecha: row.date ? dayjs(row.date).format('YYYY-MM-DD') : null,
                    horaEntrada: row.entryTime || null,
                    horaSalida: row.exitTime || null,
                    totalHoras: row.totalHours === '' ? null : Number(row.totalHours),
                })),
                observacion: values.observation || '',
            };
        case 'ABSENCE_REPORT':
            return {
                fecha: isoDate,
                hora: values.time || null,
                guarda: values.guard,
                turno: values.shift,
                horaTurno: values.shiftTime,
                puestoTrabajo: values.workPosition,
                motivoAusencia: values.absenceReason,
                supervisor: values.supervisor,
            };
        case 'LATE_ARRIVAL_REPORT':
            return {
                fecha: isoDate,
                oficialSeguridad: values.securityOfficer,
                operadorAcceso: values.vehicleAccessOperator,
                horaLlegada: values.arrivalTime || null,
                puesto: values.position,
                motivo: values.reason,
                supervisor: values.supervisor,
            };
        default:
            return {};
    }
}

function validate(formType, values, overtimeRows) {
    const nextErrors = {};
    const rowErrors = {};

    if (!values.date) {
        nextErrors.date = 'La fecha es obligatoria';
    }

    if (formType?.code === 'OVERTIME_REPORT') {
        if (!values.group) nextErrors.group = 'El grupo es obligatorio';
        if (!values.supervisor) nextErrors.supervisor = 'El supervisor es obligatorio';
        if (!overtimeRows.length) {
            nextErrors.details = 'Debe existir al menos una fila de detalle';
        }

        overtimeRows.forEach((row) => {
            const current = {};
            if (!row.name) current.name = 'Requerido';
            if (!row.cedula) current.cedula = 'Requerido';
            if (!row.date) current.date = 'Requerido';
            if (!row.entryTime) current.entryTime = 'Requerido';
            if (!row.exitTime) current.exitTime = 'Requerido';
            if (row.entryTime && row.exitTime && !row.totalHours) {
                current.exitTime = 'La hora de salida debe ser posterior a la hora de entrada';
            }
            if (Object.keys(current).length > 0) {
                rowErrors[row.id] = current;
            }
        });
    }

    if (formType?.code === 'ABSENCE_REPORT') {
        if (!values.time) nextErrors.time = 'La hora es obligatoria';
        if (!values.guard) nextErrors.guard = 'El guarda es obligatorio';
        if (!values.shift) nextErrors.shift = 'El turno es obligatorio';
        if (!values.shiftTime) nextErrors.shiftTime = 'La hora del turno es obligatoria';
        if (!values.workPosition) nextErrors.workPosition = 'El puesto de trabajo es obligatorio';
        if (!values.absenceReason) nextErrors.absenceReason = 'El motivo de ausencia es obligatorio';
        if (!values.supervisor) nextErrors.supervisor = 'El supervisor es obligatorio';
    }

    if (formType?.code === 'LATE_ARRIVAL_REPORT') {
        if (!values.securityOfficer) nextErrors.securityOfficer = 'El oficial de seguridad es obligatorio';
        if (!values.vehicleAccessOperator) nextErrors.vehicleAccessOperator = 'El operador de acceso vehicular es obligatorio';
        if (!values.arrivalTime) nextErrors.arrivalTime = 'La hora de llegada tardía es obligatoria';
        if (!values.position) nextErrors.position = 'El puesto es obligatorio';
        if (!values.reason) nextErrors.reason = 'El motivo de la llegada tardía es obligatorio';
        if (!values.supervisor) nextErrors.supervisor = 'El supervisor es obligatorio';
    }

    return { nextErrors, rowErrors };
}

export function buildFormPayload(formType, values, overtimeRows = []) {
    return buildPayload(formType, values, overtimeRows);
}

export default function FormRecordCreateModal({ open, formType, onClose, onSaved }) {
    const [values, setValues] = useState(INITIAL_VALUES);
    const [overtimeRows, setOvertimeRows] = useState([]);
    const [errors, setErrors] = useState({});
    const [rowErrors, setRowErrors] = useState({});
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null);

    useEffect(() => {
        if (!open) {
            setValues(INITIAL_VALUES);
            setOvertimeRows([]);
            setErrors({});
            setRowErrors({});
            setSaving(false);
            setAlert(null);
            return;
        }

        if (formType?.code === 'OVERTIME_REPORT') {
            setOvertimeRows([createOvertimeRow(0)]);
        } else {
            setOvertimeRows([]);
        }
    }, [open, formType]);

    const title = useMemo(() => formType?.name ?? 'Nuevo formulario', [formType]);

    const handleChange = (field, value) => {
        setValues((prev) => ({ ...prev, [field]: value }));
        setErrors((prev) => ({ ...prev, [field]: '' }));

        if (field === 'date' && formType?.code === 'OVERTIME_REPORT') {
            setOvertimeRows((prev) => normalizeOvertimeRows(prev, value));
        }
    };

    const handleOvertimeRowChange = (rowId, field, value) => {
        setOvertimeRows((prev) => normalizeOvertimeRows(prev.map((row) => {
            if (row.id !== rowId) return row;
            return {
                ...row,
                [field]: value,
            };
        }), values.date));

        setErrors((prev) => ({ ...prev, details: '' }));
        setRowErrors((prev) => ({
            ...prev,
            [rowId]: {
                ...prev[rowId],
                [field]: '',
            },
        }));
    };

    const handleAddRow = () => {
        setOvertimeRows((prev) => normalizeOvertimeRows([...prev, createOvertimeRow(prev.length, values.date)], values.date));
        setErrors((prev) => ({ ...prev, details: '' }));
    };

    const handleRemoveRow = (rowId) => {
        setOvertimeRows((prev) => normalizeOvertimeRows(prev.filter((row) => row.id !== rowId), values.date));
        setRowErrors((prev) => {
            const next = { ...prev };
            delete next[rowId];
            return next;
        });
    };

    const handleSubmit = async () => {
        const normalizedRows = normalizeOvertimeRows(overtimeRows, values.date);
        const { nextErrors, rowErrors: nextRowErrors } = validate(formType, values, normalizedRows);
        setErrors(nextErrors);
        setRowErrors(nextRowErrors);

        if (Object.keys(nextErrors).length > 0 || Object.keys(nextRowErrors).length > 0 || !formType?.id) {
            return;
        }

        setSaving(true);
        try {
            await createFormRecord({
                formTypeId: formType.id,
                data: buildPayload(formType, values, normalizedRows),
            });
            setAlert({ type: 'success', message: 'Formulario registrado correctamente' });
            onSaved?.();
        } catch (error) {
            setAlert({
                type: 'error',
                message: error?.response?.data?.message ?? error?.message ?? 'No se pudo registrar el formulario',
            });
        } finally {
            setSaving(false);
        }
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                title={title}
                subtitle="Registro de formulario físico"
                maxWidth="lg"
                primaryButton={{
                    label: saving ? 'Guardando...' : 'Guardar',
                    onClick: handleSubmit,
                    disabled: saving,
                }}
                secondaryButton={{
                    label: 'Cancelar',
                    onClick: onClose,
                    disabled: saving,
                }}
                contentSx={{ p: 3 }}
            >
                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 2 }}>
                    <DatePicker
                        label="Fecha"
                        value={values.date}
                        onChange={(value) => handleChange('date', value)}
                        slotProps={{ textField: { fullWidth: true, error: !!errors.date, helperText: errors.date } }}
                    />

                    {formType?.code === 'OVERTIME_REPORT' ? (
                        <>
                            <TextField label="Grupo" value={values.group} onChange={(e) => handleChange('group', e.target.value)} error={!!errors.group} helperText={errors.group} fullWidth />
                            <TextField label="Supervisor" value={values.supervisor} onChange={(e) => handleChange('supervisor', e.target.value)} error={!!errors.supervisor} helperText={errors.supervisor} fullWidth />
                            <TextField label="Observación" value={values.observation} onChange={(e) => handleChange('observation', e.target.value)} multiline minRows={3} fullWidth sx={{ gridColumn: { md: '1 / span 2' } }} />

                            <Box sx={{ gridColumn: '1 / -1', pt: 1 }}>
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5, gap: 2, flexWrap: 'wrap' }}>
                                    <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                                        Detalle
                                    </Typography>
                                    <Button startIcon={<AddCircleOutlinedIcon />} onClick={handleAddRow} sx={{ textTransform: 'none' }}>
                                        Agregar fila
                                    </Button>
                                </Box>

                                {errors.details ? (
                                    <Typography color="error" sx={{ mb: 1.5, fontSize: 13 }}>
                                        {errors.details}
                                    </Typography>
                                ) : null}

                                <TableContainer component={Paper} variant="outlined">
                                    <Table size="small">
                                        <TableHead>
                                            <TableRow>
                                                <TableCell>No.</TableCell>
                                                <TableCell>Nombre</TableCell>
                                                <TableCell>Cédula</TableCell>
                                                <TableCell>Fecha</TableCell>
                                                <TableCell>Hora entrada</TableCell>
                                                <TableCell>Hora salida</TableCell>
                                                <TableCell>Total horas</TableCell>
                                                <TableCell align="center">Acción</TableCell>
                                            </TableRow>
                                        </TableHead>
                                        <TableBody>
                                            {overtimeRows.map((row) => (
                                                <TableRow key={row.id}>
                                                    <TableCell sx={{ minWidth: 60 }}>{row.number}</TableCell>
                                                    <TableCell sx={{ minWidth: 180 }}>
                                                        <TextField
                                                            fullWidth
                                                            size="small"
                                                            value={row.name}
                                                            onChange={(e) => handleOvertimeRowChange(row.id, 'name', e.target.value)}
                                                            error={!!rowErrors[row.id]?.name}
                                                            helperText={rowErrors[row.id]?.name}
                                                        />
                                                    </TableCell>
                                                    <TableCell sx={{ minWidth: 160 }}>
                                                        <TextField
                                                            fullWidth
                                                            size="small"
                                                            value={row.cedula}
                                                            onChange={(e) => handleOvertimeRowChange(row.id, 'cedula', e.target.value)}
                                                            error={!!rowErrors[row.id]?.cedula}
                                                            helperText={rowErrors[row.id]?.cedula}
                                                        />
                                                    </TableCell>
                                                    <TableCell sx={{ minWidth: 170 }}>
                                                        <DatePicker
                                                            value={row.date}
                                                            onChange={(value) => handleOvertimeRowChange(row.id, 'date', value)}
                                                            slotProps={{ textField: { fullWidth: true, size: 'small', error: !!rowErrors[row.id]?.date, helperText: rowErrors[row.id]?.date } }}
                                                        />
                                                    </TableCell>
                                                    <TableCell sx={{ minWidth: 150 }}>
                                                        <TextField
                                                            fullWidth
                                                            size="small"
                                                            type="time"
                                                            value={row.entryTime}
                                                            onChange={(e) => handleOvertimeRowChange(row.id, 'entryTime', e.target.value)}
                                                            error={!!rowErrors[row.id]?.entryTime}
                                                            helperText={rowErrors[row.id]?.entryTime}
                                                            slotProps={{ inputLabel: { shrink: true } }}
                                                        />
                                                    </TableCell>
                                                    <TableCell sx={{ minWidth: 150 }}>
                                                        <TextField
                                                            fullWidth
                                                            size="small"
                                                            type="time"
                                                            value={row.exitTime}
                                                            onChange={(e) => handleOvertimeRowChange(row.id, 'exitTime', e.target.value)}
                                                            error={!!rowErrors[row.id]?.exitTime}
                                                            helperText={rowErrors[row.id]?.exitTime}
                                                            slotProps={{ inputLabel: { shrink: true } }}
                                                        />
                                                    </TableCell>
                                                    <TableCell sx={{ minWidth: 130 }}>
                                                        <TextField
                                                            fullWidth
                                                            size="small"
                                                            value={row.totalHours}
                                                            slotProps={{ input: { readOnly: true } }}
                                                        />
                                                    </TableCell>
                                                    <TableCell align="center" sx={{ minWidth: 80 }}>
                                                        <IconButton onClick={() => handleRemoveRow(row.id)} disabled={overtimeRows.length === 1}>
                                                            <DeleteOutlinedIcon />
                                                        </IconButton>
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                            </Box>
                        </>
                    ) : null}

                    {formType?.code === 'ABSENCE_REPORT' ? (
                        <>
                            <TextField label="Hora" type="time" value={values.time} onChange={(e) => handleChange('time', e.target.value)} error={!!errors.time} helperText={errors.time} fullWidth slotProps={{ inputLabel: { shrink: true } }} />
                            <TextField label="Guarda" value={values.guard} onChange={(e) => handleChange('guard', e.target.value)} error={!!errors.guard} helperText={errors.guard} fullWidth />
                            <TextField label="Turno" value={values.shift} onChange={(e) => handleChange('shift', e.target.value)} error={!!errors.shift} helperText={errors.shift} fullWidth />
                            <TextField label="Hora del turno" value={values.shiftTime} onChange={(e) => handleChange('shiftTime', e.target.value)} error={!!errors.shiftTime} helperText={errors.shiftTime} fullWidth />
                            <TextField label="Puesto de trabajo" value={values.workPosition} onChange={(e) => handleChange('workPosition', e.target.value)} error={!!errors.workPosition} helperText={errors.workPosition} fullWidth />
                            <TextField label="Supervisor" value={values.supervisor} onChange={(e) => handleChange('supervisor', e.target.value)} error={!!errors.supervisor} helperText={errors.supervisor} fullWidth />
                            <TextField label="Motivo de ausencia" value={values.absenceReason} onChange={(e) => handleChange('absenceReason', e.target.value)} error={!!errors.absenceReason} helperText={errors.absenceReason} multiline minRows={3} fullWidth sx={{ gridColumn: { md: '1 / span 2' } }} />
                        </>
                    ) : null}

                    {formType?.code === 'LATE_ARRIVAL_REPORT' ? (
                        <>
                            <TextField label="Oficial de seguridad" value={values.securityOfficer} onChange={(e) => handleChange('securityOfficer', e.target.value)} error={!!errors.securityOfficer} helperText={errors.securityOfficer} fullWidth />
                            <TextField label="Operador de acceso vehicular" value={values.vehicleAccessOperator} onChange={(e) => handleChange('vehicleAccessOperator', e.target.value)} error={!!errors.vehicleAccessOperator} helperText={errors.vehicleAccessOperator} fullWidth />
                            <TextField label="Hora de llegada" type="time" value={values.arrivalTime} onChange={(e) => handleChange('arrivalTime', e.target.value)} error={!!errors.arrivalTime} helperText={errors.arrivalTime} fullWidth slotProps={{ inputLabel: { shrink: true } }} />
                            <TextField label="Puesto" value={values.position} onChange={(e) => handleChange('position', e.target.value)} error={!!errors.position} helperText={errors.position} fullWidth />
                            <TextField label="Supervisor" value={values.supervisor} onChange={(e) => handleChange('supervisor', e.target.value)} error={!!errors.supervisor} helperText={errors.supervisor} fullWidth />
                            <TextField label="Motivo" value={values.reason} onChange={(e) => handleChange('reason', e.target.value)} error={!!errors.reason} helperText={errors.reason} multiline minRows={3} fullWidth sx={{ gridColumn: { md: '1 / span 2' } }} />
                        </>
                    ) : null}
                </Box>
            </GeneralModal>

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => {
                    const isSuccess = alert?.type === 'success';
                    setAlert(null);
                    if (isSuccess) {
                        onClose?.();
                    }
                }}
            />
        </>
    );
}
