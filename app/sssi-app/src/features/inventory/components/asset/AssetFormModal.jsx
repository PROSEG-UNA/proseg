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
import DeleteIcon from '@mui/icons-material/Delete';
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined';
import CoordinateMapPicker from './CoordinateMapPicker.jsx';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import SearchableSelect from '../../../../common/components/SearchableSelect.jsx';
import CatalogFormModal from '../catalog/CatalogFormModal.jsx';
import { CATALOG_CONFIG } from '../catalog/catalogConfig.js';
import { LOCATION_CONFIG as LOCATION_CATALOG_CONFIG } from '../../../locations/components/location/locationConfig.js';
import { createCatalogItem } from '../../services/catalogService.js';
import { createAsset, updateAsset, fetchAssetById, fetchLastKnownNetworkInterface, checkAssetNumber } from '../../services/assetsService.js';
import { uploadPhoto, registerArchive, fetchAssetArchives, deleteArchive } from '../../services/assetArchiveService.js';
import {
    fetchAssetComponents,
    createAssetComponent,
    updateAssetComponent,
    deleteAssetComponent,
} from '../../services/assetComponentsService.js';
import { INVENTORY_ENDPOINTS } from '../../services/endpoints.js';
import { useAssetFormState } from '../../hooks/useAssetFormState.js';
import { useAssetCatalogOptions } from '../../hooks/useAssetCatalogOptions.js';
import { useAssetComponents } from '../../hooks/useAssetComponents.js';
import { useAssetPhotos } from '../../hooks/useAssetPhotos.js';

const STATUS_OPTIONS = [
    { value: 'APROBADO', label: 'Aprobado' },
    { value: 'DE_BAJA',  label: 'De baja' },
];

const MIN_FLOOR_NUMBER = 1;

const FLOOR_NUMBER_ERROR = `El piso debe ser ${MIN_FLOOR_NUMBER} o mayor`;

const toFloorNumberValue = (rawValue) => {
    const withoutLeadingZeros = String(rawValue).replace(/[^0-9]/g, '').replace(/^0+/, '');
    return withoutLeadingZeros === '' ? '' : Number(withoutLeadingZeros);
};

const readAsDataUrl = (file) => new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(file);
});

