import { useMemo, useState } from 'react';
import { Box, Container, Tab, Tabs } from '@mui/material';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import CleaningPanel from '../../components/cleaning/CleaningPanel.jsx';
import CleaningHistoryPanel from '../../components/cleaning/CleaningHistoryPanel.jsx';

export default function CleaningPage() {
    const [tabIndex, setTabIndex] = useState(0);
    const { hasAnyPermission } = usePermissions();

    const canExecuteCleaning = hasAnyPermission([
        PERMISSIONS.TRANSPORT.ASSIGNMENT.GENERATE,
        PERMISSIONS.TRANSPORT.ASSIGNMENT.UPDATE,
    ]);

    const canReadHistory = hasAnyPermission([
        PERMISSIONS.TRANSPORT.TOURS.READ,
        PERMISSIONS.TRANSPORT.TOURS.MANAGE,
        PERMISSIONS.TRANSPORT.TOURS.DELETE,
        PERMISSIONS.TRANSPORT.ASSIGNMENT.GENERATE,
        PERMISSIONS.TRANSPORT.ASSIGNMENT.UPDATE,
    ]);

    const tabs = useMemo(() => [
        canReadHistory ? { key: 'history', label: 'Historial' } : null,
        canExecuteCleaning ? { key: 'new', label: 'Nueva depuración' } : null,
    ].filter(Boolean), [canExecuteCleaning, canReadHistory]);

    if (!tabs.length) return <AccessDeniedState />;

    const safeTabIndex = tabIndex < tabs.length ? tabIndex : 0;
    const currentTab = tabs[safeTabIndex] ?? tabs[0];

    return (
        <Box className="transport-cleaning-page">
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
                    {currentTab?.key === 'new' ? <CleaningPanel /> : null}
                    {currentTab?.key === 'history' ? <CleaningHistoryPanel /> : null}
                </Box>
            </Container>
        </Box>
    );
}
