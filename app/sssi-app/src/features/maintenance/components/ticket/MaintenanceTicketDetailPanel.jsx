import { useEffect, useMemo, useState } from 'react';
import { Avatar, Box, Button, Chip, Divider, Stack, TextField, Typography } from '@mui/material';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined';
import { formatDateTime } from '../../maintenanceUtils';
import { addMaintenanceTicketComment } from '../../services/ticketsService';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';

const isImageType = (type) => typeof type === 'string' && type.startsWith('image/');

function InfoRow({ label, value }) {
    return (
        <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'baseline' }}>
            <Typography sx={{ minWidth: 110, fontSize: 11.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                {label}
            </Typography>
            <Typography sx={{ fontSize: 13.25, fontWeight: 600, color: 'text.primary' }}>
                {value || '—'}
            </Typography>
        </Box>
    );
}

export default function MaintenanceTicketDetailPanel({ ticket }) {
    const { hasPermission } = usePermissions();
    const canComment = hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.COMMENT);
    const [commentText, setCommentText] = useState('');
    const [commentSaving, setCommentSaving] = useState(false);
    const [commentError, setCommentError] = useState('');
    const [comments, setComments] = useState(ticket?.comments ?? []);

    useEffect(() => {
        setComments(ticket?.comments ?? []);
        setCommentText('');
        setCommentError('');
    }, [ticket?.id, ticket?.comments]);

    const sortedComments = useMemo(
        () => [...(comments ?? [])].sort((a, b) => new Date(a.createdAt || 0) - new Date(b.createdAt || 0)),
        [comments]
    );

    const handleAddComment = async () => {
        const content = commentText.trim();
        if (!content) {
            setCommentError('Escribe un comentario para continuar.');
            return;
        }

        if (!ticket?.id) return;

        setCommentSaving(true);
        setCommentError('');
        try {
            const created = await addMaintenanceTicketComment(ticket.id, content);
            setComments((prev) => [...(prev ?? []), created]);
            setCommentText('');
        } catch (error) {
            setCommentError(error?.response?.data?.message ?? error?.message ?? 'No se pudo agregar el comentario');
        } finally {
            setCommentSaving(false);
        }
    };

    if (!ticket) return null;

    return (
        <Box sx={{ p: 2.5, display: 'flex', flexDirection: 'column', gap: 2.5 }}>
            <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <DescriptionOutlinedIcon sx={{ fontSize: 16, color: 'text.disabled' }} />
                    <Typography sx={{ fontSize: 11.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        Información del ticket
                    </Typography>
                </Box>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75, pl: 2.5 }}>
                    <InfoRow label="Descripción" value={ticket.description} />
                    <InfoRow label="Sede" value={ticket.siteName} />
                    <InfoRow label="Edificio" value={ticket.buildingName} />
                    <InfoRow label="Piso" value={ticket.floorName} />
                    <InfoRow label="Ubicación" value={ticket.locationDescription} />
                    <InfoRow label="Creado por" value={ticket.createdBy} />
                    <InfoRow label="Rol asignado" value={ticket.assignedRole} />
                    <InfoRow label="Creado" value={formatDateTime(ticket.createdAt)} />
                    <InfoRow label="Actualizado" value={formatDateTime(ticket.updatedAt)} />
                </Box>
            </Box>

            <Divider />

            <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <Typography sx={{ fontSize: 11.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        Discusión y comentarios
                    </Typography>
                </Box>

                <Box sx={{ pl: 2.5, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                    <Box
                        sx={{
                            border: '1px solid',
                            borderColor: 'divider',
                            borderRadius: '12px',
                            p: 1.25,
                            background: 'linear-gradient(180deg, rgba(255,255,255,0.55) 0%, rgba(245,249,255,0.55) 100%)',
                        }}
                    >
                        <TextField
                            label="Agregar comentario"
                            value={commentText}
                            onChange={(e) => setCommentText(e.target.value)}
                            disabled={!canComment || commentSaving}
                            multiline
                            minRows={3}
                            fullWidth
                            size="small"
                            helperText={commentError || (canComment ? 'Comparte avance, bloqueo o contexto del ticket.' : 'No tienes permiso para comentar.')}
                            error={!!commentError}
                        />
                        <Box sx={{ mt: 1, display: 'flex', justifyContent: 'flex-end' }}>
                            <Button
                                variant="contained"
                                size="small"
                                onClick={handleAddComment}
                                disabled={!canComment || commentSaving}
                            >
                                {commentSaving ? 'Publicando...' : 'Publicar comentario'}
                            </Button>
                        </Box>
                    </Box>

                    {(sortedComments ?? []).length === 0 ? (
                        <Typography sx={{ color: 'text.secondary', fontSize: 13.25 }}>
                            Todavía no hay comentarios en este ticket.
                        </Typography>
                    ) : (
                        <Stack spacing={1.25}>
                            {sortedComments.map((comment) => (
                                <Box
                                    key={comment.id}
                                    sx={{
                                        border: '1px solid',
                                        borderColor: 'divider',
                                        borderRadius: '12px',
                                        p: 1.25,
                                        backgroundColor: 'background.paper',
                                    }}
                                >
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.75 }}>
                                        <Avatar sx={{ width: 26, height: 26, fontSize: 11, bgcolor: 'primary.main' }}>
                                            {(comment.authorName ?? comment.authorId ?? '?').charAt(0).toUpperCase()}
                                        </Avatar>
                                        <Typography sx={{ fontSize: 12.5, fontWeight: 700 }}>
                                            {comment.authorName ?? comment.authorId ?? 'Usuario'}
                                        </Typography>
                                        <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>
                                            {formatDateTime(comment.createdAt)}
                                        </Typography>
                                    </Box>
                                    <Typography sx={{ whiteSpace: 'pre-wrap', fontSize: 13.25, color: 'text.primary' }}>
                                        {comment.content}
                                    </Typography>
                                </Box>
                            ))}
                        </Stack>
                    )}
                </Box>
            </Box>

            <Divider />

            <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <Inventory2OutlinedIcon sx={{ fontSize: 16, color: 'text.disabled' }} />
                    <Typography sx={{ fontSize: 11.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        Activos vinculados
                    </Typography>
                </Box>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, pl: 2.5 }}>
                    {(ticket.assets ?? []).length === 0 ? (
                        <Typography sx={{ color: 'text.secondary', fontSize: 13.25 }}>No hay activos asociados a este ticket.</Typography>
                    ) : (
                        ticket.assets.map((asset) => (
                            <Chip
                                key={asset.id}
                                label={`${asset.assetNumber ?? asset.assetId} · ${asset.assetName ?? 'Sin nombre'}`}
                                size="small"
                                variant="outlined"
                                color="secondary"
                            />
                        ))
                    )}
                </Box>
            </Box>

            <Divider />

            <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <ImageOutlinedIcon sx={{ fontSize: 16, color: 'text.disabled' }} />
                    <Typography sx={{ fontSize: 11.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        Archivos adjuntos
                    </Typography>
                </Box>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, pl: 2.5 }}>
                    {(ticket.photos ?? []).length === 0 ? (
                        <Typography sx={{ color: 'text.secondary', fontSize: 13.25 }}>No hay archivos adjuntos para este ticket.</Typography>
                    ) : (
                        ticket.photos.map((photo) => (
                            <Box
                                key={photo.id}
                                sx={{
                                    border: '1px solid',
                                    borderColor: 'divider',
                                    borderRadius: '12px',
                                    p: 1.5,
                                    display: 'flex',
                                    flexDirection: 'column',
                                    gap: 0.5,
                                }}
                            >
                                <Typography sx={{ fontWeight: 700, fontSize: 13.25 }} noWrap>
                                    {photo.fileName || photo.objectName}
                                </Typography>
                                {photo.imageUrl && isImageType(photo.contentType) ? (
                                    <Box
                                        component="img"
                                        src={photo.imageUrl}
                                        alt={photo.fileName || photo.objectName}
                                        sx={{
                                            width: '100%',
                                            maxHeight: 180,
                                            objectFit: 'cover',
                                            borderRadius: '10px',
                                            border: '1px solid',
                                            borderColor: 'divider',
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
                                        sx={{ alignSelf: 'flex-start' }}
                                    >
                                        Descargar
                                    </Button>
                                ) : null}
                            </Box>
                        ))
                    )}
                </Box>
            </Box>
        </Box>
    );
}