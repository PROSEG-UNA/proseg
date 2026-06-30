import { useContext, useEffect, useMemo, useRef, useState } from 'react';
import {
    Autocomplete,
    Box,
    Button,
    Chip,
    Dialog,
    Divider,
    FormControl,
    FormControlLabel,
    IconButton,
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
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import CloseIcon from '@mui/icons-material/Close';
import CheckIcon from '@mui/icons-material/Check';
import DeleteOutlinedIcon from "@mui/icons-material/DeleteOutlined";
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import HistoryOutlinedIcon from '@mui/icons-material/HistoryOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import SearchableSelect from '../../../../common/components/SearchableSelect.jsx';
import {
    addMaintenanceTicketComment,
    createMaintenanceTicket,
    deleteMaintenanceTicketComment,
    fetchMaintenanceTicketAssignees,
    fetchMaintenanceTicketHistory,
    fetchMaintenanceTicketPhotos,
    updateMaintenanceTicketAssignedTo,
    updateMaintenanceTicketComment,
    updateMaintenanceTicket,
    updateMaintenanceTicketStatus,
} from '../../services/ticketsService';
import MaintenanceTicketHistoryTab from './MaintenanceTicketHistoryTab';
import {
    fetchBuildingsByCampus,
    fetchCampuses,
    fetchFloorsByBuilding,
    fetchLocationsByBuilding,
} from '../../services/locationsService';
import { fetchMaintenanceAssets } from '../../services/request/requestsService';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { AuthContext } from '../../../../common/context/AuthContext';
import { MAINTENANCE_PRIORITY_OPTIONS, MAINTENANCE_TICKET_STATUS_OPTIONS, formatDateHourMinute } from '../../maintenanceUtils';

const INITIAL_VALUES = {
    title: '',
    description: '',
    comment: '',
    priority: 'LOW',
    status: 'OPEN',
    assignedTo: '',
    requiresLocation: 'true',
    siteId: '',
    buildingId: '',
    floorId: '',
    locationId: '',
    requiresAsset: 'false',
};

const LOCATION_PAGE_OPTIONS = { page: 0, size: 100 };
const ASSET_PAGE_OPTIONS = { page: 0, size: 100 };
const SAFE_TEXT_REGEX = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ0-9.,()#&/\-\s]+$/u;
const SAFE_TEXT_HELPER = 'Solo se permiten letras, números, espacios y . , ( ) # & / -';
const readAsDataUrl = (file) => new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(file);
});

const isImageType = (type) => typeof type === 'string' && type.startsWith('image/');

const buildAssetLabel = (asset) => {
    const assetNumber = asset.assetNumber ?? asset.id;
    const serialNumber = asset.serialNumber ? ` - ${asset.serialNumber}` : '';
    const model = asset.model?.name ?? asset.modelName ?? asset.assetName ?? asset.type ?? 'Sin modelo';
    const type = (typeof asset.type === 'string' ? asset.type : asset.type?.name) ?? asset.typeName ?? asset.assetType?.name ?? asset.assetType ?? 'Sin tipo';
    const brand = asset.model?.brand?.name ?? asset.brand ?? 'Sin marca';
    const location = asset.location?.description
        ?? asset.locationName
        ?? asset.locationDescription
        ?? asset.building?.name
        ?? asset.buildingName
        ?? asset.campus?.name
        ?? asset.campusName
        ?? 'Sin ubicación';

    return `${assetNumber}${serialNumber} - ${model} - ${type} - ${brand} - ${location}`;
};

const mergeAssetOptions = (assetOptions, selectedOptions) => {
    const merged = new Map();

    [...selectedOptions, ...assetOptions].forEach((asset) => {
        if (asset?.id) {
            merged.set(asset.id, asset);
        }
    });

    return Array.from(merged.values());
};

const getCommentDateValue = (comment) => {
    const value = comment?.createdAt ?? comment?.updatedAt;
    return value ? new Date(value).getTime() : 0;
};

const isCommentEdited = (comment) => {
    if (comment?.edited) {
        return true;
    }

    if (!comment?.createdAt || !comment?.updatedAt) {
        return false;
    }

    const createdAt = new Date(comment.createdAt).getTime();
    const updatedAt = new Date(comment.updatedAt).getTime();

    if (Number.isNaN(createdAt) || Number.isNaN(updatedAt)) {
        return comment.createdAt !== comment.updatedAt;
    }

    return updatedAt > createdAt;
};

const getTicketCampusId = (ticket) => ticket?.siteId ?? ticket?.campusId ?? ticket?.site?.id ?? ticket?.campus?.id ?? '';

const getTicketBuildingId = (ticket) => ticket?.buildingId ?? ticket?.building?.id ?? '';

const getTicketFloorId = (ticket) => ticket?.floorId ?? ticket?.floor?.id ?? '';

const getTicketLocationId = (ticket) => ticket?.locationId ?? ticket?.location?.id ?? '';

const buildAssigneeLabel = (userOption) => {
    if (!userOption) return 'Usuario';

    const name = [userOption.firstName, userOption.lastName].filter(Boolean).join(' ').trim();
    if (name && userOption.email) {
        return `${name} - ${userOption.email}`;
    }

    return name || userOption.email || userOption.username || userOption.id;
};

