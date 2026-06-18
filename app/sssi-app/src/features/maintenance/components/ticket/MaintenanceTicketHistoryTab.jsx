import { Avatar, Box, CircularProgress, Stack, Typography } from '@mui/material';
import AttachFileOutlinedIcon from '@mui/icons-material/AttachFileOutlined';
import FlagOutlinedIcon from '@mui/icons-material/FlagOutlined';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import CommentOutlinedIcon from '@mui/icons-material/CommentOutlined';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';

const TYPE_CONFIG = {
    ATTACHMENT_ADDED:     { icon: AttachFileOutlinedIcon,      color: '#2196f3', label: (h) => `agregó el adjunto ${h.newValue ?? ''}`.trim() },
    ATTACHMENT_REMOVED:   { icon: AttachFileOutlinedIcon,      color: '#f44336', label: (h) => `eliminó el adjunto ${h.oldValue ?? ''}`.trim() },
    PRIORITY_CHANGED:     { icon: FlagOutlinedIcon,            color: '#ff9800', label: (h) => `cambió la prioridad de ${h.oldValue ?? '?'} a ${h.newValue ?? '?'}` },
    STATUS_CHANGED:       { icon: SwapHorizOutlinedIcon,       color: '#9c27b0', label: (h) => `cambió el estado de ${h.oldValue ?? '?'} a ${h.newValue ?? '?'}` },
    ASSIGNED_ROLE_CHANGED:{ icon: PersonOutlineOutlinedIcon,   color: '#00bcd4', label: (h) => h.oldValue ? `cambió el rol asignado de "${h.oldValue}" a "${h.newValue}"` : `asignó el rol "${h.newValue}"` },
    EDITED:               { icon: EditOutlinedIcon,            color: '#607d8b', label: (h) => h.fieldName === 'title' ? 'editó el título' : h.fieldName === 'description' ? 'editó la descripción' : `editó ${h.fieldName ?? 'un campo'}` },
    COMMENT_ADDED:        { icon: CommentOutlinedIcon,         color: '#4caf50', label: () => 'agregó un comentario' },
    RESOLVED:             { icon: CheckCircleOutlineIcon,      color: '#4caf50', label: () => 'resolvió el ticket' },
    OTHER:                { icon: HelpOutlineIcon,             color: '#9e9e9e', label: (h) => h.fieldName ?? 'realizó un cambio' },
};

function getInitials(name) {
    if (!name) return '?';
    return name.split(' ').slice(0, 2).map((w) => w[0]).join('').toUpperCase();
}

function formatHistoryDate(value) {
    if (!value) return '';
    return new Date(value).toLocaleString('es-CR', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
    });
}

function HistoryEntry({ entry, isLast }) {
    const config = TYPE_CONFIG[entry.type] ?? TYPE_CONFIG.OTHER;
    const Icon = config.icon;
    const label = config.label(entry);
    const author = entry.authorName ?? entry.authorId ?? 'Sistema';

    return (
        <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start', position: 'relative' }}>
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', flexShrink: 0 }}>
                <Avatar sx={{ width: 28, height: 28, fontSize: 11, bgcolor: config.color }}>
                    {getInitials(author)}
                </Avatar>
                {!isLast && (
                    <Box sx={{ width: 2, flex: 1, minHeight: 20, bgcolor: 'divider', mt: 0.5 }} />
                )}
            </Box>
            <Box sx={{ flex: 1, minWidth: 0, pb: isLast ? 0 : 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 0.75, flexWrap: 'wrap' }}>
                    <Typography sx={{ fontWeight: 700, fontSize: 12.5 }}>{author}</Typography>
                    <Typography sx={{ color: 'text.disabled', fontSize: 11.5 }}>{formatHistoryDate(entry.createdAt)}</Typography>
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 0.25 }}>
                    <Icon sx={{ fontSize: 13, color: config.color, flexShrink: 0 }} />
                    <Typography sx={{ fontSize: 12.5, color: 'text.primary' }}>{label}</Typography>
                </Box>
            </Box>
        </Box>
    );
}

export default function MaintenanceTicketHistoryTab({ history = [], loading = false }) {
    if (loading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                <CircularProgress size={26} />
            </Box>
        );
    }

    if (!history.length) {
        return (
            <Box sx={{ py: 3 }}>
                <Typography sx={{ color: 'text.secondary', fontSize: 13, textAlign: 'center' }}>
                    No hay cambios registrados para este ticket.
                </Typography>
            </Box>
        );
    }

    return (
        <Stack>
            {history.map((entry, index) => (
                <HistoryEntry key={entry.id ?? index} entry={entry} isLast={index === history.length - 1} />
            ))}
        </Stack>
    );
}
