import { useEffect, useState } from 'react';
import { Box, Button, Chip, Divider, Typography, useTheme } from '@mui/material';
import ConstructionIcon from '@mui/icons-material/Construction';
import SaveIcon from '@mui/icons-material/Save';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import TimeSelect from '../../../../common/components/TimeSelect.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { fetchRegisterByRequest, updateRegister, finalizeRegister } from '../../services/register/registerService';
import { fetchCampusById, fetchBuildingById } from '../../services/locationsService';
import { statusLabel, checkScheduleConsistency } from '../../maintenanceUtils';
import RegisterAssetsTable from './RegisterAssetsTable.jsx';

const INITIAL_VALUES = { startDate: null, endDate: null, startTime: null, endTime: null };

const SCHEDULE_DATE_ERROR = 'La fecha de fin no puede ser anterior a la fecha de inicio';
const SCHEDULE_TIME_ERROR = 'La hora de fin no puede ser anterior a la hora de inicio';

function scheduleErrorsFor(values) {
    const { endDateInvalid, endTimeInvalid } = checkScheduleConsistency(values);
    return {
        endDate: endDateInvalid ? SCHEDULE_DATE_ERROR : '',
        endTime: endTimeInvalid ? SCHEDULE_TIME_ERROR : '',
    };
}

function InfoRow({ label, value }) {
    return (
        <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'baseline' }}>
            <Typography sx={{ minWidth: 96, fontSize: 11.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                {label}
            </Typography>
            <Typography sx={{ fontSize: 13.25, fontWeight: 600, color: 'text.primary' }}>
                {value || '—'}
            </Typography>
        </Box>
    );
}

