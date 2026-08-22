import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
    Avatar,
    Box,
    Chip,
    Card,
    Divider,
    Grid,
    Paper,
    Skeleton,
    Stack,
    Typography,
    useTheme,
} from '@mui/material';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import Inventory2OutlinedIcon from '@mui/icons-material/Inventory2Outlined';
import PlaceOutlinedIcon from '@mui/icons-material/PlaceOutlined';
import EventOutlinedIcon from '@mui/icons-material/EventOutlined';
import RouterOutlinedIcon from '@mui/icons-material/RouterOutlined';
import WidgetsOutlinedIcon from '@mui/icons-material/WidgetsOutlined';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import EngineeringOutlinedIcon from '@mui/icons-material/EngineeringOutlined';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import NotesOutlinedIcon from '@mui/icons-material/NotesOutlined';
import TagOutlinedIcon from '@mui/icons-material/TagOutlined';
import PhotoLibraryOutlinedIcon from '@mui/icons-material/PhotoLibraryOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import CoordinateMapPicker from './CoordinateMapPicker.jsx';
import { formatDate, formatDateTime } from '../../../../common/utils/formatters.js';
import { fetchAssetById, fetchLastKnownNetworkInterface } from '../../services/assetsService.js';
import { fetchAssetArchives } from '../../services/assetArchiveService.js';
import { fetchAssetComponents } from '../../services/assetComponentsService.js';
import { queryKeys } from '../../../../common/query';

const EMPTY_LIST = [];

function buildAssetDisplayName(asset) {
    const parts = [
        asset?.model?.type?.name,
        asset?.model?.brand?.name,
        asset?.model?.name,
    ].filter(Boolean);

    if (parts.length === 0) {
        return asset?.assetNumber ? `Activo ${asset.assetNumber}` : 'Activo';
    }

    return parts.join(' - ');
}

