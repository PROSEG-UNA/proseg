import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Box, CircularProgress } from '@mui/material';
import { usePermissions } from '../../../../common/hooks/index.js';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import CancelWithReasonModal from '../../../../common/components/CancelWithReasonModal.jsx';
import { fetchMaintenanceRequestById, acceptMaintenanceRequest, cancelMaintenanceRequest } from '../../services/request/requestsService';
import { formatDate, statusLabel } from '../../maintenanceUtils';

export default function MaintenanceRequestActionPage({ action }) {
    const { id } = useParams();
    const navigate = useNavigate();
    const { hasPermission } = usePermissions();

    const [request, setRequest] = useState(null);
    const [phase, setPhase] = useState('loading');
    const [submitting, setSubmitting] = useState(false);
    const [errorMsg, setErrorMsg] = useState('');

    const requiredPermission = action === 'accept'
        ? PERMISSIONS.MAINTENANCE.REQUESTS.ACCEPT
        : PERMISSIONS.MAINTENANCE.REQUESTS.CANCEL;
    const allowed = hasPermission(requiredPermission);

    useEffect(() => {
        let cancelled = false;

        async function loadRequest() {
            if (!allowed) return;
            setPhase('loading');
            try {
                const data = await fetchMaintenanceRequestById(id);
                if (cancelled) return;
                if (!data) {
                    setErrorMsg('No se encontró la solicitud de mantenimiento.');
                    setPhase('error');
                    return;
                }
                setRequest(data);
                setPhase('action');
            } catch {
                if (cancelled) return;
                setErrorMsg('No se pudo cargar la solicitud de mantenimiento.');
                setPhase('error');
            }
        }

        loadRequest();

        return () => {
            cancelled = true;
        };
    }, [id, allowed]);

    const goHome = () => navigate('/home');

    const handleAcceptConfirm = async () => {
        if (submitting) return;
        setSubmitting(true);
        try {
            await acceptMaintenanceRequest(id);
            setPhase('success');
        } catch (error) {
            setErrorMsg(error?.response?.data?.message ?? error?.message ?? 'No se pudo aceptar la solicitud.');
            setPhase('error');
        } finally {
            setSubmitting(false);
        }
    };

    const handleCancelConfirm = async (reason) => {
        if (submitting) return;
        setSubmitting(true);
        try {
            await cancelMaintenanceRequest(id, reason);
            setPhase('success');
        } catch (error) {
            setErrorMsg(error?.response?.data?.message ?? error?.message ?? 'No se pudo cancelar la solicitud.');
            setPhase('error');
        } finally {
            setSubmitting(false);
        }
    };

    if (!allowed) {
        return <AccessDeniedState />;
    }

    if (phase === 'loading') {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 'calc(100vh - 180px)' }}>
                <CircularProgress />
            </Box>
        );
    }

    const companyName = request?.company?.name ?? 'la empresa';
    const successMessage = action === 'accept'
        ? 'Solicitud aceptada correctamente.'
        : 'Solicitud cancelada correctamente.';

    return (
        <>
            {action === 'accept' && (
                <DialogModal
                    type="success"
                    open={phase === 'action'}
                    title="Aceptar solicitud"
                    message={`¿Deseas marcar como aceptada la solicitud de ${companyName}?`}
                    onClose={goHome}
                    onConfirm={handleAcceptConfirm}
                    confirmLabel="Aceptar"
                />
            )}

            {action === 'cancel' && (
                <CancelWithReasonModal
                    open={phase === 'action'}
                    onClose={goHome}
                    onConfirm={handleCancelConfirm}
                    loading={submitting}
                    title="Cancelar solicitud"
                    subtitle={request?.company?.name}
                    details={[
                        { label: 'Empresa', value: request?.company?.name },
                        { label: 'Estado actual', value: statusLabel(request?.status) },
                        { label: 'Inicio', value: formatDate(request?.startDate) },
                        { label: 'Fin', value: formatDate(request?.endDate) },
                    ]}
                    confirmLabel="Cancelar solicitud"
                />
            )}

            <DialogModal
                type="success"
                open={phase === 'success'}
                title="Operación exitosa"
                message={successMessage}
                onClose={goHome}
            />

            <DialogModal
                type="error"
                open={phase === 'error'}
                message={errorMsg}
                onClose={goHome}
            />
        </>
    );
}
