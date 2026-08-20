import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Box, Typography, Skeleton, Dialog, IconButton, Button } from '@mui/material';
import RouterIcon from '@mui/icons-material/Router';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import WidgetsOutlinedIcon from '@mui/icons-material/WidgetsOutlined';
import AddIcon from '@mui/icons-material/Add';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteIcon from '@mui/icons-material/Delete';
import CloseIcon from '@mui/icons-material/Close';
import { deleteAssetComponent } from '../../services/assetComponentsService';
import AssetComponentFormModal from './AssetComponentFormModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { queryKeys } from '../../../../common/query';
import { assetComponentsQueryOptions, assetDetailQueryOptions } from './assetDetailQueries.js';

function InfoRow({ label, value }) {
    return (
        <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1.5 }}>
            <Typography
                sx={{
                    fontSize: '0.7rem',
                    fontWeight: 600,
                    color: 'text.disabled',
                    minWidth: 44,
                    textTransform: 'uppercase',
                    letterSpacing: '0.08em',
                }}
            >
                {label}
            </Typography>
            <Typography
                sx={{
                    fontSize: '0.82rem',
                    fontWeight: 600,
                    color: 'text.primary',
                    fontFamily: '"Roboto Mono", monospace',
                }}
            >
                {value}
            </Typography>
        </Box>
    );
}

function SectionHeader({ icon: Icon, label }) {
    return (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, mb: 1 }}>
            <Icon sx={{ fontSize: 14, color: 'text.disabled' }} />
            <Typography
                sx={{
                    fontSize: '0.68rem',
                    fontWeight: 700,
                    color: 'text.disabled',
                    textTransform: 'uppercase',
                    letterSpacing: '0.1em',
                }}
            >
                {label}
            </Typography>
        </Box>
    );
}

