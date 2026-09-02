import { useContext, useEffect, useRef, useState } from 'react';
import {
    Alert,
    Avatar,
    Box,
    Button,
    Chip,
    Tooltip,
    Typography,
} from '@mui/material';
import PhotoCameraOutlinedIcon from '@mui/icons-material/PhotoCameraOutlined';
import PersonOutlineRoundedIcon from '@mui/icons-material/PersonOutlineRounded';
import GeneralModal from '../GeneralModal';
import { uploadArchiveFile } from '../../services/archiveService';
import { updateCurrentUserProfileImage } from '../../../features/auth/services/authService';
import { getFriendlyApiErrorMessage } from '../../utils';
import { AuthContext } from '../../context/AuthContext';
import { ChangePasswordModal } from '../../../features/security/components/ChangePasswordModal';

const normalizeText = (value) => (typeof value === 'string' ? value.trim() : '');

const toRoleLabel = (value) =>
    normalizeText(value)
        .replace(/^ROLE_/i, '')
        .split('_')
        .filter(Boolean)
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ');

const resolveRole = (user) => {
    if (!user || typeof user !== 'object') return '';

    const explicitRoleCandidates = [
        user.role,
        ...(Array.isArray(user.roles) ? user.roles : []),
    ]
        .map((value) => {
            if (typeof value === 'string') return value;
            if (value && typeof value === 'object') {
                return value.name || value.role || value.authority || '';
            }
            return '';
        })
        .filter(Boolean);

    if (explicitRoleCandidates.length > 0) {
        const explicitRole = explicitRoleCandidates.find((role) => /^ROLE_/i.test(role)) || explicitRoleCandidates[0];
        return toRoleLabel(explicitRole);
    }

    const authorityCandidates = [
        ...(Array.isArray(user.authorities) ? user.authorities : []),
        ...(Array.isArray(user.permissions) ? user.permissions : []),
    ]
        .filter((value) => typeof value === 'string')
        .map((value) => value.trim())
        .filter(Boolean);

    const roleAuthority = authorityCandidates.find((authority) => /^ROLE_/i.test(authority));
    if (roleAuthority) return toRoleLabel(roleAuthority);

    return '';
};

const resolveFullName = (user) => {
    const firstName = normalizeText(user?.firstName);
    const lastName = normalizeText(user?.lastName);
    const fullName = `${firstName} ${lastName}`.trim();
    return fullName || normalizeText(user?.username);
};

const resolveInitial = (displayName) => {
    const text = normalizeText(displayName);
    if (!text) return '';
    return text.charAt(0).toUpperCase();
};

const resolveArchiveImageUrl = (objectName) => {
    const normalizedObjectName = normalizeText(objectName);
    if (!normalizedObjectName) return '';
    return `/api/v1/archive/files/${normalizedObjectName}`;
};

const resolveProfileImageUrl = (user) =>
    normalizeText(user?.profileImageUrl) || resolveArchiveImageUrl(user?.profileImageObjectName);

