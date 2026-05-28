import {usePermissions} from "../../../common/hooks/index.js";

import { useState } from 'react';
import { Box, Container, Typography, useMediaQuery, useTheme } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { Header } from '../../../common/components/Header';
import { NavDrawer } from '../../../common/components/NavDrawer';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import { PrimaryButton } from '../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../common/constants/permissions';
import CompanyTable from '../components/company/CompanyTable.jsx';
import CompanyFormModal from '../components/company/CompanyFormModal.jsx';
import CompanyUsersModal from '../components/company/CompanyUsersModal.jsx';

export default function CompaniesPage() {
    const [drawerOpen, setDrawerOpen] = useState(false);
    const [companyFormOpen, setCompanyFormOpen] = useState(false);
    const [companyFormId, setCompanyFormId] = useState(null);
    const [companyUsersTarget, setCompanyUsersTarget] = useState(null);
    const [companiesRefresh, setCompaniesRefresh] = useState(0);

    const { hasPermission, hasAnyPermission } = usePermissions();
    const theme = useTheme();
    const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

    const canViewCompanies = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.COMPANIES.READ,
        PERMISSIONS.MAINTENANCE.COMPANIES.MANAGE,
        PERMISSIONS.MAINTENANCE.COMPANIES.DELETE,
        PERMISSIONS.MAINTENANCE.COMPANY_USERS.READ,
        PERMISSIONS.MAINTENANCE.COMPANY_USERS.MANAGE,
        PERMISSIONS.MAINTENANCE.COMPANY_USERS.DELETE,
    ]);

    const openCreateCompany = () => {
        setCompanyFormId(null);
        setCompanyFormOpen(true);
    };
    const openEditCompany = (company) => {
        setCompanyFormId(company.id);
        setCompanyFormOpen(true);
    };
    const openCompanyUsers = (company) => setCompanyUsersTarget(company);
    const refreshCompanies = () => setCompaniesRefresh((v) => v + 1);

    if (!canViewCompanies) {
        return <AccessDeniedState />;
    }

    return (
        <Box className="maintenance-companies-page">
            <Header
                title="Gestión de Mantenimiento"
                onMenuClick={isMediumOrDown ? () => setDrawerOpen(true) : undefined}
            />
            <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />

            <Container maxWidth="xl" sx={{ pb: 3, pt: 3 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 2 }}>
                    <Box>
                        <Typography variant="h4" sx={{ fontWeight: 800, color: 'primary.icon', fontSize: { xs: '1.55rem', sm: '1.9rem' } }}>
                            Empresas
                        </Typography>
                        <Typography sx={{ color: 'text.secondary', fontSize: 14 }}>
                            Gestión de empresas y usuarios de empresas.
                        </Typography>
                    </Box>
                </Box>

                <Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 2 }}>
                        <Typography variant="h5" sx={{ fontWeight: 700, color: 'primary.icon' }}>
                            Lista de empresas
                        </Typography>
                        {hasPermission(PERMISSIONS.MAINTENANCE.COMPANIES.MANAGE) ? (
                            <PrimaryButton startIcon={<AddIcon />} onClick={openCreateCompany} sx={{ px: '28px' }}>
                                Crear
                            </PrimaryButton>
                        ) : null}
                    </Box>
                    <CompanyTable
                        refreshKey={companiesRefresh}
                        onRefresh={refreshCompanies}
                        onEditCompany={openEditCompany}
                        onManageUsers={openCompanyUsers}
                    />
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
        </Box>
    );
}