export default function AssetFormModal({ open, onClose, onSaved, assetId = null }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const isEdit = !!assetId;

    const formState = useAssetFormState(open);
    const catalogOptions = useAssetCatalogOptions(open);
    const assetComponents = useAssetComponents(open);
    const assetPhotos = useAssetPhotos(open);

    const [catalogModal, setCatalogModal] = useState(null);
    const fileInputRef = useRef(null);

    const { formValues, setFormValues, errors, setErrors, touched, setTouched, saving, setSaving, alert, setAlert, loadingAsset, setLoadingAsset, assetNumberExists, setAssetNumberExists, showAssetNumberConfirm, setShowAssetNumberConfirm, pendingTypeChange, setPendingTypeChange } = formState;
    const { options, upsertOption, loadingOptions } = catalogOptions;
    const { brands, types, models, campuses, buildings, locations } = options;
    const { components, setComponents, componentErrors, setComponentErrors, componentsToDelete, setComponentsToDelete } = assetComponents;

    const selectedType             = types.find(t => t.id === formValues.typeId) ?? null;
    const requiresNetworkInterface = selectedType?.requiresNetworkInterface ?? false;

    const filteredModels = models.filter(m => {
        const matchBrand = !formValues.brandId || m.brand?.id === formValues.brandId;
        const matchType  = !formValues.typeId  || m.type?.id  === formValues.typeId;
        return matchBrand && matchType;
    });

    const filteredBuildings = buildings.filter(b => {
        if (b.name === '-') return false;
        return !formValues.campusId || b.campus?.id === formValues.campusId;
    });

    const filteredLocations = locations.filter(l => {
        if (l.description === '-') return false;
        if (!formValues.buildingId) return false;
        return l.floor?.building?.id === formValues.buildingId;
    });

    const {
        addComponent,
        updateComponentField,
        removeComponent,
        validateComponents,
    } = assetComponents;

    const { photos, setPhotos, existingPhotos, setExistingPhotos, photosToDelete, setPhotosToDelete, isDragOver, setIsDragOver } = assetPhotos;

    useEffect(() => {
        if (!open || !assetId) return undefined;

        let cancelled = false;

        const assetLoadHandle = window.setTimeout(() => {
            setLoadingAsset(true);

            fetchAssetById(assetId)
                .then(asset => {
                    if (cancelled) return;
                    if (asset) {
                        setFormValues({
                            executingUnit:          asset.executingUnit ?? '',
                            responsibleEmployee:    asset.responsibleEmployee ?? '',
                            responsibleEmployeeId:  asset.responsibleEmployeeId ?? '',
                            brandId:                asset.model?.brand?.id ?? '',
                            typeId:                 asset.model?.type?.id ?? '',
                            modelId:                asset.model?.id ?? '',
                            campusId:               asset.location?.floor?.building?.campus?.id ?? '',
                            buildingId:             asset.location?.floor?.building?.id ?? '',
                            floorNumber:            asset.location?.floor?.name ? parseInt(asset.location.floor.name) : '',
                            locationId:             asset.location?.id ?? '',
                            status:                 asset.status ?? '',
                            acquisitionDate:        asset.acquisitionDate ?? '',
                            warrantyEndDate:        asset.warrantyEndDate ?? '',
                            firmwareSupportEndDate: asset.firmwareSupportEndDate ?? '',
                            decommissionDate:       asset.decommissionDate ?? '',
                            ipAddress:              asset.networkInterface?.ipAddress ?? '',
                            macAddress:             asset.networkInterface?.macAddress ?? '',
                            assetNumber:            asset.assetNumber ?? '',
                            serialNumber:           asset.serialNumber ?? '',
                            latitude:               asset.latitude != null ? String(asset.latitude) : '',
                            longitude:              asset.longitude != null ? String(asset.longitude) : '',
                        });
                        upsertOption('brands', asset.model?.brand);
                        upsertOption('types', asset.model?.type);
                        upsertOption('models', asset.model);
                        upsertOption('campuses', asset.location?.floor?.building?.campus);
                        upsertOption('buildings', asset.location?.floor?.building);
                        upsertOption('locations', asset.location);
                    }
                })
                .catch(() => {
                    if (!cancelled) setAlert({ type: 'error', message: 'No se pudo cargar el activo' });
                })
                .finally(() => {
                    if (!cancelled) setLoadingAsset(false);
                });

            fetchAssetArchives(assetId)
                .then(archives => {
                    if (cancelled) return;
                    setExistingPhotos(archives.filter(a => !!a.imageUrl));
                })
                .catch(() => {});

            fetchAssetComponents(assetId)
                .then(assetComponents => {
                    if (cancelled) return;
                    setComponents((assetComponents ?? []).map(component => ({
                        localId: component.id,
                        id: component.id,
                        name: component.name ?? '',
                        quantity: component.quantity != null ? String(component.quantity) : '1',
                        location: component.location ?? '',
                        observations: component.observations ?? '',
                    })));
                    setComponentErrors({});
                    setComponentsToDelete([]);
                })
                .catch(() => {});
        }, 0);

        return () => {
            cancelled = true;
            window.clearTimeout(assetLoadHandle);
        };
    }, [open, assetId]);

    const validateField = (key, value) => {
        const required = ['brandId', 'typeId', 'modelId', 'campusId', 'status', 'assetNumber'];
        let error = '';

        if (required.includes(key) && (!value || (typeof value === 'string' && !value.trim()))) {
            error = 'Este campo es requerido';
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

        if (!error && key === 'floorNumber' && value !== '' && value != null) {
            const floor = Number(value);
            if (!Number.isInteger(floor) || floor < MIN_FLOOR_NUMBER) error = FLOOR_NUMBER_ERROR;
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
        if (key === 'status' && value === 'APROBADO') {
            setFormValues(prev => ({ ...prev, status: value, decommissionDate: '' }));
            setErrors(prev => ({ ...prev, decommissionDate: '' }));
            if (touched[key]) validateField(key, value);
            return;
        }
        setFormValues(prev => ({ ...prev, [key]: value }));
        if (touched[key]) validateField(key, value);
    };

    const handleFloorNumberChange = (rawValue) => {
        handleChange('floorNumber', toFloorNumberValue(rawValue));
    };

    const handleCampusChange = (value) => {
        setFormValues(prev => ({ ...prev, campusId: value, buildingId: '', floorNumber: '', locationId: '' }));
        if (touched.campusId) validateField('campusId', value);
    };

    const handleBuildingChange = (value) => {
        const building = buildings.find(b => b.id === value);
        if (building && building.campus?.id && building.campus.id !== formValues.campusId) {
            setFormValues(prev => ({ ...prev, campusId: building.campus.id, buildingId: value, floorNumber: '', locationId: '' }));
        } else {
            setFormValues(prev => ({ ...prev, buildingId: value, floorNumber: '', locationId: '' }));
        }
        if (touched.buildingId) validateField('buildingId', value);
    };

    const handleCoordinatesChange = (lat, lng) => {
        const latStr = lat.toFixed(7);
        const lngStr = lng.toFixed(7);
        setFormValues(prev => ({ ...prev, latitude: latStr, longitude: lngStr }));
        setTouched(prev => ({ ...prev, latitude: true, longitude: true }));
        setErrors(prev => ({ ...prev, latitude: null, longitude: null }));
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

    const selectType = (value, newType) => {
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

    const handleTypeChange = (value) => selectType(value, types.find(t => t.id === value));

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

    const refreshAssetNumberExists = async () => {
        if (!formValues.assetNumber?.trim()) {
            setAssetNumberExists(false);
            return false;
        }
        try {
            const exists = await checkAssetNumber(
                formValues.assetNumber.trim(),
                isEdit ? assetId : null
            );
            setAssetNumberExists(exists);
            return exists;
        } catch {
            setAssetNumberExists(false);
            return false;
        }
    };

    const handleAssetNumberBlur = async () => {
        setTouched(prev => ({ ...prev, assetNumber: true }));
        await refreshAssetNumberExists();
    };

    const clearErrorsFor = (keys) => {
        setErrors(prev => {
            const next = { ...prev };
            keys.forEach(key => { next[key] = ''; });
            return next;
        });
    };

    const openCatalogModal = (fieldKey) => {
        const configByField = {
            brandId:    CATALOG_CONFIG.brand,
            typeId:     CATALOG_CONFIG.type,
            modelId:    CATALOG_CONFIG.model,
            campusId:   LOCATION_CATALOG_CONFIG.campus,
            buildingId: LOCATION_CATALOG_CONFIG.building,
            locationId: LOCATION_CATALOG_CONFIG.location,
        };
        const initialValuesByField = {
            modelId: {
                brandId: formValues.brandId,
                typeId: formValues.typeId,
            },
            buildingId: {
                campusId: formValues.campusId,
            },
            locationId: {
                campusId: formValues.campusId,
                buildingId: formValues.buildingId,
                floorNumber: formValues.floorNumber || 1,
            },
        };
        setCatalogModal({
            config: configByField[fieldKey],
            fieldKey,
            initialValues: initialValuesByField[fieldKey],
        });
    };

    const selectCreatedBrand = (brand) => {
        upsertOption('brands', brand);
        handleBrandChange(brand.id);
    };

    const selectCreatedType = (type) => {
        upsertOption('types', type);
        selectType(type.id, type);
    };

    const selectCreatedModel = (model) => {
        upsertOption('brands', model.brand);
        upsertOption('types', model.type);
        upsertOption('models', model);
        setFormValues(prev => ({
            ...prev,
            brandId: model.brand?.id ?? prev.brandId,
            typeId:  model.type?.id  ?? prev.typeId,
            modelId: model.id,
        }));
        clearErrorsFor(['brandId', 'typeId', 'modelId']);
    };

    const selectCreatedCampus = (campus) => {
        upsertOption('campuses', campus);
        handleCampusChange(campus.id);
    };

    const selectCreatedBuilding = (building) => {
        upsertOption('campuses', building.campus);
        upsertOption('buildings', building);
        setFormValues(prev => ({
            ...prev,
            campusId: building.campus?.id ?? prev.campusId,
            buildingId: building.id,
            floorNumber: '',
            locationId: '',
        }));
        clearErrorsFor(['campusId', 'buildingId']);
    };

    const selectCreatedLocation = (location) => {
        const building = location.floor?.building;
        upsertOption('campuses', building?.campus);
        upsertOption('buildings', building);
        upsertOption('locations', location);
        setFormValues(prev => ({
            ...prev,
            campusId: building?.campus?.id ?? prev.campusId,
            buildingId: building?.id ?? prev.buildingId,
            floorNumber: location.floor?.name ? parseInt(location.floor.name) : prev.floorNumber,
            locationId: location.id,
        }));
        clearErrorsFor(['campusId', 'buildingId', 'locationId']);
    };

    const handleCatalogSaved = (created) => {
        const fieldKey = catalogModal?.fieldKey;
        setCatalogModal(null);
        if (!fieldKey || !created?.id) return;

        const selectByField = {
            brandId:    selectCreatedBrand,
            typeId:     selectCreatedType,
            modelId:    selectCreatedModel,
            campusId:   selectCreatedCampus,
            buildingId: selectCreatedBuilding,
            locationId: selectCreatedLocation,
        };
        selectByField[fieldKey]?.(created);
    };

    const resolveLocationId = async () => {
        const { campusId, buildingId, floorNumber, locationId } = formValues;
        if (locationId) return locationId;

        let resolvedBuildingId = buildingId;
        if (!resolvedBuildingId) {
            const created = await createCatalogItem(INVENTORY_ENDPOINTS.buildings, { name: '-', campusId });
            resolvedBuildingId = created.id;
        }

        const created = await createCatalogItem(INVENTORY_ENDPOINTS.locations, {
            description: '-',
            campusId,
            buildingId: resolvedBuildingId,
            floorNumber: floorNumber || 1,
        });
        return created.id;
    };

    const doSave = async () => {
        setSaving(true);
        try {
            const finalLocationId = await resolveLocationId();

            const payload = {
                executingUnit:           formValues.executingUnit?.trim()         || null,
                responsibleEmployee:     formValues.responsibleEmployee?.trim()   || null,
                responsibleEmployeeId:   formValues.responsibleEmployeeId?.trim() || null,
                modelId:                 formValues.modelId,
                locationId:              finalLocationId,
                status:                  formValues.status,
                acquisitionDate:         formValues.acquisitionDate               || null,
                warrantyEndDate:         formValues.warrantyEndDate               || null,
                firmwareSupportEndDate:  formValues.firmwareSupportEndDate        || null,
                decommissionDate:        formValues.decommissionDate             || null,
                assetNumber:             formValues.assetNumber?.trim()           || null,
                serialNumber:            formValues.serialNumber?.trim()          || null,
                latitude:                formValues.latitude !== '' ? parseFloat(formValues.latitude) : null,
                longitude:               formValues.longitude !== '' ? parseFloat(formValues.longitude) : null,
                ...(requiresNetworkInterface && {
                    networkInterface: {
                        ipAddress:  formValues.ipAddress?.trim()  || null,
                        macAddress: formValues.macAddress?.trim() || null,
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

            if (componentsToDelete.length > 0) {
                await Promise.all(componentsToDelete.map(componentId => deleteAssetComponent(componentId)));
            }

            if (components.length > 0) {
                await Promise.all(components.map(component => {
                    const componentPayload = {
                        name: component.name.trim(),
                        quantity: Number(component.quantity),
                        location: component.location?.trim() || null,
                        observations: component.observations?.trim() || null,
                    };

                    if (component.id) {
                        return updateAssetComponent(component.id, componentPayload);
                    }

                    return createAssetComponent(savedAssetId, componentPayload);
                }));
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
        const requiredFields = ['brandId', 'typeId', 'modelId', 'campusId', 'status', 'assetNumber'];

        const newTouched = {};
        const newErrors  = {};

        requiredFields.forEach(key => {
            newTouched[key] = true;
            const val = formValues[key];
            if (!val || (typeof val === 'string' && !val.trim())) {
                newErrors[key] = 'Este campo es requerido';
            }
        });
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
        if (!formValues.locationId && formValues.floorNumber !== '') {
            const floor = Number(formValues.floorNumber);
            if (!Number.isInteger(floor) || floor < MIN_FLOOR_NUMBER) {
                newErrors.floorNumber  = FLOOR_NUMBER_ERROR;
                newTouched.floorNumber = true;
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

        if (!validateComponents()) {
            setAlert({ type: 'warning', message: 'Revisa los componentes asociados antes de continuar' });
            return;
        }

        const duplicatedAssetNumber = await refreshAssetNumberExists();
        if (duplicatedAssetNumber) {
            setShowAssetNumberConfirm(true);
            return;
        }

        await doSave();
    };

    const handleFileSelect = async (files) => {
        const all     = Array.from(files);
        const valid   = all.filter(f => f.type.startsWith('image/'));
        const invalid = all.filter(f => !f.type.startsWith('image/'));

        if (invalid.length > 0) {
            setAlert({
                type: 'warning',
                message: `Formato no permitido: ${invalid.map(f => f.name).join(', ')}. Solo se aceptan JPG, PNG y WEBP.`,
            });
        }

        if (valid.length === 0) return;
        const next = await Promise.all(
            valid.map(async f => ({ file: f, preview: await readAsDataUrl(f), name: f.name }))
        );
        setPhotos(prev => [...prev, ...next]);
    };

    const handleRemovePhoto = (index) => {
        setPhotos(prev => prev.filter((_, i) => i !== index));
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
                                label="Tipo de activo" value={formValues.typeId} required
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
                                createLabel="Crear nuevo Tipo de Activo"
                            />

                            <SearchableSelect
                                label="Modelo" value={formValues.modelId} required
                                onChange={v => handleChange('modelId', v)}
                                onBlur={() => handleBlur('modelId')}
                                fullWidth size="small" disabled={modelDisabled}
                                error={touched.modelId && !!errors.modelId}
                                helperText={
                                    (!formValues.brandId || !formValues.typeId)
                                        ? 'Selecciona marca y tipo de activo primero'
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
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                            <SearchableSelect
                                label="Campus" value={formValues.campusId} required
                                onChange={handleCampusChange}
                                onBlur={() => handleBlur('campusId')}
                                fullWidth size="small" disabled={saving || loadingOptions}
                                error={touched.campusId && !!errors.campusId}
                                helperText={touched.campusId ? (errors.campusId || ' ') : ' '}
                                sx={fieldSx}
                                items={campuses}
                                getItemLabel={s => s.name}
                                getItemValue={s => s.id}
                                onCreate={() => openCatalogModal('campusId')}
                                createLabel="Crear nuevo Campus"
                            />

                            <SearchableSelect
                                label="Edificio" value={formValues.buildingId}
                                onChange={handleBuildingChange}
                                onBlur={() => handleBlur('buildingId')}
                                fullWidth size="small"
                                disabled={saving || loadingOptions || !formValues.campusId}
                                error={touched.buildingId && !!errors.buildingId}
                                helperText={
                                    !formValues.campusId
                                        ? 'Selecciona un campus primero'
                                        : (touched.buildingId ? (errors.buildingId || ' ') : ' ')
                                }
                                sx={fieldSx}
                                items={filteredBuildings}
                                getItemLabel={b => b.name}
                                getItemValue={b => b.id}
                                onCreate={() => openCatalogModal('buildingId')}
                                createLabel="Crear nuevo Edificio"
                            />

                            <TextField
                                label="Número de piso"
                                value={formValues.floorNumber}
                                onChange={e => handleFloorNumberChange(e.target.value)}
                                onBlur={() => handleBlur('floorNumber')}
                                fullWidth size="small"
                                type="number"
                                inputProps={{ min: MIN_FLOOR_NUMBER }}
                                disabled={saving || !formValues.campusId || !formValues.buildingId}
                                error={touched.floorNumber && !!errors.floorNumber}
                                helperText={touched.floorNumber ? (errors.floorNumber || ' ') : ' '}
                                sx={fieldSx}
                            />

                            <SearchableSelect
                                label="Locación" value={formValues.locationId}
                                onChange={v => handleChange('locationId', v)}
                                onBlur={() => handleBlur('locationId')}
                                fullWidth size="small"
                                disabled={saving || loadingOptions || !formValues.campusId || !formValues.buildingId}
                                error={touched.locationId && !!errors.locationId}
                                helperText={
                                    (!formValues.campusId || !formValues.buildingId)
                                        ? 'Selecciona campus y edificio primero'
                                        : (touched.locationId ? (errors.locationId || ' ') : ' ')
                                }
                                sx={fieldSx}
                                items={filteredLocations}
                                getItemLabel={l => l.description + (l.floor?.name ? ` (Piso ${l.floor.name})` : '')}
                                getItemValue={l => l.id}
                                onCreate={() => openCatalogModal('locationId')}
                                createLabel="Crear nueva Locación"
                            />
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Estado')}
                        <Box sx={{ display: 'grid', gridTemplateColumns: formValues.status === 'DE_BAJA' ? { xs: '1fr', sm: '1fr 1fr' } : '1fr', gap: 2 }}>
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
                            {formValues.status === 'DE_BAJA' && (
                                <DatePicker
                                    label="Fecha de baja"
                                    value={formValues.decommissionDate ? dayjs(formValues.decommissionDate) : null}
                                    onChange={v => handleChange('decommissionDate', v ? v.format('YYYY-MM-DD') : '')}
                                    disabled={saving}
                                    slotProps={{
                                        textField: {
                                            size: 'small',
                                            fullWidth: true,
                                            error: touched.decommissionDate && !!errors.decommissionDate,
                                            helperText: touched.decommissionDate ? (errors.decommissionDate || ' ') : ' ',
                                            sx: fieldSx,
                                            onBlur: () => handleBlur('decommissionDate'),
                                        },
                                    }}
                                />
                            )}
                        </Box>
                    </Box>

                    <Divider />

                    {requiresNetworkInterface && (
                        <>
                            <Box>
                                {sectionLabel('IP y MAC')}
                                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
                                    <TextField
                                        label="Dirección IP" value={formValues.ipAddress}
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
                                        label="Dirección MAC" value={formValues.macAddress}
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
                            <Divider />
                        </>
                    )}

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

                    <Divider />

                    <Box>
                        {sectionLabel('Coordenadas')}
                        <CoordinateMapPicker
                            latitude={formValues.latitude}
                            longitude={formValues.longitude}
                            onCoordinatesChange={handleCoordinatesChange}
                            onLatitudeChange={v => handleChange('latitude', v)}
                            onLongitudeChange={v => handleChange('longitude', v)}
                            onBlur={key => handleBlur(key)}
                            disabled={saving}
                            errors={errors}
                            touched={touched}
                        />
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Responsable')}
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr 1fr' }, gap: 2 }}>
                            <TextField
                                label="Unidad Ejecutora" value={formValues.executingUnit}
                                onChange={e => handleChange('executingUnit', e.target.value)}
                                fullWidth size="small" disabled={saving}
                                sx={fieldSx}
                            />
                            <TextField
                                label="Identificación Funcionario" value={formValues.responsibleEmployeeId}
                                onChange={e => handleChange('responsibleEmployeeId', e.target.value)}
                                fullWidth size="small" disabled={saving}
                                sx={fieldSx}
                            />
                            <TextField
                                label="Nombre Funcionario" value={formValues.responsibleEmployee}
                                onChange={e => handleChange('responsibleEmployee', e.target.value)}
                                fullWidth size="small" disabled={saving}
                                sx={fieldSx}
                            />
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        {sectionLabel('Componentes asociados')}
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                            {components.length === 0 ? (
                                <Typography sx={{ color: 'text.secondary', fontSize: 13.5 }}>
                                    No hay componentes asociados.
                                </Typography>
                            ) : (
                                components.map((component, index) => {
                                    const componentFieldErrors = componentErrors[component.localId] ?? {};

                                    return (
                                        <Box
                                            key={component.localId}
                                            sx={{
                                                p: 1.5,
                                                border: '1px solid',
                                                borderColor: 'divider',
                                                borderRadius: '12px',
                                                display: 'flex',
                                                flexDirection: 'column',
                                                gap: 1.5,
                                            }}
                                        >
                                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1 }}>
                                                <Typography sx={{ fontSize: 12.5, fontWeight: 700, color: 'text.secondary' }}>
                                                    Componente #{index + 1}
                                                </Typography>
                                                <IconButton
                                                    size="small"
                                                    onClick={() => removeComponent(component.localId)}
                                                    disabled={saving}
                                                    sx={{ color: 'error.main' }}
                                                >
                                                    <DeleteIcon sx={{ fontSize: 18 }} />
                                                </IconButton>
                                            </Box>

                                            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1.8fr 0.8fr' }, gap: 2 }}>
                                                <TextField
                                                    label="Nombre"
                                                    value={component.name}
                                                    onChange={e => updateComponentField(component.localId, 'name', e.target.value)}
                                                    fullWidth
                                                    size="small"
                                                    disabled={saving}
                                                    error={!!componentFieldErrors.name}
                                                    helperText={componentFieldErrors.name || ' '}
                                                    sx={fieldSx}
                                                />
                                                <TextField
                                                    label="Cantidad"
                                                    value={component.quantity}
                                                    onChange={e => updateComponentField(component.localId, 'quantity', e.target.value.replace(/[^0-9]/g, ''))}
                                                    fullWidth
                                                    size="small"
                                                    disabled={saving}
                                                    error={!!componentFieldErrors.quantity}
                                                    helperText={componentFieldErrors.quantity || ' '}
                                                    sx={fieldSx}
                                                />
                                            </Box>

                                            <TextField
                                                label="Ubicación"
                                                value={component.location}
                                                onChange={e => updateComponentField(component.localId, 'location', e.target.value)}
                                                fullWidth
                                                size="small"
                                                disabled={saving}
                                                error={!!componentFieldErrors.location}
                                                helperText={componentFieldErrors.location || ' '}
                                                sx={fieldSx}
                                            />

                                            <TextField
                                                label="Observaciones"
                                                value={component.observations}
                                                onChange={e => updateComponentField(component.localId, 'observations', e.target.value)}
                                                fullWidth
                                                size="small"
                                                multiline
                                                minRows={2}
                                                disabled={saving}
                                                error={!!componentFieldErrors.observations}
                                                helperText={componentFieldErrors.observations || ' '}
                                                sx={fieldSx}
                                            />
                                        </Box>
                                    );
                                })
                            )}

                            <Box>
                                <IconButton
                                    onClick={addComponent}
                                    disabled={saving}
                                    sx={{
                                        border: '1px dashed',
                                        borderColor: 'divider',
                                        borderRadius: '10px',
                                        px: 1.25,
                                        py: 0.75,
                                        gap: 0.5,
                                    }}
                                >
                                    <AddCircleOutlinedIcon sx={{ fontSize: 18 }} />
                                    <Typography sx={{ fontSize: 12.5, fontWeight: 600, color: 'text.secondary' }}>
                                        Agregar componente
                                    </Typography>
                                </IconButton>
                            </Box>
                        </Box>
                    </Box>

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
                    initialValues={catalogModal.initialValues}
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
                    'El nuevo tipo de activo seleccionado no requiere IP y MAC. La IP y MAC actualmente asociada a este activo será eliminada al guardar.',
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
