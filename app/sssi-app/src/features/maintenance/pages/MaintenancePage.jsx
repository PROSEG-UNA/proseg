import { useEffect, useMemo, useState } from 'react';
import {
    Box,
    Container,
    Tab,
    Tabs,
    Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import BusinessIcon from '@mui/icons-material/Business';
import ConstructionIcon from '@mui/icons-material/Construction';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import { PrimaryButton } from '../../../common/components/PrimaryButton.jsx';
import { usePermissions } from '../../../common/hooks/index.js';
import { PERMISSIONS } from '../../../common/constants/permissions';
import CompanyTable from '../components/company/CompanyTable.jsx';
import CompanyFormModal from '../components/company/CompanyFormModal.jsx';
import CompanyUsersModal from '../components/company/CompanyUsersModal.jsx';
import MaintenanceRequestTable from '../components/request/MaintenanceRequestTable.jsx';
import MaintenanceRequestFormModal from '../components/request/MaintenanceRequestFormModal.jsx';

export default function MaintenancePage() {
    const [tabIndex, setTabIndex] = useState(0);

    const [companyFormOpen, setCompanyFormOpen] = useState(false);
    const [companyFormId, setCompanyFormId] = useState(null);
    const [companyUsersTarget, setCompanyUsersTarget] = useState(null);
    const [requestFormState, setRequestFormState] = useState({ open: false, requestId: null, initialCompanyId: '' });

    const [companiesRefresh, setCompaniesRefresh] = useState(0);
    const [requestsRefresh, setRequestsRefresh] = useState(0);

    const { hasPermission, hasAnyPermission } = usePermissions();

    const canViewCompanies = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.COMPANIES.READ,
        PERMISSIONS.MAINTENANCE.COMPANIES.MANAGE,
        PERMISSIONS.MAINTENANCE.COMPANIES.DELETE,
        PERMISSIONS.MAINTENANCE.COMPANY_USERS.READ,
        PERMISSIONS.MAINTENANCE.COMPANY_USERS.MANAGE,
        PERMISSIONS.MAINTENANCE.COMPANY_USERS.DELETE,
    ]);

    const canViewRequests = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.REQUESTS.READ,
        PERMISSIONS.MAINTENANCE.REQUESTS.REQUEST,
        PERMISSIONS.MAINTENANCE.REQUESTS.UPDATE,
        PERMISSIONS.MAINTENANCE.REQUESTS.DELETE,
    ]);

    const canCreateRequests = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.REQUESTS.REQUEST,
    ]);

    const tabs = useMemo(() => [
        canViewCompanies ? { key: 'companies', label: 'Empresas', icon: BusinessIcon } : null,
        canViewRequests ? { key: 'requests', label: 'Solicitudes', icon: ConstructionIcon } : null,
    ].filter(Boolean), [canViewCompanies, canViewRequests]);

    const currentTab = tabs[tabIndex] ?? tabs[0] ?? null;

    useEffect(() => {
        if (tabIndex >= tabs.length) {
            setTabIndex(0);
        }
    }, [tabIndex, tabs.length]);

    const handleTabChange = (_, newValue) => setTabIndex(newValue);

    const openCreateCompany = () => {
        setCompanyFormId(null);
        setCompanyFormOpen(true);
    };
    const openEditCompany = (company) => {
        setCompanyFormId(company.id);
        setCompanyFormOpen(true);
    };
    const openCompanyUsers = (company) => setCompanyUsersTarget(company);
    const openCreateRequest = (initialCompanyId = '') => setRequestFormState({ open: true, requestId: null, initialCompanyId });
    const openEditRequest = (request) => setRequestFormState({ open: true, requestId: request.id, initialCompanyId: request.companyId ?? '' });

    const refreshCompanies = () => setCompaniesRefresh((value) => value + 1);
    const refreshRequests = () => setRequestsRefresh((value) => value + 1);

    if (!canViewCompanies && !canViewRequests) {
        return <AccessDeniedState />;
    }

    return (
        <Box className="maintenance-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <Box sx={{ display: 'flex', alignItems: { xs: 'flex-start', md: 'center' }, justifyContent: 'space-between', gap: 2, mb: 2, flexDirection: { xs: 'column', md: 'row' } }}>
                    <Box sx={{ flex: 1 }}>
                        <Typography variant="h4" sx={{ fontWeight: 800, color: 'primary.icon', fontSize: { xs: '1.55rem', sm: '1.9rem' } }}>
                            Mantenimiento
                        </Typography>
                        <Typography sx={{ color: 'text.secondary', fontSize: 14 }}>
                            Gestión de empresas y solicitudes desde un solo módulo.
                        </Typography>
                    </Box>
                    {currentTab?.key === 'companies' && hasPermission(PERMISSIONS.MAINTENANCE.COMPANIES.MANAGE) ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={openCreateCompany} sx={{ px: '28px' }}>
                            Crear
                        </PrimaryButton>
                    ) : currentTab?.key === 'requests' && canCreateRequests ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={() => openCreateRequest('')} sx={{ px: '28px' }}>
                            Crear
                        </PrimaryButton>
                    ) : null}
                </Box>

                <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 1 }}>
                    <Tabs value={tabIndex} onChange={handleTabChange} variant="scrollable" scrollButtons="auto" allowScrollButtonsMobile>
                        {tabs.map((tab) => {
                            const Icon = tab.icon;
                            return (
                                <Tab
                                    key={tab.key}
                                    label={tab.label}
                                    icon={<Icon sx={{ fontSize: 18 }} />}
                                    iconPosition="start"
                                    sx={{ textTransform: 'none', fontWeight: 700 }}
                                />
                            );
                        })}
                    </Tabs>
                </Box>

                <Box sx={{ pt: 3 }}>
                    {currentTab?.key === 'companies' && (
                        <Box>
                            <Typography variant="h5" sx={{ fontWeight: 700, color: 'primary.icon', mb: 2 }}>
                                Lista de empresas
                            </Typography>
                            <CompanyTable
                                refreshKey={companiesRefresh}
                                onRefresh={refreshCompanies}
                                onEditCompany={openEditCompany}
                                onManageUsers={openCompanyUsers}
                            />
                        </Box>
                    )}

                    {currentTab?.key === 'requests' && (
                        <Box>
                            <Typography variant="h5" sx={{ fontWeight: 700, color: 'primary.icon', mb: 2 }}>
                                Lista de solicitudes
                            </Typography>
                            <MaintenanceRequestTable
                                refreshKey={requestsRefresh}
                                onRefresh={refreshRequests}
                                onEditRequest={openEditRequest}
                            />
                        </Box>
                    )}
                </Box>
            </Container>

            <CompanyFormModal
                open={companyFormOpen}
                companyId={companyFormId}
                onClose={() => setCompanyFormOpen(false)}
                onSaved={refreshCompanies}
            />

            <CompanyUsersModal
                open={!!companyUsersTarget}
                companyId={companyUsersTarget?.id}
                companyName={companyUsersTarget?.name}
                onClose={() => setCompanyUsersTarget(null)}
                onSaved={refreshCompanies}
            />

            <MaintenanceRequestFormModal
                open={requestFormState.open}
                requestId={requestFormState.requestId}
                initialCompanyId={requestFormState.initialCompanyId}
                onClose={() => setRequestFormState({ open: false, requestId: null, initialCompanyId: '' })}
                onSaved={refreshRequests}
            />
        </Box>
    );
}