function HeroMetricCard({ label, value, color = 'text.primary' }) {
    return (
        <Card
            variant="outlined"
            sx={{
                p: 1.5,
                borderRadius: '14px',
                bgcolor: 'background.paperWarm',
                display: 'flex',
                flexDirection: 'column',
                gap: 0.45,
                minHeight: 84,
            }}
        >
            <Typography sx={{ fontSize: 10.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                {label}
            </Typography>
            <Typography sx={{ fontSize: 17, fontWeight: 800, color, wordBreak: 'break-word', lineHeight: 1.25 }}>
                {value || '—'}
            </Typography>
        </Card>
    );
}

function DetailCard({ label, value, icon: Icon }) {
    return (
        <Card
            variant="outlined"
            sx={{
                p: 1.5,
                borderRadius: '14px',
                display: 'flex',
                flexDirection: 'column',
                gap: 0.8,
                minHeight: 98,
                bgcolor: 'hsla(220, 20%, 50%, 0.02)',
            }}
        >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                {Icon ? <Icon sx={{ fontSize: 16, color: 'text.disabled' }} /> : null}
                <Typography sx={{ fontSize: 10.75, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                    {label}
                </Typography>
            </Box>
            <Typography sx={{ fontSize: 14, fontWeight: 700, color: 'text.primary', wordBreak: 'break-word', lineHeight: 1.45 }}>
                {value || '—'}
            </Typography>
        </Card>
    );
}

function SectionBlock({ icon, title, children }) {
    const SectionIcon = icon;

    return (
        <Paper
            variant="outlined"
            sx={{
                p: { xs: 1.5, sm: 2 },
                borderRadius: '16px',
                display: 'flex',
                flexDirection: 'column',
                gap: 1.5,
                bgcolor: 'background.paperWarm',
            }}
        >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.1 }}>
                <Avatar
                    sx={{
                        width: 32,
                        height: 32,
                        bgcolor: 'hsla(343, 68%, 48%, 0.10)',
                        color: 'hsl(343, 68%, 48%)',
                    }}
                >
                    <SectionIcon sx={{ fontSize: 17 }} />
                </Avatar>
                <Box>
                    <Typography sx={{ fontSize: 14, fontWeight: 800, color: 'text.primary', lineHeight: 1.2 }}>
                        {title}
                    </Typography>
                </Box>
            </Box>
            <Divider />
            {children}
        </Paper>
    );
}

export default function AssetDetailModal({ open, onClose, assetId }) {
    const theme = useTheme();

    const detailEnabled = Boolean(open && assetId);

    const { data, isPending } = useQuery({
        queryKey: queryKeys.inventory.assetFullDetail(assetId),
        queryFn: async () => {
            const [assetData, archivesData, componentsData, lastKnown] = await Promise.all([
                fetchAssetById(assetId),
                fetchAssetArchives(assetId),
                fetchAssetComponents(assetId),
                fetchLastKnownNetworkInterface(assetId),
            ]);

            return {
                asset: assetData,
                archives: archivesData,
                components: componentsData,
                networkInterface: assetData?.networkInterface ?? lastKnown,
            };
        },
        enabled: detailEnabled,
    });

    const loading = detailEnabled && isPending;
    const asset = data?.asset ?? null;
    const archives = data?.archives ?? EMPTY_LIST;
    const components = data?.components ?? EMPTY_LIST;
    const networkInterface = data?.networkInterface ?? null;

    const imageArchives = useMemo(() => archives.filter((archive) => !!archive.imageUrl), [archives]);
    const statusColor = asset?.status === 'APROBADO' ? 'success' : (asset?.status === 'DE_BAJA' ? 'error' : 'default');
    const assetDisplayName = buildAssetDisplayName(asset);
    const assetLocationSummary = [
        asset?.location?.description,
        asset?.location?.floor?.name ? `Piso ${asset.location.floor.name}` : null,
        asset?.location?.floor?.building?.name,
        asset?.location?.floor?.building?.campus?.name,
    ].filter(Boolean).join(' - ');

    const modalTitle = asset?.assetNumber ? `Detalle del activo ${asset.assetNumber}` : 'Detalle del activo';
    const modalSubtitle = asset?.serialNumber ? `Serie: ${asset.serialNumber}` : 'Ficha técnica del activo';

    return (
        <GeneralModal
            open={open}
            onClose={onClose}
            maxWidth={false}
            fillHeight
            icon={VisibilityOutlinedIcon}
            title={modalTitle}
            subtitle={modalSubtitle}
            loading={loading}
            showCloseButton
            primaryButton={{ label: 'Cerrar', onClick: onClose }}
            paperSx={{
                width: { xs: '100%', md: '90vw' },
                maxWidth: '1800px',
                maxHeight: { xs: '100dvh', md: '90vh' },
                height: { xs: '100dvh', md: '90vh' },
            }}
            contentSx={{
                overflow: 'hidden',
            }}
        >
            <Box
                sx={{
                    height: '100%',
                    overflowY: 'auto',
                    px: { xs: 2, sm: 2.5, md: 3 },
                    pt: 2,
                    pb: 3,
                    '&::-webkit-scrollbar': { width: '6px' },
                    '&::-webkit-scrollbar-track': { background: 'transparent' },
                    '&::-webkit-scrollbar-thumb': {
                        background: 'color-mix(in srgb, hsl(343, 68%, 48%) 25%, transparent)',
                        borderRadius: '5px',
                    },
                }}
            >
                <Stack spacing={2}>
                    {!loading && asset && (
                        <Paper
                            variant="outlined"
                            sx={{
                                p: { xs: 1.75, sm: 2, md: 2.5 },
                                borderRadius: '18px',
                                background: `linear-gradient(135deg, ${theme.vars.palette.tones.rose.footerBg} 0%, ${theme.vars.palette.tones.rose.headerOverlay} 100%)`,
                                overflow: 'hidden',
                            }}
                        >
                            <Grid container spacing={2} alignItems="stretch">
                                <Grid item xs={12} lg={7}>
                                    <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start', height: '100%' }}>


                                        <Box sx={{ minWidth: 0, display: 'flex', flexDirection: 'column', gap: 1.25 }}>
                                            <Box>
                                                <Typography sx={{ fontSize: { xs: 22, md: 28 }, fontWeight: 900, color: 'text.primary', lineHeight: 1.15, letterSpacing: '-0.02em' }}>
                                                    {assetDisplayName}
                                                </Typography>
                                                <Typography sx={{ mt: 0.5, fontSize: 14, color: 'text.secondary', lineHeight: 1.5 }}>
                                                    {assetLocationSummary || 'Sin ubicación detallada registrada'}
                                                </Typography>
                                            </Box>

                                            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                                               <Chip label={asset.status || '—'} color={statusColor} variant="outlined" />
                                            </Box>
                                        </Box>
                                    </Box>
                                </Grid>

                                <Grid item xs={12} lg={5}>
                                    <Grid container spacing={1.25}>
                                        <Grid item xs={12} sm={4}>
                                            <HeroMetricCard label="Componentes" value={String(components.length)} color="primary.main" />
                                        </Grid>
                                        <Grid item xs={12} sm={4}>
                                            <HeroMetricCard label="Imágenes" value={String(imageArchives.length)} color="primary.main" />
                                        </Grid>
                                    </Grid>
                                </Grid>
                            </Grid>
                        </Paper>
                    )}

                    {loading && (
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr', lg: '1fr 1fr 1fr' }, gap: 1.5 }}>
                            {[1, 2, 3, 4, 5, 6, 7, 8, 9].map((item) => <Skeleton key={item} variant="rounded" height={96} />)}
                        </Box>
                    )}

                    {!loading && !asset && (
                        <Typography sx={{ color: 'text.secondary', textAlign: 'center', py: 3 }}>
                            No se pudo cargar el detalle del activo.
                        </Typography>
                    )}

                    {!loading && asset && (
                        <>
                            <SectionBlock icon={InfoOutlinedIcon} title="Información General">
                                <Grid container spacing={1.5}>
                                    <Grid item xs={12} sm={6} xl={4}><DetailCard label="Número de activo" value={asset.assetNumber} icon={Inventory2OutlinedIcon} /></Grid>
                                    <Grid item xs={12} sm={6} xl={4}><DetailCard label="Número de serie" value={asset.serialNumber} icon={TagOutlinedIcon} /></Grid>
                                    <Grid item xs={12} sm={6} xl={4}><DetailCard label="Tipo" value={asset.model?.type?.name} icon={InfoOutlinedIcon} /></Grid>
                                    <Grid item xs={12} sm={6} xl={4}><DetailCard label="Marca" value={asset.model?.brand?.name} icon={InfoOutlinedIcon} /></Grid>
                                    <Grid item xs={12} sm={6} xl={4}><DetailCard label="Modelo" value={asset.model?.name} icon={InfoOutlinedIcon} /></Grid>
                                </Grid>
                            </SectionBlock>

                            <SectionBlock icon={Inventory2OutlinedIcon} title="Información del Activo">
                                <Grid container spacing={1.5}>
                                    <Grid item xs={12} sm={6} lg={4}><DetailCard label="Fecha de adquisición" value={formatDate(asset.acquisitionDate)} icon={EventOutlinedIcon} /></Grid>
                                    <Grid item xs={12} sm={6} lg={4}><DetailCard label="Fin de garantía" value={formatDate(asset.warrantyEndDate)} icon={EventOutlinedIcon} /></Grid>
                                    <Grid item xs={12} sm={6} lg={4}><DetailCard label="Fin de soporte firmware" value={formatDate(asset.firmwareSupportEndDate)} icon={EventOutlinedIcon} /></Grid>
                                    <Grid item xs={12} sm={6} lg={4}><DetailCard label="Fecha de baja" value={formatDate(asset.decommissionDate)} icon={EventOutlinedIcon} /></Grid>
                                    <Grid item xs={12} sm={6} lg={4}><DetailCard label="Actualizado" value={formatDateTime(asset.updatedAt)} icon={EventOutlinedIcon} /></Grid>
                                </Grid>
                            </SectionBlock>

                            <SectionBlock icon={PlaceOutlinedIcon} title="Ubicación">
                                <Stack spacing={1.25}>
                                    <Typography sx={{ fontSize: 13.5, color: 'text.secondary' }}>
                                        {assetLocationSummary || 'Sin ubicación detallada registrada'}
                                    </Typography>

                                    <Paper variant="outlined" sx={{ p: 1.25, borderRadius: '14px' }}>
                                        <CoordinateMapPicker
                                            latitude={asset.latitude != null ? String(asset.latitude) : ''}
                                            longitude={asset.longitude != null ? String(asset.longitude) : ''}
                                            onCoordinatesChange={() => {}}
                                            onLatitudeChange={() => {}}
                                            onLongitudeChange={() => {}}
                                            onBlur={() => {}}
                                            disabled
                                            errors={{}}
                                            touched={{}}
                                            showCoordinateFields={false}
                                            mapHeight={360}
                                        />
                                    </Paper>
                                </Stack>
                            </SectionBlock>

                            <SectionBlock icon={PersonOutlineOutlinedIcon} title="Responsable">
                                <Grid container spacing={1.5}>
                                    <Grid item xs={12} md={4}><DetailCard label="Unidad ejecutora" value={asset.executingUnit} icon={PersonOutlineOutlinedIcon} /></Grid>
                                    <Grid item xs={12} md={4}><DetailCard label="Funcionario responsable" value={asset.responsibleEmployee} icon={PersonOutlineOutlinedIcon} /></Grid>
                                    <Grid item xs={12} md={4}><DetailCard label="ID del funcionario" value={asset.responsibleEmployeeId} icon={PersonOutlineOutlinedIcon} /></Grid>
                                </Grid>
                            </SectionBlock>

                            <SectionBlock icon={EngineeringOutlinedIcon} title="Información Técnica">
                                <Grid container spacing={1.5}>
                                    <Grid item xs={12} sm={6} lg={3}><DetailCard label="IP" value={networkInterface?.ipAddress} icon={RouterOutlinedIcon} /></Grid>
                                    <Grid item xs={12} sm={6} lg={3}><DetailCard label="MAC" value={networkInterface?.macAddress} icon={RouterOutlinedIcon} /></Grid>
                                    <Grid item xs={12} sm={6} lg={3}><DetailCard label="Archivos asociados" value={String(archives.length)} icon={PhotoLibraryOutlinedIcon} /></Grid>
                                    <Grid item xs={12} sm={6} lg={3}><DetailCard label="Creado" value={formatDateTime(asset.createdAt)} icon={EventOutlinedIcon} /></Grid>
                                </Grid>
                            </SectionBlock>

                            <SectionBlock icon={ImageOutlinedIcon} title="Galería del Activo">
                                {imageArchives.length === 0 ? (
                                    <Paper
                                        variant="outlined"
                                        sx={{
                                            p: 2.25,
                                            borderStyle: 'dashed',
                                            borderRadius: '14px',
                                            bgcolor: 'hsla(220, 20%, 50%, 0.02)',
                                            textAlign: 'center',
                                        }}
                                    >
                                        <Typography sx={{ color: 'text.secondary', fontSize: 13.5 }}>
                                            Este activo no posee imágenes asociadas.
                                        </Typography>
                                    </Paper>
                                ) : (
                                    <Grid container spacing={1.25}>
                                        {imageArchives.map((image) => (
                                            <Grid item xs={12} sm={6} md={4} lg={3} key={image.id}>
                                                <Card
                                                    variant="outlined"
                                                    sx={{
                                                        borderRadius: '14px',
                                                        overflow: 'hidden',
                                                    }}
                                                >
                                                    <Box
                                                        component="img"
                                                        src={image.imageUrl}
                                                        alt={image.caption || ''}
                                                        sx={{
                                                            width: '100%',
                                                            height: { xs: 170, md: 190 },
                                                            objectFit: 'cover',
                                                            display: 'block',
                                                        }}
                                                    />
                                                    <Box sx={{ p: 1.25 }}>
                                                        <Typography sx={{ fontSize: 12.75, fontWeight: 600, color: 'text.secondary' }}>
                                                            {image.caption || 'Imagen del activo'}
                                                        </Typography>
                                                    </Box>
                                                </Card>
                                            </Grid>
                                        ))}
                                    </Grid>
                                )}
                            </SectionBlock>

                            <SectionBlock icon={WidgetsOutlinedIcon} title="Componentes Asociados">
                                {components.length === 0 ? (
                                    <Paper
                                        variant="outlined"
                                        sx={{
                                            p: 2,
                                            borderStyle: 'dashed',
                                            borderRadius: '10px',
                                            bgcolor: 'hsla(220, 20%, 50%, 0.02)',
                                            textAlign: 'center',
                                        }}
                                    >
                                        <Typography sx={{ color: 'text.secondary', fontSize: 13.5 }}>
                                            Este activo no posee componentes asociados.
                                        </Typography>
                                    </Paper>
                                ) : (
                                    <Grid container spacing={1.5}>
                                        {components.map((component) => (
                                            <Grid item xs={12} sm={6} lg={4} key={component.id}>
                                                <Card
                                                    variant="outlined"
                                                    sx={{
                                                        height: '100%',
                                                        p: 1.6,
                                                        borderRadius: '14px',
                                                        bgcolor: 'hsla(220, 20%, 50%, 0.02)',
                                                        display: 'flex',
                                                        flexDirection: 'column',
                                                        gap: 1.1,
                                                    }}
                                                >
                                                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1 }}>
                                                        <Typography sx={{ fontSize: 14.25, fontWeight: 800, color: 'text.primary', wordBreak: 'break-word' }}>
                                                            {component.name || '—'}
                                                        </Typography>
                                                        <Chip
                                                            size="small"
                                                            label={`x${component.quantity ?? '—'}`}
                                                            color="primary"
                                                        />
                                                    </Box>
                                                    <Divider />
                                                    <Grid container spacing={1.25}>
                                                        <Grid item xs={12}><DetailCard label="Ubicación" value={component.location || '—'} icon={PlaceOutlinedIcon} /></Grid>
                                                        <Grid item xs={12}><DetailCard label="Observaciones" value={component.observations || '—'} icon={NotesOutlinedIcon} /></Grid>
                                                    </Grid>
                                                </Card>
                                            </Grid>
                                        ))}
                                    </Grid>
                                )}
                            </SectionBlock>

                            <SectionBlock icon={NotesOutlinedIcon} title="Observaciones">
                                <Grid container spacing={1.5}>
                                    <Grid item xs={12} md={6}><DetailCard label="Resumen de ubicación" value={assetLocationSummary || 'Sin ubicación resumida'} icon={NotesOutlinedIcon} /></Grid>
                                    <Grid item xs={12} md={6}><DetailCard label="Observaciones registradas" value="No hay observaciones adicionales disponibles para este activo." icon={NotesOutlinedIcon} /></Grid>
                                </Grid>
                            </SectionBlock>
                        </>
                    )}
                </Stack>
            </Box>
        </GeneralModal>
    );
}

