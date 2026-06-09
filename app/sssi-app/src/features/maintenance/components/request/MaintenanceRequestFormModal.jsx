import { useEffect, useState } from 'react';
import { Box, Button, Chip, Divider, MenuItem, TextField, Typography, useTheme } from '@mui/material';
import ConstructionIcon from '@mui/icons-material/Construction';
import AddCircleOutlinedIcon from '@mui/icons-material/AddCircleOutlined';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import SearchableSelect from '../../../../common/components/SearchableSelect.jsx';
import TimeSelect from '../../../../common/components/TimeSelect.jsx';
import { createMaintenanceRequest, fetchMaintenanceRequestById, updateMaintenanceRequest } from '../../services/request/requestsService';
import { fetchCompanies, fetchCompanyTechnicians } from '../../services/company/companiesService';
import { fetchCampuses, fetchBuildingsByCampus, fetchBuildingEmails, fetchCampusEmails } from '../../services/locationsService';
import { MAINTENANCE_STATUS_OPTIONS, checkScheduleConsistency } from '../../maintenanceUtils';

const SCHEDULE_DATE_ERROR = 'La fecha de fin no puede ser anterior a la fecha de inicio';
const SCHEDULE_TIME_ERROR = 'La hora de salida no puede ser anterior a la hora de llegada';

function scheduleErrorsFor(values) {
    const { endDateInvalid, endTimeInvalid } = checkScheduleConsistency(values);
    return {
        endDate: endDateInvalid ? SCHEDULE_DATE_ERROR : '',
        endTime: endTimeInvalid ? SCHEDULE_TIME_ERROR : '',
    };
}

const INITIAL_VALUES = {
    companyId: '',
    emails: [],
    status: 'PENDING',
    description: '',
    startDate: null,
    endDate: null,
    startTime: null,
    endTime: null,
    campusId: '',
    buildingId: '',
};

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

function technicianLabel(technician) {
    return technician?.userEmail || technician?.keycloakUserId || 'Técnico';
}