export default function RegisterFormModal({ open, onClose, onSaved, requestId, canManage = false }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const [register, setRegister] = useState(null);
    const [formValues, setFormValues] = useState(INITIAL_VALUES);
    const [locationNames, setLocationNames] = useState({ campus: null, building: null });
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [finalizing, setFinalizing] = useState(false);
    const [confirmFinalize, setConfirmFinalize] = useState(false);
    const [alert, setAlert] = useState(null);
    const [errors, setErrors] = useState({});

    useEffect(() => {
        if (!open) {
            setRegister(null);
            setFormValues(INITIAL_VALUES);
            setLocationNames({ campus: null, building: null });
            setLoading(false);
            setSaving(false);
            setFinalizing(false);
            setConfirmFinalize(false);
            setAlert(null);
            setErrors({});
        }
    }, [open]);

    const loadRegister = () => {
        if (!open || !requestId) return undefined;
        let cancelled = false;
        setLoading(true);

        fetchRegisterByRequest(requestId)
            .then((data) => {
                if (cancelled || !data) return;
                setRegister(data);
                setFormValues({
                    startDate: data.startDate ? dayjs(data.startDate) : null,
                    endDate: data.endDate ? dayjs(data.endDate) : null,
                    startTime: data.startTime ? dayjs(`2000-01-01T${data.startTime}`) : null,
                    endTime: data.endTime ? dayjs(`2000-01-01T${data.endTime}`) : null,
                });
                setErrors({});
            })
            .catch((err) => {
                if (!cancelled) {
                    setAlert({ type: 'error', message: err?.response?.data?.message ?? err?.message ?? 'No se pudo cargar el registro' });
                }
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => {
            cancelled = true;
        };
    };

    useEffect(loadRegister, [open, requestId]);

    const loadLocations = () => {
        const request = register?.request;
        if (!request) return undefined;
        let cancelled = false;

        const resolveCampus = request.campusId ? fetchCampusById(request.campusId).catch(() => null) : Promise.resolve(null);
        const resolveBuilding = request.buildingId ? fetchBuildingById(request.buildingId).catch(() => null) : Promise.resolve(null);

        Promise.all([resolveCampus, resolveBuilding]).then(([campus, building]) => {
            if (cancelled) return;
            setLocationNames({ campus: campus?.name ?? null, building: building?.name ?? null });
        });

        return () => {
            cancelled = true;
        };
    };

    useEffect(loadLocations, [register]);

    const handleChange = (key, value) => {
        const merged = { ...formValues, [key]: value };
        setFormValues(merged);
        const schedule = scheduleErrorsFor(merged);
        setErrors({
            endDate: merged.endDate ? schedule.endDate : '',
            endTime: merged.endTime ? schedule.endTime : '',
        });
    };

    const handleSave = async () => {
        if (!register) return;
        if (!formValues.startDate || !formValues.endDate || !formValues.startTime || !formValues.endTime) {
            setAlert({ type: 'warning', message: 'Completa las fechas y horas antes de guardar' });
            return;
        }

        const schedule = scheduleErrorsFor(formValues);
        if (schedule.endDate || schedule.endTime) {
            setErrors(schedule);
            setAlert({ type: 'warning', message: 'Revisa las fechas y horas antes de guardar' });
            return;
        }

        setSaving(true);
        try {
            const payload = {
                startDate: dayjs(formValues.startDate).format('YYYY-MM-DD'),
                endDate: dayjs(formValues.endDate).format('YYYY-MM-DD'),
                startTime: dayjs(formValues.startTime).format('HH:mm:ss'),
                endTime: dayjs(formValues.endTime).format('HH:mm:ss'),
            };
            const updated = await updateRegister(register.id, payload);
            setRegister((prev) => ({ ...prev, ...updated }));
            setAlert({ type: 'success', message: 'Fechas actualizadas correctamente' });
        } catch (err) {
            setAlert({ type: 'error', message: err?.response?.data?.message ?? err?.message ?? 'No se pudieron guardar las fechas' });
        } finally {
            setSaving(false);
        }
    };

    const handleFinalize = async () => {
        if (!register) return;
        setFinalizing(true);
        try {
            await finalizeRegister(register.id);
            setConfirmFinalize(false);
            onSaved?.();
            onClose?.();
        } catch (err) {
            setConfirmFinalize(false);
            setAlert({ type: 'error', message: err?.response?.data?.message ?? err?.message ?? 'No se pudo finalizar el registro' });
        } finally {
            setFinalizing(false);
        }
    };

    const request = register?.request;
    const isPending = request?.status === 'PENDING';
    const canRegister = canManage && isPending;
    const leaderName = request?.leaderUserCompany?.userEmail ?? request?.leaderUserCompany?.keycloakUserId ?? '—';

    const fieldSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': { borderColor: 'divider' },
            '&:hover fieldset': { borderColor: `color-mix(in srgb, ${accentColor} 50%, transparent)` },
            '&.Mui-focused fieldset': { borderColor: accentColor },
        },
        '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
    };

    const sectionLabel = (text) => (
        <Typography sx={{ fontSize: 11.5, fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: accentColor, mb: 1.5 }}>
            {text}
        </Typography>
    );

    const finalizeButton = canRegister ? (
        <Button
            variant="outlined"
            size="small"
            color="success"
            startIcon={<DoneAllIcon sx={{ fontSize: 18 }} />}
            onClick={() => setConfirmFinalize(true)}
            disabled={saving || finalizing}
            sx={{ textTransform: 'none', fontWeight: 600, fontSize: 12.5, borderRadius: '8px' }}
        >
            Finalizar
        </Button>
    ) : null;

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="md"
                icon={ConstructionIcon}
                title="Registro de mantenimiento"
                subtitle={request ? request.company?.name : 'Cargando…'}
                loading={loading || saving || finalizing}
                footerLeft={finalizeButton}
                secondaryButton={{ label: 'Cerrar', onClick: onClose, disabled: saving || finalizing }}
                primaryButton={canRegister ? {
                    label: saving ? 'Guardando…' : 'Guardar fechas',
                    onClick: handleSave,
                    disabled: loading || saving || finalizing,
                    startIcon: <SaveIcon />,
                } : undefined}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'flex', flexDirection: 'column', gap: 3 }}>

                    <Box>
                        {sectionLabel('Información general')}
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75 }}>
                            <InfoRow label="Empresa" value={request?.company?.name} />
                            <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center' }}>
                                <Typography sx={{ minWidth: 96, fontSize: 11.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                                    Estado
                                </Typography>
                                <Chip
                                    label={statusLabel(register?.status)}
                                    size="small"
                                    variant="outlined"
                                    color={register?.status === 'COMPLETED' ? 'success' : 'warning'}
                                />
                            </Box>
                            <InfoRow label="Encargado" value={leaderName} />
                            <InfoRow label="Campus" value={locationNames.campus} />
                            <InfoRow label="Edificio" value={locationNames.building} />
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Programación del registro')}
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                            <DatePicker
                                label="Fecha de inicio"
                                value={formValues.startDate}
                                onChange={(value) => handleChange('startDate', value)}
                                disabled={!canRegister || loading || saving || finalizing}
                                maxDate={formValues.endDate || undefined}
                                slotProps={{ textField: { size: 'small', fullWidth: true, sx: fieldSx, helperText: ' ' } }}
                            />
                            <DatePicker
                                label="Fecha de fin"
                                value={formValues.endDate}
                                onChange={(value) => handleChange('endDate', value)}
                                disabled={!canRegister || loading || saving || finalizing}
                                minDate={formValues.startDate || undefined}
                                slotProps={{ textField: { size: 'small', fullWidth: true, sx: fieldSx, error: !!errors.endDate, helperText: errors.endDate || ' ' } }}
                            />
                            <TimeSelect
                                label="Hora de inicio"
                                value={formValues.startTime}
                                onChange={(value) => handleChange('startTime', value)}
                                disabled={!canRegister || loading || saving || finalizing}
                                size="small"
                                fullWidth
                                sx={fieldSx}
                                helperText=" "
                            />
                            <TimeSelect
                                label="Hora de fin"
                                value={formValues.endTime}
                                onChange={(value) => handleChange('endTime', value)}
                                disabled={!canRegister || loading || saving || finalizing}
                                size="small"
                                fullWidth
                                sx={fieldSx}
                                error={!!errors.endTime}
                                helperText={errors.endTime || ' '}
                            />
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Activos de la ubicación')}
                        {register?.id ? (
                            <RegisterAssetsTable registerId={register.id} canRegister={canRegister} />
                        ) : (
                            <Typography sx={{ fontSize: 13.5, color: 'text.secondary' }}>Cargando activos…</Typography>
                        )}
                    </Box>

                </Box>
            </GeneralModal>

            <DialogModal
                type="warning"
                open={confirmFinalize}
                title="Finalizar registro"
                message={'¿Seguro que deseas finalizar este registro de mantenimiento?\nLa solicitud pasará a estado completada y no se podrán agregar más registros.'}
                onClose={() => setConfirmFinalize(false)}
                onConfirm={handleFinalize}
                confirmLabel="Finalizar"
            />

            <DialogModal open={!!alert} type={alert?.type} message={alert?.message} onClose={() => setAlert(null)} />
        </>
    );
}
