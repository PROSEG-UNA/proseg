import { useState, useEffect } from 'react';
import {
    Box,
    Typography,
    TextField,
    IconButton,
    InputAdornment,
    Skeleton,
    Chip,
    Tooltip,
    Divider,
    Button,
    Collapse,
} from '@mui/material';
import EmailIcon from '@mui/icons-material/Email';
import AddIcon from '@mui/icons-material/Add';
import SearchIcon from '@mui/icons-material/Search';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import ApartmentIcon from '@mui/icons-material/Apartment';
import BusinessIcon from '@mui/icons-material/Business';
import VisibilityIcon from '@mui/icons-material/Visibility';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import { useTheme } from '@mui/material/styles';
import SearchableSelect from '../../../../common/components/SearchableSelect.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { fetchCatalogOptions } from '../../services/catalogService';
import { LOCATION_ENDPOINTS } from '../../services/endpoints';
import {
    fetchEmailsByBuilding,
    createBuildingEmail,
    deleteBuildingEmail,
    fetchEmailsByCampus,
} from '../../services/buildingMailService.js';
import CampusEmailsModal from './CampusEmailModal.jsx';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const normalize = (str) => (str ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

function EmailSkeleton() {
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {[1, 2, 3].map((i) => (
                <Box
                    key={i}
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        px: 2,
                        py: 1.25,
                        borderRadius: '10px',
                        border: '1px solid',
                        borderColor: 'divider',
                    }}
                >
                    <Skeleton variant="text" width={200} height={18} />
                    <Skeleton variant="circular" width={28} height={28} />
                </Box>
            ))}
        </Box>
    );
}

function EmailRow({ email, canDelete, onDelete, accentColor }) {
    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                px: 2,
                py: 1.1,
                borderRadius: '10px',
                border: '1px solid',
                borderColor: 'divider',
                transition: 'border-color 0.18s ease, background 0.18s ease',
                '&:hover': {
                    borderColor: `color-mix(in srgb, ${accentColor} 30%, transparent)`,
                    background: `color-mix(in srgb, ${accentColor} 4%, transparent)`,
                },
            }}
        >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, minWidth: 0 }}>
                <EmailIcon sx={{ fontSize: 15, color: 'text.disabled', flexShrink: 0 }} />
                <Typography
                    sx={{
                        fontSize: '0.835rem',
                        fontWeight: 500,
                        color: 'text.primary',
                        fontFamily: '"Roboto Mono", monospace',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                    }}
                >
                    {email.email}
                </Typography>
            </Box>
            {canDelete && (
                <Tooltip title="Eliminar correo" placement="left">
                    <IconButton
                        size="small"
                        onClick={() => onDelete(email)}
                        sx={{
                            color: 'text.disabled',
                            flexShrink: 0,
                            '&:hover': { color: 'error.main', bgcolor: 'error.lighter' },
                        }}
                    >
                        <DeleteForeverIcon sx={{ fontSize: 17 }} />
                    </IconButton>
                </Tooltip>
            )}
        </Box>
    );
}

function CampusEmailGroup({ buildingName, emails, accentColor, canDelete, onDelete }) {
    const [open, setOpen] = useState(true);

    return (
        <Box>
            <Box
                onClick={() => setOpen((v) => !v)}
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1,
                    cursor: 'pointer',
                    py: 0.75,
                    borderRadius: '8px',
                    px: 1,
                    mx: -1,
                    transition: 'background 0.15s',
                    '&:hover': { bgcolor: `color-mix(in srgb, ${accentColor} 5%, transparent)` },
                    userSelect: 'none',
                }}
            >
                <ApartmentIcon sx={{ fontSize: 14, color: accentColor, flexShrink: 0 }} />
                <Typography sx={{ fontSize: '0.8rem', fontWeight: 700, color: 'text.primary', flex: 1 }}>
                    {buildingName}
                </Typography>
                <Chip
                    label={emails.length}
                    size="small"
                    sx={{
                        height: 18,
                        fontSize: '0.66rem',
                        fontWeight: 700,
                        bgcolor: `color-mix(in srgb, ${accentColor} 12%, transparent)`,
                        color: accentColor,
                        border: 'none',
                    }}
                />
                <IconButton size="small" sx={{ p: 0.25, color: 'text.disabled' }}>
                    {open ? <ExpandLessIcon sx={{ fontSize: 15 }} /> : <ExpandMoreIcon sx={{ fontSize: 15 }} />}
                </IconButton>
            </Box>

            <Collapse in={open}>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75, pl: 2.5, pt: 0.5, pb: 0.75 }}>
                    {emails.map((email) => (
                        <EmailRow
                            key={email.id}
                            email={email}
                            canDelete={canDelete}
                            onDelete={onDelete}
                            accentColor={accentColor}
                        />
                    ))}
                </Box>
            </Collapse>
        </Box>
    );
}