export function ProfileCard({ user, minimized = false }) {
    const [isProfileOpen, setIsProfileOpen] = useState(false);
    const [isPasswordChangeOpen, setIsPasswordChangeOpen] = useState(false);
    const [pendingPasswordChange, setPendingPasswordChange] = useState(false);
    const [profileModalVersion, setProfileModalVersion] = useState(0);
    const [latestProfileImageUrl, setLatestProfileImageUrl] = useState('');
    const passwordChangeTimerRef = useRef(null);
    const displayName = resolveFullName(user);
    const email = normalizeText(user?.email);
    const role = resolveRole(user);
    const profileImageUrl = latestProfileImageUrl || resolveProfileImageUrl(user);
    const hasInfo = Boolean(displayName || email || role);
    const avatarFallback = resolveInitial(displayName);
    const openProfileModal = () => setIsProfileOpen(true);
    const openPasswordChangeModal = () => {
        setPendingPasswordChange(true);
        setIsProfileOpen(false);
        if (passwordChangeTimerRef.current) {
            clearTimeout(passwordChangeTimerRef.current);
        }
        passwordChangeTimerRef.current = setTimeout(() => {
            setPendingPasswordChange(false);
            setIsPasswordChangeOpen(true);
            passwordChangeTimerRef.current = null;
        }, 300);
    };
    const closeProfileModal = () => {
        setIsProfileOpen(false);
        setPendingPasswordChange(false);
        setProfileModalVersion((current) => current + 1);
        setIsPasswordChangeOpen(false);
        if (passwordChangeTimerRef.current) {
            clearTimeout(passwordChangeTimerRef.current);
            passwordChangeTimerRef.current = null;
        }
    };

    useEffect(() => () => {
        if (passwordChangeTimerRef.current) {
            clearTimeout(passwordChangeTimerRef.current);
        }
    }, []);

    if (!hasInfo) return null;

    if (minimized) {
        const tooltipParts = [displayName, email, role].filter(Boolean);
        return (
            <>
                <Tooltip title={tooltipParts.join('\n')} placement="right" arrow>
                    <Avatar
                        src={profileImageUrl || undefined}
                        onClick={openProfileModal}
                        sx={(t) => ({
                            width: 42,
                            height: 42,
                            mx: 'auto',
                            mb: 1.25,
                            fontSize: '0.95rem',
                            fontWeight: 700,
                            color: '#fff',
                            bgcolor: t.vars.palette.tones.rose.headerBg,
                            boxShadow: t.palette.tones.rose.shadowResting,
                            cursor: 'pointer',
                            transition: 'transform 0.2s ease',
                            '&:hover': { transform: 'translateY(-1px)' },
                        })}
                    >
                        {avatarFallback || <PersonOutlineRoundedIcon sx={{ fontSize: 20 }} />}
                    </Avatar>
                </Tooltip>
                <ProfileReadOnlyModal
                    key={profileModalVersion}
                    open={isProfileOpen}
                    onClose={closeProfileModal}
                    onOpenPasswordChange={openPasswordChangeModal}
                    onProfileImageSaved={setLatestProfileImageUrl}
                    user={user}
                    displayName={displayName}
                    email={email}
                    role={role}
                    profileImageUrl={profileImageUrl}
                    avatarFallback={avatarFallback}
                />
                <ChangePasswordModal
                    open={isPasswordChangeOpen}
                    onClose={() => setIsPasswordChangeOpen(false)}
                />
            </>
        );
    }

    return (
        <>
            <Box
                role="button"
                tabIndex={0}
                onClick={openProfileModal}
                onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        openProfileModal();
                    }
                }}
                sx={(t) => ({
                    mb: 1.5,
                    p: 1.5,
                    borderRadius: '12px',
                    border: '1px solid',
                    borderColor: 'divider',
                    cursor: 'pointer',
                    background: 'hsla(220, 20%, 50%, 0.04)',
                    transition: 'border-color 0.2s ease, background 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease',
                    '&:hover': {
                        borderColor: t.vars.palette.tones.rose.ring,
                        background: t.vars.palette.tones.rose.softSubtle,
                        transform: 'translateY(-1px)',
                        boxShadow: t.palette.tones.rose.shadowResting,
                    },
                    '&:focus-visible': {
                        outline: 'none',
                        borderColor: t.vars.palette.tones.rose.ring,
                        boxShadow: t.palette.tones.rose.shadowResting,
                    },
                    ...t.applyStyles('dark', {
                        background: 'hsla(220, 20%, 80%, 0.03)',
                    }),
                })}
            >
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.25, minWidth: 0 }}>
                    <Avatar
                        src={profileImageUrl || undefined}
                        sx={(t) => ({
                            width: 42,
                            height: 42,
                            fontSize: '0.95rem',
                            fontWeight: 700,
                            color: '#fff',
                            bgcolor: t.vars.palette.tones.rose.headerBg,
                            boxShadow: t.palette.tones.rose.shadowResting,
                            flexShrink: 0,
                        })}
                    >
                        {avatarFallback || <PersonOutlineRoundedIcon sx={{ fontSize: 20 }} />}
                    </Avatar>
                    <Box sx={{ minWidth: 0, display: 'grid', gap: 0.4, flex: 1 }}>
                        {displayName && (
                            <Typography noWrap sx={{ fontSize: '0.86rem', fontWeight: 700, color: 'text.primary' }}>
                                {displayName}
                            </Typography>
                        )}
                        {email && (
                            <Typography
                                sx={{
                                    fontSize: '0.76rem',
                                    color: 'text.secondary',
                                    lineHeight: 1.35,
                                    wordBreak: 'break-word',
                                    overflowWrap: 'anywhere',
                                }}
                            >
                                {email}
                            </Typography>
                        )}
                    </Box>
                </Box>
                {role && (
                    <Chip
                        label={role}
                        sx={{
                            mt: 1,
                            maxWidth: '100%',
                            '& .MuiChip-label': {
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                            },
                        }}
                    />
                )}
            </Box>
            <ProfileReadOnlyModal
                key={profileModalVersion}
                open={isProfileOpen}
                onClose={closeProfileModal}
                onOpenPasswordChange={openPasswordChangeModal}
                onProfileImageSaved={setLatestProfileImageUrl}
                user={user}
                displayName={displayName}
                email={email}
                role={role}
                profileImageUrl={profileImageUrl}
                avatarFallback={avatarFallback}
            />
            <ChangePasswordModal
                open={isPasswordChangeOpen}
                onClose={() => setIsPasswordChangeOpen(false)}
            />
        </>
    );
}

