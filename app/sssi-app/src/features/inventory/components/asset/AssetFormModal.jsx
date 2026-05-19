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
import DialogModal from '../../../../common/components/DialogModal.jsx';
import SearchableSelect from '../../../../common/components/SearchableSelect.jsx';
import CatalogFormModal from '../catalog/CatalogFormModal.jsx';
import { CATALOG_CONFIG } from '../catalog/catalogConfig.js';
import { fetchCatalogOptions } from '../../services/catalogService.js';
import { createAsset, updateAsset, fetchAssetById, fetchLastKnownNetworkInterface, checkAssetNumber } from '../../services/assetsService.js';
import { uploadPhoto, registerArchive, fetchAssetArchives, deleteArchive } from '../../services/assetArchiveService.js';
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
    assetNumber: '', serialNumber: '',
    latitude: '', longitude: '',
};

const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

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

    const [catalogModal, setCatalogModal]           = useState(null);
    const [photos, setPhotos]                       = useState([]);
    const [existingPhotos, setExistingPhotos]       = useState([]);
    const [photosToDelete, setPhotosToDelete]       = useState([]);
    const [isDragOver, setIsDragOver]               = useState(false);
    const [pendingTypeChange, setPendingTypeChange] = useState(null);
    const [assetNumberExists, setAssetNumberExists] = useState(false);
    const [showAssetNumberConfirm, setShowAssetNumberConfirm] = useState(false);
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
        setExistingPhotos([]);
        setPhotosToDelete([]);
        setFormValues(INIT);
        setErrors({});
        setTouched({});
        setSaving(false);
        setAlert(null);
        setIsDragOver(false);
        setPendingTypeChange(null);
        setAssetNumberExists(false);
        setShowAssetNumberConfirm(false);
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
            setBrands(prev => {
                const ids = new Set(b.map(x => x.id));
                return [...b, ...prev.filter(x => !ids.has(x.id))];
            });
            setTypes(prev => {
                const ids = new Set(t.map(x => x.id));
                return [...t, ...prev.filter(x => !ids.has(x.id))];
            });
            setModels(prev => {
                const ids = new Set(m.map(x => x.id));
                return [...m, ...prev.filter(x => !ids.has(x.id))];
            });
            setLocations(prev => {
                const ids = new Set(l.map(x => x.id));
                return [...l, ...prev.filter(x => !ids.has(x.id))];
            });
        }).catch(() => {}).finally(() => { if (!cancelled) setLoadingOptions(false); });
        return () => { cancelled = true; };
    }, [open]);

    const loadAssetData = () => {
        if (!open || !assetId) return;
        let cancelled = false;
        setLoadingAsset(true);

        fetchAssetById(assetId)
            .then(asset => {
                if (cancelled) return;
                if (asset) {
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
                        assetNumber:            asset.assetNumber ?? '',
                        serialNumber:           asset.serialNumber ?? '',
                        latitude:               asset.latitude != null ? String(asset.latitude) : '',
                        longitude:              asset.longitude != null ? String(asset.longitude) : '',
                    });
                    if (asset.model?.type) {
                        setTypes(prev => prev.some(t => t.id === asset.model.type.id) ? prev : [...prev, asset.model.type]);
                    }
                    if (asset.model?.brand) {
                        setBrands(prev => prev.some(b => b.id === asset.model.brand.id) ? prev : [...prev, asset.model.brand]);
                    }
                    if (asset.model) {
                        setModels(prev => prev.some(m => m.id === asset.model.id) ? prev : [...prev, asset.model]);
                    }
                    if (asset.location) {
                        setLocations(prev => prev.some(l => l.id === asset.location.id) ? prev : [...prev, asset.location]);
                    }
                }
            })
            .catch(() => {
                if (!cancelled) setAlert({ type: 'error', message: 'No se pudo cargar el activo' });
            })
            .finally(() => { if (!cancelled) setLoadingAsset(false); });

        fetchAssetArchives(assetId)
            .then(archives => {
                if (cancelled) return;
                setExistingPhotos(archives.filter(a => !!a.imageUrl));
            })
            .catch(() => {});

        return () => { cancelled = true; };
    };

    useEffect(loadAssetData, [open, assetId]);

    const validateField = (key, value) => {
        const required = ['name', 'brandId', 'typeId', 'modelId', 'locationId', 'status', 'assetNumber', 'serialNumber'];
        let error = '';

        if (required.includes(key) && (!value || (typeof value === 'string' && !value.trim()))) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'name' && value?.trim()) {
            if (value.trim().length < 2)   error = 'Mínimo 2 caracteres';
            if (value.trim().length > 150) error = 'Máximo 150 caracteres';
        }

        if (!error && key === 'ipAddress' && value?.trim()) {
            if (!/^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/.test(value.trim())) {
                error = 'La dirección IP no tiene un formato válido';
            }
        }
        if (!error && key === 'macAddress' && value?.trim()) {
            const mac = value.trim();
            if (mac.length < 12 || mac.length > 17) {
                error = 'La dirección MAC debe tener entre 12 y 17 caracteres';
            }
        }

        if (!error && key === 'latitude' && value !== '') {
            const n = parseFloat(value);
            if (isNaN(n) || n < -90 || n > 90) error = 'La latitud debe estar entre -90 y 90';
        }
        if (!error && key === 'longitude' && value !== '') {
            const n = parseFloat(value);
            if (isNaN(n) || n < -180 || n > 180) error = 'La longitud debe estar entre -180 y 180';
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

    const prefillLastKnownNi = async () => {
        const ni = await fetchLastKnownNetworkInterface(assetId);
        if (!ni) return;
        setFormValues(prev => ({
            ...prev,
            ipAddress:  prev.ipAddress  || ni.ipAddress  || '',
            macAddress: prev.macAddress || ni.macAddress || '',
        }));
    };

    const applyTypeChange = (value, newType) => {
        setFormValues(prev => ({
            ...prev,
            typeId: value,
            modelId: '',
            ...(!newType?.requiresNetworkInterface && { ipAddress: '', macAddress: '' }),
        }));
        if (touched.typeId) validateField('typeId', value);
        if (isEdit && newType?.requiresNetworkInterface && !formValues.ipAddress && !formValues.macAddress) {
            prefillLastKnownNi();
        }
    };

    const handleTypeChange = (value) => {
        const newType = types.find(t => t.id === value);
        const willDeleteNi = isEdit
            && requiresNetworkInterface
            && !newType?.requiresNetworkInterface
            && (!!formValues.ipAddress || !!formValues.macAddress);

        if (willDeleteNi) {
            setPendingTypeChange({
                value,
                newType,
                ipAddress: formValues.ipAddress,
                macAddress: formValues.macAddress,
            });
            return;
        }
        applyTypeChange(value, newType);
    };

    const confirmTypeChange = () => {
        if (pendingTypeChange) {
            applyTypeChange(pendingTypeChange.value, pendingTypeChange.newType);
        }
        setPendingTypeChange(null);
    };

    const cancelTypeChange = () => setPendingTypeChange(null);

    const handleBlur = (key) => {
        setTouched(prev => ({ ...prev, [key]: true }));
        validateField(key, formValues[key]);
    };

    const handleAssetNumberBlur = async () => {
        setTouched(prev => ({ ...prev, assetNumber: true }));
        if (!formValues.assetNumber?.trim()) {
            setAssetNumberExists(false);
            return;
        }
        try {
            const exists = await checkAssetNumber(
                formValues.assetNumber.trim(),
                isEdit ? assetId : null
            );
            setAssetNumberExists(exists);
        } catch {
            setAssetNumberExists(false);
        }
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

    const doSave = async () => {
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
                assetNumber:             formValues.assetNumber?.trim()           || null,
                serialNumber:            formValues.serialNumber?.trim()          || null,
                latitude:                formValues.latitude !== '' ? parseFloat(formValues.latitude) : null,
                longitude:               formValues.longitude !== '' ? parseFloat(formValues.longitude) : null,
                ...(requiresNetworkInterface && formValues.ipAddress?.trim() && formValues.macAddress?.trim() && {
                    networkInterface: {
                        ipAddress:  formValues.ipAddress.trim(),
                        macAddress: formValues.macAddress.trim(),
                    },
                }),
            };
            let savedAssetId;
            if (isEdit) {
                await updateAsset(assetId, payload);
                savedAssetId = assetId;
            } else {
                const created = await createAsset(payload);
                savedAssetId = created.id;
            }

            if (photos.length > 0) {
                const results = await Promise.allSettled(
                    photos.map(async (photo) => {
                        const objectName = await uploadPhoto(savedAssetId, photo.file);
                        await registerArchive(savedAssetId, objectName);
                    })
                );
                const failed = results.filter(r => r.status === 'rejected').length;
                if (failed > 0) {
                    setAlert({ type: 'warning', message: `El activo se guardó, pero ${failed} imagen(es) no pudieron subirse.` });
                    onSaved?.();
                    onClose();
                    return;
                }
            }

            if (photosToDelete.length > 0) {
                await Promise.allSettled(photosToDelete.map(id => deleteArchive(id)));
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

    const handleSave = async () => {
        const requiredFields = ['name', 'brandId', 'typeId', 'modelId', 'locationId', 'status', 'assetNumber', 'serialNumber'];

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
        if (!newErrors.ipAddress && formValues.ipAddress?.trim()) {
            if (!/^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/.test(formValues.ipAddress.trim())) {
                newErrors.ipAddress  = 'La dirección IP no tiene un formato válido';
                newTouched.ipAddress = true;
            }
        }
        if (!newErrors.macAddress && formValues.macAddress?.trim()) {
            const mac = formValues.macAddress.trim();
            if (mac.length < 12 || mac.length > 17) {
                newErrors.macAddress  = 'La dirección MAC debe tener entre 12 y 17 caracteres';
                newTouched.macAddress = true;
            }
        }
        if (formValues.latitude !== '') {
            const n = parseFloat(formValues.latitude);
            if (isNaN(n) || n < -90 || n > 90) {
                newErrors.latitude  = 'La latitud debe estar entre -90 y 90';
                newTouched.latitude = true;
            }
        }
        if (formValues.longitude !== '') {
            const n = parseFloat(formValues.longitude);
            if (isNaN(n) || n < -180 || n > 180) {
                newErrors.longitude  = 'La longitud debe estar entre -180 y 180';
                newTouched.longitude = true;
            }
        }

        setTouched(prev => ({ ...prev, ...newTouched }));
        setErrors(prev  => ({ ...prev, ...newErrors  }));

        if (Object.keys(newErrors).length > 0) {
            setAlert({ type: 'warning', message: 'Revisa los datos antes de continuar' });
            return;
        }

        if (assetNumberExists) {
            setShowAssetNumberConfirm(true);
            return;
        }

        await doSave();
    };

    const handleFileSelect = (files) => {
        const all     = Array.from(files);
        const valid   = all.filter(f => ALLOWED_IMAGE_TYPES.includes(f.type));
        const invalid = all.filter(f => !ALLOWED_IMAGE_TYPES.includes(f.type));

        if (invalid.length > 0) {
            setAlert({
                type: 'warning',
                message: `Formato no permitido: ${invalid.map(f => f.name).join(', ')}. Solo se aceptan JPG, PNG y WEBP.`,
            });
        }

        if (valid.length === 0) return;
        const next = valid.map(f => ({ file: f, preview: URL.createObjectURL(f), name: f.name }));
        setPhotos(prev => [...prev, ...next]);
    };

    const handleRemovePhoto = (index) => {
        setPhotos(prev => {
            URL.revokeObjectURL(prev[index].preview);
            return prev.filter((_, i) => i !== index);
        });
    };

    const handleRemoveExistingPhoto = (archiveId) => {
        setExistingPhotos(prev => prev.filter(p => p.id !== archiveId));
        setPhotosToDelete(prev => [...prev, archiveId]);
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
                        {sectionLabel('Identificación')}
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                            <Box>
                                <TextField
                                    label="Número de activo" value={formValues.assetNumber}
                                    required
                                    onChange={e => handleChange('assetNumber', e.target.value)}
                                    onBlur={handleAssetNumberBlur}
                                    fullWidth size="small" disabled={saving}
                                    error={touched.assetNumber && !!errors.assetNumber}
                                    sx={fieldSx}
                                    helperText={
                                        touched.assetNumber && errors.assetNumber
                                            ? errors.assetNumber
                                            : assetNumberExists
                                                ? 'Este número ya está en uso (válido para adquisiciones en conjunto)'
                                                : ' '
                                    }
                                    FormHelperTextProps={{
                                        sx: (!errors.assetNumber || !touched.assetNumber) && assetNumberExists
                                            ? { color: 'warning.main' }
                                            : undefined,
                                    }}
                                />
                            </Box>
                            <TextField
                                label="Número de serie" value={formValues.serialNumber}
                                required
                                onChange={e => handleChange('serialNumber', e.target.value)}
                                onBlur={() => handleBlur('serialNumber')}
                                fullWidth size="small" disabled={saving}
                                error={touched.serialNumber && !!errors.serialNumber}
                                helperText={touched.serialNumber ? (errors.serialNumber || ' ') : ' '}
                                sx={fieldSx}
                            />
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Clasificación')}
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                            <SearchableSelect
                                label="Marca" value={formValues.brandId} required
                                onChange={handleBrandChange}
                                onBlur={() => handleBlur('brandId')}
                                fullWidth size="small" disabled={saving || loadingOptions}
                                error={touched.brandId && !!errors.brandId}
                                helperText={touched.brandId ? (errors.brandId || ' ') : ' '}
                                sx={fieldSx}
                                items={brands}
                                getItemLabel={b => b.name}
                                getItemValue={b => b.id}
                                onCreate={() => openCatalogModal('brandId')}
                                createLabel="Crear nueva Marca"
                            />

                            <SearchableSelect
                                label="Tipo" value={formValues.typeId} required
                                onChange={handleTypeChange}
                                onBlur={() => handleBlur('typeId')}
                                fullWidth size="small" disabled={saving || loadingOptions}
                                error={touched.typeId && !!errors.typeId}
                                helperText={touched.typeId ? (errors.typeId || ' ') : ' '}
                                sx={fieldSx}
                                items={types}
                                getItemLabel={t => t.name}
                                getItemValue={t => t.id}
                                onCreate={() => openCatalogModal('typeId')}
                                createLabel="Crear nuevo Tipo"
                            />

                            <SearchableSelect
                                label="Modelo" value={formValues.modelId} required
                                onChange={v => handleChange('modelId', v)}
                                onBlur={() => handleBlur('modelId')}
                                fullWidth size="small" disabled={modelDisabled}
                                error={touched.modelId && !!errors.modelId}
                                helperText={
                                    (!formValues.brandId || !formValues.typeId)
                                        ? 'Selecciona marca y tipo primero'
                                        : (touched.modelId ? (errors.modelId || ' ') : ' ')
                                }
                                sx={{ ...fieldSx, gridColumn: '1 / -1' }}
                                items={filteredModels}
                                getItemLabel={m => m.name}
                                getItemValue={m => m.id}
                                onCreate={() => openCatalogModal('modelId')}
                                createLabel="Crear nuevo Modelo"
                            />
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Ubicación')}
                        <SearchableSelect
                            label="Locación" value={formValues.locationId} required
                            onChange={v => handleChange('locationId', v)}
                            onBlur={() => handleBlur('locationId')}
                            fullWidth size="small" disabled={saving || loadingOptions}
                            error={touched.locationId && !!errors.locationId}
                            helperText={touched.locationId ? (errors.locationId || ' ') : ' '}
                            sx={fieldSx}
                            items={locations}
                            getItemLabel={l => l.name + (l.site?.name ? ` — ${l.site.name}` : '')}
                            getItemValue={l => l.id}
                            onCreate={() => openCatalogModal('locationId')}
                            createLabel="Crear nueva Locación"
                        />
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Coordenadas')}
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                            <TextField
                                label="Latitud" value={formValues.latitude}
                                onChange={e => handleChange('latitude', e.target.value)}
                                onBlur={() => handleBlur('latitude')}
                                fullWidth size="small" disabled={saving}
                                type="number"
                                inputProps={{ step: 'any' }}
                                error={touched.latitude && !!errors.latitude}
                                helperText={touched.latitude ? (errors.latitude || ' ') : ' '}
                                placeholder="-33.4500000"
                                sx={fieldSx}
                            />
                            <TextField
                                label="Longitud" value={formValues.longitude}
                                onChange={e => handleChange('longitude', e.target.value)}
                                onBlur={() => handleBlur('longitude')}
                                fullWidth size="small" disabled={saving}
                                type="number"
                                inputProps={{ step: 'any' }}
                                error={touched.longitude && !!errors.longitude}
                                helperText={touched.longitude ? (errors.longitude || ' ') : ' '}
                                placeholder="-70.6500000"
                                sx={fieldSx}
                            />
                        </Box>
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
                                {sectionLabel('IP y MAC')}
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
                                JPG, PNG, WEBP
                            </Typography>
                        </Box>
                        <input
                            ref={fileInputRef}
                            type="file"
                            accept="image/jpeg,image/png,image/webp"
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

                        {isEdit && existingPhotos.length > 0 && (
                            <Box sx={{ mt: 2.5 }}>
                                <Typography sx={{
                                    fontSize: 11, fontWeight: 600, letterSpacing: '0.06em',
                                    textTransform: 'uppercase', color: 'text.secondary', mb: 1,
                                }}>
                                    Fotos guardadas
                                </Typography>
                                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5 }}>
                                    {existingPhotos.map(photo => (
                                        <Box
                                            key={photo.id}
                                            sx={{
                                                position: 'relative', width: 80, height: 80,
                                                borderRadius: '10px', overflow: 'hidden',
                                                border: '1px solid', borderColor: 'divider',
                                            }}
                                        >
                                            <Box
                                                component="img"
                                                src={photo.imageUrl}
                                                alt={photo.caption || ''}
                                                sx={{ width: '100%', height: '100%', objectFit: 'cover' }}
                                            />
                                            <IconButton
                                                size="small"
                                                onClick={() => handleRemoveExistingPhoto(photo.id)}
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

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />

            <DialogModal
                type="warning"
                open={showAssetNumberConfirm}
                title="Número de activo duplicado"
                message={'El número de activo ingresado ya existe en otro activo.\n\nEsto es válido en adquisiciones en conjunto (por ejemplo, varias cámaras de un mismo paquete).\n\n¿Deseas continuar de todas formas?'}
                onClose={() => setShowAssetNumberConfirm(false)}
                onConfirm={async () => { setShowAssetNumberConfirm(false); await doSave(); }}
                confirmLabel="Continuar"
            />

            <DialogModal
                type="warning"
                open={!!pendingTypeChange}
                title="Eliminar IP y MAC"
                message={[
                    'El nuevo tipo seleccionado no requiere IP y MAC. La IP y MAC actualmente asociada a este activo será eliminada al guardar.',
                    pendingTypeChange?.ipAddress  ? `IP: ${pendingTypeChange.ipAddress}`   : null,
                    pendingTypeChange?.macAddress ? `MAC: ${pendingTypeChange.macAddress}` : null,
                    '¿Deseas continuar?',
                ].filter(Boolean).join('\n\n')}
                onClose={cancelTypeChange}
                onConfirm={confirmTypeChange}
                confirmLabel="Eliminar y continuar"
            />
        </>
    );
}
