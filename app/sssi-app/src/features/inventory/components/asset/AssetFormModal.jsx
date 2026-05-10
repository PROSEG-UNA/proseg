import { useState, useEffect, useRef } from 'react';
import {
    Box, Typography, TextField, MenuItem,
    Divider, IconButton, useTheme,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import dayjs from 'dayjs';
import AddCircleOutlinedIcon from '@mui/icons-material/AddCircleOutlined';
import CloseIcon from '@mui/icons-material/Close';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import AlertModal from '../../../../common/components/AlertModal.jsx';
import CatalogFormModal from '../catalog/CatalogFormModal.jsx';
import { CATALOG_CONFIG } from '../catalog/catalogConfig.js';
import { fetchCatalogOptions } from '../../services/catalogService.js';
import { createAsset, updateAsset, fetchAssetById } from '../../services/assetsService.js';
import { INVENTORY_ENDPOINTS } from '../../services/endpoints.js';

const STATUS_OPTIONS = [
    { value: 'BUENO',           label: 'Bueno' },
    { value: 'REGULAR',         label: 'Regular' },
    { value: 'MALO',            label: 'Malo' },
    { value: 'EN_REPARACION',   label: 'En reparación' },
    { value: 'BAJA',            label: 'Baja' },
];

const INIT = {
    name: '', description: '',
    brandId: '', typeId: '', modelId: '',
    locationId: '',
    status: '', statusDescription: '',
    acquisitionDate: '', warrantyEndDate: '', firmwareSupportEndDate: '',
    ipAddress: '', macAddress: '',
};

export default function AssetFormModal({ open, onClose, onSaved, assetId = null }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const isEdit = !!assetId;

    const [formValues, setFormValues] = useState(INIT);
    const [errors, setErrors]         = useState({});
    const [touched, setTouched]       = useState({});
    const [saving, setSaving]         = useState(false);
    const [alert, setAlert]           = useState(null);
    const [loadingAsset, setLoadingAsset] = useState(false);

    const [brands, setBrands]       = useState([]);
    const [types, setTypes]         = useState([]);
    const [models, setModels]       = useState([]);
    const [locations, setLocations] = useState([]);
    const [loadingOptions, setLoadingOptions] = useState(false);

    const [catalogModal, setCatalogModal] = useState(null);
    const [photos, setPhotos]             = useState([]);
    const [isDragOver, setIsDragOver]     = useState(false);
    const fileInputRef = useRef(null);

    const selectedType             = types.find(t => t.id === formValues.typeId) ?? null;
    const requiresNetworkInterface = selectedType?.requiresNetworkInterface ?? false;

    const filteredModels = models.filter(m => {
        const matchBrand = !formValues.brandId || m.brand?.id === formValues.brandId;
        const matchType  = !formValues.typeId  || m.type?.id  === formValues.typeId;
        return matchBrand && matchType;
    });

    useEffect(() => {
        if (!open) return;
        setPhotos(prev => { prev.forEach(p => URL.revokeObjectURL(p.preview)); return []; });
        setFormValues(INIT);
        setErrors({});
        setTouched({});
        setSaving(false);
        setAlert(null);
        setIsDragOver(false);
    }, [open]);

    useEffect(() => {
        if (!open) return;
        let cancelled = false;
        setLoadingOptions(true);
        Promise.all([
            fetchCatalogOptions(INVENTORY_ENDPOINTS.brands),
            fetchCatalogOptions(INVENTORY_ENDPOINTS.types),
            fetchCatalogOptions(INVENTORY_ENDPOINTS.models),
            fetchCatalogOptions(INVENTORY_ENDPOINTS.locations),
        ]).then(([b, t, m, l]) => {
            if (cancelled) return;
            setBrands(b); setTypes(t); setModels(m); setLocations(l);
        }).catch(() => {}).finally(() => { if (!cancelled) setLoadingOptions(false); });
        return () => { cancelled = true; };
    }, [open]);

    useEffect(() => {
        if (!open || !assetId) return;
        let cancelled = false;
        setLoadingAsset(true);
        fetchAssetById(assetId)
            .then((asset) => {
                if (cancelled || !asset) return;
                setFormValues({
                    name:                   asset.name ?? '',
                    description:            asset.description ?? '',
                    brandId:                asset.model?.brand?.id ?? '',
                    typeId:                 asset.model?.type?.id ?? '',
                    modelId:                asset.model?.id ?? '',
                    locationId:             asset.location?.id ?? '',
                    status:                 asset.status ?? '',
                    statusDescription:      asset.statusDescription ?? '',
                    acquisitionDate:        asset.acquisitionDate ?? '',
                    warrantyEndDate:        asset.warrantyEndDate ?? '',
                    firmwareSupportEndDate: asset.firmwareSupportEndDate ?? '',
                    ipAddress:              asset.networkInterface?.ipAddress ?? '',
                    macAddress:             asset.networkInterface?.macAddress ?? '',
                });
            })
            .catch(() => {
                if (!cancelled) setAlert({ type: 'error', message: 'No se pudo cargar el activo' });
            })
            .finally(() => { if (!cancelled) setLoadingAsset(false); });
        return () => { cancelled = true; };
    }, [open, assetId]);

    const validateField = (key, value) => {
        const required = ['name', 'brandId', 'typeId', 'modelId', 'locationId', 'status'];
        let error = '';

        if (required.includes(key) && (!value || (typeof value === 'string' && !value.trim()))) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'name' && value?.trim()) {
            if (value.trim().length < 2)   error = 'Mínimo 2 caracteres';
            if (value.trim().length > 150) error = 'Máximo 150 caracteres';
        }

        if (!error && requiresNetworkInterface) {
            if (key === 'ipAddress') {
                if (!value?.trim()) {
                    error = 'La dirección IP es obligatoria';
                } else if (!/^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/.test(value.trim())) {
                    error = 'La dirección IP no tiene un formato válido';
                }
            }
            if (key === 'macAddress') {
                if (!value?.trim()) {
                    error = 'La dirección MAC es obligatoria';
                } else if (value.trim().length < 12 || value.trim().length > 17) {
                    error = 'La dirección MAC debe tener entre 12 y 17 caracteres';
                }
            }
        }

        setErrors(prev => ({ ...prev, [key]: error }));
        return !error;
    };

    const handleChange = (key, value) => {
        setFormValues(prev => ({ ...prev, [key]: value }));
        if (touched[key]) validateField(key, value);
    };

    const handleBrandChange = (value) => {
        setFormValues(prev => ({ ...prev, brandId: value, modelId: '' }));
        if (touched.brandId) validateField('brandId', value);
    };

    const handleTypeChange = (value) => {
        const newType = types.find(t => t.id === value);
        setFormValues(prev => ({
            ...prev,
            typeId: value,
            modelId: '',
            ...(!newType?.requiresNetworkInterface && { ipAddress: '', macAddress: '' }),
        }));
        if (touched.typeId) validateField('typeId', value);
    };

    const handleBlur = (key) => {
        setTouched(prev => ({ ...prev, [key]: true }));
        validateField(key, formValues[key]);
    };

    const getOptionsByKey = (key) => {
        if (key === 'brandId')    return brands;
        if (key === 'typeId')     return types;
        if (key === 'modelId')    return models;
        if (key === 'locationId') return locations;
        return [];
    };

    const setOptionsByKey = (key, opts) => {
        if (key === 'brandId')    setBrands(opts);
        else if (key === 'typeId')     setTypes(opts);
        else if (key === 'modelId')    setModels(opts);
        else if (key === 'locationId') setLocations(opts);
    };

    const openCatalogModal = (fieldKey) => {
        const configMap = {
            brandId:    CATALOG_CONFIG.brand,
            typeId:     CATALOG_CONFIG.type,
            modelId:    CATALOG_CONFIG.model,
            locationId: CATALOG_CONFIG.location,
        };
        setCatalogModal({ config: configMap[fieldKey], fieldKey, prevOptions: getOptionsByKey(fieldKey) });
    };

    const handleCatalogSaved = async () => {
        if (!catalogModal) return;
        const { fieldKey, config, prevOptions } = catalogModal;
        setCatalogModal(null);
        try {
            const fresh = await fetchCatalogOptions(config.baseUrl);
            setOptionsByKey(fieldKey, fresh);
            const prevIds = new Set(prevOptions.map(x => x.id));
            const newItem = fresh.find(x => !prevIds.has(x.id));
            if (!newItem) return;
            if (fieldKey === 'brandId') {
                setFormValues(prev => ({ ...prev, brandId: newItem.id, modelId: '' }));
            } else if (fieldKey === 'typeId') {
                setFormValues(prev => ({
                    ...prev,
                    typeId: newItem.id,
                    modelId: '',
                    ...(!newItem.requiresNetworkInterface && { ipAddress: '', macAddress: '' }),
                }));
            } else {
                setFormValues(prev => ({ ...prev, [fieldKey]: newItem.id }));
            }
        } catch (_) {}
    };

    const handleSave = async () => {
        const requiredFields = ['name', 'brandId', 'typeId', 'modelId', 'locationId', 'status'];
        if (requiresNetworkInterface) requiredFields.push('ipAddress', 'macAddress');

        const newTouched = {};
        const newErrors  = {};

        requiredFields.forEach(key => {
            newTouched[key] = true;
            const val = formValues[key];
            if (!val || (typeof val === 'string' && !val.trim())) {
                newErrors[key] = 'Este campo es requerido';
            }
        });

        if (!newErrors.name && formValues.name.trim().length < 2) {
            newErrors.name    = 'Mínimo 2 caracteres';
            newTouched.name   = true;
        }
        if (requiresNetworkInterface && !newErrors.ipAddress && formValues.ipAddress?.trim()) {
            if (!/^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/.test(formValues.ipAddress.trim())) {
                newErrors.ipAddress  = 'La dirección IP no tiene un formato válido';
                newTouched.ipAddress = true;
            }
        }
        if (requiresNetworkInterface && !newErrors.macAddress && formValues.macAddress?.trim()) {
            const mac = formValues.macAddress.trim();
            if (mac.length < 12 || mac.length > 17) {
                newErrors.macAddress  = 'La dirección MAC debe tener entre 12 y 17 caracteres';
                newTouched.macAddress = true;
            }
        }

        setTouched(prev => ({ ...prev, ...newTouched }));
        setErrors(prev  => ({ ...prev, ...newErrors  }));

        if (Object.keys(newErrors).length > 0) {
            setAlert({ type: 'warning', message: 'Revisa los datos antes de continuar' });
            return;
        }

        setSaving(true);
        try {
            const payload = {
                name:                    formValues.name.trim(),
                description:             formValues.description?.trim()           || null,
                modelId:                 formValues.modelId,
                locationId:              formValues.locationId,
                status:                  formValues.status,
                statusDescription:       formValues.statusDescription?.trim()     || null,
                acquisitionDate:         formValues.acquisitionDate               || null,
                warrantyEndDate:         formValues.warrantyEndDate               || null,
                firmwareSupportEndDate:  formValues.firmwareSupportEndDate        || null,
                ...(requiresNetworkInterface && {
                    networkInterface: {
                        ipAddress:  formValues.ipAddress.trim(),
                        macAddress: formValues.macAddress.trim(),
                    },
                }),
            };
            if (isEdit) {
                await updateAsset(assetId, payload);
            } else {
                await createAsset(payload);
            }
            onSaved?.();
            onClose();
        } catch (e) {
            const data = e?.response?.data;
            const mainMsg = data?.message ?? e?.message ?? 'Error al guardar';
            const fieldErrors = data?.errors;
            const fullMsg = fieldErrors?.length
                ? `${mainMsg}:\n${fieldErrors.map(err => `• ${err}`).join('\n')}`
                : mainMsg;
            setAlert({ type: 'error', message: fullMsg });
        } finally {
            setSaving(false);
        }
    };

    const handleFileSelect = (files) => {
        const images = Array.from(files).filter(f => f.type.startsWith('image/'));
        const next   = images.map(f => ({ file: f, preview: URL.createObjectURL(f), name: f.name }));
        setPhotos(prev => [...prev, ...next]);
    };

    const handleRemovePhoto = (index) => {
        setPhotos(prev => {
            URL.revokeObjectURL(prev[index].preview);
            return prev.filter((_, i) => i !== index);
        });
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

    const createMenuItem = (label) => (
        <MenuItem
            key="__CREATE__"
            value="__CREATE__"
            sx={{
                borderTop: '1px solid', borderColor: 'divider', mt: 0.5,
                color: accentColor, fontWeight: 600, fontSize: 13.5, gap: 1,
            }}
        >
            <AddCircleOutlinedIcon sx={{ fontSize: 16 }} />
            {label}
        </MenuItem>
    );

    const modelDisabled = saving || loadingOptions || !formValues.brandId || !formValues.typeId;

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="md"
                icon={Inventory2OutlinedIcon}
                title={isEdit ? 'Editar Activo' : 'Nuevo Activo'}
                subtitle={isEdit ? 'Actualiza los datos del activo' : 'Completa los datos para registrar el activo'}
                loading={saving || loadingAsset}
                secondaryButton={{ label: 'Cancelar', onClick: onClose, disabled: saving }}
                primaryButton={{
                    label: saving
                        ? 'Guardando…'
                        : isEdit ? 'Guardar cambios' : 'Crear Activo',
                    onClick: handleSave,
                    disabled: saving || loadingAsset,
                    startIcon: <AddCircleOutlinedIcon />,
                }}
                contentSx={contentSx}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'flex', flexDirection: 'column', gap: 3 }}>

                    <Box>
                        {sectionLabel('Información básica')}
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                            <TextField
                                label="Nombre" value={formValues.name} required
                                onChange={e => handleChange('name', e.target.value)}
                                onBlur={() => handleBlur('name')}
                                fullWidth size="small" disabled={saving}
                                error={touched.name && !!errors.name}
                                helperText={touched.name ? (errors.name || ' ') : ' '}
                                sx={fieldSx}
                            />
                            <TextField
                                label="Descripción" value={formValues.description}
                                onChange={e => handleChange('description', e.target.value)}
                                fullWidth size="small" multiline rows={2} disabled={saving}
                                sx={fieldSx}
                            />
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Clasificación')}
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                            <TextField
                                select label="Marca" value={formValues.brandId} required
                                onChange={e => {
                                    const v = e.target.value;
                                    if (v === '__CREATE__') { openCatalogModal('brandId'); return; }
                                    handleBrandChange(v);
                                }}
                                onBlur={() => handleBlur('brandId')}
                                fullWidth size="small" disabled={saving || loadingOptions}
                                error={touched.brandId && !!errors.brandId}
                                helperText={touched.brandId ? (errors.brandId || ' ') : ' '}
                                sx={fieldSx}
                            >
                                {brands.map(b => <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>)}
                                {createMenuItem('Crear nueva Marca')}
                            </TextField>

                            <TextField
                                select label="Tipo" value={formValues.typeId} required
                                onChange={e => {
                                    const v = e.target.value;
                                    if (v === '__CREATE__') { openCatalogModal('typeId'); return; }
                                    handleTypeChange(v);
                                }}
                                onBlur={() => handleBlur('typeId')}
                                fullWidth size="small" disabled={saving || loadingOptions}
                                error={touched.typeId && !!errors.typeId}
                                helperText={touched.typeId ? (errors.typeId || ' ') : ' '}
                                sx={fieldSx}
                            >
                                {types.map(t => <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>)}
                                {createMenuItem('Crear nuevo Tipo')}
                            </TextField>

                            <TextField
                                select label="Modelo" value={formValues.modelId} required
                                onChange={e => {
                                    const v = e.target.value;
                                    if (v === '__CREATE__') { openCatalogModal('modelId'); return; }
                                    handleChange('modelId', v);
                                }}
                                onBlur={() => handleBlur('modelId')}
                                fullWidth size="small" disabled={modelDisabled}
                                error={touched.modelId && !!errors.modelId}
                                helperText={
                                    (!formValues.brandId || !formValues.typeId)
                                        ? 'Selecciona marca y tipo primero'
                                        : (touched.modelId ? (errors.modelId || ' ') : ' ')
                                }
                                sx={{ ...fieldSx, gridColumn: '1 / -1' }}
                            >
                                {filteredModels.map(m => <MenuItem key={m.id} value={m.id}>{m.name}</MenuItem>)}
                                {createMenuItem('Crear nuevo Modelo')}
                            </TextField>
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Ubicación')}
                        <TextField
                            select label="Locación" value={formValues.locationId} required
                            onChange={e => {
                                const v = e.target.value;
                                if (v === '__CREATE__') { openCatalogModal('locationId'); return; }
                                handleChange('locationId', v);
                            }}
                            onBlur={() => handleBlur('locationId')}
                            fullWidth size="small" disabled={saving || loadingOptions}
                            error={touched.locationId && !!errors.locationId}
                            helperText={touched.locationId ? (errors.locationId || ' ') : ' '}
                            sx={fieldSx}
                        >
                            {locations.map(l => (
                                <MenuItem key={l.id} value={l.id}>
                                    {l.name}{l.site?.name ? ` — ${l.site.name}` : ' '}
                                </MenuItem>
                            ))}
                            {createMenuItem('Crear nueva Locación')}
                        </TextField>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Estado')}
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 2fr' }, gap: 2 }}>
                            <TextField
                                select label="Estado" value={formValues.status} required
                                onChange={e => handleChange('status', e.target.value)}
                                onBlur={() => handleBlur('status')}
                                fullWidth size="small" disabled={saving}
                                error={touched.status && !!errors.status}
                                helperText={touched.status ? (errors.status || ' ') : ' '}
                                sx={fieldSx}
                            >
                                {STATUS_OPTIONS.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
                            </TextField>
                            <TextField
                                label="Descripción del estado" value={formValues.statusDescription}
                                onChange={e => handleChange('statusDescription', e.target.value)}
                                fullWidth size="small" disabled={saving}
                                sx={fieldSx}
                            />
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Fechas')}
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', md: 'repeat(3, 1fr)' }, gap: 2 }}>
                            <DatePicker
                                label="Fecha de adquisición"
                                value={formValues.acquisitionDate ? dayjs(formValues.acquisitionDate) : null}
                                onChange={v => handleChange('acquisitionDate', v ? v.format('YYYY-MM-DD') : '')}
                                disabled={saving}
                                slotProps={{ textField: { size: 'small', fullWidth: true, sx: fieldSx } }}
                            />
                            <DatePicker
                                label="Fin de garantía"
                                value={formValues.warrantyEndDate ? dayjs(formValues.warrantyEndDate) : null}
                                onChange={v => handleChange('warrantyEndDate', v ? v.format('YYYY-MM-DD') : '')}
                                disabled={saving}
                                slotProps={{ textField: { size: 'small', fullWidth: true, sx: fieldSx } }}
                            />
                            <DatePicker
                                label="Fin de soporte firmware"
                                value={formValues.firmwareSupportEndDate ? dayjs(formValues.firmwareSupportEndDate) : null}
                                onChange={v => handleChange('firmwareSupportEndDate', v ? v.format('YYYY-MM-DD') : '')}
                                disabled={saving}
                                slotProps={{ textField: { size: 'small', fullWidth: true, sx: fieldSx } }}
                            />
                        </Box>
                    </Box>

                    {requiresNetworkInterface && (
                        <>
                            <Divider />
                            <Box>
                                {sectionLabel('Interfaz de red')}
                                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                                    <TextField
                                        label="Dirección IP" value={formValues.ipAddress} required
                                        onChange={e => {
                                            const raw = e.target.value;
                                            const prev = formValues.ipAddress;
                                            const isDeleting = raw.length < (prev?.length ?? 0);

                                            const cleaned = raw
                                                .replace(/[^\d.]/g, '')
                                                .replace(/\.{2,}/g, '.')
                                                .replace(/^\./, '');

                                            const parts = cleaned.split('.');
                                            const processed = parts.slice(0, 4).map(p => p.slice(0, 3));
                                            const joined = processed.join('.');

                                            const lastPart = processed[processed.length - 1];
                                            const autoAddDot = !isDeleting
                                                && lastPart.length === 3
                                                && processed.length < 4
                                                && !cleaned.endsWith('.');

                                            handleChange('ipAddress', autoAddDot ? joined + '.' : joined);
                                        }}
                                        onBlur={() => handleBlur('ipAddress')}
                                        fullWidth size="small" disabled={saving}
                                        error={touched.ipAddress && !!errors.ipAddress}
                                        helperText={touched.ipAddress ? (errors.ipAddress || ' ') : ' '}
                                        placeholder="192.168.0.1"
                                        sx={fieldSx}
                                    />
                                    <TextField
                                        label="Dirección MAC" value={formValues.macAddress} required
                                        onChange={e => {
                                            const hex = e.target.value.replace(/[^0-9A-Fa-f]/g, '').toUpperCase().slice(0, 12);
                                            const formatted = hex.match(/.{1,2}/g)?.join(':') ?? '';
                                            handleChange('macAddress', formatted);
                                        }}
                                        onBlur={() => handleBlur('macAddress')}
                                        fullWidth size="small" disabled={saving}
                                        error={touched.macAddress && !!errors.macAddress}
                                        helperText={touched.macAddress ? (errors.macAddress || ' ') : ' '}
                                        placeholder="AA:BB:CC:DD:EE:FF"
                                        sx={fieldSx}
                                    />
                                </Box>
                            </Box>
                        </>
                    )}

                    <Divider />

                    <Box>
                        {sectionLabel('Fotos')}
                        <Box
                            onDragOver={e => { e.preventDefault(); setIsDragOver(true); }}
                            onDragLeave={() => setIsDragOver(false)}
                            onDrop={e => { e.preventDefault(); setIsDragOver(false); handleFileSelect(e.dataTransfer.files); }}
                            onClick={() => fileInputRef.current?.click()}
                            sx={{
                                border: '2px dashed',
                                borderColor: isDragOver ? accentColor : 'divider',
                                borderRadius: '12px',
                                p: 3,
                                display: 'flex', flexDirection: 'column',
                                alignItems: 'center', justifyContent: 'center',
                                gap: 1, cursor: 'pointer',
                                bgcolor: isDragOver ? `color-mix(in srgb, ${accentColor} 5%, transparent)` : 'transparent',
                                transition: 'all 0.2s',
                                '&:hover': {
                                    borderColor: accentColor,
                                    bgcolor: `color-mix(in srgb, ${accentColor} 5%, transparent)`,
                                },
                            }}
                        >
                            <CloudUploadIcon sx={{ fontSize: 32, color: isDragOver ? accentColor : 'text.secondary' }} />
                            <Typography sx={{ fontSize: 13.5, color: 'text.secondary', textAlign: 'center' }}>
                                Arrastra imágenes aquí o{' '}
                                <Typography component="span" sx={{ color: accentColor, fontWeight: 600 }}>
                                    selecciona archivos
                                </Typography>
                            </Typography>
                            <Typography sx={{ fontSize: 11.5, color: 'text.disabled' }}>
                                JPG, PNG, WEBP, GIF
                            </Typography>
                        </Box>
                        <input
                            ref={fileInputRef}
                            type="file"
                            accept="image/*"
                            multiple
                            style={{ display: 'none' }}
                            onChange={e => { handleFileSelect(e.target.files); e.target.value = ''; }}
                        />
                        {photos.length > 0 && (
                            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5, mt: 2 }}>
                                {photos.map((photo, index) => (
                                    <Box
                                        key={index}
                                        sx={{
                                            position: 'relative', width: 80, height: 80,
                                            borderRadius: '10px', overflow: 'hidden',
                                            border: '1px solid', borderColor: 'divider',
                                        }}
                                    >
                                        <Box
                                            component="img"
                                            src={photo.preview}
                                            alt={photo.name}
                                            sx={{ width: '100%', height: '100%', objectFit: 'cover' }}
                                        />
                                        <IconButton
                                            size="small"
                                            onClick={e => { e.stopPropagation(); handleRemovePhoto(index); }}
                                            sx={{
                                                position: 'absolute', top: 2, right: 2,
                                                bgcolor: 'rgba(0,0,0,0.55)', color: '#fff',
                                                p: 0.25,
                                                '&:hover': { bgcolor: 'rgba(0,0,0,0.75)' },
                                            }}
                                        >
                                            <CloseIcon sx={{ fontSize: 12 }} />
                                        </IconButton>
                                    </Box>
                                ))}
                            </Box>
                        )}
                    </Box>
                </Box>
            </GeneralModal>

            {catalogModal && (
                <CatalogFormModal
                    open
                    onClose={() => setCatalogModal(null)}
                    onSaved={handleCatalogSaved}
                    config={catalogModal.config}
                />
            )}

            <AlertModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}
