import { useCallback, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Box, Button, Container, Menu, MenuItem, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import ExportIcon from '../../../common/components/icons/ExportIcon.jsx';
import { NavDrawer } from '../../../common/components/Sidebar';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import DialogModal from '../../../common/components/DialogModal.jsx';
import { PrimaryButton } from '../../../common/components/PrimaryButton.jsx';
import { usePermissions } from '../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../common/constants/permissions';
import MaintenanceTicketFormModal from '../components/ticket/MaintenanceTicketFormModal.jsx';
import MaintenanceTicketTable from '../components/ticket/MaintenanceTicketTable.jsx';
import { fetchMaintenanceTicketById } from '../services/ticketsService';
import { exportMaintenanceTickets, triggerBrowserDownload } from '../services/maintenanceExportService.js';
import { queryKeys } from '../../../common/query';

export default function TicketsPage() {
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [formOpen, setFormOpen] = useState(false);
    const [selectedTicket, setSelectedTicket] = useState(null);
    const [viewMode, setViewMode] = useState(false);
    const queryClient = useQueryClient();
    const [loadingTicketDetail, setLoadingTicketDetail] = useState(false);
    const detailRequestRef = useRef(0);
    const [exportAnchorEl, setExportAnchorEl] = useState(null);
    const [exporting, setExporting] = useState(false);
    const [alert, setAlert] = useState(null);

    const { hasPermission } = usePermissions();

    const canViewTickets = hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.READ)
        || hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.CREATE)
        || hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.EDIT)
        || hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.DELETE);
    const canCreateTickets = hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.CREATE);
    const canExportTickets = hasPermission(PERMISSIONS.MAINTENANCE.TICKETS.READ);
    const exportMenuOpen = Boolean(exportAnchorEl);

    const handleRefresh = useCallback(
        () => { void queryClient.invalidateQueries({ queryKey: queryKeys.maintenance.tickets() }); },
        [queryClient]
    );

    const handleOpenCreate = () => {
        setSelectedTicket(null);
        setViewMode(false);
        setFormOpen(true);
    };

    const openTicketDetail = async (ticket, openAsReadOnly) => {
        const requestId = detailRequestRef.current + 1;
        detailRequestRef.current = requestId;

        setViewMode(openAsReadOnly);
        setFormOpen(true);
        setLoadingTicketDetail(true);

        try {
            const detail = await fetchMaintenanceTicketById(ticket.id);
            if (detailRequestRef.current !== requestId) return;
            setSelectedTicket(detail);
        } finally {
            if (detailRequestRef.current === requestId) {
                setLoadingTicketDetail(false);
            }
        }
    };

    const handleOpenEdit = (ticket) => openTicketDetail(ticket, false);

    const handleOpenView = (ticket) => openTicketDetail(ticket, true);

    const handleCloseForm = () => {
        detailRequestRef.current += 1;
        setSelectedTicket(null);
        setViewMode(false);
        setFormOpen(false);
        setLoadingTicketDetail(false);
    };

    const handleSaved = () => {
        detailRequestRef.current += 1;
        setSelectedTicket(null);
        setViewMode(false);
        setFormOpen(false);
        setLoadingTicketDetail(false);
        handleRefresh();
    };

    const handleOpenExportMenu = (event) => {
        setExportAnchorEl(event.currentTarget);
    };

    const handleCloseExportMenu = () => {
        if (exporting) return;
        setExportAnchorEl(null);
    };

    const handleExport = async (format) => {
        setExporting(true);
        try {
            const { blob, filename } = await exportMaintenanceTickets({ format });
            triggerBrowserDownload(blob, filename);
            setAlert({
                type: 'success',
                message: `Exportación de tickets completada (${format.toUpperCase()})`,
            });
        } catch (error) {
            const message = error?.message || 'No fue posible exportar los tickets';
            setAlert({ type: 'error', message });
        } finally {
            setExporting(false);
            setExportAnchorEl(null);
        }
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
                    <Box sx={{ display: 'flex', gap: 1.25, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                        {canExportTickets ? (
                            <>
                                <Button
                                    variant="outlined"
                                    startIcon={<ExportIcon style={{ fontSize: 16, marginRight: 2 }} />}
                                    onClick={handleOpenExportMenu}
                                    sx={{
                                        textTransform: 'none',
                                        fontWeight: 700,
                                        borderRadius: '10px',
                                        px: 2.5,
                                    }}
                                    disabled={exporting}
                                >
                                    {exporting ? 'Exportando...' : 'Exportar'}
                                </Button>
                                <Menu
                                    anchorEl={exportAnchorEl}
                                    open={exportMenuOpen}
                                    onClose={handleCloseExportMenu}
                                    anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
                                    transformOrigin={{ vertical: 'top', horizontal: 'left' }}
                                >
                                    <MenuItem onClick={() => handleExport('xlsx')} disabled={exporting}>
                                        Exportar en Excel (.xlsx)
                                    </MenuItem>
                                    <MenuItem onClick={() => handleExport('csv')} disabled={exporting}>
                                        Exportar en CSV (.csv)
                                    </MenuItem>
                                </Menu>
                            </>
                        ) : null}
                        {canCreateTickets ? (
                            <PrimaryButton startIcon={<AddIcon />} onClick={handleOpenCreate}>
                                Crear ticket
                            </PrimaryButton>
                        ) : null}
                    </Box>
                </Box>

                <MaintenanceTicketTable
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

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </Box>
    );
}