function ProfileReadOnlyModal({
    open,
    onClose,
    onOpenPasswordChange,
    onProfileImageSaved,
    user,
    displayName,
    email,
    role,
    profileImageUrl,
    avatarFallback,
}) {
    const { refreshAuth } = useContext(AuthContext);
    const [selectedFile, setSelectedFile] = useState(null);
    const [previewUrl, setPreviewUrl] = useState('');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [saving, setSaving] = useState(false);
    const [isImagePreviewOpen, setIsImagePreviewOpen] = useState(false);
    const activeImageUrl = previewUrl || profileImageUrl;
    const canPreviewImage = Boolean(activeImageUrl);

    useEffect(() => () => {
        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
        }
    }, [previewUrl]);

    const handleFileChange = (event) => {
        const file = event.target.files?.[0];
        if (!file) return;

        if (!file.type.startsWith('image/')) {
            setError('Selecciona un archivo de imagen válido.');
            setSuccess('');
            return;
        }

        if (file.size > 10 * 1024 * 1024) {
            setError('La imagen supera el tamaño máximo permitido de 10 MB.');
            setSuccess('');
            return;
        }

        const nextPreview = URL.createObjectURL(file);
        if (previewUrl) {
            URL.revokeObjectURL(previewUrl);
        }

        setSelectedFile(file);
        setPreviewUrl(nextPreview);
        setError('');
        setSuccess('');
    };

    const handleSaveProfileImage = async () => {
        if (!selectedFile || !user?.id || saving) return;

        setSaving(true);
        setError('');
        setSuccess('');
        try {
            const uploadResponse = await uploadArchiveFile({
                file: selectedFile,
                folder: `users/${user.id}/profile`,
            });
            const objectName = uploadResponse?.data?.objectName;
            if (!objectName) {
                throw new Error('No se recibió el objeto de imagen desde el servicio de archivos.');
            }

            await updateCurrentUserProfileImage(objectName);
            await refreshAuth();
            onProfileImageSaved?.(`${resolveArchiveImageUrl(objectName)}?v=${Date.now()}`);

            setSelectedFile(null);
            if (previewUrl) {
                URL.revokeObjectURL(previewUrl);
                setPreviewUrl('');
            }
            setSuccess('Imagen de perfil actualizada correctamente.');
        } catch (apiError) {
            setError(getFriendlyApiErrorMessage(apiError, 'No se pudo actualizar la imagen de perfil.'));
        } finally {
            setSaving(false);
        }
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="xs"
                icon={PersonOutlineRoundedIcon}
                title="Perfil del usuario"
                subtitle="Información y edición de perfil"
                secondaryButton={{ label: 'Cerrar', onClick: onClose }}
                primaryButton={{
                    label: 'Guardar imagen',
                    onClick: handleSaveProfileImage,
                    disabled: !selectedFile || saving,
                    loading: saving,
                    startIcon: <PhotoCameraOutlinedIcon sx={{ fontSize: 16 }} />,
                }}
            >
                <Box sx={{ px: 2.5, py: 2.25, display: 'grid', gap: 1.75 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
                        <Avatar
                            src={activeImageUrl || undefined}
                            onClick={canPreviewImage ? () => setIsImagePreviewOpen(true) : undefined}
                            sx={(t) => ({
                                width: 46,
                                height: 46,
                                fontSize: '1rem',
                                fontWeight: 700,
                                color: '#fff',
                                bgcolor: t.vars.palette.tones.rose.headerBg,
                                boxShadow: t.palette.tones.rose.shadowResting,
                                flexShrink: 0,
                                cursor: canPreviewImage ? 'zoom-in' : 'default',
                                transition: canPreviewImage ? 'transform 0.2s ease, box-shadow 0.2s ease' : undefined,
                                '&:hover': canPreviewImage
                                    ? {
                                        transform: 'scale(1.04)',
                                        boxShadow: t.palette.tones.rose.shadowHover,
                                    }
                                    : undefined,
                            })}
                        >
                            {avatarFallback || <PersonOutlineRoundedIcon sx={{ fontSize: 22 }} />}
                        </Avatar>
                        <Box sx={{ minWidth: 0, display: 'grid', gap: 0.35 }}>
                            {displayName && (
                                <Typography sx={{ fontSize: '0.92rem', fontWeight: 700, color: 'text.primary', lineHeight: 1.25 }}>
                                    {displayName}
                                </Typography>
                            )}
                            {email && (
                                <Typography
                                    sx={{
                                        fontSize: '0.82rem',
                                        color: 'text.secondary',
                                        lineHeight: 1.35,
                                        wordBreak: 'break-word',
                                        overflowWrap: 'anywhere',
                                    }}
                                >
                                    {email}
                                </Typography>
                            )}
                        </Box>
                    </Box>
                    {role && (
                        <Box sx={{ display: 'grid', gap: 0.5 }}>
                            <Typography sx={{ fontSize: '0.72rem', letterSpacing: '0.06em', textTransform: 'uppercase', color: 'text.disabled', fontWeight: 700 }}>
                                Rol
                            </Typography>
                            <Chip label={role} sx={{ width: 'fit-content', maxWidth: '100%' }} />
                        </Box>
                    )}
                    <Box sx={{ display: 'grid', gap: 0.75 }}>
                        <Typography sx={{ fontSize: '0.72rem', letterSpacing: '0.06em', textTransform: 'uppercase', color: 'text.disabled', fontWeight: 700 }}>
                            Seguridad
                        </Typography>
                        <Button
                            variant="outlined"
                            onClick={onOpenPasswordChange}
                            sx={{ justifyContent: 'flex-start', textTransform: 'none' }}
                        >
                            Cambiar contraseña
                        </Button>
                    </Box>
                    <Box sx={{ display: 'grid', gap: 0.75 }}>
                        <Typography sx={{ fontSize: '0.72rem', letterSpacing: '0.06em', textTransform: 'uppercase', color: 'text.disabled', fontWeight: 700 }}>
                            Imagen de perfil
                        </Typography>
                        <Button
                            variant="outlined"
                            component="label"
                            startIcon={<PhotoCameraOutlinedIcon sx={{ fontSize: 18 }} />}
                            disabled={saving}
                            sx={{ justifyContent: 'flex-start', textTransform: 'none' }}
                        >
                            Seleccionar imagen
                            <input type="file" accept="image/*" hidden onChange={handleFileChange} />
                        </Button>
                        {selectedFile && (
                            <Typography sx={{ fontSize: '0.77rem', color: 'text.secondary', wordBreak: 'break-word' }}>
                                {selectedFile.name}
                            </Typography>
                        )}
                        {error && <Alert severity="error">{error}</Alert>}
                        {success && <Alert severity="success">{success}</Alert>}
                    </Box>
                </Box>
            </GeneralModal>

            <GeneralModal
                open={isImagePreviewOpen}
                onClose={() => setIsImagePreviewOpen(false)}
                maxWidth="md"
                icon={PersonOutlineRoundedIcon}
                title="Foto de perfil"
                subtitle="Vista ampliada"
                primaryButton={{ label: 'Cerrar', onClick: () => setIsImagePreviewOpen(false) }}
            >
                <Box sx={{ p: 2, display: 'flex', justifyContent: 'center', alignItems: 'center', bgcolor: 'background.paperWarm' }}>
                    {activeImageUrl && (
                        <Box
                            component="img"
                            src={activeImageUrl}
                            alt={displayName || 'Foto de perfil'}
                            sx={{
                                width: '100%',
                                maxWidth: 760,
                                maxHeight: '70vh',
                                objectFit: 'contain',
                                borderRadius: '12px',
                                border: '1px solid',
                                borderColor: 'divider',
                            }}
                        />
                    )}
                </Box>
            </GeneralModal>
        </>
    );
}

export default ProfileCard;