export default function BuildingEmailManager() {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const { hasPermission } = usePermissions();
    const canManage = hasPermission(PERMISSIONS.INVENTORY.LOCATIONS.MANAGE);
    const canDelete = hasPermission(PERMISSIONS.INVENTORY.LOCATIONS.DELETE);

    const [campuses, setCampuses] = useState([]);
    const [buildings, setBuildings] = useState([]);
    const [loadingOptions, setLoadingOptions] = useState(false);

    const [selectedCampusId, setSelectedCampusId] = useState('');
    const [selectedBuildingId, setSelectedBuildingId] = useState('');

    const [emails, setEmails] = useState([]);
    const [loadingEmails, setLoadingEmails] = useState(false);
    const [campusEmails, setCampusEmails] = useState([]);
    const [loadingCampusEmails, setLoadingCampusEmails] = useState(false);
    const [emailSearch, setEmailSearch] = useState('');

    const [newEmail, setNewEmail] = useState('');
    const [emailError, setEmailError] = useState('');
    const [emailTouched, setEmailTouched] = useState(false);
    const [saving, setSaving] = useState(false);

    const [deletingEmail, setDeletingEmail] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [alert, setAlert] = useState(null);
    const [campusModalOpen, setCampusModalOpen] = useState(false);

    const loadOptions = async () => {
        setLoadingOptions(true);
        try {
            const [campusList, buildingList] = await Promise.all([
                fetchCatalogOptions(LOCATION_ENDPOINTS.campuses),
                fetchCatalogOptions(LOCATION_ENDPOINTS.buildings),
            ]);
            setCampuses(campusList);
            setBuildings(buildingList);
        } catch {
            setCampuses([]);
            setBuildings([]);
        } finally {
            setLoadingOptions(false);
        }
    };

    useEffect(() => {
        loadOptions();
    }, []);

    const loadEmails = async (buildingId, signal) => {
        setLoadingEmails(true);
        setEmails([]);
        try {
            const data = await fetchEmailsByBuilding(buildingId);
            if (!signal.cancelled) setEmails(data);
        } catch {
            if (!signal.cancelled) setEmails([]);
        } finally {
            if (!signal.cancelled) setLoadingEmails(false);
        }
    };

    useEffect(() => {
        if (!selectedBuildingId) {
            setEmails([]);
            return;
        }
        const signal = { cancelled: false };
        setEmailSearch('');
        setNewEmail('');
        setEmailError('');
        setEmailTouched(false);
        loadEmails(selectedBuildingId, signal);
        return () => { signal.cancelled = true; };
    }, [selectedBuildingId]);

    const loadCampusEmails = async (campusId, signal) => {
        setLoadingCampusEmails(true);
        setCampusEmails([]);
        try {
            const data = await fetchEmailsByCampus(campusId);
            if (!signal.cancelled) setCampusEmails(data);
        } catch {
            if (!signal.cancelled) setCampusEmails([]);
        } finally {
            if (!signal.cancelled) setLoadingCampusEmails(false);
        }
    };

    useEffect(() => {
        if (selectedBuildingId || !selectedCampusId) {
            setCampusEmails([]);
            return;
        }
        const signal = { cancelled: false };
        setEmailSearch('');
        loadCampusEmails(selectedCampusId, signal);
        return () => { signal.cancelled = true; };
    }, [selectedCampusId, selectedBuildingId]);

    const selectedBuilding = buildings.find((b) => b.id === selectedBuildingId) ?? null;
    const selectedCampus = campuses.find((c) => c.id === selectedCampusId) ?? null;
    const filteredBuildings = selectedCampusId
        ? buildings.filter((b) => b.campus?.id === selectedCampusId)
        : buildings;

    const visibleEmails = emails.filter((em) => normalize(em.email).includes(normalize(emailSearch)));

    const visibleCampusEmails = campusEmails.filter((em) => normalize(em.email).includes(normalize(emailSearch)));
    const campusGroups = Object.values(
        visibleCampusEmails.reduce((acc, email) => {
            const bid = email.building?.id ?? 'unknown';
            const bname = email.building?.name ?? 'Edificio desconocido';
            if (!acc[bid]) acc[bid] = { name: bname, emails: [] };
            acc[bid].emails.push(email);
            return acc;
        }, {})
    );

    const handleCampusChange = (id) => {
        setSelectedCampusId(id || '');
        if (id && selectedBuilding && selectedBuilding.campus?.id !== id) {
            setSelectedBuildingId('');
        }
    };

    const handleBuildingChange = (id) => {
        setSelectedBuildingId(id || '');
        const building = buildings.find((b) => b.id === id);
        if (building?.campus?.id) setSelectedCampusId(building.campus.id);
    };

    const validateEmail = (value) => {
        if (!value.trim()) return 'El correo electrónico es obligatorio';
        if (!EMAIL_REGEX.test(value.trim())) return 'El formato del correo no es válido';
        return '';
    };

    const handleEmailChange = (e) => {
        const val = e.target.value;
        setNewEmail(val);
        if (emailTouched) setEmailError(validateEmail(val));
    };

    const handleEmailBlur = () => {
        setEmailTouched(true);
        setEmailError(validateEmail(newEmail));
    };

    const handleAdd = async () => {
        setEmailTouched(true);
        const err = validateEmail(newEmail);
        if (err) { setEmailError(err); return; }
        if (emails.some((em) => normalize(em.email) === normalize(newEmail))) {
            setEmailError('Este correo ya está registrado');
            return;
        }

        setSaving(true);
        try {
            const created = await createBuildingEmail(selectedBuildingId, { email: newEmail.trim() });
            setEmails((prev) => [...prev, created]);
            setNewEmail('');
            setEmailError('');
            setEmailTouched(false);
        } catch (e) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(e, 'Error al agregar correo') });
        } finally {
            setSaving(false);
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') { e.preventDefault(); handleAdd(); }
    };

    const handleConfirmDelete = async () => {
        setDeleting(true);
        try {
            await deleteBuildingEmail(deletingEmail.id);
            setEmails((prev) => prev.filter((em) => em.id !== deletingEmail.id));
            setCampusEmails((prev) => prev.filter((em) => em.id !== deletingEmail.id));
            setDeletingEmail(null);
        } catch (e) {
            setDeletingEmail(null);
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(e, 'Error al eliminar correo') });
        } finally {
            setDeleting(false);
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

    return (
        <>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                <Box
                    sx={{
                        display: 'grid',
                        gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
                        gap: 2,
                    }}
                >
                    <SearchableSelect
                        label="Campus"
                        value={selectedCampusId}
                        onChange={handleCampusChange}
                        items={campuses}
                        getItemLabel={(c) => c.name}
                        getItemValue={(c) => c.id}
                        fullWidth
                        size="small"
                        clearable
                        disabled={loadingOptions}
                        sx={fieldSx}
                    />
                    <SearchableSelect
                        label="Edificio"
                        value={selectedBuildingId}
                        onChange={handleBuildingChange}
                        items={filteredBuildings}
                        getItemLabel={(b) => (b.campus?.name ? `${b.name} · ${b.campus.name}` : b.name)}
                        getItemValue={(b) => b.id}
                        fullWidth
                        size="small"
                        clearable
                        disabled={loadingOptions}
                        sx={fieldSx}
                    />
                </Box>

                {selectedBuildingId ? (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                            <ApartmentIcon sx={{ fontSize: 15, color: 'text.disabled' }} />
                            <Typography sx={{ fontSize: '0.78rem', color: 'text.secondary', fontWeight: 500 }}>
                                {selectedBuilding?.campus?.name && (
                                    <Box component="span" sx={{ color: 'text.disabled' }}>
                                        {selectedBuilding.campus.name} /{' '}
                                    </Box>
                                )}
                                {selectedBuilding?.name}
                            </Typography>
                            <Chip
                                label={`${emails.length} correo${emails.length !== 1 ? 's' : ''}`}
                                size="small"
                                sx={{
                                    height: 20,
                                    fontSize: '0.68rem',
                                    fontWeight: 600,
                                    bgcolor: `color-mix(in srgb, ${accentColor} 12%, transparent)`,
                                    color: accentColor,
                                    border: 'none',
                                }}
                            />
                            <Box sx={{ flex: 1 }} />
                            {selectedCampusId && (
                                <Button
                                    size="small"
                                    variant="text"
                                    startIcon={<VisibilityIcon sx={{ fontSize: 16 }} />}
                                    onClick={() => setCampusModalOpen(true)}
                                    sx={{
                                        textTransform: 'none',
                                        fontSize: 12.5,
                                        fontWeight: 600,
                                        color: accentColor,
                                        '&:hover': { bgcolor: `color-mix(in srgb, ${accentColor} 8%, transparent)` },
                                    }}
                                >
                                    Ver correos del campus
                                </Button>
                            )}
                        </Box>

                        <Divider />

                        {canManage && (
                            <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}>
                                <TextField
                                    label="Nuevo correo electrónico"
                                    value={newEmail}
                                    onChange={handleEmailChange}
                                    onBlur={handleEmailBlur}
                                    onKeyDown={handleKeyDown}
                                    fullWidth
                                    size="small"
                                    type="email"
                                    disabled={saving}
                                    error={emailTouched && !!emailError}
                                    helperText={emailTouched ? (emailError || ' ') : ' '}
                                    sx={fieldSx}
                                    placeholder="correo@ejemplo.com"
                                />
                                <Tooltip title="Agregar correo">
                                    <span>
                                        <IconButton
                                            onClick={handleAdd}
                                            disabled={saving}
                                            sx={{
                                                mt: 0.25,
                                                width: 38,
                                                height: 38,
                                                borderRadius: '10px',
                                                bgcolor: `color-mix(in srgb, ${accentColor} 12%, transparent)`,
                                                color: accentColor,
                                                border: '1px solid',
                                                borderColor: `color-mix(in srgb, ${accentColor} 25%, transparent)`,
                                                flexShrink: 0,
                                                transition: 'background 0.18s, border-color 0.18s',
                                                '&:hover': {
                                                    bgcolor: `color-mix(in srgb, ${accentColor} 20%, transparent)`,
                                                    borderColor: accentColor,
                                                },
                                                '&.Mui-disabled': { opacity: 0.45 },
                                            }}
                                        >
                                            <AddIcon sx={{ fontSize: 19 }} />
                                        </IconButton>
                                    </span>
                                </Tooltip>
                            </Box>
                        )}

                        <TextField
                            value={emailSearch}
                            onChange={(e) => setEmailSearch(e.target.value)}
                            fullWidth
                            size="small"
                            placeholder="Buscar correo..."
                            sx={fieldSx}
                            slotProps={{
                                input: {
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <SearchIcon sx={{ fontSize: 18, color: 'text.disabled' }} />
                                        </InputAdornment>
                                    ),
                                },
                            }}
                        />

                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75 }}>
                            {loadingEmails ? (
                                <EmailSkeleton />
                            ) : emails.length === 0 ? (
                                <Box
                                    sx={{
                                        display: 'flex',
                                        flexDirection: 'column',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        py: 4,
                                        gap: 1,
                                        borderRadius: '12px',
                                        border: '1px dashed',
                                        borderColor: 'divider',
                                    }}
                                >
                                    <EmailIcon sx={{ fontSize: 28, color: 'text.disabled', opacity: 0.5 }} />
                                    <Typography sx={{ fontSize: '0.8rem', color: 'text.disabled' }}>
                                        Sin correos registrados
                                    </Typography>
                                </Box>
                            ) : visibleEmails.length === 0 ? (
                                <Box
                                    sx={{
                                        display: 'flex',
                                        flexDirection: 'column',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        py: 4,
                                        gap: 1,
                                        borderRadius: '12px',
                                        border: '1px dashed',
                                        borderColor: 'divider',
                                    }}
                                >
                                    <SearchIcon sx={{ fontSize: 28, color: 'text.disabled', opacity: 0.5 }} />
                                    <Typography sx={{ fontSize: '0.8rem', color: 'text.disabled' }}>
                                        Sin coincidencias para "{emailSearch}"
                                    </Typography>
                                </Box>
                            ) : (
                                visibleEmails.map((email) => (
                                    <EmailRow
                                        key={email.id}
                                        email={email}
                                        canDelete={canDelete}
                                        onDelete={setDeletingEmail}
                                        accentColor={accentColor}
                                    />
                                ))
                            )}
                        </Box>
                    </Box>
                ) : selectedCampusId ? (
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                            <BusinessIcon sx={{ fontSize: 15, color: 'text.disabled' }} />
                            <Typography sx={{ fontSize: '0.78rem', color: 'text.secondary', fontWeight: 500 }}>
                                {selectedCampus?.name}
                            </Typography>
                            <Chip
                                label={`${campusEmails.length} correo${campusEmails.length !== 1 ? 's' : ''}`}
                                size="small"
                                sx={{
                                    height: 20,
                                    fontSize: '0.68rem',
                                    fontWeight: 600,
                                    bgcolor: `color-mix(in srgb, ${accentColor} 12%, transparent)`,
                                    color: accentColor,
                                    border: 'none',
                                }}
                            />
                            <Box sx={{ flex: 1 }} />
                            <Typography sx={{ fontSize: '0.72rem', color: 'text.disabled' }}>
                                Selecciona un edificio para gestionar sus correos
                            </Typography>
                        </Box>

                        <Divider />

                        <TextField
                            value={emailSearch}
                            onChange={(e) => setEmailSearch(e.target.value)}
                            fullWidth
                            size="small"
                            placeholder="Buscar correo..."
                            sx={fieldSx}
                            slotProps={{
                                input: {
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <SearchIcon sx={{ fontSize: 18, color: 'text.disabled' }} />
                                        </InputAdornment>
                                    ),
                                },
                            }}
                        />

                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.25 }}>
                            {loadingCampusEmails ? (
                                <EmailSkeleton />
                            ) : campusEmails.length === 0 ? (
                                <Box
                                    sx={{
                                        display: 'flex',
                                        flexDirection: 'column',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        py: 4,
                                        gap: 1,
                                        borderRadius: '12px',
                                        border: '1px dashed',
                                        borderColor: 'divider',
                                    }}
                                >
                                    <EmailIcon sx={{ fontSize: 28, color: 'text.disabled', opacity: 0.5 }} />
                                    <Typography sx={{ fontSize: '0.8rem', color: 'text.disabled' }}>
                                        Sin correos registrados en este campus
                                    </Typography>
                                </Box>
                            ) : campusGroups.length === 0 ? (
                                <Box
                                    sx={{
                                        display: 'flex',
                                        flexDirection: 'column',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        py: 4,
                                        gap: 1,
                                        borderRadius: '12px',
                                        border: '1px dashed',
                                        borderColor: 'divider',
                                    }}
                                >
                                    <SearchIcon sx={{ fontSize: 28, color: 'text.disabled', opacity: 0.5 }} />
                                    <Typography sx={{ fontSize: '0.8rem', color: 'text.disabled' }}>
                                        Sin coincidencias para "{emailSearch}"
                                    </Typography>
                                </Box>
                            ) : (
                                campusGroups.map((group, i) => (
                                    <Box key={group.name}>
                                        <CampusEmailGroup
                                            buildingName={group.name}
                                            emails={group.emails}
                                            accentColor={accentColor}
                                            canDelete={canDelete}
                                            onDelete={setDeletingEmail}
                                        />
                                        {i < campusGroups.length - 1 && <Divider sx={{ my: 0.5, opacity: 0.5 }} />}
                                    </Box>
                                ))
                            )}
                        </Box>
                    </Box>
                ) : (
                    <Box
                        sx={{
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            py: 7,
                            gap: 1.25,
                            borderRadius: '14px',
                            border: '1px dashed',
                            borderColor: 'divider',
                        }}
                    >
                        <ApartmentIcon sx={{ fontSize: 34, color: 'text.disabled', opacity: 0.5 }} />
                        <Typography sx={{ fontSize: '0.85rem', color: 'text.disabled' }}>
                            Selecciona un campus o un edificio para ver sus correos
                        </Typography>
                    </Box>
                )}
            </Box>

            <CampusEmailsModal
                open={campusModalOpen}
                onClose={() => setCampusModalOpen(false)}
                campus={selectedCampus ? { id: selectedCampus.id, name: selectedCampus.name } : null}
            />

            <DialogModal
                type="delete"
                open={!!deletingEmail}
                title="Eliminar correo"
                message={`¿Deseas eliminar "${deletingEmail?.email}"?\nEsta acción no se puede deshacer.`}
                onClose={() => !deleting && setDeletingEmail(null)}
                onConfirm={handleConfirmDelete}
                confirmLabel="Eliminar"
            />

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}
