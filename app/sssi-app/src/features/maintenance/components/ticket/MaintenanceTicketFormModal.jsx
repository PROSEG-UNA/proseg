import { useEffect, useMemo, useState } from 'react';
import {
    Autocomplete,
    Box,
    Button,
    Chip,
    Divider,
    FormControl,
    FormControlLabel,
    MenuItem,
    Radio,
    RadioGroup,
    Stack,
    Tab,
    Tabs,
    TextField,
    Typography,
    useTheme,
} from '@mui/material';
import ConstructionIcon from '@mui/icons-material/Construction';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import SearchableSelect from '../../../../common/components/SearchableSelect.jsx';
import {
    addMaintenanceTicketComment,
    createMaintenanceTicket,
    updateMaintenanceTicket,
} from '../../services/ticketsService';
import {
    fetchAssetsByLocation,
    fetchBuildingsByCampus,
    fetchCampuses,
    fetchFloorsByBuilding,
    fetchLocationsByBuilding,
} from '../../services/locationsService';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { MAINTENANCE_PRIORITY_OPTIONS } from '../../maintenanceUtils';

const INITIAL_VALUES = {
    title: '',
    description: '',
    comment: '',
    priority: 'LOW',
    siteId: '',
    buildingId: '',
    floorId: '',
    locationId: '',
    requiresAsset: 'false',
};

const LOCATION_PAGE_OPTIONS = { page: 0, size: 100 };
const ASSET_PAGE_OPTIONS = { page: 0, size: 100 };

const buildAssetLabel = (asset) => {
    const assetNumber = asset.assetNumber ?? asset.id;
    const serialNumber = asset.serialNumber ? ` - ${asset.serialNumber}` : '';
    const model = asset.model?.name ?? asset.assetName ?? 'Sin modelo';
    const location = asset.location?.description ?? asset.locationDescription ?? 'Sin ubicación';

    return `${assetNumber}${serialNumber} - ${model} - ${location}`;
};

