import { useState, useEffect } from 'react';
import { Box, Typography, Skeleton, Dialog, IconButton } from '@mui/material';
import RouterIcon from '@mui/icons-material/Router';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import CloseIcon from '@mui/icons-material/Close';
import { fetchNetworkInterfaceByAsset } from '../../services/assetsService';
import { fetchAssetArchives } from '../../services/assetArchiveService';

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

export default function AssetDetailPanel({ assetId }) {
    const [netIface, setNetIface] = useState(null);
    const [images, setImages]     = useState([]);
    const [loading, setLoading]   = useState(true);
    const [lightbox, setLightbox] = useState(null);

    const loadDetails = () => {
        let cancelled = false;
        setLoading(true);
        setNetIface(null);
        setImages([]);

        Promise.all([
            fetchNetworkInterfaceByAsset(assetId).catch(() => null),
            fetchAssetArchives(assetId).catch(() => []),
        ]).then(([iface, archives]) => {
            if (cancelled) return;
            setNetIface(iface);
            setImages(archives.filter(a => !!a.imageUrl));
        }).finally(() => {
            if (!cancelled) setLoading(false);
        });

        return () => { cancelled = true; };
    };

    useEffect(loadDetails, [assetId]);

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

    if (!hasNetIface && !hasImages) return null;

    return (
        <>
            <Box sx={{ p: 2.5, display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                {hasNetIface && (
                    <Box>
                        <SectionHeader icon={RouterIcon} label="Interfaz de red" />
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
        </>
    );
}