export default function MaintenanceRequestFormModal({ open, onClose, onSaved, requestId = null, initialCompanyId = '' }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;
    const isEdit = !!requestId;

    const [formValues, setFormValues] = useState(INITIAL_VALUES);
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [loadingRequest, setLoadingRequest] = useState(false);
    const [loadingOptions, setLoadingOptions] = useState(false);
    const [companies, setCompanies] = useState([]);
    const [campuses, setCampuses] = useState([]);
    const [buildings, setBuildings] = useState([]);
    const [loadingCampuses, setLoadingCampuses] = useState(false);
    const [loadingBuildings, setLoadingBuildings] = useState(false);
    const [emailOptions, setEmailOptions] = useState([]);
    const [loadingEmails, setLoadingEmails] = useState(false);
    const [technicianOptions, setTechnicianOptions] = useState([]);
    const [loadingTechnicians, setLoadingTechnicians] = useState(false);
    const [selectedTechnicians, setSelectedTechnicians] = useState({});
    const [technicianToAdd, setTechnicianToAdd] = useState('');
    const [leaderId, setLeaderId] = useState('');
    const [alert, setAlert] = useState(null);

    useEffect(() => {
        if (!open) {
            setFormValues(INITIAL_VALUES);
            setErrors({});
            setTouched({});
            setSaving(false);
            setLoadingRequest(false);
            setLoadingOptions(false);
            setCompanies([]);
            setCampuses([]);
            setBuildings([]);
            setEmailOptions([]);
            setLoadingEmails(false);
            setTechnicianOptions([]);
            setSelectedTechnicians({});
            setTechnicianToAdd('');
            setLeaderId('');
            setAlert(null);
            return;
        }

        setFormValues((prev) => ({ ...INITIAL_VALUES, companyId: initialCompanyId || prev.companyId || '' }));
    }, [open, initialCompanyId]);

    const loadCompanies = () => {
        if (!open) return;
        let cancelled = false;
        setLoadingOptions(true);

        fetchCompanies({ page: 0, size: 200 })
            .then((page) => {
                if (!cancelled) setCompanies(page.content ?? []);
            })
            .catch(() => {})
            .finally(() => {
                if (!cancelled) setLoadingOptions(false);
            });

        return () => {
            cancelled = true;
        };
    };

    useEffect(loadCompanies, [open]);

    const loadCampuses = () => {
        if (!open) return;
        let cancelled = false;
        setLoadingCampuses(true);

        fetchCampuses({ page: 0, size: 200 })
            .then((page) => {
                if (!cancelled) setCampuses(page.content ?? []);
            })
            .catch(() => {})
            .finally(() => {
                if (!cancelled) setLoadingCampuses(false);
            });

        return () => {
            cancelled = true;
        };
    };

    useEffect(loadCampuses, [open]);

    const loadBuildings = () => {
        if (!open || !formValues.campusId) {
            setBuildings([]);
            return;
        }
        let cancelled = false;
        setLoadingBuildings(true);

        fetchBuildingsByCampus(formValues.campusId, { page: 0, size: 200 })
            .then((page) => {
                if (!cancelled) setBuildings(page.content ?? []);
            })
            .catch(() => {})
            .finally(() => {
                if (!cancelled) setLoadingBuildings(false);
            });

        return () => {
            cancelled = true;
        };
    };

    useEffect(loadBuildings, [open, formValues.campusId]);

    const loadEmails = () => {
        if (!open || (!formValues.buildingId && !formValues.campusId)) {
            setEmailOptions([]);
            return;
        }
        let cancelled = false;
        setLoadingEmails(true);

        const fetcher = formValues.buildingId
            ? fetchBuildingEmails(formValues.buildingId)
            : fetchCampusEmails(formValues.campusId);

        fetcher
            .then((data) => {
                if (!cancelled) setEmailOptions(Array.isArray(data) ? data : []);
            })
            .catch(() => {
                if (!cancelled) setEmailOptions([]);
            })
            .finally(() => {
                if (!cancelled) setLoadingEmails(false);
            });

        return () => {
            cancelled = true;
        };
    };

    useEffect(loadEmails, [open, formValues.campusId, formValues.buildingId]);

    const loadTechnicians = () => {
        if (!open || !formValues.companyId) {
            setTechnicianOptions([]);
            return;
        }
        let cancelled = false;
        setLoadingTechnicians(true);

        fetchCompanyTechnicians(formValues.companyId)
            .then((data) => {
                if (!cancelled) setTechnicianOptions(Array.isArray(data) ? data : []);
            })
            .catch(() => {})
            .finally(() => {
                if (!cancelled) setLoadingTechnicians(false);
            });

        return () => {
            cancelled = true;
        };
    };

    useEffect(loadTechnicians, [open, formValues.companyId]);

    const loadRequest = () => {
        if (!open || !requestId) return;
        let cancelled = false;
        setLoadingRequest(true);

        fetchMaintenanceRequestById(requestId)
            .then((request) => {
                if (cancelled || !request) return;
                setFormValues({
                    companyId: request.company?.id ?? initialCompanyId ?? '',
                    emails: Array.isArray(request.emails) ? request.emails : [],
                    status: request.status ?? 'PENDING',
                    description: request.description ?? '',
                    startDate: request.startDate ? dayjs(request.startDate) : null,
                    endDate: request.endDate ? dayjs(request.endDate) : null,
                    startTime: request.startTime ? dayjs(`2000-01-01T${request.startTime}`) : null,
                    endTime: request.endTime ? dayjs(`2000-01-01T${request.endTime}`) : null,
                    campusId: request.campusId ?? '',
                    buildingId: request.buildingId ?? '',
                });
                const assigned = Array.isArray(request.assignedTechnicians) ? request.assignedTechnicians : [];
                setSelectedTechnicians(assigned.reduce((acc, technician) => {
                    acc[technician.id] = technician;
                    return acc;
                }, {}));
                setLeaderId(request.leaderUserCompany?.id ?? '');
            })
            .catch((error) => {
                if (!cancelled) {
                    setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo cargar la solicitud' });
                }
            })
            .finally(() => {
                if (!cancelled) setLoadingRequest(false);
            });

        return () => {
            cancelled = true;
        };
    };

    useEffect(loadRequest, [open, requestId, initialCompanyId]);

    const validateField = (key, value) => {
        let error = '';
        const trimmed = typeof value === 'string' ? value.trim() : value;

        if (['companyId', 'campusId'].includes(key) && !trimmed) {
            error = 'Este campo es requerido';
        }

        if (['startDate', 'endDate', 'startTime', 'endTime'].includes(key) && !value) {
            error = 'Este campo es requerido';
        }

        setErrors((prev) => ({ ...prev, [key]: error }));
        return !error;
    };

    const PICKER_FIELDS = ['startDate', 'endDate', 'startTime', 'endTime'];

    const applyScheduleErrors = (values) => {
        const schedule = scheduleErrorsFor(values);
        setErrors((prev) => ({
            ...prev,
            endDate: values.endDate ? schedule.endDate : prev.endDate,
            endTime: values.endTime ? schedule.endTime : prev.endTime,
        }));
        setTouched((prev) => ({
            ...prev,
            ...(schedule.endDate ? { endDate: true } : {}),
            ...(schedule.endTime ? { endTime: true } : {}),
        }));
    };

    const handleChange = (key, value) => {
        let merged;
        if (key === 'companyId') {
            merged = { ...formValues, companyId: value };
            setFormValues((prev) => ({ ...prev, companyId: value }));
            setSelectedTechnicians({});
            setTechnicianToAdd('');
            setLeaderId('');
        } else if (key === 'campusId') {
            merged = { ...formValues, campusId: value, buildingId: '' };
            setFormValues((prev) => ({ ...prev, campusId: value, buildingId: '' }));
        } else {
            merged = { ...formValues, [key]: value };
            setFormValues((prev) => ({ ...prev, [key]: value }));
        }
        if (touched[key] || PICKER_FIELDS.includes(key)) validateField(key, value);
        if (PICKER_FIELDS.includes(key)) applyScheduleErrors(merged);
    };

    const handleBlur = (key) => {
        setTouched((prev) => ({ ...prev, [key]: true }));
        setFormValues((current) => {
            validateField(key, current[key]);
            return current;
        });
    };

    const handleAddTechnician = () => {
        if (!technicianToAdd) return;
        const technician = technicianOptions.find((item) => item.id === technicianToAdd);
        if (!technician) return;
        setSelectedTechnicians((prev) => ({ ...prev, [technician.id]: technician }));
        setTechnicianToAdd('');
        setErrors((prev) => ({ ...prev, technicians: '' }));
    };

    const handleRemoveTechnician = (technicianId) => {
        if (technicianId === leaderId) setLeaderId('');
        setSelectedTechnicians((prev) => {
            const copy = { ...prev };
            delete copy[technicianId];
            return copy;
        });
    };

    const handleEmailsChange = (values) => {
        setFormValues((prev) => ({ ...prev, emails: values }));
        if (values.length > 0) setErrors((prev) => ({ ...prev, emails: '' }));
    };

    const handleAddManualEmail = (rawEmail) => {
        const email = (rawEmail || '').trim();
        if (!email) return;
        if (!EMAIL_REGEX.test(email)) {
            setErrors((prev) => ({ ...prev, emails: 'El correo electrónico tiene un formato inválido' }));
            return;
        }
        setFormValues((prev) => (
            prev.emails.includes(email)
                ? prev
                : { ...prev, emails: [...prev.emails, email] }
        ));
        setErrors((prev) => ({ ...prev, emails: '' }));
    };

    const handleRemoveEmail = (email) => {
        setFormValues((prev) => ({ ...prev, emails: prev.emails.filter((item) => item !== email) }));
    };

    const handleLeaderChange = (value) => {
        setLeaderId(value);
        setErrors((prev) => ({ ...prev, leader: '' }));
        if (value && !selectedTechnicians[value]) {
            const technician = technicianOptions.find((item) => item.id === value);
            if (technician) {
                setSelectedTechnicians((prev) => ({ ...prev, [technician.id]: technician }));
                setErrors((prev) => ({ ...prev, technicians: '', leader: '' }));
            }
        }
    };

    const handleSave = async () => {
        const requiredFields = ['companyId', 'startDate', 'endDate', 'startTime', 'endTime', 'campusId'];
        const nextTouched = {};
        const nextErrors = {};

        requiredFields.forEach((key) => {
            nextTouched[key] = true;
            const value = formValues[key];
            const isString = typeof value === 'string';
            if (isString ? !value.trim() : !value) {
                nextErrors[key] = 'Este campo es requerido';
            }
        });

        if (!formValues.emails || formValues.emails.length === 0) {
            nextErrors.emails = 'Se requiere al menos un correo electrónico';
        }

        const schedule = scheduleErrorsFor(formValues);
        if (!nextErrors.endDate && schedule.endDate) nextErrors.endDate = schedule.endDate;
        if (!nextErrors.endTime && schedule.endTime) nextErrors.endTime = schedule.endTime;

        const technicianIds = Object.keys(selectedTechnicians);
        if (technicianIds.length === 0) {
            nextErrors.technicians = 'Selecciona al menos un técnico';
        }

        if (!leaderId) {
            nextErrors.leader = 'Selecciona un encargado';
        }

        setTouched((prev) => ({ ...prev, ...nextTouched }));
        setErrors((prev) => ({ ...prev, ...nextErrors }));

        if (Object.keys(nextErrors).length > 0) {
            setAlert({ type: 'warning', message: 'Revisa los datos antes de continuar' });
            return;
        }

        setSaving(true);
        try {
            const payload = {
                companyId: formValues.companyId,
                emails: formValues.emails,
                description: formValues.description.trim() || null,
                startDate: formValues.startDate ? dayjs(formValues.startDate).format('YYYY-MM-DD') : null,
                endDate: formValues.endDate ? dayjs(formValues.endDate).format('YYYY-MM-DD') : null,
                startTime: formValues.startTime ? dayjs(formValues.startTime).format('HH:mm:ss') : null,
                endTime: formValues.endTime ? dayjs(formValues.endTime).format('HH:mm:ss') : null,
                campusId: formValues.campusId,
                buildingId: formValues.buildingId || null,
                assignedTechnicianIds: technicianIds,
                leaderUserCompanyId: leaderId || null,
            };

            if (isEdit) {
                payload.status = formValues.status;
                await updateMaintenanceRequest(requestId, payload);
            } else {
                await createMaintenanceRequest(payload);
            }

            onSaved?.();
            onClose?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo guardar la solicitud' });
        } finally {
            setSaving(false);
        }
    };

    const fieldSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': { borderColor: 'divider' },
            '&:hover fieldset': { borderColor: `color-mix(in srgb, ${accentColor} 50%, transparent)` },
            '&.Mui-focused fieldset': { borderColor: accentColor },
        },
        '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
    };

    const contentSx = {
        overflowY: 'auto',
        '&::-webkit-scrollbar': { width: '5px' },
        '&::-webkit-scrollbar-track': { background: 'transparent' },
        '&::-webkit-scrollbar-thumb': { background: `color-mix(in srgb, ${accentColor} 25%, transparent)`, borderRadius: '4px' },
        '&::-webkit-scrollbar-thumb:hover': { background: `color-mix(in srgb, ${accentColor} 45%, transparent)` },
    };

    const sectionLabel = (text) => (
        <Typography sx={{
            fontSize: 11.5, fontWeight: 700, letterSpacing: '0.08em',
            textTransform: 'uppercase', color: accentColor, mb: 1.5,
        }}>
            {text}
        </Typography>
    );

    const anyLoading = saving || loadingRequest || loadingOptions || loadingCampuses;
    const noCompany = !formValues.companyId;
    const availableToAdd = technicianOptions.filter((technician) => !selectedTechnicians[technician.id]);
    const selectedList = Object.values(selectedTechnicians);

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="md"
                icon={ConstructionIcon}
                title={isEdit ? 'Editar solicitud' : 'Nueva solicitud de mantenimiento'}
                subtitle={isEdit ? 'Actualiza los datos de la solicitud' : 'Registra una nueva solicitud de mantenimiento'}
                loading={saving || loadingRequest}
                secondaryButton={{ label: 'Cancelar', onClick: onClose, disabled: saving }}
                primaryButton={{
                    label: saving ? 'Guardando…' : isEdit ? 'Guardar cambios' : 'Crear solicitud',
                    onClick: handleSave,
                    disabled: anyLoading,
                    startIcon: <AddCircleOutlinedIcon />,
                }}
                contentSx={contentSx}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'flex', flexDirection: 'column', gap: 3 }}>

                    <Box>
                        {sectionLabel('Empresa')}
                        <SearchableSelect
                            label="Empresa"
                            value={formValues.companyId}
                            onChange={(value) => handleChange('companyId', value)}
                            onBlur={() => handleBlur('companyId')}
                            required
                            fullWidth
                            size="small"
                            disabled={anyLoading}
                            error={touched.companyId && !!errors.companyId}
                            helperText={touched.companyId ? (errors.companyId || ' ') : ' '}
                            sx={fieldSx}
                            items={companies}
                            getItemLabel={(company) => `${company.name} — ${company.legalId}`}
                            getItemValue={(company) => company.id}
                        />
                    </Box>

                    {isEdit && (
                        <>
                            <Divider />
                            <Box>
                                {sectionLabel('Estado')}
                                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                                    <TextField
                                        select
                                        label="Estado"
                                        value={formValues.status}
                                        onChange={(e) => handleChange('status', e.target.value)}
                                        fullWidth
                                        size="small"
                                        disabled={anyLoading}
                                        sx={fieldSx}
                                        helperText=" "
                                    >
                                        {MAINTENANCE_STATUS_OPTIONS.map((option) => (
                                            <MenuItem key={option.value} value={option.value}>
                                                {option.label}
                                            </MenuItem>
                                        ))}
                                    </TextField>
                                </Box>
                            </Box>
                        </>
                    )}

                    <Divider />

                    <Box>
                        {sectionLabel('Programación')}
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                            <DatePicker
                                label="Fecha de inicio *"
                                value={formValues.startDate}
                                onChange={(value) => handleChange('startDate', value)}
                                onClose={() => handleBlur('startDate')}
                                disabled={anyLoading}
                                maxDate={formValues.endDate || undefined}
                                slotProps={{
                                    textField: {
                                        size: 'small',
                                        fullWidth: true,
                                        sx: fieldSx,
                                        error: touched.startDate && !!errors.startDate,
                                        helperText: touched.startDate ? (errors.startDate || ' ') : ' ',
                                    },
                                }}
                            />
                            <DatePicker
                                label="Fecha de fin *"
                                value={formValues.endDate}
                                onChange={(value) => handleChange('endDate', value)}
                                onClose={() => handleBlur('endDate')}
                                disabled={anyLoading}
                                minDate={formValues.startDate || undefined}
                                slotProps={{
                                    textField: {
                                        size: 'small',
                                        fullWidth: true,
                                        sx: fieldSx,
                                        error: touched.endDate && !!errors.endDate,
                                        helperText: touched.endDate ? (errors.endDate || ' ') : ' ',
                                    },
                                }}
                            />
                            <TimeSelect
                                label="Hora de llegada *"
                                value={formValues.startTime}
                                onChange={(value) => handleChange('startTime', value)}
                                onClose={() => handleBlur('startTime')}
                                disabled={anyLoading}
                                size="small"
                                fullWidth
                                sx={fieldSx}
                                error={touched.startTime && !!errors.startTime}
                                helperText={touched.startTime ? (errors.startTime || ' ') : ' '}
                            />
                            <TimeSelect
                                label="Hora de salida *"
                                value={formValues.endTime}
                                onChange={(value) => handleChange('endTime', value)}
                                onClose={() => handleBlur('endTime')}
                                disabled={anyLoading}
                                size="small"
                                fullWidth
                                sx={fieldSx}
                                error={touched.endTime && !!errors.endTime}
                                helperText={touched.endTime ? (errors.endTime || ' ') : ' '}
                            />
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Ubicación')}
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                            <SearchableSelect
                                label="Campus"
                                value={formValues.campusId}
                                onChange={(value) => handleChange('campusId', value)}
                                onBlur={() => handleBlur('campusId')}
                                required
                                clearable
                                fullWidth
                                size="small"
                                disabled={anyLoading || loadingCampuses}
                                error={touched.campusId && !!errors.campusId}
                                helperText={touched.campusId ? (errors.campusId || ' ') : ' '}
                                sx={fieldSx}
                                items={campuses}
                                getItemLabel={(campus) => campus.name}
                                getItemValue={(campus) => campus.id}
                            />
                            <SearchableSelect
                                label="Edificio (opcional)"
                                value={formValues.buildingId}
                                onChange={(value) => handleChange('buildingId', value)}
                                clearable
                                fullWidth
                                size="small"
                                disabled={anyLoading || !formValues.campusId || loadingBuildings}
                                helperText={!formValues.campusId ? 'Selecciona un campus primero' : ' '}
                                sx={fieldSx}
                                items={buildings}
                                getItemLabel={(building) => building.name}
                                getItemValue={(building) => building.id}
                            />
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Técnicos')}
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
                                    <SearchableSelect
                                        label="Agregar técnico"
                                        value={technicianToAdd}
                                        onChange={setTechnicianToAdd}
                                        fullWidth
                                        size="small"
                                        disabled={anyLoading || noCompany || loadingTechnicians}
                                        error={!!errors.technicians}
                                        helperText={
                                            noCompany
                                                ? 'Selecciona una empresa primero'
                                                : (errors.technicians || ' ')
                                        }
                                        sx={fieldSx}
                                        items={availableToAdd}
                                        getItemLabel={technicianLabel}
                                        getItemValue={(technician) => technician.id}
                                    />
                                    <Button
                                        variant="outlined"
                                        onClick={handleAddTechnician}
                                        disabled={anyLoading || noCompany || !technicianToAdd}
                                        sx={{ textTransform: 'none', mt: 0.25, whiteSpace: 'nowrap' }}
                                    >
                                        Agregar
                                    </Button>
                                </Box>
                                <SearchableSelect
                                    label="Encargado *"
                                    value={leaderId}
                                    onChange={handleLeaderChange}
                                    fullWidth
                                    size="small"
                                    disabled={anyLoading || noCompany || loadingTechnicians}
                                    error={!!errors.leader}
                                    helperText={
                                        noCompany
                                            ? 'Selecciona una empresa primero'
                                            : (errors.leader || 'El encargado se agrega a la lista de técnicos')
                                    }
                                    sx={fieldSx}
                                    items={technicianOptions}
                                    getItemLabel={technicianLabel}
                                    getItemValue={(technician) => technician.id}
                                />
                            </Box>

                            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                                {selectedList.length === 0 ? (
                                    <Typography sx={{ color: 'text.secondary', fontSize: 13.5 }}>
                                        No hay técnicos seleccionados.
                                    </Typography>
                                ) : (
                                    selectedList.map((technician) => {
                                        const isLeader = technician.id === leaderId;
                                        return (
                                            <Chip
                                                key={technician.id}
                                                label={isLeader ? `${technicianLabel(technician)} · Encargado` : technicianLabel(technician)}
                                                color={isLeader ? 'primary' : 'default'}
                                                variant={isLeader ? 'filled' : 'outlined'}
                                                onDelete={() => handleRemoveTechnician(technician.id)}
                                            />
                                        );
                                    })
                                )}
                            </Box>
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Descripción')}
                        <TextField
                            label="Descripción"
                            value={formValues.description}
                            onChange={(e) => handleChange('description', e.target.value)}
                            fullWidth
                            size="small"
                            disabled={anyLoading}
                            multiline
                            minRows={3}
                            sx={fieldSx}
                        />
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Contacto')}
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                            <SearchableSelect
                                label="Correos electrónicos *"
                                multiple
                                addMode
                                validateCreate={(s) => EMAIL_REGEX.test((s || '').trim())}
                                value={formValues.emails}
                                onChange={handleEmailsChange}
                                onCreate={handleAddManualEmail}
                                fullWidth
                                size="small"
                                disabled={anyLoading || loadingEmails}
                                error={!!errors.emails}
                                helperText={
                                    errors.emails
                                        || (!formValues.campusId
                                            ? 'Selecciona un campus o edificio para ver sus correos'
                                            : 'Elige de la lista o escribe un correo para agregarlo')
                                }
                                sx={fieldSx}
                                items={emailOptions}
                                getItemLabel={(email) => email}
                                getItemValue={(email) => email}
                            />

                            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                                {formValues.emails.length === 0 ? (
                                    <Typography sx={{ color: 'text.secondary', fontSize: 13.5 }}>
                                        No hay correos seleccionados.
                                    </Typography>
                                ) : (
                                    formValues.emails.map((email) => (
                                        <Chip
                                            key={email}
                                            label={email}
                                            onDelete={() => handleRemoveEmail(email)}
                                        />
                                    ))
                                )}
                            </Box>
                        </Box>
                    </Box>

                </Box>
            </GeneralModal>

            <DialogModal open={!!alert} type={alert?.type} message={alert?.message} onClose={() => setAlert(null)} />
        </>
    );
}
