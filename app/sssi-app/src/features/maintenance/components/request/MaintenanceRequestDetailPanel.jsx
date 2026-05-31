import { useEffect, useState } from 'react';
import { Box, Button, Chip, Divider, Skeleton, Typography } from '@mui/material';
import ConstructionIcon from '@mui/icons-material/Construction';
import MiscellaneousServicesIcon from '@mui/icons-material/MiscellaneousServices';
import { fetchMaintenanceRequestById } from '../../services/requestsService';
import { fetchTechniciansByRequestId } from '../../services/techniciansService';
import { formatDate, priorityLabel, statusLabel } from '../../maintenanceUtils';

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

export default function MaintenanceRequestDetailPanel({ requestId, onCreateTechnician, canAddTechnician = false }) {
    const [request, setRequest] = useState(null);
    const [technicians, setTechnicians] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        setRequest(null);
        setTechnicians([]);

        Promise.all([
            fetchMaintenanceRequestById(requestId).catch(() => null),
            fetchTechniciansByRequestId(requestId, { page: 0, size: 25 }).catch(() => ({ content: [] })),
        ])
            .then(([requestData, technicianPage]) => {
                if (cancelled) return;
                setRequest(requestData);
                setTechnicians(technicianPage?.content ?? []);
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [requestId]);

    if (loading) {
        return (
            <Box sx={{ p: 2.5, display: 'flex', flexDirection: 'column', gap: 1 }}>
                <Skeleton variant="text" width={220} height={18} />
                <Skeleton variant="text" width={300} height={14} />
                <Skeleton variant="rounded" width="100%" height={120} />
            </Box>
        );
    }

    if (!request) return null;

    return (
        <Box sx={{ p: 2.5, display: 'flex', flexDirection: 'column', gap: 2.5 }}>
            <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <ConstructionIcon sx={{ fontSize: 16, color: 'text.disabled' }} />
                    <Typography sx={{ fontSize: 11.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        Información de la solicitud
                    </Typography>
                </Box>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75, pl: 2.5 }}>
                    <InfoRow label="Título" value={request.title} />
                    <InfoRow label="Empresa" value={request.company?.name} />
                    <InfoRow label="Cédula" value={request.company?.legalId} />
                    <InfoRow label="Activo" value={request.assetId} />
                    <InfoRow label="Estado" value={statusLabel(request.status)} />
                    <InfoRow label="Prioridad" value={priorityLabel(request.priority)} />
                    <InfoRow label="Programada" value={formatDate(request.scheduledDate)} />
                    <InfoRow label="Observaciones" value={request.observations} />
                </Box>
            </Box>

            <Divider />

            <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1, mb: 1 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <MiscellaneousServicesIcon sx={{ fontSize: 16, color: 'text.disabled' }} />
                        <Typography sx={{ fontSize: 11.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                            Técnicos asociados
                        </Typography>
                    </Box>
                    {canAddTechnician ? (
                        <Button size="small" variant="outlined" startIcon={<MiscellaneousServicesIcon sx={{ fontSize: 16 }} />} onClick={() => onCreateTechnician?.(request.id)} sx={{ textTransform: 'none' }}>
                            Agregar técnico
                        </Button>
                    ) : null}
                </Box>

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, pl: 2.5 }}>
                    {technicians.length === 0 ? (
                        <Typography sx={{ color: 'text.secondary', fontSize: 13.25 }}>No hay técnicos asociados a esta solicitud.</Typography>
                    ) : (
                        technicians.map((technician) => (
                            <Box
                                key={technician.id}
                                sx={{
                                    border: '1px solid',
                                    borderColor: 'divider',
                                    borderRadius: '12px',
                                    p: 1.5,
                                    display: 'flex',
                                    flexDirection: 'column',
                                    gap: 0.75,
                                }}
                            >
                                <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 1, alignItems: 'center' }}>
                                    <Typography sx={{ fontWeight: 700, fontSize: 13.25 }} noWrap>
                                        {technician.fullName}
                                    </Typography>
                                    <Chip label={technician.leader ? 'Líder' : 'Técnico'} size="small" variant="outlined" color={technician.leader ? 'success' : 'default'} />
                                </Box>
                                <Typography sx={{ color: 'text.secondary', fontSize: 12.5 }} noWrap>
                                    {technician.position || 'Sin puesto'} · {technician.email || 'Sin correo'}
                                </Typography>
                            </Box>
                        ))
                    )}
                </Box>
            </Box>
        </Box>
    );
}



