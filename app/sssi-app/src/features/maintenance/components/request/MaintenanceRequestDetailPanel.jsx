import { useEffect, useState } from 'react';
import { Box, Skeleton, Typography } from '@mui/material';
import ConstructionIcon from '@mui/icons-material/Construction';
import { fetchMaintenanceRequestById } from '../../services/request/requestsService';
import { fetchCampusById, fetchBuildingById } from '../../services/locationsService';
import { formatDate, statusLabel } from '../../maintenanceUtils';

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

function formatTime(value) {
    if (!value) return '—';
    return String(value).substring(0, 5);
}

export default function MaintenanceRequestDetailPanel({ requestId }) {
    const [request, setRequest] = useState(null);
    const [loading, setLoading] = useState(true);
    const [locationNames, setLocationNames] = useState({ campus: null, building: null });

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        setRequest(null);
        setLocationNames({ campus: null, building: null });

        fetchMaintenanceRequestById(requestId)
            .then((requestData) => {
                if (cancelled) return;
                setRequest(requestData);
            })
            .catch(() => null)
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [requestId]);

    useEffect(() => {
        if (!request) return;
        let cancelled = false;

        const resolveCampus = request.campusId ? fetchCampusById(request.campusId).catch(() => null) : Promise.resolve(null);
        const resolveBuilding = request.buildingId ? fetchBuildingById(request.buildingId).catch(() => null) : Promise.resolve(null);

        Promise.all([resolveCampus, resolveBuilding]).then(([campus, building]) => {
            if (cancelled) return;
            setLocationNames({
                campus: campus?.name ?? null,
                building: building?.name ?? null,
            });
        });

        return () => {
            cancelled = true;
        };
    }, [request]);

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
                    <InfoRow label="Empresa" value={request.company?.name} />
                    <InfoRow label="Cédula" value={request.company?.legalId} />
                    <InfoRow label="Estado" value={statusLabel(request.status)} />
                    <InfoRow label="Descripción" value={request.description} />
                    {request.status === 'CANCELLED' && (
                        <InfoRow label="Motivo cancelación" value={request.cancellationReason} />
                    )}
                </Box>
            </Box>
            <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <Typography sx={{ fontSize: 11.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        Programación
                    </Typography>
                </Box>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75, pl: 2.5 }}>
                    <InfoRow label="Inicio" value={formatDate(request.startDate)} />
                    <InfoRow label="Fin" value={formatDate(request.endDate)} />
                    <InfoRow label="Llegada" value={formatTime(request.startTime)} />
                    <InfoRow label="Salida" value={formatTime(request.endTime)} />
                </Box>
            </Box>
            <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <Typography sx={{ fontSize: 11.5, fontWeight: 700, color: 'text.disabled', letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                        Ubicación
                    </Typography>
                </Box>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75, pl: 2.5 }}>
                    <InfoRow label="Campus" value={locationNames.campus} />
                    <InfoRow label="Edificio" value={locationNames.building} />
                </Box>
            </Box>
        </Box>
    );
}
