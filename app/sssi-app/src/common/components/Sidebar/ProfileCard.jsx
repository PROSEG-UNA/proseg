import { useState } from 'react';
import { Avatar, Box, Chip, Tooltip, Typography } from '@mui/material';
import PersonOutlineRoundedIcon from '@mui/icons-material/PersonOutlineRounded';
import GeneralModal from '../GeneralModal';

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

export function ProfileCard({ user, minimized = false }) {
    const [isProfileOpen, setIsProfileOpen] = useState(false);
    const displayName = resolveFullName(user);
    const email = normalizeText(user?.email);
    const role = resolveRole(user);
    const hasInfo = Boolean(displayName || email || role);
    const avatarFallback = resolveInitial(displayName);

    if (!hasInfo) return null;

    if (minimized) {
        const tooltipParts = [displayName, email, role].filter(Boolean);
        return (
            <>
                <Tooltip title={tooltipParts.join('\n')} placement="right" arrow>
                    <Avatar
                        onClick={() => setIsProfileOpen(true)}
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
                    open={isProfileOpen}
                    onClose={() => setIsProfileOpen(false)}
                    displayName={displayName}
                    email={email}
                    role={role}
                    avatarFallback={avatarFallback}
                />
            </>
        );
    }

    return (
        <>
            <Box
                role="button"
                tabIndex={0}
                onClick={() => setIsProfileOpen(true)}
                onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        setIsProfileOpen(true);
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
                open={isProfileOpen}
                onClose={() => setIsProfileOpen(false)}
                displayName={displayName}
                email={email}
                role={role}
                avatarFallback={avatarFallback}
            />
        </>
    );
}

function ProfileReadOnlyModal({ open, onClose, displayName, email, role, avatarFallback }) {
    return (
        <GeneralModal
            open={open}
            onClose={onClose}
            maxWidth="xs"
            icon={PersonOutlineRoundedIcon}
            title="Perfil del usuario"
            subtitle="Vista de información personal"
            primaryButton={{ label: 'Cerrar', onClick: onClose }}
        >
            <Box sx={{ px: 2.5, py: 2.25, display: 'grid', gap: 1.5 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
                    <Avatar
                        sx={(t) => ({
                            width: 46,
                            height: 46,
                            fontSize: '1rem',
                            fontWeight: 700,
                            color: '#fff',
                            bgcolor: t.vars.palette.tones.rose.headerBg,
                            boxShadow: t.palette.tones.rose.shadowResting,
                            flexShrink: 0,
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
            </Box>
        </GeneralModal>
    );
}

export default ProfileCard;
