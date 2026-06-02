import { useEffect, useMemo, useState } from 'react';
import {
    Autocomplete,
    Box,
    Button,
    Chip,
    Divider,
    MenuItem,
    Stack,
    Tab,
    Tabs,
    TextField,
    Typography,
} from '@mui/material';
import ConstructionIcon from '@mui/icons-material/Construction';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import SearchableSelect from '../../../../common/components/SearchableSelect.jsx';
import {
    addMaintenanceTicketComment,
    createMaintenanceTicket,
    updateMaintenanceTicket,
    fetchTicketAssets,
    fetchTicketBuildings,
    fetchTicketCampuses,
    fetchTicketFloors,
    fetchTicketLocations,
} from '../../services/ticketsService';
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
};

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

export default function MaintenanceTicketFormModal({ open, onClose, onCreated, ticket = null, readOnly = false }) {
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

    useEffect(() => {
        if (!open) {
            setActiveTab('details');
            setFormValues(INITIAL_VALUES);
            setErrors({});
            setTouched({});
            setSaving(false);
            setSavingComment(false);
            setLoadingOptions(false);
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

        let cancelled = false;
        setLoadingOptions(true);

        Promise.all([
            fetchTicketCampuses(),
            fetchTicketBuildings(),
            fetchTicketFloors(),
            fetchTicketLocations(),
            fetchTicketAssets(),
        ])
            .then(([campusItems, buildingItems, floorItems, locationItems, assetItems]) => {
                if (cancelled) return;

                const assetOptions = assetItems ?? [];

                setCampuses(campusItems ?? []);
                setBuildings(buildingItems ?? []);
                setFloors(floorItems ?? []);
                setLocations(locationItems ?? []);
                setAssets(assetOptions);

                if (ticket) {
                    setFormValues({
                        title: ticket.title ?? '',
                        description: ticket.description ?? '',
                        comment: '',
                        priority: ticket.priority ?? 'LOW',
                        siteId: ticket.siteId ?? '',
                        buildingId: ticket.buildingId ?? '',
                        floorId: ticket.floorId ?? '',
                        locationId: ticket.locationId ?? '',
                    });

                    setSelectedAssets((ticket.assets ?? []).map((ticketAsset) => {
                        const assetId = ticketAsset.assetId ?? ticketAsset.id;
                        const asset = assetOptions.find((item) => item.id === assetId);

                        return asset ?? {
                            id: assetId,
                            assetNumber: ticketAsset.assetNumber,
                            serialNumber: ticketAsset.serialNumber,
                            assetName: ticketAsset.assetName,
                            locationDescription: ticketAsset.locationDescription,
                        };
                    }));
                } else {
                    setFormValues(INITIAL_VALUES);
                    setSelectedAssets([]);
                }
            })
            .catch((error) => {
                if (!cancelled) {
                    setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudieron cargar las opciones' });
                }
            })
            .finally(() => {
                if (!cancelled) setLoadingOptions(false);
            });

        return () => {
            cancelled = true;
        };
    }, [open, ticket]);

    const filteredBuildings = useMemo(
        () => buildings.filter((building) => !formValues.siteId || building.campus?.id === formValues.siteId),
        [buildings, formValues.siteId]
    );

    const filteredFloors = useMemo(
        () => floors.filter((floor) => !formValues.buildingId || floor.building?.id === formValues.buildingId),
        [floors, formValues.buildingId]
    );

    const filteredLocations = useMemo(() => {
        return locations.filter((location) => {
            const floorMatch = !formValues.floorId || location.floor?.id === formValues.floorId;
            const buildingMatch = !formValues.buildingId || location.floor?.building?.id === formValues.buildingId;
            return floorMatch && buildingMatch;
        });
    }, [locations, formValues.buildingId, formValues.floorId]);

    const commentsToShow = useMemo(
        () => [...ticketComments, ...pendingComments],
        [ticketComments, pendingComments]
    );

    const validateField = (key, value) => {
        let error = '';
        const trimmed = typeof value === 'string' ? value.trim() : value;
        const requiredKeys = ['title', 'description', 'siteId', 'buildingId', ...(canSetPriority ? ['priority'] : [])];

        if (requiredKeys.includes(key) && !trimmed) {
            error = 'Este campo es requerido';
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
            setFormValues((prev) => ({ ...prev, siteId: value, buildingId: '', floorId: '', locationId: '' }));
            if (touched.siteId) validateField('siteId', value);
            if (touched.buildingId) validateField('buildingId', '');
            return;
        }

        if (key === 'buildingId') {
            setFormValues((prev) => ({ ...prev, buildingId: value, floorId: '', locationId: '' }));
            if (touched.buildingId) validateField('buildingId', value);
            return;
        }

        if (key === 'floorId') {
            setFormValues((prev) => ({ ...prev, floorId: value, locationId: '' }));
            if (touched.floorId) validateField('floorId', value);
            return;
        }

        setFormValues((prev) => ({ ...prev, [key]: value }));
        if (touched[key]) validateField(key, value);
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
                ...prev,
                {
                    id: `pending-${Date.now()}`,
                    content: comment,
                    authorId: 'Tú',
                    createdAt: new Date().toISOString(),
                    pending: true,
                },
            ]);
            setFormValues((prev) => ({ ...prev, comment: '' }));
            setErrors((prev) => ({ ...prev, comment: '' }));
            setTouched((prev) => ({ ...prev, comment: false }));
            return;
        }

        setSavingComment(true);

        try {
            const response = await addMaintenanceTicketComment(ticket.id, comment);
            const savedComment = response?.data ?? response ?? {
                id: `comment-${Date.now()}`,
                content: comment,
                createdAt: new Date().toISOString(),
            };

            setTicketComments((prev) => [...prev, savedComment]);
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
                assetIds: selectedAssets.map((asset) => asset.id),
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
            borderRadius: 0,
            backgroundColor: 'background.paper',
            '& fieldset': { borderColor: 'divider' },
        },
    };

    const sectionTitleSx = {
        fontWeight: 700,
        fontSize: 16,
        mb: 1.5,
    };

    const chipSx = {
        height: 'auto',
        maxWidth: '100%',
        alignItems: 'flex-start',
        '& .MuiChip-label': {
            display: 'block',
            whiteSpace: 'normal',
            overflow: 'visible',
            textOverflow: 'clip',
            py: 0.5,
        },
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
                loading={saving || loadingOptions}
                secondaryButton={isReadOnly ? undefined : { label: 'Cancelar', onClick: onClose, disabled: saving }}
                primaryButton={isReadOnly
                    ? { label: 'Cerrar', onClick: onClose, disabled: false }
                    : { label: saving ? 'Guardando...' : 'Guardar y cerrar', onClick: handleSave, disabled: saving || loadingOptions || savingComment }
                }
                contentSx={{ overflowY: 'auto', p: 0 }}
            >
                <Box sx={{ borderTop: '1px solid', borderColor: 'divider' }}>
                    <Box sx={{ px: { xs: 2, sm: 3 }, py: 2 }}>
                        <TextField
                            value={formValues.title}
                            onChange={(event) => handleChange('title', event.target.value)}
                            onBlur={() => handleBlur('title')}
                            placeholder="Título"
                            required
                            fullWidth
                            size="small"
                            disabled={saving || loadingOptions || isReadOnly}
                            error={touched.title && !!errors.title}
                            helperText={touched.title ? (errors.title || ' ') : ' '}
                            sx={{
                                ...fieldSx,
                                '& .MuiInputBase-input': {
                                    fontSize: { xs: 18, sm: 22 },
                                    fontWeight: 500,
                                    lineHeight: 1.25,
                                    py: 0.8,
                                },
                            }}
                        />
                    </Box>

                    <Box sx={{ px: { xs: 2, sm: 3 }, borderTop: '1px solid', borderBottom: '1px solid', borderColor: 'divider' }}>
                        <Tabs value={activeTab} onChange={(_, value) => setActiveTab(value)}>
                            <Tab value="details" label="Detalles" />
                            <Tab value="comments" label={`Comentarios (${commentsToShow.length})`} />
                            <Tab value="attachments" label={`Adjuntos (${(ticket?.photos?.length ?? 0) + photos.length})`} />
                        </Tabs>
                    </Box>

                    {activeTab === 'details' ? (
                        <Box
                            sx={{
                                px: { xs: 2, sm: 3 },
                                py: 3,
                                display: 'grid',
                                gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1fr) 320px' },
                                gap: 3,
                            }}
                        >
                            <Box>
                                <Typography sx={sectionTitleSx}>
                                    Descripción
                                </Typography>

                                <TextField
                                    value={formValues.description}
                                    onChange={(event) => handleChange('description', event.target.value)}
                                    onBlur={() => handleBlur('description')}
                                    required
                                    fullWidth
                                    multiline
                                    minRows={6}
                                    disabled={saving || loadingOptions || isReadOnly}
                                    error={touched.description && !!errors.description}
                                    helperText={touched.description ? (errors.description || ' ') : 'Máximo 500 caracteres'}
                                    sx={fieldSx}
                                />

                                <Typography sx={{ ...sectionTitleSx, mt: 3 }}>
                                    Activos vinculados
                                </Typography>

                                <Autocomplete
                                    multiple
                                    options={assets}
                                    value={selectedAssets}
                                    onChange={(_, value) => setSelectedAssets(value)}
                                    getOptionLabel={buildAssetLabel}
                                    isOptionEqualToValue={(option, value) => option.id === value.id}
                                    loading={loadingOptions}
                                    disabled={isReadOnly}
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
                                            placeholder="Busca y selecciona activos"
                                            size="small"
                                            sx={fieldSx}
                                            helperText="Opcional"
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

                            <Box sx={{ borderLeft: { xs: 0, md: '1px solid' }, borderColor: 'divider', pl: { xs: 0, md: 3 } }}>
                                <Typography sx={sectionTitleSx}>
                                    Planeación
                                </Typography>

                                <Stack spacing={2}>
                                    <Box>
                                        <Typography sx={{ color: 'text.secondary', fontSize: 12, mb: 0.5 }}>
                                            Estado
                                        </Typography>
                                        <Typography sx={{ fontWeight: 600 }}>
                                            {ticket?.status ?? 'Nuevo'}
                                        </Typography>
                                    </Box>

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
                                            disabled={saving || loadingOptions || isReadOnly}
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

                                <Divider sx={{ my: 3 }} />

                                <Typography sx={sectionTitleSx}>
                                    Ubicación
                                </Typography>

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
                                        items={filteredBuildings}
                                        getItemLabel={(building) => building.name}
                                        getItemValue={(building) => building.id}
                                        required
                                        fullWidth
                                        size="small"
                                        disabled={saving || loadingOptions || isReadOnly || !formValues.siteId || filteredBuildings.length === 0}
                                        error={touched.buildingId && !!errors.buildingId}
                                        helperText={touched.buildingId ? (errors.buildingId || ' ') : ' '}
                                        sx={fieldSx}
                                    />

                                    <SearchableSelect
                                        label="Piso"
                                        value={formValues.floorId}
                                        onChange={(value) => handleChange('floorId', value)}
                                        onBlur={() => handleBlur('floorId')}
                                        items={filteredFloors}
                                        getItemLabel={(floor) => floor.name}
                                        getItemValue={(floor) => floor.id}
                                        fullWidth
                                        size="small"
                                        disabled={saving || loadingOptions || isReadOnly || !formValues.buildingId || filteredFloors.length === 0}
                                        helperText={touched.floorId ? (errors.floorId || ' ') : ' '}
                                        sx={fieldSx}
                                    />

                                    <SearchableSelect
                                        label="Ubicación"
                                        value={formValues.locationId}
                                        onChange={(value) => handleChange('locationId', value)}
                                        onBlur={() => handleBlur('locationId')}
                                        items={filteredLocations}
                                        getItemLabel={(location) => location.description}
                                        getItemValue={(location) => location.id}
                                        fullWidth
                                        size="small"
                                        disabled={saving || loadingOptions || isReadOnly || !formValues.buildingId || filteredLocations.length === 0}
                                        helperText={touched.locationId ? (errors.locationId || ' ') : ' '}
                                        sx={fieldSx}
                                    />
                                </Stack>
                            </Box>
                        </Box>
                    ) : null}

                    {activeTab === 'comments' ? (
                        <Box sx={{ px: { xs: 2, sm: 3 }, py: 3 }}>
                            <Typography sx={sectionTitleSx}>
                                Nuevo comentario
                            </Typography>

                            <TextField
                                value={formValues.comment}
                                onChange={(event) => handleChange('comment', event.target.value)}
                                onBlur={() => handleBlur('comment')}
                                placeholder={isEditing ? 'Agrega un nuevo comentario al ticket' : 'Agrega comentarios antes de crear el ticket'}
                                fullWidth
                                multiline
                                minRows={5}
                                disabled={saving || loadingOptions || savingComment || isReadOnly}
                                error={touched.comment && !!errors.comment}
                                helperText={touched.comment ? (errors.comment || ' ') : 'Máximo 1000 caracteres'}
                                sx={fieldSx}
                            />

                            <Box sx={{ mt: 1.5, display: 'flex', justifyContent: 'flex-end' }}>
                                <Button
                                    variant="contained"
                                    onClick={handleAddComment}
                                    disabled={saving || loadingOptions || savingComment || isReadOnly}
                                >
                                    {savingComment ? 'Guardando...' : 'Agregar comentario'}
                                </Button>
                            </Box>

                            <Divider sx={{ my: 3 }} />

                            <Typography sx={sectionTitleSx}>
                                Historial de comentarios
                            </Typography>

                            {commentsToShow.length > 0 ? (
                                <Stack spacing={1.5}>
                                    {commentsToShow.map((comment) => (
                                        <Box
                                            key={comment.id}
                                            sx={{
                                                border: '1px solid',
                                                borderColor: 'divider',
                                                backgroundColor: 'background.paper',
                                                p: 1.5,
                                            }}
                                        >
                                            <Stack direction="row" spacing={1} sx={{ mb: 0.5, flexWrap: 'wrap' }}>
                                                <Typography sx={{ fontWeight: 700, fontSize: 13 }}>
                                                    {comment.authorName ?? comment.authorId ?? 'Usuario'}
                                                </Typography>
                                                <Typography sx={{ color: 'text.secondary', fontSize: 13 }}>
                                                    {comment.pending ? 'Pendiente de guardar' : formatCommentDate(comment.createdAt)}
                                                </Typography>
                                            </Stack>
                                            <Typography sx={{ whiteSpace: 'pre-wrap' }}>
                                                {comment.content}
                                            </Typography>
                                        </Box>
                                    ))}
                                </Stack>
                            ) : (
                                <Typography sx={{ color: 'text.secondary' }}>
                                    No hay comentarios registrados.
                                </Typography>
                            )}
                        </Box>
                    ) : null}

                    {activeTab === 'attachments' ? (
                        <Box sx={{ px: { xs: 2, sm: 3 }, py: 3 }}>
                            <Typography sx={sectionTitleSx}>
                                Agregar fotos
                            </Typography>

                            <TextField
                                type="file"
                                inputProps={{ multiple: true, accept: 'image/*' }}
                                onChange={(event) => setPhotos(Array.from(event.target.files || []))}
                                fullWidth
                                size="small"
                                disabled={saving || loadingOptions || isReadOnly}
                                helperText={photos.length > 0 ? `${photos.length} archivo(s) seleccionado(s)` : 'Opcional'}
                                sx={fieldSx}
                            />

                            <Divider sx={{ my: 3 }} />

                            <Typography sx={sectionTitleSx}>
                                Adjuntos actuales
                            </Typography>

                            {(ticket?.photos?.length ?? 0) > 0 ? (
                                <Stack spacing={1.5}>
                                    {ticket.photos.map((photo) => (
                                        <Box
                                            key={photo.id}
                                            sx={{
                                                border: '1px solid',
                                                borderColor: 'divider',
                                                backgroundColor: 'background.paper',
                                                p: 1.5,
                                            }}
                                        >
                                            <Typography sx={{ fontWeight: 700 }}>
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
                                                    }}
                                                />
                                            ) : null}
                                        </Box>
                                    ))}
                                </Stack>
                            ) : (
                                <Typography sx={{ color: 'text.secondary' }}>
                                    No hay adjuntos registrados.
                                </Typography>
                            )}

                            {photos.length > 0 ? (
                                <>
                                    <Divider sx={{ my: 3 }} />

                                    <Typography sx={sectionTitleSx}>
                                        Adjuntos por guardar
                                    </Typography>

                                    <Stack spacing={1}>
                                        {photos.map((photo) => (
                                            <Typography key={`${photo.name}-${photo.size}`} sx={{ color: 'text.secondary' }}>
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