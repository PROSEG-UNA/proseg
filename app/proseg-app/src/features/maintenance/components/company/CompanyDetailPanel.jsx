import { useQuery } from '@tanstack/react-query';
import { Box, Button, Divider, Skeleton, Typography } from '@mui/material';
import BusinessIcon from '@mui/icons-material/Business';
import PeopleIcon from '@mui/icons-material/People';
import LaunchIcon from '@mui/icons-material/Launch';
import { formatDateTime } from '../../maintenanceUtils';
import { companyDetailQueryOptions } from './companyDetailQueries.js';

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

export default function CompanyDetailPanel({ companyId, listRow = null, onManageUsers, canManageUsers = false }) {
    const { data, isPending } = useQuery({
        ...companyDetailQueryOptions(companyId),
        enabled: Boolean(companyId),
        placeholderData: listRow ?? undefined,
    });

    const company = data ?? null;
    const loading = isPending && !company;

    if (loading) {
        return (
            <Box sx={{ p: 2.5, display: 'flex', flexDirection: 'column', gap: 1 }}>
                <Skeleton variant="text" width={220} height={18} />
                <Skeleton variant="text" width={300} height={14} />
                <Skeleton variant="rounded" width="100%" height={120} />
            </Box>
        );
    }

    if (!company) {
        return null;
    }

    return (
        <Box sx={{ p: 2.5, display: 'flex', flexDirection: 'column', gap: 2.5 }}>
            <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <BusinessIcon sx={{ fontSize: 16, color: 'text.disabled' }} />
                    <Typography sx={{ fontSize: 11.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        Información de la empresa
                    </Typography>
                </Box>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75, pl: 2.5 }}>
                    <InfoRow label="Nombre" value={company.name} />
                    <InfoRow label="Cédula" value={company.legalId} />
                    <InfoRow label="Correo" value={company.contactEmail} />
                    <InfoRow label="Teléfono" value={company.contactPhone} />
                    <InfoRow label="Dirección" value={company.address} />
                    <InfoRow label="Creada" value={formatDateTime(company.createdAt)} />
                    <InfoRow label="Actualizada" value={formatDateTime(company.updatedAt)} />
                </Box>
            </Box>

            <Divider />

            <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1, mb: 1 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <PeopleIcon sx={{ fontSize: 16, color: 'text.disabled' }} />
                        <Typography sx={{ fontSize: 11.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                            Usuarios vinculados
                        </Typography>
                    </Box>
                    {canManageUsers ? (
                        <Button size="small" variant="outlined" startIcon={<LaunchIcon sx={{ fontSize: 16 }} />} onClick={() => onManageUsers?.(company)} sx={{ textTransform: 'none' }}>
                            Gestionar
                        </Button>
                    ) : null}
                </Box>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, pl: 2.5 }}>
                    {(company.userCompanies?.length ?? 0) === 0 ? (
                        <Typography sx={{ color: 'text.secondary', fontSize: 13.25 }}>Sin usuarios asociados.</Typography>
                    ) : (
                        company.userCompanies.map((userCompany) => (
                            <Box
                                key={userCompany.id}
                                sx={{
                                    display: 'flex',
                                    flexDirection: 'column',
                                    gap: 0.5,
                                    border: '1px solid',
                                    borderColor: 'divider',
                                    borderRadius: '8px',
                                    p: 1.5,
                                }}
                            >
                                <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', flexWrap: 'wrap' }}>
                                    <Typography sx={{ fontSize: 12.5, fontWeight: 600, color: 'text.primary' }}>
                                        <strong>{userCompany.userEmail || '—'}</strong>
                                    </Typography>
                                </Box>
                            </Box>
                        ))
                    )}
                </Box>
            </Box>
        </Box>
    );
}

