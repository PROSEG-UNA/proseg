import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
    Alert,
    Box,
    Button,
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
import { AssetReadOnlyField, AssetSectionTitle } from './AssetModalFields.jsx';

const EMPTY_LIST = [];

function HeroMetricCard({ label, value, color }) {
    return (
        <Paper variant="outlined" sx={{ p: 1.5, height: '100%', borderRadius: '12px' }}>
            <Typography sx={{ color, fontSize: 24, fontWeight: 800 }}>{value}</Typography>
            <Typography variant="body2" color="text.secondary">{label}</Typography>
        </Paper>
    );
}

function DetailCard({ label, value, icon: Icon, multiline = false, minRows }) {
    return (
        <AssetReadOnlyField
            label={label}
            value={value}
            icon={Icon}
            multiline={multiline}
            minRows={minRows}
        />
    );
}

function SectionBlock({ icon, title, children }) {
    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
            <AssetSectionTitle icon={icon}>{title}</AssetSectionTitle>
            {children}
            <Divider sx={{ mt: 1 }} />
        </Box>
    );
}

export default function AssetDetailModal({ open, onClose, onEdit, canEdit = false, assetId }) {
    const theme = useTheme();
    const detailEnabled = Boolean(open && assetId);

    const { data, isPending, error, refetch } = useQuery({
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
    const assetLocationSummary = [
        asset?.location?.description,
        asset?.location?.floor?.name ? `Piso ${asset.location.floor.name}` : null,
        asset?.location?.floor?.building?.name,
        asset?.location?.floor?.building?.campus?.name,
    ].filter(Boolean).join(' - ');

    const assetDisplayName = [asset?.model?.brand?.name, asset?.model?.name]
        .filter(Boolean).join(' ') || asset?.assetNumber || 'Activo';
    const statusColor = asset?.status === 'DE_BAJA' ? 'error' : 'default';
    const rosePalette = theme.vars?.palette?.tones?.rose;
    const heroStart = rosePalette?.footerBg ?? theme.palette.background.paper;
    const heroEnd = rosePalette?.headerOverlay ?? theme.palette.action.hover;

    const modalTitle = asset?.assetNumber ? `Detalle del activo ${asset.assetNumber}` : 'Detalle del activo';
    const modalSubtitle = asset?.serialNumber ? `Serie: ${asset.serialNumber}` : 'Ficha técnica del activo';

    return (
        <GeneralModal
            open={open}
            onClose={onClose}
            maxWidth="md"
            fillHeight
            icon={VisibilityOutlinedIcon}
            title={modalTitle}
            subtitle={modalSubtitle}
            loading={loading}
            showCloseButton
            primaryButton={{ label: 'Cerrar', onClick: onClose }}
            secondaryButton={canEdit ? { label: 'Editar activo', onClick: onEdit } : undefined}
            contentSx={{
                overflow: 'hidden',
            }}
        >
            <Box
                sx={{
                    height: '100%',
                    overflowY: 'auto',
                    px: { xs: 2.5, sm: 3 },
                    pt: 2.5,
                    pb: 3,
                    '&::-webkit-scrollbar': { width: '6px' },
                    '&::-webkit-scrollbar-track': { background: 'transparent' },
                    '&::-webkit-scrollbar-thumb': {
                        background: 'color-mix(in srgb, hsl(343, 68%, 48%) 25%, transparent)',
                        borderRadius: '5px',
                    },
                }}
            >
                <Stack spacing={3}>
                    {!loading && asset && (
                        <Paper
                            variant="outlined"
                            sx={{
                                p: { xs: 1.75, sm: 2, md: 2.5 },
                                borderRadius: '18px',
                                background: `linear-gradient(135deg, ${heroStart} 0%, ${heroEnd} 100%)`,
                                overflow: 'hidden',
                            }}
                        >
                            <Grid container spacing={2} alignItems="stretch">
                                <Grid size={{ xs: 12, lg: 7 }}>
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

                                <Grid size={{ xs: 12, lg: 5 }}>
                                    <Grid container spacing={1.25}>
                                        <Grid size={{ xs: 12, sm: 6 }}>
                                            <HeroMetricCard label="Componentes" value={String(components.length)} color="primary.main" />
                                        </Grid>
                                        <Grid size={{ xs: 12, sm: 6 }}>
                                            <HeroMetricCard label="Imágenes" value={String(imageArchives.length)} color="primary.main" />
                                        </Grid>
                                    </Grid>
                                </Grid>
                            </Grid>
                        </Paper>
                    )}

                    {loading && (
                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                            {[1, 2, 3, 4, 5, 6, 7].map((item) => <Skeleton key={item} variant="rounded" height={56} />)}
                        </Box>
                    )}

                    {!loading && error && (
                        <Alert
                            severity="error"
                            action={<Button color="inherit" size="small" onClick={() => void refetch()}>Reintentar</Button>}
                        >
                            No se pudo cargar el detalle del activo.
                        </Alert>
                    )}

                    {!loading && !error && !asset && (
                        <Typography sx={{ color: 'text.secondary', textAlign: 'center', py: 3 }}>
                            No se encontró información para este activo.
                        </Typography>
                    )}

                    {!loading && asset && (
                        <>
                            <SectionBlock icon={InfoOutlinedIcon} title="Identificación">
                                <Grid container spacing={2}>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Número de activo" value={asset.assetNumber} icon={Inventory2OutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Número de serie" value={asset.serialNumber} icon={TagOutlinedIcon} /></Grid>
                                </Grid>
                            </SectionBlock>

                            <SectionBlock icon={Inventory2OutlinedIcon} title="Clasificación">
                                <Grid container spacing={2}>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Tipo de activo" value={asset.model?.type?.name} icon={InfoOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Marca" value={asset.model?.brand?.name} icon={InfoOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12 }}><DetailCard label="Modelo" value={asset.model?.name} icon={InfoOutlinedIcon} /></Grid>
                                </Grid>
                            </SectionBlock>

                            <SectionBlock icon={PlaceOutlinedIcon} title="Ubicación">
                                <Grid container spacing={2}>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Campus" value={asset.location?.floor?.building?.campus?.name} icon={PlaceOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Edificio" value={asset.location?.floor?.building?.name} icon={PlaceOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Número de piso" value={asset.location?.floor?.name} icon={PlaceOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Detalle de ubicación" value={asset.location?.description} icon={PlaceOutlinedIcon} /></Grid>
                                </Grid>
                            </SectionBlock>

                            <SectionBlock icon={Inventory2OutlinedIcon} title="Estado">
                                <Grid container spacing={2}>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Estado" value={asset.status} icon={Inventory2OutlinedIcon} /></Grid>
                                    {asset.status === 'DE_BAJA' && (
                                        <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Fecha de baja" value={formatDate(asset.decommissionDate)} icon={EventOutlinedIcon} /></Grid>
                                    )}
                                </Grid>
                            </SectionBlock>

                            <SectionBlock icon={EngineeringOutlinedIcon} title="IP y MAC">
                                <Grid container spacing={2}>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Dirección IP" value={networkInterface?.ipAddress} icon={RouterOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Dirección MAC" value={networkInterface?.macAddress} icon={RouterOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Archivos asociados" value={String(archives.length)} icon={PhotoLibraryOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Creado" value={formatDateTime(asset.createdAt)} icon={EventOutlinedIcon} /></Grid>
                                </Grid>
                            </SectionBlock>

                            <SectionBlock icon={EventOutlinedIcon} title="Fechas">
                                <Grid container spacing={2}>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Fecha de adquisición" value={formatDate(asset.acquisitionDate)} icon={EventOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Fin de garantía" value={formatDate(asset.warrantyEndDate)} icon={EventOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Fin de soporte firmware" value={formatDate(asset.firmwareSupportEndDate)} icon={EventOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Actualizado" value={formatDateTime(asset.updatedAt)} icon={EventOutlinedIcon} /></Grid>
                                </Grid>
                            </SectionBlock>

                            <SectionBlock icon={PlaceOutlinedIcon} title="Coordenadas">
                                <Grid container spacing={2}>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Latitud" value={asset.latitude} /></Grid>
                                    <Grid size={{ xs: 12, sm: 6 }}><DetailCard label="Longitud" value={asset.longitude} /></Grid>
                                    <Grid size={{ xs: 12 }}>
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
                                            mapHeight={300}
                                        />
                                    </Grid>
                                </Grid>
                            </SectionBlock>

                            <SectionBlock icon={PersonOutlineOutlinedIcon} title="Responsable">
                                <Grid container spacing={1.5}>
                                    <Grid size={{ xs: 12, md: 4 }}><DetailCard label="Unidad ejecutora" value={asset.executingUnit?.name} icon={PersonOutlineOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, md: 4 }}><DetailCard label="Funcionario responsable" value={asset.employee?.name} icon={PersonOutlineOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, md: 4 }}><DetailCard label="ID del funcionario" value={asset.employee?.identification} icon={PersonOutlineOutlinedIcon} /></Grid>
                                </Grid>
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
                                            <Grid size={{ xs: 12, sm: 6, lg: 4 }} key={component.id}>
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
                                                        <Grid size={{ xs: 12 }}><DetailCard label="Ubicación" value={component.location || '—'} icon={PlaceOutlinedIcon} /></Grid>
                                                        <Grid size={{ xs: 12 }}><DetailCard label="Observaciones" value={component.observations} icon={NotesOutlinedIcon} multiline minRows={2} /></Grid>
                                                    </Grid>
                                                </Card>
                                            </Grid>
                                        ))}
                                    </Grid>
                                )}
                            </SectionBlock>

                            <SectionBlock icon={ImageOutlinedIcon} title="Fotos">
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
                                            <Grid size={{ xs: 12, sm: 6, md: 4 }} key={image.id}>
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

                            <SectionBlock icon={NotesOutlinedIcon} title="Observaciones">
                                <Grid container spacing={1.5}>
                                    <Grid size={{ xs: 12, md: 6 }}><DetailCard label="Resumen de ubicación" value={assetLocationSummary || 'Sin ubicación resumida'} icon={NotesOutlinedIcon} /></Grid>
                                    <Grid size={{ xs: 12, md: 6 }}><DetailCard label="Observaciones registradas" value="No hay observaciones adicionales disponibles para este activo." icon={NotesOutlinedIcon} multiline minRows={2} /></Grid>
                                </Grid>
                            </SectionBlock>
                        </>
                    )}
                </Stack>
            </Box>
        </GeneralModal>
    );
}