export default function MaintenanceTicketFormModal({ open, onClose, onCreated, ticket = null, readOnly = false, loadingDetail = false }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const { hasPermission } = usePermissions();
    const { user } = useContext(AuthContext);
    const canSetPriority = hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.SET_PRIORITY);
    const canAssignTicket = hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.ASSIGN_TICKET);
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
    const [removedPhotoIds, setRemovedPhotoIds] = useState([]);
    const [ticketComments, setTicketComments] = useState([]);
    const [pendingComments, setPendingComments] = useState([]);
    const [alert, setAlert] = useState(null);
    const [rightTab, setRightTab] = useState('details');
    const [ticketHistory, setTicketHistory] = useState([]);
    const [historyLoading, setHistoryLoading] = useState(false);
    const [isDragOver, setIsDragOver] = useState(false);
    const [photoPreview, setPhotoPreview] = useState(null);
    const [hoveredCommentId, setHoveredCommentId] = useState(null);
    const [editingCommentId, setEditingCommentId] = useState(null);
    const [editingCommentContent, setEditingCommentContent] = useState('');
    const [commentActionLoadingId, setCommentActionLoadingId] = useState(null);
    const [commentToDelete, setCommentToDelete] = useState(null);
    const [ticketPhotos, setTicketPhotos] = useState([]);
    const [loadingPhotos, setLoadingPhotos] = useState(false);
    const [assigneeOptions, setAssigneeOptions] = useState([]);
    const [loadingAssignees, setLoadingAssignees] = useState(false);
    const [assetSearchTerm, setAssetSearchTerm] = useState('');
    const fileInputRef = useRef(null);

    const isAssetMode = formValues.requiresAsset === 'true';
    const requiresLocationSelection = !isAssetMode;

    const currentUserIds = useMemo(() => (
        [
            user?.sub,
            user?.keycloakId,
            user?.keycloakUserId,
            user?.keycloak_id,
            user?.userId,
            user?.id,
        ]
            .filter(Boolean)
            .map((value) => String(value))
    ), [user]);

    const isTicketCreator = useMemo(() => {
        if (!isEditing) return true;
        if (!ticket?.createdBy) return false;

        return currentUserIds.includes(String(ticket.createdBy));
    }, [isEditing, currentUserIds, ticket?.createdBy]);

    const isRestrictedEditor = isEditing && !isReadOnly && !isTicketCreator;
    const canEditCoreFields = !isReadOnly && !isRestrictedEditor;
    const showPlanningTitle = isEditing || canSetPriority || canAssignTicket;

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
            setRemovedPhotoIds([]);
            setTicketComments([]);
            setPendingComments([]);
            setAlert(null);
            setRightTab('details');
            setTicketHistory([]);
            setHistoryLoading(false);
            setIsDragOver(false);
            setPhotoPreview(null);
            setHoveredCommentId(null);
            setEditingCommentId(null);
            setEditingCommentContent('');
            setCommentActionLoadingId(null);
            setCommentToDelete(null);
            setTicketPhotos([]);
            setLoadingPhotos(false);
            setAssigneeOptions([]);
            setLoadingAssignees(false);
            setAssetSearchTerm('');
            return;
        }

        setActiveTab('details');
        setRightTab('details');
        setTicketHistory([]);
        setTicketComments(ticket?.comments ?? []);
        setPendingComments([]);
        setPhotos([]);
        setRemovedPhotoIds([]);
        setErrors({});
        setTouched({});
        setHoveredCommentId(null);
        setEditingCommentId(null);
        setEditingCommentContent('');
        setCommentActionLoadingId(null);
        setCommentToDelete(null);
        setTicketPhotos([]);
        setLoadingPhotos(false);
        setAssigneeOptions([]);
        setLoadingAssignees(false);
        setAssetSearchTerm('');
        setBuildings([]);
        setFloors([]);
        setLocations([]);
        setAssets([]);

        if (ticket) {
            const ticketAssets = ticket.assets ?? [];
            const ticketSiteId = getTicketCampusId(ticket);

            setFormValues({
                title: ticket.title ?? '',
                description: ticket.description ?? '',
                comment: '',
                priority: ticket.priority ?? 'LOW',
                status: ticket.status ?? 'OPEN',
                assignedTo: ticket.assignedTo ?? '',
                requiresLocation: ticketAssets.length > 0 ? 'false' : 'true',
                siteId: ticketSiteId,
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
                modelName: ticketAsset.modelName ?? ticketAsset.assetName,
                type: ticketAsset.type,
                brand: ticketAsset.brand,
                locationDescription: ticketAsset.locationDescription,
            })));
        } else {
            setFormValues(INITIAL_VALUES);
            setSelectedAssets([]);
        }
    }, [open, ticket]);

    useEffect(() => {
        if (!open || !isEditing || !canAssignTicket) {
            setAssigneeOptions([]);
            return;
        }

        let cancelled = false;

        async function loadAssignees() {
            setLoadingAssignees(true);
            try {
                const usersResponse = await fetchMaintenanceTicketAssignees();
                if (cancelled) return;

                const users = Array.isArray(usersResponse) ? usersResponse : [];

                const normalizedAssignedTo = ticket?.assignedTo ? String(ticket.assignedTo) : '';
                const alreadyIncluded = normalizedAssignedTo
                    ? users.some((u) => String(u.id) === normalizedAssignedTo)
                    : true;

                const merged = !alreadyIncluded && normalizedAssignedTo
                    ? [{
                        id: normalizedAssignedTo,
                        firstName: ticket?.assignedToName ?? '',
                        lastName: '',
                        email: '',
                        username: ticket?.assignedToName ?? normalizedAssignedTo,
                    }, ...users]
                    : users;

                setAssigneeOptions(merged);
            } catch {
                if (!cancelled) setAssigneeOptions([]);
            } finally {
                if (!cancelled) setLoadingAssignees(false);
            }
        }

        loadAssignees();
        return () => {
            cancelled = true;
        };
    }, [open, isEditing, canAssignTicket, ticket?.assignedTo, ticket?.assignedToName]);

    useEffect(() => {
        if (!open || !requiresLocationSelection) {
            setCampuses([]);
            return;
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
    }, [open, requiresLocationSelection]);

    useEffect(() => {
        if (!open || !requiresLocationSelection || !formValues.siteId) {
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
    }, [open, requiresLocationSelection, formValues.siteId]);

    useEffect(() => {
        if (!open || !requiresLocationSelection || !formValues.buildingId) {
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
    }, [open, requiresLocationSelection, formValues.buildingId]);

    const filteredLocations = useMemo(() => {
        if (!formValues.floorId) {
            return [];
        }

        return locations.filter((location) => {
            const locationFloorId = location.floor?.id ?? location.floorId;
            return locationFloorId === formValues.floorId;
        });
    }, [locations, formValues.floorId]);

    useEffect(() => {
        if (!open || !isAssetMode) {
            setLoadingAssets(false);
            setAssets([]);
            return;
        }

        const trimmedSearch = assetSearchTerm.trim();
        if (trimmedSearch.length < 2) {
            setLoadingAssets(false);
            setAssets(selectedAssets);
            return;
        }

        let cancelled = false;

        async function loadAssets() {
            setLoadingAssets(true);

            try {
                const assetPage = await fetchMaintenanceAssets({
                    ...ASSET_PAGE_OPTIONS,
                    size: 50,
                    search: trimmedSearch,
                    sort: ['assetNumber,asc'],
                });

                if (cancelled) return;

                const assetItems = assetPage?.content ?? [];
                setAssets(mergeAssetOptions(assetItems, selectedAssets));
            } catch (error) {
                if (!cancelled) {
                    setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudieron cargar los activos' });
                }
            } finally {
                setLoadingAssets(false);
            }
        }

        loadAssets();

        return () => {
            cancelled = true;
        };
    }, [open, isAssetMode, assetSearchTerm, selectedAssets]);

    useEffect(() => {
        if (!open || !ticket?.id || rightTab !== 'history') return;

        let cancelled = false;

        async function loadHistory() {
            setHistoryLoading(true);
            try {
                const page = await fetchMaintenanceTicketHistory(ticket.id, { page: 0, size: 100 });
                if (!cancelled) setTicketHistory(page?.content ?? []);
            } catch {
                if (!cancelled) setTicketHistory([]);
            } finally {
                if (!cancelled) setHistoryLoading(false);
            }
        }

        loadHistory();
        return () => { cancelled = true; };
    }, [open, ticket?.id, rightTab]);

    useEffect(() => {
        if (!open || !ticket?.id || activeTab !== 'attachments') return;

        let cancelled = false;

        async function loadPhotos() {
            setLoadingPhotos(true);
            try {
                const photoList = await fetchMaintenanceTicketPhotos(ticket.id);
                if (!cancelled) setTicketPhotos(photoList ?? []);
            } catch {
                if (!cancelled) setTicketPhotos([]);
            } finally {
                if (!cancelled) setLoadingPhotos(false);
            }
        }

        loadPhotos();
        return () => { cancelled = true; };
    }, [open, ticket?.id, activeTab]);

    const commentsToShow = useMemo(
        () => [...ticketComments, ...pendingComments].sort((a, b) => getCommentDateValue(b) - getCommentDateValue(a)),
        [ticketComments, pendingComments]
    );

    const assignedToDisplayName = useMemo(() => {
        if (!isEditing) {
            return 'Sin asignar';
        }

        const assignedValue = formValues.assignedTo ? String(formValues.assignedTo) : '';
        if (!assignedValue) {
            return 'Sin asignar';
        }

        const selectedOption = assigneeOptions.find((option) => String(option.id) === assignedValue);
        if (selectedOption) {
            return buildAssigneeLabel(selectedOption);
        }

        if (ticket?.assignedTo && String(ticket.assignedTo) === assignedValue && ticket?.assignedToName) {
            return ticket.assignedToName;
        }

        return assignedValue;
    }, [isEditing, formValues.assignedTo, assigneeOptions, ticket?.assignedTo, ticket?.assignedToName]);

    const availableStatusOptions = useMemo(() => {
        if (ticket?.status === 'OPEN') {
            return MAINTENANCE_TICKET_STATUS_OPTIONS.filter((option) => option.value !== 'REOPENED');
        }

        if (ticket?.status === 'REOPENED') {
            return MAINTENANCE_TICKET_STATUS_OPTIONS.filter((option) => option.value !== 'OPEN');
        }

        return MAINTENANCE_TICKET_STATUS_OPTIONS;
    }, [ticket?.status]);

    const canManageComment = (comment) => {
        if (isReadOnly) {
            return false;
        }

        if (comment?.pending) {
            return true;
        }

        return (
            isEditing
            && !!comment?.authorId
            && currentUserIds.includes(String(comment.authorId))
        );
    };

    const validateSafeTextField = (trimmedValue, message) => {
        if (!trimmedValue) {
            return '';
        }

        return SAFE_TEXT_REGEX.test(trimmedValue) ? '' : message;
    };

    const validateField = (key, value, values = formValues) => {
        let error = '';
        const trimmed = typeof value === 'string' ? value.trim() : value;
        const requiredKeys = ['title', 'description', ...(canSetPriority ? ['priority'] : [])];

        if (requiredKeys.includes(key) && !trimmed) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'siteId' && values.requiresAsset !== 'true' && !trimmed) {
            error = 'Este campo es requerido';
        }

        if (!error && key === 'title' && trimmed && trimmed.length > 120) {
            error = 'El título no puede superar los 120 caracteres';
        }

        if (!error && key === 'description' && trimmed && trimmed.length > 500) {
            error = 'La descripción no puede superar los 500 caracteres';
        }

        if (!error && key === 'description') {
            error = validateSafeTextField(trimmed, `La descripción contiene caracteres inválidos. ${SAFE_TEXT_HELPER}`);
        }

        if (!error && key === 'comment' && trimmed && trimmed.length > 1000) {
            error = 'El comentario no puede superar los 1000 caracteres';
        }

        if (!error && key === 'comment') {
            error = validateSafeTextField(trimmed, `El comentario contiene caracteres inválidos. ${SAFE_TEXT_HELPER}`);
        }

        setErrors((prev) => ({ ...prev, [key]: error }));
        return !error;
    };

    const handleChange = (key, value) => {
        if (isRestrictedEditor && ['title', 'description', 'requiresLocation', 'siteId', 'buildingId', 'floorId', 'locationId', 'requiresAsset'].includes(key)) {
            return;
        }

        if (key === 'requiresLocation') {
            const nextValues = {
                ...formValues,
                requiresLocation: value,
                ...(value !== 'true' ? {
                    siteId: '',
                    buildingId: '',
                    floorId: '',
                    locationId: '',
                    requiresAsset: 'false',
                } : {}),
            };

            setFormValues(nextValues);

            if (value !== 'true') {
                setCampuses([]);
                setBuildings([]);
                setFloors([]);
                setLocations([]);
                setAssets([]);
                setSelectedAssets([]);
                setErrors((prev) => ({ ...prev, siteId: '', buildingId: '', locationId: '' }));
            }

            return;
        }

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

            if (touched.floorId) validateField('floorId', value, nextValues);
            if (touched.locationId) validateField('locationId', '', nextValues);
            return;
        }

        if (key === 'locationId') {
            const selectedLocation = locations.find((location) => location.id === value);
            const derivedFloorId = selectedLocation
                ? (selectedLocation.floor?.id ?? selectedLocation.floorId ?? '')
                : '';

            const nextValues = { ...formValues, locationId: value, floorId: derivedFloorId };

            setFormValues(nextValues);
            setAssets([]);
            setSelectedAssets([]);

            if (touched.locationId) validateField('locationId', value, nextValues);
            return;
        }

        if (key === 'requiresAsset') {
            const nextValues = {
                ...formValues,
                requiresAsset: value,
                requiresLocation: value === 'true' ? 'false' : 'true',
                siteId: '',
                buildingId: '',
                floorId: '',
                locationId: '',
            };

            setFormValues(nextValues);
            setAssetSearchTerm('');
            setAssets([]);
            setSelectedAssets([]);
            setCampuses([]);
            setBuildings([]);
            setFloors([]);
            setLocations([]);
            setErrors((prev) => ({
                ...prev,
                siteId: '',
                buildingId: '',
                floorId: '',
                locationId: '',
            }));
            setTouched((prev) => ({
                ...prev,
                siteId: false,
                buildingId: false,
                floorId: false,
                locationId: false,
            }));

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
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo guardar el comentario' });
        } finally {
            setSavingComment(false);
        }
    };

    const handleStartEditComment = (comment) => {
        if (!canManageComment(comment)) {
            return;
        }
        setEditingCommentId(comment.id);
        setEditingCommentContent(comment.content ?? '');
    };

    const handleCancelEditComment = () => {
        setEditingCommentId(null);
        setEditingCommentContent('');
    };

    const handleSaveEditComment = async (comment) => {
        if (!canManageComment(comment)) {
            return;
        }

        const trimmedContent = editingCommentContent.trim();
        if (!trimmedContent) {
            setAlert({ type: 'warning', message: 'El comentario no puede quedar vacío' });
            return;
        }
        if (trimmedContent.length > 1000) {
            setAlert({ type: 'warning', message: 'El comentario no puede superar los 1000 caracteres' });
            return;
        }

        if (comment?.pending) {
            setPendingComments((prev) => prev.map((item) => (
                item.id === comment.id
                    ? { ...item, content: trimmedContent }
                    : item
            )));
            setEditingCommentId(null);
            setEditingCommentContent('');
            return;
        }

        setCommentActionLoadingId(comment.id);
        try {
            const updatedComment = await updateMaintenanceTicketComment(ticket.id, comment.id, trimmedContent);
            setTicketComments((prev) => prev.map((item) => (
                item.id === comment.id
                    ? { ...updatedComment, edited: trimmedContent !== (comment.content ?? '').trim() }
                    : item
            )));
            setEditingCommentId(null);
            setEditingCommentContent('');
        } catch (error) {
            setAlert({
                type: 'error',
                message: error?.response?.data?.message ?? error?.message ?? 'No se pudo editar el comentario',
            });
        } finally {
            setCommentActionLoadingId(null);
        }
    };

    const handleDeleteComment = async (comment) => {
        if (!canManageComment(comment)) {
            return;
        }

        if (comment?.pending) {
            setPendingComments((prev) => prev.filter((item) => item.id !== comment.id));
            if (editingCommentId === comment.id) {
                setEditingCommentId(null);
                setEditingCommentContent('');
            }
            setCommentToDelete(null);
            return;
        }

        setCommentActionLoadingId(comment.id);
        try {
            await deleteMaintenanceTicketComment(ticket.id, comment.id);
            setTicketComments((prev) => prev.filter((item) => item.id !== comment.id));
            if (editingCommentId === comment.id) {
                setEditingCommentId(null);
                setEditingCommentContent('');
            }
        } catch (error) {
            setAlert({
                type: 'error',
                message: error?.response?.data?.message ?? error?.message ?? 'No se pudo eliminar el comentario',
            });
        } finally {
            setCommentActionLoadingId(null);
            setCommentToDelete(null);
        }
    };

    const toggleRemovedPhoto = (photoId) => {
        setRemovedPhotoIds((prev) => (
            prev.includes(photoId)
                ? prev.filter((id) => id !== photoId)
                : [...prev, photoId]
        ));
    };

    const handleFileSelect = async (files) => {
        const allFiles = Array.from(files ?? []);

        if (allFiles.length === 0) {
            return;
        }

        const nextPhotos = await Promise.all(
            allFiles.map(async (file) => ({
                file,
                name: file.name,
                contentType: file.type,
                preview: isImageType(file.type) ? await readAsDataUrl(file) : null,
            }))
        );

        setPhotos((prev) => [...prev, ...nextPhotos]);
    };

    const handleRemovePhoto = (index) => {
        setPhotos((prev) => {
            const next = [...prev];
            next.splice(index, 1);
            return next;
        });
    };

    const handleSave = async () => {
        if (isReadOnly) {
            onClose?.();
            return;
        }

        const requiredFields = ['title', 'description', ...(canSetPriority ? ['priority'] : [])];
        const nextTouched = {};
        const nextErrors = {};

        requiredFields.forEach((key) => {
            nextTouched[key] = true;
            if (!formValues[key]?.trim()) {
                nextErrors[key] = 'Este campo es requerido';
            }
        });

        if (formValues.requiresAsset !== 'true' && !formValues.siteId?.trim()) {
            nextTouched.siteId = true;
            nextErrors.siteId = 'Este campo es requerido';
        }

        if (formValues.title?.trim()?.length > 120) {
            nextErrors.title = 'El título no puede superar los 120 caracteres';
            nextTouched.title = true;
        }

        if (formValues.description?.trim()?.length > 500) {
            nextErrors.description = 'La descripción no puede superar los 500 caracteres';
            nextTouched.description = true;
        }

        const descriptionPatternError = validateSafeTextField(
            formValues.description?.trim(),
            `La descripción contiene caracteres inválidos. ${SAFE_TEXT_HELPER}`
        );

        if (descriptionPatternError) {
            nextErrors.description = descriptionPatternError;
            nextTouched.description = true;
        }

        if (formValues.comment?.trim()?.length > 1000) {
            nextErrors.comment = 'El comentario no puede superar los 1000 caracteres';
            nextTouched.comment = true;
        }

        const commentPatternError = validateSafeTextField(
            formValues.comment?.trim(),
            `El comentario contiene caracteres inválidos. ${SAFE_TEXT_HELPER}`
        );

        if (commentPatternError) {
            nextErrors.comment = commentPatternError;
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
            const existingAssetIds = (ticket?.assets ?? []).map((asset) => asset.assetId ?? asset.id).filter(Boolean);
            const protectedSiteId = ticket ? getTicketCampusId(ticket) : (formValues.requiresLocation === 'true' ? formValues.siteId : null);
            const protectedBuildingId = ticket ? (getTicketBuildingId(ticket) || null) : (formValues.requiresLocation === 'true' ? (formValues.buildingId || null) : null);
            const protectedFloorId = ticket ? (getTicketFloorId(ticket) || null) : (formValues.requiresLocation === 'true' ? (formValues.floorId || null) : null);
            const protectedLocationId = ticket ? (getTicketLocationId(ticket) || null) : (formValues.requiresLocation === 'true' ? (formValues.locationId || null) : null);

            const payload = {
                title: isRestrictedEditor ? (ticket?.title ?? formValues.title.trim()) : formValues.title.trim(),
                description: isRestrictedEditor ? (ticket?.description ?? formValues.description.trim()) : formValues.description.trim(),
                ...(canSetPriority ? { priority: formValues.priority } : {}),
                siteId: isRestrictedEditor ? protectedSiteId : (formValues.siteId || null),
                buildingId: isRestrictedEditor ? protectedBuildingId : (formValues.buildingId || null),
                floorId: isRestrictedEditor ? protectedFloorId : (formValues.floorId || null),
                locationId: isRestrictedEditor ? protectedLocationId : (formValues.locationId || null),
                assetIds: isRestrictedEditor
                    ? existingAssetIds
                    : (formValues.requiresAsset === 'true' ? selectedAssets.map((asset) => asset.id) : []),
                ...(isEditing && !isRestrictedEditor && removedPhotoIds.length > 0 ? { removedPhotoIds } : {}),
            };

            const savedTicket = isEditing
                ? await updateMaintenanceTicket(ticket.id, payload, photos)
                : await createMaintenanceTicket(payload, photos);

            if (isEditing && formValues.status && formValues.status !== ticket?.status) {
                await updateMaintenanceTicketStatus(ticket.id, formValues.status);
            }

            if (isEditing && canAssignTicket
                && String(formValues.assignedTo ?? '') !== String(ticket?.assignedTo ?? '')) {
                await updateMaintenanceTicketAssignedTo(ticket.id, formValues.assignedTo);
            }

            if (!isEditing) {
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
            '&.Mui-disabled': {
                backgroundColor: 'color-mix(in srgb, #000 2%, #fff)',
            },
            '&.Mui-disabled .MuiInputBase-input': {
                WebkitTextFillColor: 'rgba(0, 0, 0, 0.82)',
                color: 'rgba(0, 0, 0, 0.82)',
            },
        },
        '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
        '& .MuiInputLabel-root.Mui-disabled': {
            color: 'rgba(0, 0, 0, 0.68)',
        },
        '& .MuiFormHelperText-root.Mui-disabled': {
            color: 'rgba(0, 0, 0, 0.62)',
        },
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
                fillHeight
                icon={ConstructionIcon}
                title={isReadOnly ? 'Detalle de ticket de mantenimiento' : isEditing ? 'Editar ticket de mantenimiento' : 'Crear ticket de mantenimiento'}
                subtitle={isReadOnly ? 'Visualiza los datos del ticket' : isEditing ? 'Actualiza la información del ticket' : 'Registra una incidencia en el ticket'}
                loading={saving || loadingCatalogs || loadingDetail}
                secondaryButton={isReadOnly ? undefined : { label: 'Cancelar', onClick: onClose, disabled: saving }}
                primaryButton={isReadOnly
                    ? { label: 'Cerrar', onClick: onClose, disabled: false }
                    : { label: saving ? 'Guardando...' : isEditing ? 'Guardar cambios' : 'Crear ticket', onClick: handleSave, disabled: saving || loadingCatalogs || loadingDetail || savingComment || loadingAssignees }
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
                            disabled={saving || loadingCatalogs || !canEditCoreFields}
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
                            <Tab value="details" label={buildTabLabel(DescriptionOutlinedIcon, 'Información')} />
                            <Tab value="attachments" label={buildTabLabel(ImageOutlinedIcon, 'Adjuntos')} />
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
                                    disabled={saving || loadingCatalogs || !canEditCoreFields}
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
                                                    <Box
                                                        key={comment.id}
                                                        sx={cardSx}
                                                        onMouseEnter={() => setHoveredCommentId(comment.id)}
                                                        onMouseLeave={() => setHoveredCommentId((prev) => (prev === comment.id ? null : prev))}
                                                    >
                                                        <Stack
                                                            direction="row"
                                                            spacing={1}
                                                            sx={{
                                                                mb: 0.75,
                                                                flexWrap: 'wrap',
                                                                alignItems: 'center',
                                                                justifyContent: 'space-between',
                                                            }}
                                                        >
                                                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                                                                <Typography sx={{ fontWeight: 700, fontSize: 13 }}>
                                                                    {comment.authorName ?? 'Usuario'}
                                                                </Typography>
                                                                <Typography sx={{ color: 'text.secondary', fontSize: 12.5 }}>
                                                                    {formatDateHourMinute(comment.createdAt) || 'Pendiente de guardar'}
                                                                    {!comment.pending && isCommentEdited(comment) ? ' (editado)' : ''}
                                                                </Typography>
                                                            </Box>

                                                            {canManageComment(comment) ? (
                                                                <Box
                                                                    sx={{
                                                                        display: 'flex',
                                                                        alignItems: 'center',
                                                                        opacity: hoveredCommentId === comment.id || editingCommentId === comment.id ? 1 : 0,
                                                                        transition: 'opacity 0.2s ease',
                                                                        pointerEvents: hoveredCommentId === comment.id || editingCommentId === comment.id ? 'auto' : 'none',
                                                                    }}
                                                                >
                                                                    {editingCommentId === comment.id ? (
                                                                        <>
                                                                            <IconButton
                                                                                size="small"
                                                                                color="success"
                                                                                disabled={commentActionLoadingId === comment.id}
                                                                                onClick={() => handleSaveEditComment(comment)}
                                                                            >
                                                                                <CheckIcon fontSize="small" />
                                                                            </IconButton>
                                                                            <IconButton
                                                                                size="small"
                                                                                disabled={commentActionLoadingId === comment.id}
                                                                                onClick={handleCancelEditComment}
                                                                            >
                                                                                <CloseIcon fontSize="small" />
                                                                            </IconButton>
                                                                        </>
                                                                    ) : (
                                                                        <>
                                                                            <IconButton
                                                                                size="small"
                                                                                disabled={commentActionLoadingId === comment.id}
                                                                                onClick={() => handleStartEditComment(comment)}
                                                                            >
                                                                                <EditOutlinedIcon fontSize="small" />
                                                                            </IconButton>
                                                                            <IconButton
                                                                                size="small"
                                                                                color="error"
                                                                                disabled={commentActionLoadingId === comment.id}
                                                                                onClick={() => setCommentToDelete(comment)}
                                                                            >
                                                                                <DeleteOutlinedIcon fontSize="small" />
                                                                            </IconButton>
                                                                        </>
                                                                    )}
                                                                </Box>
                                                            ) : null}
                                                        </Stack>

                                                        {editingCommentId === comment.id ? (
                                                            <TextField
                                                                value={editingCommentContent}
                                                                onChange={(event) => setEditingCommentContent(event.target.value)}
                                                                fullWidth
                                                                multiline
                                                                minRows={3}
                                                                size="small"
                                                                disabled={commentActionLoadingId === comment.id}
                                                                helperText="Máximo 1000 caracteres"
                                                                sx={fieldSx}
                                                            />
                                                        ) : (
                                                            <Typography sx={{ whiteSpace: 'pre-wrap', fontSize: 13.5 }}>
                                                                {comment.content}
                                                            </Typography>
                                                        )}
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

                            <Box sx={{ borderLeft: { xs: 0, md: '1px solid' }, borderColor: 'divider', pl: { xs: 0, md: 0 }, display: 'flex', flexDirection: 'column' }}>
                                <Box sx={{ borderBottom: '1px solid', borderColor: 'divider', mb: 0 }}>
                                    <Tabs
                                        value={rightTab}
                                        onChange={(_, v) => setRightTab(v)}
                                        TabIndicatorProps={{ sx: { backgroundColor: accentColor } }}
                                        sx={{
                                            minHeight: 38,
                                            '& .MuiTab-root': {
                                                minHeight: 38,
                                                textTransform: 'none',
                                                fontWeight: 700,
                                                fontSize: 12.5,
                                                color: 'text.secondary',
                                                px: 1.5,
                                            },
                                            '& .MuiTab-root.Mui-selected': { color: accentColor },
                                        }}
                                    >
                                        <Tab value="details" label={buildTabLabel(DescriptionOutlinedIcon, 'Detalles')} />
                                        <Tab value="history" label={buildTabLabel(HistoryOutlinedIcon, 'Historial')} />
                                    </Tabs>
                                </Box>

                                {rightTab === 'details' ? (
                                    <Box sx={{ pt: 2, pl: { xs: 0, md: 3 } }}>
                                        {showPlanningTitle ? <Typography sx={sectionTitleSx}>Gestión</Typography> : null}

                                        <Stack spacing={2}>
                                            {isEditing ? (
                                                <TextField
                                                    select
                                                    label="Estado"
                                                    value={formValues.status}
                                                    onChange={(event) => handleChange('status', event.target.value)}
                                                    fullWidth
                                                    size="small"
                                                    disabled={saving || loadingCatalogs || isReadOnly}
                                                    helperText=" "
                                                    sx={fieldSx}
                                                >
                                                    {availableStatusOptions.map((option) => (
                                                        <MenuItem key={option.value} value={option.value}>
                                                            {option.label}
                                                        </MenuItem>
                                                    ))}
                                                </TextField>
                                            ) : null}

                                            {isEditing && canAssignTicket ? (
                                                <SearchableSelect
                                                    label="Asignado a"
                                                    value={formValues.assignedTo}
                                                    onChange={(value) => handleChange('assignedTo', value)}
                                                    items={assigneeOptions}
                                                    getItemLabel={buildAssigneeLabel}
                                                    getItemValue={(assignee) => assignee.id}
                                                    fullWidth
                                                    size="small"
                                                    disabled={saving || loadingCatalogs || loadingAssignees || isReadOnly}
                                                    helperText={loadingAssignees
                                                        ? 'Cargando usuarios...'
                                                        : (typeof formValues.assignedTo === 'string' && formValues.assignedTo.trim())
                                                            ? ' '
                                                            : 'Sin asignar'}
                                                    sx={fieldSx}
                                                />
                                            ) : null}

                                            {isEditing && !canAssignTicket ? (
                                                <TextField
                                                    label="Asignado a"
                                                    value={assignedToDisplayName}
                                                    fullWidth
                                                    size="small"
                                                    disabled
                                                    helperText=" "
                                                    sx={fieldSx}
                                                />
                                            ) : null}

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

                                        <Typography sx={sectionTitleSx}>¿Se requiere activo?</Typography>

                                        <FormControl disabled={saving || loadingCatalogs || !canEditCoreFields}>
                                            <RadioGroup
                                                row
                                                value={formValues.requiresAsset}
                                                onChange={(event) => handleChange('requiresAsset', event.target.value)}
                                            >
                                                <FormControlLabel value="true" control={<Radio />} label="Sí" />
                                                <FormControlLabel value="false" control={<Radio />} label="No" />
                                            </RadioGroup>
                                        </FormControl>

                                        {isAssetMode ? (
                                            <Box sx={{ mt: 1.5 }}>
                                                <Typography sx={{ fontWeight: 700, fontSize: 13.5, mb: 1 }}>
                                                    Activos
                                                </Typography>

                                                <Autocomplete
                                                    multiple
                                                    options={assets}
                                                    value={selectedAssets}
                                                    inputValue={assetSearchTerm}
                                                    onInputChange={(_, value, reason) => {
                                                        if (reason === 'input' || reason === 'clear') {
                                                            setAssetSearchTerm(value);
                                                        }
                                                    }}
                                                    onChange={(_, value) => setSelectedAssets(value)}
                                                    getOptionLabel={buildAssetLabel}
                                                    isOptionEqualToValue={(option, value) => option.id === value.id}
                                                    filterOptions={(options) => options}
                                                    loading={loadingAssets}
                                                    disabled={!canEditCoreFields}
                                                    noOptionsText={assetSearchTerm.trim().length < 2 ? 'Escribe al menos 2 caracteres' : 'No se encontraron activos'}
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
                                                            placeholder="Busca por n° activo, n° serie, tipo de activo o modelo"
                                                            size="small"
                                                            sx={fieldSx}
                                                            helperText={assetSearchTerm.trim().length < 2
                                                                ? ''
                                                                : 'Selecciona uno o varios activos del inventario'}
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
                                        ) : (
                                            <Stack spacing={2} sx={{ mt: 1.5 }}>
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
                                                    disabled={saving || loadingOptions || !canEditCoreFields}
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
                                                    clearable
                                                    fullWidth
                                                    size="small"
                                                    disabled={saving || loadingBuildings || !canEditCoreFields || !formValues.siteId}
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
                                                    disabled={saving || loadingFloors || !canEditCoreFields || !formValues.buildingId}
                                                    helperText={touched.floorId ? (errors.floorId || ' ') : 'Opcional'}
                                                    sx={fieldSx}
                                                />

                                                <SearchableSelect
                                                    label="Locación"
                                                    value={formValues.locationId}
                                                    onChange={(value) => handleChange('locationId', value)}
                                                    onBlur={() => handleBlur('locationId')}
                                                    items={filteredLocations}
                                                    getItemLabel={(location) => location.description ?? location.name}
                                                    getItemValue={(location) => location.id}
                                                    clearable
                                                    fullWidth
                                                    size="small"
                                                    disabled={saving || loadingLocations || !canEditCoreFields || !formValues.buildingId || !formValues.floorId}
                                                    error={touched.locationId && !!errors.locationId}
                                                    helperText={touched.locationId ? (errors.locationId || ' ') : (!formValues.floorId ? 'Selecciona un piso primero' : 'Opcional')}
                                                    sx={fieldSx}
                                                />
                                            </Stack>
                                        )}
                                    </Box>
                                ) : null}

                                {rightTab === 'history' ? (
                                    <Box sx={{ pt: 2, pl: { xs: 0, md: 3 }, overflowY: 'auto', maxHeight: 520 }}>
                                        <MaintenanceTicketHistoryTab history={ticketHistory} loading={historyLoading} />
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

                                    <Box
                                        onDragOver={(event) => {
                                            event.preventDefault();
                                            setIsDragOver(true);
                                        }}
                                        onDragLeave={() => setIsDragOver(false)}
                                        onDrop={(event) => {
                                            event.preventDefault();
                                            setIsDragOver(false);
                                            if (saving || loadingCatalogs) {
                                                return;
                                            }
                                            handleFileSelect(event.dataTransfer.files);
                                        }}
                                        onClick={() => {
                                            if (!saving && !loadingCatalogs) {
                                                fileInputRef.current?.click();
                                            }
                                        }}
                                        sx={{
                                            border: '2px dashed',
                                            borderColor: isDragOver ? accentColor : 'divider',
                                            borderRadius: '12px',
                                            p: 3,
                                            display: 'flex',
                                            flexDirection: 'column',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            gap: 1,
                                            cursor: saving || loadingCatalogs ? 'not-allowed' : 'pointer',
                                            opacity: saving || loadingCatalogs ? 0.6 : 1,
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
                                            Arrastra archivos aquí o{' '}
                                            <Typography component="span" sx={{ color: accentColor, fontWeight: 600 }}>
                                                selecciona archivos
                                            </Typography>
                                        </Typography>
                                        <Typography sx={{ fontSize: 11.5, color: 'text.disabled' }}>
                                            Cualquier tipo de archivo
                                        </Typography>
                                    </Box>

                                    <input
                                        ref={fileInputRef}
                                        type="file"
                                        multiple
                                        style={{ display: 'none' }}
                                        onChange={(event) => {
                                            handleFileSelect(event.target.files);
                                            event.target.value = '';
                                        }}
                                    />

                                    {photos.length > 0 ? (
                                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5, mt: 2 }}>
                                            {photos.map((photo, index) => (
                                                <Box
                                                    key={`${photo.name}-${index}`}
                                                    sx={{
                                                        position: 'relative',
                                                        width: 88,
                                                        height: 88,
                                                        borderRadius: '10px',
                                                        overflow: 'hidden',
                                                        border: '1px solid',
                                                        borderColor: 'divider',
                                                    }}
                                                >
                                                    {photo.preview ? (
                                                        <Box
                                                            component="img"
                                                            src={photo.preview}
                                                            alt={photo.name}
                                                            loading="lazy"
                                                            sx={{ width: '100%', height: '100%', objectFit: 'cover' }}
                                                        />
                                                    ) : (
                                                        <Box
                                                            sx={{
                                                                width: '100%',
                                                                height: '100%',
                                                                display: 'flex',
                                                                flexDirection: 'column',
                                                                alignItems: 'center',
                                                                justifyContent: 'center',
                                                                gap: 0.5,
                                                                p: 0.5,
                                                                bgcolor: 'action.hover',
                                                            }}
                                                        >
                                                            <DescriptionOutlinedIcon sx={{ fontSize: 28, color: 'text.secondary' }} />
                                                            <Typography sx={{ fontSize: 10, color: 'text.secondary', textAlign: 'center', wordBreak: 'break-word', lineHeight: 1.1 }} noWrap>
                                                                {photo.name}
                                                            </Typography>
                                                        </Box>
                                                    )}
                                                    <IconButton
                                                        size="small"
                                                        onClick={(event) => {
                                                            event.stopPropagation();
                                                            handleRemovePhoto(index);
                                                        }}
                                                        sx={{
                                                            position: 'absolute',
                                                            top: 2,
                                                            right: 2,
                                                            bgcolor: 'rgba(0,0,0,0.55)',
                                                            color: '#fff',
                                                            p: 0.25,
                                                            '&:hover': { bgcolor: 'rgba(0,0,0,0.75)' },
                                                        }}
                                                    >
                                                        <CloseIcon sx={{ fontSize: 12 }} />
                                                    </IconButton>
                                                </Box>
                                            ))}
                                        </Box>
                                    ) : null}

                                    <Divider sx={{ my: 2.5 }} />
                                </>
                            ) : null}

                            <Typography sx={sectionTitleSx}>Adjuntos actuales</Typography>

                            {loadingPhotos ? (
                                <Typography sx={{ color: 'text.secondary', fontSize: 13.5 }}>
                                    Cargando adjuntos...
                                </Typography>
                            ) : (ticketPhotos.length > 0) ? (
                                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5 }}>
                                    {ticketPhotos.map((photo) => (
                                        <Box
                                            key={photo.id}
                                            sx={{
                                                ...cardSx,
                                                opacity: removedPhotoIds.includes(photo.id) ? 0.6 : 1,
                                                width: { xs: '100%', sm: 210 },
                                                p: 1,
                                            }}
                                        >
                                            <Typography sx={{ fontWeight: 700, fontSize: 13.5 }}>
                                                {photo.fileName ?? photo.objectName}
                                            </Typography>
                                            {!isReadOnly && isEditing ? (
                                                <Box sx={{ mt: 1 }}>
                                                    <Button
                                                        size="small"
                                                        variant={removedPhotoIds.includes(photo.id) ? 'outlined' : 'text'}
                                                        color={removedPhotoIds.includes(photo.id) ? 'success' : 'error'}
                                                        onClick={() => toggleRemovedPhoto(photo.id)}
                                                        disabled={isRestrictedEditor}
                                                    >
                                                        {removedPhotoIds.includes(photo.id) ? 'Restaurar adjunto' : 'Quitar adjunto'}
                                                    </Button>
                                                </Box>
                                            ) : null}
                                            {photo.imageUrl && isImageType(photo.contentType) ? (
                                                <Box
                                                    component="img"
                                                    src={photo.imageUrl}
                                                    alt={photo.fileName ?? photo.objectName}
                                                    loading="lazy"
                                                    onClick={() => setPhotoPreview(photo)}
                                                    sx={{
                                                        mt: 1,
                                                        width: '100%',
                                                        height: 130,
                                                        objectFit: 'cover',
                                                        border: '1px solid',
                                                        borderColor: 'divider',
                                                        borderRadius: '10px',
                                                        cursor: 'zoom-in',
                                                    }}
                                                />
                                            ) : photo.imageUrl ? (
                                                <Button
                                                    size="small"
                                                    variant="outlined"
                                                    startIcon={<DescriptionOutlinedIcon />}
                                                    component="a"
                                                    href={photo.imageUrl}
                                                    target="_blank"
                                                    rel="noopener"
                                                    sx={{ mt: 1, alignSelf: 'flex-start' }}
                                                >
                                                    Descargar
                                                </Button>
                                            ) : null}
                                        </Box>
                                    ))}
                                </Box>
                            ) : (
                                <Typography sx={{ color: 'text.secondary', fontSize: 13.5 }}>
                                    No hay adjuntos registrados.
                                </Typography>
                            )}

                            {!isReadOnly && isEditing && removedPhotoIds.length > 0 ? (
                                <Typography sx={{ mt: 1, color: 'warning.main', fontSize: 12.5 }}>
                                    {removedPhotoIds.length} adjunto(s) se eliminará(n) al guardar.
                                </Typography>
                            ) : null}

                            {!isReadOnly && photos.length > 0 ? (
                                <Typography sx={{ mt: 2, color: 'text.secondary', fontSize: 13.5 }}>
                                    {photos.length} archivo(s) listo(s) para adjuntar al guardar.
                                </Typography>
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

            <DialogModal
                type="warning"
                open={!!commentToDelete}
                title="Eliminar comentario"
                message="¿Deseas eliminar este comentario? Esta acción no se puede deshacer."
                onClose={() => setCommentToDelete(null)}
                onConfirm={() => {
                    if (commentToDelete) {
                        handleDeleteComment(commentToDelete);
                    }
                }}
                confirmLabel={commentToDelete && commentActionLoadingId === commentToDelete.id ? 'Eliminando...' : 'Eliminar'}
            />

            <Dialog
                open={!!photoPreview}
                onClose={() => setPhotoPreview(null)}
                maxWidth="md"
                fullWidth
                slotProps={{
                    paper: {
                        sx: {
                            borderRadius: '14px',
                            overflow: 'hidden',
                        },
                    },
                }}
            >
                {photoPreview ? (
                    <Box sx={{ p: 1.5 }}>
                        <Typography sx={{ fontWeight: 700, mb: 1, fontSize: 14 }}>
                            {photoPreview.fileName ?? photoPreview.objectName}
                        </Typography>
                        <Box
                            component="img"
                            src={photoPreview.imageUrl}
                            alt={photoPreview.fileName ?? photoPreview.objectName}
                            sx={{
                                width: '100%',
                                maxHeight: '75vh',
                                objectFit: 'contain',
                                borderRadius: '10px',
                                bgcolor: 'background.default',
                            }}
                        />
                    </Box>
                ) : null}
            </Dialog>
        </>
    );
}