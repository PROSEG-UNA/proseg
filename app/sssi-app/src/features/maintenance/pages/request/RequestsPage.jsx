import { usePermissions } from '../../../../common/hooks/index.js';

import { useState } from 'react';
import { Box, Button, Container, Menu, MenuItem } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DownloadForOfflineOutlinedIcon from '@mui/icons-material/DownloadForOfflineOutlined';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { PrimaryButton } from '../../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import MaintenanceRequestTable from '../../components/request/MaintenanceRequestTable.jsx';
import MaintenanceRequestFormModal from '../../components/request/MaintenanceRequestFormModal.jsx';
import { exportMaintenanceRequests, triggerBrowserDownload } from '../../services/maintenanceExportService.js';

export default function RequestsPage() {
    const [requestFormState, setRequestFormState] = useState({ open: false, requestId: null, initialCompanyId: '' });
    const [requestsRefresh, setRequestsRefresh] = useState(0);
    const [exportAnchorEl, setExportAnchorEl] = useState(null);
    const [exporting, setExporting] = useState(false);
    const [alert, setAlert] = useState(null);

    const { hasAnyPermission } = usePermissions();

    const canViewRequests = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.REQUESTS.READ,
        PERMISSIONS.MAINTENANCE.REQUESTS.REQUEST,
        PERMISSIONS.MAINTENANCE.REQUESTS.UPDATE,
        PERMISSIONS.MAINTENANCE.REQUESTS.DELETE,
    ]);

    const canCreateRequests = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.REQUESTS.REQUEST,
    ]);
    const canExportRequests = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.REQUESTS.READ,
    ]);
    const exportMenuOpen = Boolean(exportAnchorEl);

    const openCreateRequest = (initialCompanyId = '') => setRequestFormState({ open: true, requestId: null, initialCompanyId });
    const openEditRequest = (request) => setRequestFormState({ open: true, requestId: request.id, initialCompanyId: request.companyId ?? '' });

    const refreshRequests = () => setRequestsRefresh((previousValue) => previousValue + 1);

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
            const { blob, filename } = await exportMaintenanceRequests({ format });
            triggerBrowserDownload(blob, filename);
            setAlert({
                type: 'success',
                message: `Exportación de solicitudes completada (${format.toUpperCase()})`,
            });
        } catch (error) {
            const message = error?.message || 'No fue posible exportar las solicitudes';
            setAlert({ type: 'error', message });
        } finally {
            setExporting(false);
            setExportAnchorEl(null);
        }
    };

    if (!canViewRequests) return <AccessDeniedState />;

    return (
        <Box className="maintenance-requests-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Solicitudes"
                    description="Gestión de solicitudes"
                    action={(canExportRequests || canCreateRequests) ? (
                        <Box sx={{ display: 'flex', gap: 1.25, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                            {canExportRequests ? (
                                <>
                                    <Button
                                        variant="outlined"
                                        startIcon={<DownloadForOfflineOutlinedIcon />}
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
                            {canCreateRequests ? (
                                <PrimaryButton startIcon={<AddIcon />} onClick={() => openCreateRequest('')} sx={{ px: '28px' }}>
                                    Crear
                                </PrimaryButton>
                            ) : null}
                        </Box>
                    ) : null}
                />

                <Box sx={{ pt: 3 }}>
                    <MaintenanceRequestTable
                        refreshKey={requestsRefresh}
                        onRefresh={refreshRequests}
                        onEditRequest={openEditRequest}
                    />
                </Box>
            </Container>

            <MaintenanceRequestFormModal
                open={requestFormState.open}
                requestId={requestFormState.requestId}
                initialCompanyId={requestFormState.initialCompanyId}
                onClose={() => setRequestFormState({ open: false, requestId: null, initialCompanyId: '' })}
                onSaved={refreshRequests}
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
