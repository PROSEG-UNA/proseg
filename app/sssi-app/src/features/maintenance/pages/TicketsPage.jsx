import { useCallback, useState } from 'react';
import { Box, Container, Typography, useMediaQuery, useTheme } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { Header } from '../../../common/components/Header';
import { NavDrawer } from '../../../common/components/Sidebar';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import { PrimaryButton } from '../../../common/components/PrimaryButton.jsx';
import { usePermissions } from '../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../common/constants/permissions';
import MaintenanceTicketFormModal from '../components/ticket/MaintenanceTicketFormModal.jsx';
import MaintenanceTicketTable from '../components/ticket/MaintenanceTicketTable.jsx';
import { fetchMaintenanceTicketById } from '../services/ticketsService';

export default function TicketsPage() {
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [formOpen, setFormOpen] = useState(false);
    const [selectedTicket, setSelectedTicket] = useState(null);
    const [viewMode, setViewMode] = useState(false);
    const [refreshKey, setRefreshKey] = useState(0);
    const [loadingTicketDetail, setLoadingTicketDetail] = useState(false);

    const { hasPermission } = usePermissions();
    const theme = useTheme();
    const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

    const canViewTickets = hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.READ)
        || hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.CREATE)
        || hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.EDIT)
        || hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.DELETE);
    const canCreateTickets = hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.CREATE);

    const handleRefresh = useCallback(() => setRefreshKey((value) => value + 1), []);

    const handleOpenCreate = () => {
        setSelectedTicket(null);
        setViewMode(false);
        setFormOpen(true);
    };

    const handleOpenEdit = async (ticket) => {
        setViewMode(false);
        setFormOpen(true);
        setLoadingTicketDetail(true);

        try {
            const detail = await fetchMaintenanceTicketById(ticket.id);
            setSelectedTicket(detail);
        } finally {
            setLoadingTicketDetail(false);
        }
    };

    const handleOpenView = async (ticket) => {
        setViewMode(true);
        setFormOpen(true);
        setLoadingTicketDetail(true);

        try {
            const detail = await fetchMaintenanceTicketById(ticket.id);
            setSelectedTicket(detail);
        } finally {
            setLoadingTicketDetail(false);
        }
    };

    const handleCloseForm = () => {
        setSelectedTicket(null);
        setViewMode(false);
        setFormOpen(false);
        setLoadingTicketDetail(false);
    };

    const handleSaved = () => {
        setSelectedTicket(null);
        setViewMode(false);
        setFormOpen(false);
        setLoadingTicketDetail(false);
        handleRefresh();
    };

    if (!canViewTickets) {
        return <AccessDeniedState />;
    }

    return (
        <Box className="maintenance-tickets-page">
            <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />

            <Container maxWidth="xl" sx={{ pb: 3, pt: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 2 }}>
                    <Box>
                        <Typography variant="h4" sx={{ fontWeight: 800, color: 'primary.icon', fontSize: { xs: '1.55rem', sm: '1.9rem' } }}>
                            Tickets
                        </Typography>
                        <Typography sx={{ color: 'text.secondary', fontSize: 14 }}>
                            Gestión de incidencias, edición rápida y seguimiento.
                        </Typography>
                    </Box>
                    {canCreateTickets ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={handleOpenCreate}>
                            Crear ticket
                        </PrimaryButton>
                    ) : null}
                </Box>

                <MaintenanceTicketTable
                    refreshKey={refreshKey}
                    onRefresh={handleRefresh}
                    onEdit={handleOpenEdit}
                    onView={handleOpenView}
                />
            </Container>

            <MaintenanceTicketFormModal
                open={formOpen}
                ticket={selectedTicket}
                readOnly={viewMode}
                onClose={handleCloseForm}
                onCreated={handleSaved}
                loadingDetail={loadingTicketDetail}
            />
        </Box>
    );
}