export default function AssetDetailPanel({ assetId, canManageAssets = false }) {
    const [lightbox, setLightbox] = useState(null);
    const [componentModal, setComponentModal] = useState(null);
    const [componentToDelete, setComponentToDelete] = useState(null);
    const [alert, setAlert] = useState(null);
    const queryClient = useQueryClient();

    const detailQuery = useQuery({
        ...assetDetailQueryOptions(assetId),
        enabled: Boolean(assetId),
    });

    const componentsQuery = useQuery({
        ...assetComponentsQueryOptions(assetId),
        enabled: Boolean(assetId),
    });

    const invalidateComponents = () => queryClient.invalidateQueries({
        queryKey: queryKeys.inventory.assetComponents(assetId),
    });

    const deleteComponentMutation = useMutation({
        mutationFn: (componentId) => deleteAssetComponent(componentId),
        onSuccess: async () => {
            setComponentToDelete(null);
            setAlert({ type: 'success', message: 'Componente eliminado correctamente' });
            await invalidateComponents();
        },
        onError: (deleteError) => {
            const data = deleteError?.response?.data;
            setAlert({ type: 'error', message: data?.message ?? deleteError?.message ?? 'No se pudo eliminar el componente' });
        },
    });

    const netIface = detailQuery.data?.netIface ?? null;
    const images = detailQuery.data?.images ?? [];
    const components = componentsQuery.data ?? [];
    const deletingComponent = deleteComponentMutation.isPending;
    const loading = Boolean(assetId) && (detailQuery.isPending || componentsQuery.isPending);

    if (loading) {
        return (
            <Box sx={{ p: 2.5, display: 'flex', flexDirection: 'column', gap: 0.75 }}>
                <Skeleton variant="text" width={120} height={14} />
                <Skeleton variant="text" width={200} height={18} />
                <Skeleton variant="text" width={220} height={18} />
            </Box>
        );
    }

    const hasNetIface = !!netIface;
    const hasImages   = images.length > 0;

    const handleComponentSaved = async () => {
        try {
            await invalidateComponents();
            setAlert({ type: 'success', message: 'Componente guardado correctamente' });
        } catch {
            setAlert({ type: 'error', message: 'No se pudieron cargar los componentes' });
        }
    };

    const handleDeleteComponent = () => {
        if (!componentToDelete) return;
        deleteComponentMutation.mutate(componentToDelete.id);
    };

    return (
        <>
            <Box sx={{ p: 2.5, display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                {hasNetIface && (
                    <Box>
                        <SectionHeader icon={RouterIcon} label="IP y MAC" />
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.625, pl: 2.5 }}>
                            {netIface.ipAddress  && <InfoRow label="IP"  value={netIface.ipAddress} />}
                            {netIface.macAddress && <InfoRow label="MAC" value={netIface.macAddress} />}
                        </Box>
                    </Box>
                )}

                {hasImages && (
                    <Box>
                        <SectionHeader icon={ImageOutlinedIcon} label="Imágenes" />
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, pl: 2.5 }}>
                            {images.map((img) => (
                                <Box
                                    key={img.id}
                                    component="img"
                                    src={img.imageUrl}
                                    alt={img.caption || ''}
                                    onClick={() => setLightbox(img.imageUrl)}
                                    sx={{
                                        width: 80,
                                        height: 80,
                                        objectFit: 'cover',
                                        borderRadius: '8px',
                                        border: '1px solid',
                                        borderColor: 'divider',
                                        cursor: 'pointer',
                                        transition: 'opacity 0.15s',
                                        '&:hover': { opacity: 0.8 },
                                    }}
                                />
                            ))}
                        </Box>
                    </Box>
                )}

                <Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1, mb: 1 }}>
                        <SectionHeader icon={WidgetsOutlinedIcon} label="Componentes Asociados" />
                        {canManageAssets && (
                            <Button
                                size="small"
                                variant="outlined"
                                startIcon={<AddIcon sx={{ fontSize: 16 }} />}
                                onClick={() => setComponentModal({ mode: 'create', component: null })}
                                sx={{ textTransform: 'none', mt: -1 }}
                            >
                                Agregar
                            </Button>
                        )}
                    </Box>

                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, pl: 2.5 }}>
                        {components.length === 0 ? (
                            <Typography sx={{ color: 'text.secondary', fontSize: 13.25 }}>
                                Sin componentes asociados.
                            </Typography>
                        ) : (
                            components.map((component) => (
                                <Box
                                    key={component.id}
                                    sx={{
                                        display: 'flex',
                                        alignItems: 'flex-start',
                                        justifyContent: 'space-between',
                                        gap: 1.5,
                                        border: '1px solid',
                                        borderColor: 'divider',
                                        borderRadius: '10px',
                                        p: 1.5,
                                    }}
                                >
                                    <Box sx={{ minWidth: 0, display: 'flex', flexDirection: 'column', gap: 0.25 }}>
                                        <Typography sx={{ fontSize: 13.25, fontWeight: 700 }}>{component.name || '—'}</Typography>
                                        <Typography sx={{ fontSize: 12.5, color: 'text.secondary' }}>
                                            Cantidad: {component.quantity ?? '—'}
                                        </Typography>
                                        <Typography sx={{ fontSize: 12.5, color: 'text.secondary' }}>
                                            Ubicación: {component.location || '—'}
                                        </Typography>
                                        <Typography sx={{ fontSize: 12.5, color: 'text.secondary', whiteSpace: 'pre-wrap' }}>
                                            Observaciones: {component.observations || '—'}
                                        </Typography>
                                    </Box>

                                    {canManageAssets && (
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                                            <IconButton
                                                size="small"
                                                onClick={() => setComponentModal({ mode: 'edit', component })}
                                                sx={{ color: 'text.secondary' }}
                                            >
                                                <EditOutlinedIcon sx={{ fontSize: 18 }} />
                                            </IconButton>
                                            <IconButton
                                                size="small"
                                                onClick={() => setComponentToDelete(component)}
                                                sx={{ color: 'error.main' }}
                                            >
                                                <DeleteIcon sx={{ fontSize: 18 }} />
                                            </IconButton>
                                        </Box>
                                    )}
                                </Box>
                            ))
                        )}
                    </Box>
                </Box>
            </Box>

            <Dialog
                open={!!lightbox}
                onClose={() => setLightbox(null)}
                maxWidth={false}
                slotProps={{
                    backdrop: { sx: { bgcolor: 'rgba(0,0,0,0.88)' } },
                    paper: {
                        sx: {
                            bgcolor: 'transparent',
                            boxShadow: 'none',
                            overflow: 'visible',
                        },
                    },
                }}
            >
                <Box sx={{ position: 'relative' }}>
                    <Box
                        component="img"
                        src={lightbox ?? ''}
                        sx={{
                            maxWidth: '90vw',
                            maxHeight: '90vh',
                            objectFit: 'contain',
                            borderRadius: '10px',
                            display: 'block',
                        }}
                    />
                    <IconButton
                        onClick={() => setLightbox(null)}
                        sx={{
                            position: 'absolute',
                            top: -16,
                            right: -16,
                            bgcolor: 'rgba(0,0,0,0.6)',
                            color: '#fff',
                            p: 0.5,
                            '&:hover': { bgcolor: 'rgba(0,0,0,0.85)' },
                        }}
                    >
                        <CloseIcon sx={{ fontSize: 18 }} />
                    </IconButton>
                </Box>
            </Dialog>

            <AssetComponentFormModal
                open={!!componentModal}
                assetId={assetId}
                componentData={componentModal?.component ?? null}
                onClose={() => setComponentModal(null)}
                onSaved={handleComponentSaved}
            />

            <DialogModal
                type="delete"
                open={!!componentToDelete}
                title="Eliminar componente"
                message={`¿Seguro que deseas eliminar el componente "${componentToDelete?.name || ''}"?`}
                onClose={() => {
                    if (!deletingComponent) setComponentToDelete(null);
                }}
                onConfirm={handleDeleteComponent}
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
