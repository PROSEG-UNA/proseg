import { Box, Divider, Skeleton, Typography, useTheme } from '@mui/material';
import HistoryIcon from '@mui/icons-material/History';
import PersonIcon from '@mui/icons-material/Person';
import BusinessIcon from '@mui/icons-material/Business';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import { formatDateTime } from '../../../../common/utils/formatters.js';
import { useAssetMaintenanceHistory } from '../../hooks/useAssetMaintenanceHistory';

export default function AssetMaintenanceHistoryModal({ open, onClose, asset, refreshKey = 0 }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const { rows, loading, error, totalElements } = useAssetMaintenanceHistory({
        assetId: open ? asset?.id : null,
        pageSize: 50,
        refreshKey,
    });

    return (
        <GeneralModal
            open={open}
            onClose={onClose}
            maxWidth="sm"
            icon={HistoryIcon}
            title="Historial de mantenimiento"
            subtitle={asset ? `Activo ${asset.assetNumber} · Serie ${asset.serialNumber}` : ''}
            loading={loading}
            secondaryButton={{ label: 'Cerrar', onClick: onClose }}
        >
            <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'flex', flexDirection: 'column', gap: 2 }}>
                {loading ? (
                    <>
                        <Skeleton variant="rounded" width="100%" height={70} />
                        <Skeleton variant="rounded" width="100%" height={70} />
                    </>
                ) : error ? (
                    <Typography sx={{ fontSize: 13.5, color: 'error.main' }}>{error}</Typography>
                ) : totalElements === 0 ? (
                    <Typography sx={{ fontSize: 13.5, color: 'text.secondary' }}>
                        Este activo aún no tiene registros de mantenimiento.
                    </Typography>
                ) : (
                    rows.map((record) => (
                        <Box
                            key={record.id}
                            sx={{
                                border: '1px solid',
                                borderColor: 'divider',
                                borderRadius: '12px',
                                p: 1.75,
                                display: 'flex',
                                flexDirection: 'column',
                                gap: 1,
                            }}
                        >
                            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1 }}>
                                <Typography sx={{ fontSize: 12, fontWeight: 700, color: accentColor }}>
                                    {formatDateTime(record.createdAt)}
                                </Typography>
                            </Box>
                            <Typography sx={{ fontSize: 13.5, color: 'text.primary' }}>{record.description}</Typography>
                            <Divider />
                            <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                                    <PersonIcon sx={{ fontSize: 15, color: 'text.disabled' }} />
                                    <Typography sx={{ fontSize: 12.5, color: 'text.secondary' }}>{record.userEmail}</Typography>
                                </Box>
                                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
                                    <BusinessIcon sx={{ fontSize: 15, color: 'text.disabled' }} />
                                    <Typography sx={{ fontSize: 12.5, color: 'text.secondary' }}>{record.companyName}</Typography>
                                </Box>
                            </Box>
                        </Box>
                    ))
                )}
            </Box>
        </GeneralModal>
    );
}
