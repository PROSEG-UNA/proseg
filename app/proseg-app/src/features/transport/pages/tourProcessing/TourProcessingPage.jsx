import { useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { Box, Container, Tab, Tabs } from '@mui/material';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import TourProcessingPanel from '../../components/tourProcessing/TourProcessingPanel.jsx';
import TourProcessingHistoryPanel from '../../components/tourProcessing/TourProcessingHistoryPanel.jsx';

export default function TourProcessingPage() {
    const location = useLocation();
    const [tabIndex, setTabIndex] = useState(location.state?.openHistory ? 1 : 0);
    const { hasAnyPermission } = usePermissions();

    const canExecuteTourProcessing = hasAnyPermission([
        PERMISSIONS.TRANSPORT.TOURS.MANAGE,
    ]);

    const canReadHistory = hasAnyPermission([
        PERMISSIONS.TRANSPORT.TOURS.READ,
        PERMISSIONS.TRANSPORT.TOURS.MANAGE,
        PERMISSIONS.TRANSPORT.TOURS.DELETE,
    ]);

    const tabs = useMemo(() => [
        canExecuteTourProcessing ? { key: 'new', label: 'Nueva depuración' } : null,
        canReadHistory ? { key: 'history', label: 'Historial' } : null,
    ].filter(Boolean), [canExecuteTourProcessing, canReadHistory]);

    if (!tabs.length) return <AccessDeniedState />;

    const safeTabIndex = tabIndex < tabs.length ? tabIndex : Math.max(0, tabs.findIndex((tab) => tab.key === 'history'));
    const currentTab = tabs[safeTabIndex] ?? tabs[0];

    return (
        <Box className="transport-tour-processing-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Depuración"
                    description="Importa y registra giras depuradas desde el archivo previo de comisión."
                />

                <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 1 }}>
                    <Tabs value={safeTabIndex} onChange={(_, value) => setTabIndex(value)}>
                        {tabs.map((tab) => (
                            <Tab
                                key={tab.key}
                                label={tab.label}
                                sx={{ textTransform: 'none', fontWeight: 700 }}
                            />
                        ))}
                    </Tabs>
                </Box>

                <Box sx={{ pt: 3 }}>
                    {currentTab?.key === 'new' ? <TourProcessingPanel /> : null}
                    {currentTab?.key === 'history' ? <TourProcessingHistoryPanel /> : null}
                </Box>
            </Container>
        </Box>
    );
}
