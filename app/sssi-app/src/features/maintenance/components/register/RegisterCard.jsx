import { Box, Card, CardActionArea, Chip, Typography, useTheme } from '@mui/material';
import ConstructionIcon from '@mui/icons-material/Construction';
import PersonIcon from '@mui/icons-material/Person';
import EventIcon from '@mui/icons-material/Event';
import GroupIcon from '@mui/icons-material/Group';
import { formatDate, statusLabel } from '../../maintenanceUtils';

function statusColor(status) {
    if (status === 'COMPLETED') return 'success';
    if (status === 'IN_PROGRESS') return 'primary';
    if (status === 'CANCELLED') return 'error';
    return 'warning';
}

function InfoLine({ icon: Icon, children }) {
    return (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Icon sx={{ fontSize: 16, color: 'text.disabled' }} />
            <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>{children}</Typography>
        </Box>
    );
}

export default function RegisterCard({ register, onClick }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    return (
        <Card
            variant="outlined"
            sx={{
                borderRadius: '14px',
                overflow: 'hidden',
                transition: 'box-shadow 0.2s ease, transform 0.2s ease, border-color 0.2s ease',
                '&:hover': {
                    transform: 'translateY(-2px)',
                    borderColor: accentColor,
                    boxShadow: theme.vars.palette.tones.rose.shadowResting,
                },
            }}
        >
            <CardActionArea onClick={() => onClick(register)} sx={{ p: 2.25, height: '100%', alignItems: 'stretch', '&:hover .MuiCardActionArea-focusHighlight': { opacity: 0 } }}>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, height: '100%' }}>
                    <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 1 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
                            <Box
                                sx={{
                                    width: 36, height: 36, borderRadius: '10px',
                                    background: theme.vars.palette.tones.rose.softSubtle,
                                    border: `1px solid ${theme.vars.palette.tones.rose.ring}`,
                                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                                    flexShrink: 0,
                                }}
                            >
                                <ConstructionIcon sx={{ fontSize: 18, color: accentColor }} />
                            </Box>
                            <Typography sx={{ fontSize: 14.5, fontWeight: 700, lineHeight: 1.2 }}>
                                {register.companyName}
                            </Typography>
                        </Box>
                        <Chip label={statusLabel(register.statusRaw)} size="small" variant="outlined" color={statusColor(register.statusRaw)} />
                    </Box>

                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75 }}>
                        <InfoLine icon={PersonIcon}>{register.responsibleName}</InfoLine>
                        <InfoLine icon={EventIcon}>
                            {`${formatDate(register.startDate)} — ${formatDate(register.endDate)}`}
                        </InfoLine>
                        <InfoLine icon={GroupIcon}>
                            {`${register.techniciansCount} técnico(s) asignado(s)`}
                        </InfoLine>
                    </Box>
                </Box>
            </CardActionArea>
        </Card>
    );
}