const formatCommentDate = (value) => {
    if (!value) return '';

    return new Date(value).toLocaleString('es-CR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    });
};

const getCommentDateValue = (comment) => {
    const value = comment?.createdAt ?? comment?.updatedAt;
    return value ? new Date(value).getTime() : 0;
};

const getTicketCampusId = (ticket) => ticket?.siteId ?? ticket?.campusId ?? ticket?.site?.id ?? ticket?.campus?.id ?? '';

const getTicketBuildingId = (ticket) => ticket?.buildingId ?? ticket?.building?.id ?? '';

const getTicketFloorId = (ticket) => ticket?.floorId ?? ticket?.floor?.id ?? '';

const getTicketLocationId = (ticket) => ticket?.locationId ?? ticket?.location?.id ?? '';

export default function MaintenanceTicketFormModal({ open, onClose, onCreated, ticket = null, readOnly = false, loadingDetail = false }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const { hasPermission } = usePermissions();
    const canSetPriority = hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.SET_PRIORITY);
    const isEditing = !!ticket?.id;
    const isReadOnly = !!readOnly;

    const [activeTab, setActiveTab] = useState('details');
    const [formValues, setFormValues] = useState(INITIAL_VALUES);
    const [errors, setErrors] = useState({});
    const [touched, setTouched] = useState({});
    const [saving, setSaving] = useState(false);
    const [savingComment, setSavingComment] = useState(false);
    const [loadingOptions, setLoadingOptions] = useState(false);
    const [loadingBuildings, setLoadingBuildings] = useState(false);
    const [loadingFloors, setLoadingFloors] = useState(false);
    const [loadingLocations, setLoadingLocations] = useState(false);
    const [loadingAssets, setLoadingAssets] = useState(false);
    const [campuses, setCampuses] = useState([]);
    const [buildings, setBuildings] = useState([]);
    const [floors, setFloors] = useState([]);
    const [locations, setLocations] = useState([]);
    const [assets, setAssets] = useState([]);
    const [selectedAssets, setSelectedAssets] = useState([]);
    const [photos, setPhotos] = useState([]);
    const [ticketComments, setTicketComments] = useState([]);
    const [pendingComments, setPendingComments] = useState([]);
    const [alert, setAlert] = useState(null);

    const loadingCatalogs = loadingOptions || loadingBuildings || loadingFloors || loadingLocations || loadingAssets;

    useEffect(() => {
        if (!open) {
            setActiveTab('details');
            setFormValues(INITIAL_VALUES);
            setErrors({});
            setTouched({});
            setSaving(false);
            setSavingComment(false);
            setLoadingOptions(false);
            setLoadingBuildings(false);
            setLoadingFloors(false);
            setLoadingLocations(false);
            setLoadingAssets(false);
            setCampuses([]);
            setBuildings([]);
            setFloors([]);
            setLocations([]);
            setAssets([]);
            setSelectedAssets([]);
            setPhotos([]);
            setTicketComments([]);
            setPendingComments([]);
            setAlert(null);
            return;
        }

        setActiveTab('details');
        setTicketComments(ticket?.comments ?? []);
        setPendingComments([]);
        setPhotos([]);
        setErrors({});
        setTouched({});
        setBuildings([]);
        setFloors([]);
        setLocations([]);
        setAssets([]);

        if (ticket) {
            const ticketAssets = ticket.assets ?? [];

            setFormValues({
                title: ticket.title ?? '',
                description: ticket.description ?? '',
                comment: '',
                priority: ticket.priority ?? 'LOW',
                siteId: getTicketCampusId(ticket),
                buildingId: getTicketBuildingId(ticket),
                floorId: getTicketFloorId(ticket),
                locationId: getTicketLocationId(ticket),
                requiresAsset: ticketAssets.length > 0 ? 'true' : 'false',
            });

            setSelectedAssets(ticketAssets.map((ticketAsset) => ({
                id: ticketAsset.assetId ?? ticketAsset.id,
                assetNumber: ticketAsset.assetNumber,
                serialNumber: ticketAsset.serialNumber,
                assetName: ticketAsset.assetName,
                locationDescription: ticketAsset.locationDescription,
            })));
        } else {
            setFormValues(INITIAL_VALUES);
            setSelectedAssets([]);
        }

        let cancelled = false;

        async function loadCampuses() {
            setLoadingOptions(true);

            try {
                const campusPage = await fetchCampuses(LOCATION_PAGE_OPTIONS);
                if (cancelled) return;

                setCampuses(campusPage?.content ?? []);
            } catch (error) {
                if (!cancelled) {
                    setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudieron cargar las sedes' });
                }
            } finally {
                if (!cancelled) setLoadingOptions(false);
            }
        }

        loadCampuses();

        return () => {
            cancelled = true;
        };
    }, [open, ticket]);

    useEffect(() => {
        if (!open || !formValues.siteId) {
            setBuildings([]);
            return;
        }

        let cancelled = false;

        async function loadBuildings() {
            setLoadingBuildings(true);

            try {
                const buildingPage = await fetchBuildingsByCampus(formValues.siteId, LOCATION_PAGE_OPTIONS);
                if (cancelled) return;

                setBuildings(buildingPage?.content ?? []);
            } catch (error) {
                if (!cancelled) {
                    setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudieron cargar los edificios' });
                }
            } finally {
                if (!cancelled) setLoadingBuildings(false);
            }
        }

        loadBuildings();

        return () => {
            cancelled = true;
        };
    }, [open, formValues.siteId]);

    useEffect(() => {
        if (!open || !formValues.buildingId) {
            setFloors([]);
            setLocations([]);
            return;
        }

        let cancelled = false;

        async function loadFloorsAndLocations() {
            setLoadingFloors(true);
            setLoadingLocations(true);

            try {
                const [floorPage, locationPage] = await Promise.all([
                    fetchFloorsByBuilding(formValues.buildingId, LOCATION_PAGE_OPTIONS),
                    fetchLocationsByBuilding(formValues.buildingId, LOCATION_PAGE_OPTIONS),
                ]);

                if (cancelled) return;

                setFloors(floorPage?.content ?? []);
                setLocations(locationPage?.content ?? []);
            } catch (error) {
                if (!cancelled) {
                    setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudieron cargar pisos y ubicaciones' });
                }
            } finally {
                if (!cancelled) {
                    setLoadingFloors(false);
                    setLoadingLocations(false);
                }
            }
        }

        loadFloorsAndLocations();

        return () => {
            cancelled = true;
        };
    }, [open, formValues.buildingId]);

    useEffect(() => {
        if (!open || formValues.requiresAsset !== 'true' || !formValues.locationId) {
            setAssets([]);
            return;
        }

        let cancelled = false;

        async function loadAssets() {
            setLoadingAssets(true);

            try {
                const assetPage = await fetchAssetsByLocation(formValues.locationId, ASSET_PAGE_OPTIONS);
                if (cancelled) return;

                const assetItems = assetPage?.content ?? [];
                setAssets(assetItems);
                setSelectedAssets((prev) => prev.map((selected) => assetItems.find((asset) => asset.id === selected.id) ?? selected));
            } catch (error) {
                if (!cancelled) {
                    setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudieron cargar los activos' });
                }
            } finally {
                if (!cancelled) setLoadingAssets(false);
            }
        }

        loadAssets();

        return () => {
            cancelled = true;
        };
    }, [open, formValues.requiresAsset, formValues.locationId]);

    const filteredLocations = useMemo(() => {
        if (!formValues.floorId) return locations;

        return locations.filter((location) => {
            const floorId = location.floor?.id ?? location.floorId;
            return floorId === formValues.floorId;
        });
    }, [locations, formValues.floorId]);

    const commentsToShow = useMemo(
        () => [...ticketComments, ...pendingComments].sort((a, b) => getCommentDateValue(b) - getCommentDateValue(a)),
        [ticketComments, pendingComments]
    );

    const validateField = (key, value, values = formValues) => {
        let error = '';
        const trimmed = typeof value === 'string' ? value.trim() : value;
        const requiredKeys = ['title', 'description', 'siteId', 'buildingId', ...(canSetPriority ? ['priority'] : [])];

        if (requiredKeys.includes(key) && !trimmed) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'locationId' && values.requiresAsset === 'true' && !trimmed) {
            error = 'Debes seleccionar una ubicación para vincular activos';
        }

        if (!error && key === 'title' && trimmed && trimmed.length > 120) {
            error = 'El título no puede superar los 120 caracteres';
        }

        if (!error && key === 'description' && trimmed && trimmed.length > 500) {
            error = 'La descripción no puede superar los 500 caracteres';
        }

        if (!error && key === 'comment' && trimmed && trimmed.length > 1000) {
            error = 'El comentario no puede superar los 1000 caracteres';
        }

        setErrors((prev) => ({ ...prev, [key]: error }));
        return !error;
    };

    const handleChange = (key, value) => {
        if (key === 'siteId') {
            const nextValues = { ...formValues, siteId: value, buildingId: '', floorId: '', locationId: '' };

            setFormValues(nextValues);
            setBuildings([]);
            setFloors([]);
            setLocations([]);
            setAssets([]);
            setSelectedAssets([]);

            if (touched.siteId) validateField('siteId', value, nextValues);
            if (touched.buildingId) validateField('buildingId', '', nextValues);
            if (touched.locationId) validateField('locationId', '', nextValues);
            return;
        }

        if (key === 'buildingId') {
            const nextValues = { ...formValues, buildingId: value, floorId: '', locationId: '' };

            setFormValues(nextValues);
            setFloors([]);
            setLocations([]);
            setAssets([]);
            setSelectedAssets([]);

            if (touched.buildingId) validateField('buildingId', value, nextValues);
            if (touched.locationId) validateField('locationId', '', nextValues);
            return;
        }

        if (key === 'floorId') {
            const nextValues = { ...formValues, floorId: value, locationId: '' };

            setFormValues(nextValues);
            setAssets([]);
            setSelectedAssets([]);

            if (touched.floorId) validateField('floorId', value, nextValues);
            if (touched.locationId) validateField('locationId', '', nextValues);
            return;
        }

        if (key === 'locationId') {
            const nextValues = { ...formValues, locationId: value };

            setFormValues(nextValues);
            setAssets([]);
            setSelectedAssets([]);

            if (touched.locationId) validateField('locationId', value, nextValues);
            return;
        }

        if (key === 'requiresAsset') {
            const nextValues = { ...formValues, requiresAsset: value };

            setFormValues(nextValues);

            if (value !== 'true') {
                setAssets([]);
                setSelectedAssets([]);
                setErrors((prev) => ({ ...prev, locationId: '' }));
            } else if (touched.locationId) {
                validateField('locationId', nextValues.locationId, nextValues);
            }

            return;
        }

        const nextValues = { ...formValues, [key]: value };

        setFormValues(nextValues);
        if (touched[key]) validateField(key, value, nextValues);
    };

    const handleBlur = (key) => {
        setTouched((prev) => ({ ...prev, [key]: true }));
        validateField(key, formValues[key]);
    };

    const handleAddComment = async () => {
        if (isReadOnly) return;

        const comment = formValues.comment.trim();

        setTouched((prev) => ({ ...prev, comment: true }));

        if (!comment) {
            setErrors((prev) => ({ ...prev, comment: 'Debes escribir un comentario' }));
            return;
        }

        if (!validateField('comment', comment)) return;

        if (!isEditing) {
            setPendingComments((prev) => [
                {
                    id: `pending-${Date.now()}`,
                    content: comment,
                    authorName: 'Tú',
                    createdAt: new Date().toISOString(),
                    pending: true,
                },
                ...prev,
            ]);
            setFormValues((prev) => ({ ...prev, comment: '' }));
            setErrors((prev) => ({ ...prev, comment: '' }));
            setTouched((prev) => ({ ...prev, comment: false }));
            return;
        }

        setSavingComment(true);

        try {
            const savedComment = await addMaintenanceTicketComment(ticket.id, comment);

            setTicketComments((prev) => [savedComment, ...prev]);
            setFormValues((prev) => ({ ...prev, comment: '' }));
            setErrors((prev) => ({ ...prev, comment: '' }));
            setTouched((prev) => ({ ...prev, comment: false }));
            onCreated?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo guardar el comentario' });
        } finally {
            setSavingComment(false);
        }
    };

    const handleSave = async () => {
        if (isReadOnly) {
            onClose?.();
            return;
        }

        const requiredFields = ['title', 'description', 'siteId', 'buildingId', ...(canSetPriority ? ['priority'] : [])];
        const nextTouched = {};
        const nextErrors = {};

        requiredFields.forEach((key) => {
            nextTouched[key] = true;
            if (!formValues[key]?.trim()) {
                nextErrors[key] = 'Este campo es requerido';
            }
        });

        if (formValues.requiresAsset === 'true' && !formValues.locationId?.trim()) {
            nextTouched.locationId = true;
            nextErrors.locationId = 'Debes seleccionar una ubicación para vincular activos';
        }

        if (formValues.title?.trim()?.length > 120) {
            nextErrors.title = 'El título no puede superar los 120 caracteres';
            nextTouched.title = true;
        }

        if (formValues.description?.trim()?.length > 500) {
            nextErrors.description = 'La descripción no puede superar los 500 caracteres';
            nextTouched.description = true;
        }

        if (formValues.comment?.trim()?.length > 1000) {
            nextErrors.comment = 'El comentario no puede superar los 1000 caracteres';
            nextTouched.comment = true;
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
                title: formValues.title.trim(),
                description: formValues.description.trim(),
                ...(canSetPriority ? { priority: formValues.priority } : {}),
                siteId: formValues.siteId,
                buildingId: formValues.buildingId,
                floorId: formValues.floorId || null,
                locationId: formValues.locationId || null,
                assetIds: formValues.requiresAsset === 'true'
                    ? selectedAssets.map((asset) => asset.id)
                    : [],
            };

            const savedTicket = isEditing
                ? await updateMaintenanceTicket(ticket.id, payload, photos)
                : await createMaintenanceTicket(payload, photos);

            const typedComment = formValues.comment.trim();
            const comments = [
                ...pendingComments.map((comment) => comment.content),
                ...(typedComment ? [typedComment] : []),
            ];

            if (savedTicket?.id && comments.length > 0) {
                for (const comment of comments) {
                    await addMaintenanceTicketComment(savedTicket.id, comment);
                }
            }

            onCreated?.();
            onClose?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo guardar el ticket' });
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
    };

    const sectionTitleSx = {
        fontWeight: 700,
        fontSize: 15,
        mb: 1.5,
    };

    const chipSx = {
        height: 'auto',
        maxWidth: '100%',
        alignItems: 'flex-start',
        borderRadius: '10px',
        '& .MuiChip-label': {
            display: 'block',
            whiteSpace: 'normal',
            overflow: 'visible',
            textOverflow: 'clip',
            py: 0.5,
        },
    };

    const buildTabLabel = (Icon, label) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
            <Icon sx={{ fontSize: 16 }} />
            <span>{label}</span>
        </Box>
    );

    const cardSx = {
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: '8px',
        backgroundColor: 'background.paper',
        p: 1.5,
    };

    const commentComposerSx = {
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: '8px',
        backgroundColor: 'background.paper',
        p: 1.5,
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="lg"
                icon={ConstructionIcon}
                title={isReadOnly ? 'Detalle de ticket de mantenimiento' : isEditing ? 'Editar ticket de mantenimiento' : 'Nuevo ticket de mantenimiento'}
                subtitle={isReadOnly ? 'Visualiza los datos del ticket sin posibilidad de modificar' : isEditing ? 'Actualiza la información del ticket seleccionado' : 'Registra una incidencia con ubicación, activos, fotos y comentario inicial'}
                loading={saving || loadingCatalogs || loadingDetail}
                secondaryButton={isReadOnly ? undefined : { label: 'Cancelar', onClick: onClose, disabled: saving }}
                primaryButton={isReadOnly
                    ? { label: 'Cerrar', onClick: onClose, disabled: false }
                    : { label: saving ? 'Guardando...' : isEditing ? 'Guardar cambios' : 'Crear ticket', onClick: handleSave, disabled: saving || loadingCatalogs || loadingDetail || savingComment }
                }
                contentSx={contentSx}
            >
                <Box sx={{ borderTop: '1px solid', borderColor: 'divider' }}>
                    <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 2 }}>
                        <TextField
                            label="Título"
                            value={formValues.title}
                            onChange={(event) => handleChange('title', event.target.value)}
                            onBlur={() => handleBlur('title')}
                            required
                            fullWidth
                            size="small"
                            disabled={saving || loadingCatalogs || isReadOnly}
                            error={touched.title && !!errors.title}
                            helperText={touched.title ? (errors.title || ' ') : ' '}
                            sx={fieldSx}
                        />
                    </Box>

                    <Box sx={{ px: { xs: 2.5, sm: 3 }, borderTop: '1px solid', borderBottom: '1px solid', borderColor: 'divider' }}>
                        <Tabs
                            value={activeTab}
                            onChange={(_, value) => setActiveTab(value)}
                            TabIndicatorProps={{ sx: { backgroundColor: accentColor } }}
                            sx={{
                                minHeight: 44,
                                '& .MuiTab-root': {
                                    minHeight: 44,
                                    textTransform: 'none',
                                    fontWeight: 700,
                                    fontSize: 13.5,
                                    color: 'text.secondary',
                                },
                                '& .MuiTab-root.Mui-selected': {
                                    color: accentColor,
                                },
                            }}
                        >
                            <Tab value="details" label={buildTabLabel(DescriptionOutlinedIcon, 'Detalles')} />
                            <Tab value="attachments" label={buildTabLabel(ImageOutlinedIcon, `Adjuntos (${(ticket?.photos?.length ?? 0) + photos.length})`)} />
                        </Tabs>
                    </Box>

                    {activeTab === 'details' ? (
                        <Box
                            sx={{
                                px: { xs: 2.5, sm: 3 },
                                pt: 2.5,
                                pb: 3,
                                display: 'grid',
                                gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1fr) 340px' },
                                gap: 2.5,
                            }}
                        >
                            <Box>
                                <Typography sx={sectionTitleSx}>Descripción</Typography>

                                <TextField
                                    value={formValues.description}
                                    onChange={(event) => handleChange('description', event.target.value)}
                                    onBlur={() => handleBlur('description')}
                                    required
                                    fullWidth
                                    multiline
                                    minRows={6}
                                    disabled={saving || loadingCatalogs || isReadOnly}
                                    error={touched.description && !!errors.description}
                                    helperText={touched.description ? (errors.description || ' ') : 'Máximo 500 caracteres'}
                                    sx={fieldSx}
                                />

                                <Box sx={{ mt: 3 }}>
                                    <Typography sx={sectionTitleSx}>Comentarios</Typography>

                                    {!isReadOnly ? (
                                        <Box sx={commentComposerSx}>
                                            <TextField
                                                value={formValues.comment}
                                                onChange={(event) => handleChange('comment', event.target.value)}
                                                onBlur={() => handleBlur('comment')}
                                                placeholder={isEditing ? 'Escribe un comentario' : 'Agrega comentarios antes de crear el ticket'}
                                                fullWidth
                                                multiline
                                                minRows={4}
                                                disabled={saving || loadingCatalogs || savingComment}
                                                error={touched.comment && !!errors.comment}
                                                helperText={touched.comment ? (errors.comment || ' ') : 'Máximo 1000 caracteres'}
                                                sx={fieldSx}
                                            />

                                            <Box sx={{ mt: 1.5, display: 'flex', justifyContent: 'flex-end' }}>
                                                <Button
                                                    variant="contained"
                                                    onClick={handleAddComment}
                                                    disabled={saving || loadingCatalogs || savingComment}
                                                    sx={{ textTransform: 'none', borderRadius: '8px' }}
                                                >
                                                    {savingComment ? 'Guardando...' : 'Comentar'}
                                                </Button>
                                            </Box>
                                        </Box>
                                    ) : null}

                                    <Box sx={{ mt: isReadOnly ? 0 : 2 }}>
                                        {commentsToShow.length > 0 ? (
                                            <Stack spacing={1.25}>
                                                {commentsToShow.map((comment) => (
                                                    <Box key={comment.id} sx={cardSx}>
                                                        <Stack
                                                            direction="row"
                                                            spacing={1}
                                                            sx={{
                                                                mb: 0.75,
                                                                flexWrap: 'wrap',
                                                                alignItems: 'center',
                                                            }}
                                                        >
                                                            <Typography sx={{ fontWeight: 700, fontSize: 13 }}>
                                                                {comment.authorName ?? 'Usuario'}
                                                            </Typography>
                                                            <Typography sx={{ color: 'text.secondary', fontSize: 12.5 }}>
                                                                {comment.pending ? 'Pendiente de guardar' : formatCommentDate(comment.createdAt)}
                                                            </Typography>
                                                        </Stack>

                                                        <Typography sx={{ whiteSpace: 'pre-wrap', fontSize: 13.5 }}>
                                                            {comment.content}
                                                        </Typography>
                                                    </Box>
                                                ))}
                                            </Stack>
                                        ) : (
                                            <Typography sx={{ color: 'text.secondary', fontSize: 13.5 }}>
                                                No hay comentarios registrados.
                                            </Typography>
                                        )}
                                    </Box>
                                </Box>
                            </Box>

                            <Box sx={{ borderLeft: { xs: 0, md: '1px solid' }, borderColor: 'divider', pl: { xs: 0, md: 3 } }}>
                                <Typography sx={sectionTitleSx}>Planeación</Typography>

                                <Stack spacing={2}>
                                    {canSetPriority ? (
                                        <TextField
                                            select
                                            label="Prioridad"
                                            value={formValues.priority}
                                            onChange={(event) => handleChange('priority', event.target.value)}
                                            onBlur={() => handleBlur('priority')}
                                            required
                                            fullWidth
                                            size="small"
                                            disabled={saving || loadingCatalogs || isReadOnly}
                                            error={touched.priority && !!errors.priority}
                                            helperText={touched.priority ? (errors.priority || ' ') : ' '}
                                            sx={fieldSx}
                                        >
                                            {MAINTENANCE_PRIORITY_OPTIONS.map((option) => (
                                                <MenuItem key={option.value} value={option.value}>
                                                    {option.label}
                                                </MenuItem>
                                            ))}
                                        </TextField>
                                    ) : null}
                                </Stack>

                                <Divider sx={{ my: 2.5 }} />

                                <Typography sx={sectionTitleSx}>Ubicación</Typography>

                                <Stack spacing={2}>
                                    <SearchableSelect
                                        label="Sede"
                                        value={formValues.siteId}
                                        onChange={(value) => handleChange('siteId', value)}
                                        onBlur={() => handleBlur('siteId')}
                                        items={campuses}
                                        getItemLabel={(campus) => campus.name}
                                        getItemValue={(campus) => campus.id}
                                        required
                                        fullWidth
                                        size="small"
                                        disabled={saving || loadingOptions || isReadOnly}
                                        error={touched.siteId && !!errors.siteId}
                                        helperText={touched.siteId ? (errors.siteId || ' ') : ' '}
                                        sx={fieldSx}
                                    />

                                    <SearchableSelect
                                        label="Edificio"
                                        value={formValues.buildingId}
                                        onChange={(value) => handleChange('buildingId', value)}
                                        onBlur={() => handleBlur('buildingId')}
                                        items={buildings}
                                        getItemLabel={(building) => building.name}
                                        getItemValue={(building) => building.id}
                                        required
                                        fullWidth
                                        size="small"
                                        disabled={saving || loadingBuildings || isReadOnly || !formValues.siteId}
                                        error={touched.buildingId && !!errors.buildingId}
                                        helperText={touched.buildingId ? (errors.buildingId || ' ') : ' '}
                                        sx={fieldSx}
                                    />

                                    <SearchableSelect
                                        label="Piso"
                                        value={formValues.floorId}
                                        onChange={(value) => handleChange('floorId', value)}
                                        onBlur={() => handleBlur('floorId')}
                                        items={floors}
                                        getItemLabel={(floor) => floor.name}
                                        getItemValue={(floor) => floor.id}
                                        clearable
                                        fullWidth
                                        size="small"
                                        disabled={saving || loadingFloors || isReadOnly || !formValues.buildingId}
                                        helperText={touched.floorId ? (errors.floorId || ' ') : ' '}
                                        sx={fieldSx}
                                    />

                                    <SearchableSelect
                                        label="Ubicación"
                                        value={formValues.locationId}
                                        onChange={(value) => handleChange('locationId', value)}
                                        onBlur={() => handleBlur('locationId')}
                                        items={filteredLocations}
                                        getItemLabel={(location) => location.description ?? location.name}
                                        getItemValue={(location) => location.id}
                                        clearable
                                        fullWidth
                                        size="small"
                                        disabled={saving || loadingLocations || isReadOnly || !formValues.buildingId}
                                        error={touched.locationId && !!errors.locationId}
                                        helperText={touched.locationId ? (errors.locationId || ' ') : ' '}
                                        sx={fieldSx}
                                    />
                                </Stack>

                                <Divider sx={{ my: 2.5 }} />

                                <Typography sx={sectionTitleSx}>¿Se requiere activo?</Typography>

                                <FormControl disabled={saving || loadingCatalogs || isReadOnly}>
                                    <RadioGroup
                                        row
                                        value={formValues.requiresAsset}
                                        onChange={(event) => handleChange('requiresAsset', event.target.value)}
                                    >
                                        <FormControlLabel value="true" control={<Radio />} label="Sí" />
                                        <FormControlLabel value="false" control={<Radio />} label="No" />
                                    </RadioGroup>
                                </FormControl>

                                {formValues.requiresAsset === 'true' ? (
                                    <Box sx={{ mt: 1.5 }}>
                                        <Typography sx={{ fontWeight: 700, fontSize: 13.5, mb: 1 }}>
                                            Activos
                                        </Typography>

                                        <Autocomplete
                                            multiple
                                            options={assets}
                                            value={selectedAssets}
                                            onChange={(_, value) => setSelectedAssets(value)}
                                            getOptionLabel={buildAssetLabel}
                                            isOptionEqualToValue={(option, value) => option.id === value.id}
                                            loading={loadingAssets}
                                            disabled={isReadOnly || !formValues.locationId || loadingAssets}
                                            renderTags={(value, getTagProps) => value.map((option, index) => (
                                                <Chip
                                                    {...getTagProps({ index })}
                                                    key={option.id}
                                                    label={buildAssetLabel(option)}
                                                    size="small"
                                                    sx={chipSx}
                                                />
                                            ))}
                                            renderInput={(params) => (
                                                <TextField
                                                    {...params}
                                                    placeholder={formValues.locationId ? 'Busca y selecciona activos' : 'Selecciona una ubicación primero'}
                                                    size="small"
                                                    sx={fieldSx}
                                                    helperText={formValues.locationId ? 'Opcional' : 'Debes seleccionar una ubicación para ver activos'}
                                                />
                                            )}
                                        />

                                        {selectedAssets.length > 0 ? (
                                            <Box sx={{ mt: 1, display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                                                {selectedAssets.map((asset) => (
                                                    <Chip
                                                        key={asset.id}
                                                        label={buildAssetLabel(asset)}
                                                        size="small"
                                                        variant="outlined"
                                                        sx={chipSx}
                                                    />
                                                ))}
                                            </Box>
                                        ) : null}
                                    </Box>
                                ) : null}
                            </Box>
                        </Box>
                    ) : null}

                    {activeTab === 'attachments' ? (
                        <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3 }}>
                            {!isReadOnly ? (
                                <>
                                    <Typography sx={sectionTitleSx}>Agregar fotos</Typography>

                                    <TextField
                                        type="file"
                                        inputProps={{ multiple: true, accept: 'image/*' }}
                                        onChange={(event) => setPhotos(Array.from(event.target.files || []))}
                                        fullWidth
                                        size="small"
                                        disabled={saving || loadingCatalogs}
                                        helperText={photos.length > 0 ? `${photos.length} archivo(s) seleccionado(s)` : 'Opcional'}
                                        sx={fieldSx}
                                    />

                                    <Divider sx={{ my: 2.5 }} />
                                </>
                            ) : null}

                            <Typography sx={sectionTitleSx}>Adjuntos actuales</Typography>

                            {(ticket?.photos?.length ?? 0) > 0 ? (
                                <Stack spacing={1.5}>
                                    {ticket.photos.map((photo) => (
                                        <Box key={photo.id} sx={cardSx}>
                                            <Typography sx={{ fontWeight: 700, fontSize: 13.5 }}>
                                                {photo.fileName ?? photo.objectName}
                                            </Typography>
                                            <Typography sx={{ color: 'text.secondary', fontSize: 13 }}>
                                                {photo.contentType ?? 'Archivo'}
                                            </Typography>
                                            {photo.imageUrl ? (
                                                <Box
                                                    component="img"
                                                    src={photo.imageUrl}
                                                    alt={photo.fileName ?? photo.objectName}
                                                    sx={{
                                                        mt: 1,
                                                        maxWidth: 220,
                                                        maxHeight: 160,
                                                        objectFit: 'cover',
                                                        border: '1px solid',
                                                        borderColor: 'divider',
                                                        borderRadius: '10px',
                                                    }}
                                                />
                                            ) : null}
                                        </Box>
                                    ))}
                                </Stack>
                            ) : (
                                <Typography sx={{ color: 'text.secondary', fontSize: 13.5 }}>
                                    No hay adjuntos registrados.
                                </Typography>
                            )}

                            {!isReadOnly && photos.length > 0 ? (
                                <>
                                    <Divider sx={{ my: 2.5 }} />

                                    <Typography sx={sectionTitleSx}>Adjuntos por guardar</Typography>

                                    <Stack spacing={1}>
                                        {photos.map((photo) => (
                                            <Typography key={`${photo.name}-${photo.size}`} sx={{ color: 'text.secondary', fontSize: 13.5 }}>
                                                {photo.name}
                                            </Typography>
                                        ))}
                                    </Stack>
                                </>
                            ) : null}
                        </Box>
                    ) : null}
                </Box>
            </GeneralModal>

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}