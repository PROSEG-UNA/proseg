import { usePermissions } from '../../../../common/hooks/index.js';

import { useState, useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Box, Container, Typography, Tabs, Tab, Button, CircularProgress } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DomainOutlinedIcon from '@mui/icons-material/DomainOutlined';
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import { PrimaryButton } from '../../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import CompanyTable from '../../components/company/CompanyTable.jsx';
import CompanyFormModal from '../../components/company/CompanyFormModal.jsx';
import CompanyUsersModal from '../../components/company/CompanyUsersModal.jsx';
import SearchableSelect from '../../../../common/components/SearchableSelect.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { fetchCompanies, fetchCompanyUsers, assignCompanyUser, unassignCompanyUser } from '../../services/company/companiesService';
import { searchUsers } from '../../../security/services/usersService';
import { queryKeys } from '../../../../common/query';

const getUserDisplayName = (u) =>
    [u.firstName, u.lastName].filter(Boolean).join(' ').trim() || u.username || u.id;

export default function CompaniesPage() {
    const [companyFormOpen, setCompanyFormOpen] = useState(false);
    const [companyFormId, setCompanyFormId] = useState(null);
    const [companyUsersTarget, setCompanyUsersTarget] = useState(null);
    const queryClient = useQueryClient();

    const { hasPermission, hasAnyPermission } = usePermissions();

    const canViewCompanies = hasAnyPermission([
        PERMISSIONS.MAINTENANCE.COMPANIES.READ,
        PERMISSIONS.MAINTENANCE.COMPANIES.MANAGE,
        PERMISSIONS.MAINTENANCE.COMPANIES.DELETE,
        PERMISSIONS.MAINTENANCE.COMPANY_USERS.READ,
        PERMISSIONS.MAINTENANCE.COMPANY_USERS.MANAGE,
        PERMISSIONS.MAINTENANCE.COMPANY_USERS.DELETE,
    ]);

    const openCreateCompany = () => { setCompanyFormId(null); setCompanyFormOpen(true); };
    const openEditCompany   = (company) => { setCompanyFormId(company.id); setCompanyFormOpen(true); };
    const openCompanyUsers  = (company) => setCompanyUsersTarget(company);
    const refreshCompanies = () => {
        void queryClient.invalidateQueries({ queryKey: queryKeys.maintenance.companies() });
    };
    const [tabIndex, setTabIndex] = useState(0);

    const [inspectCompanyId, setInspectCompanyId]       = useState(null);
    const [inspectCompanyName, setInspectCompanyName]   = useState('');
    const [inspectUsers, setInspectUsers]               = useState([]);
    const [loadingInspectUsers, setLoadingInspectUsers] = useState(false);
    const [companiesOptions, setCompaniesOptions]       = useState([]);
    const [loadingCompaniesOptions, setLoadingCompaniesOptions] = useState(false);
    const [processingAction, setProcessingAction]       = useState(false);
    const [userSearch, setUserSearch]                   = useState('');
    const [availableUsersTab, setAvailableUsersTab]     = useState([]);
    const [userLoading, setUserLoading]                 = useState(false);
    const [selectedUserIdTab, setSelectedUserIdTab]     = useState('');
    const [selectedUserTab, setSelectedUserTab]         = useState(null);
    const [assigningUser, setAssigningUser]             = useState(false);
    const [confirmUnassignUser, setConfirmUnassignUser] = useState(null);
    const [duplicateWarning, setDuplicateWarning]       = useState(null);

    useEffect(() => {
        let cancelled = false;
        if (tabIndex !== 1) return;
        setLoadingCompaniesOptions(true);
        fetchCompanies({ page: 0, size: 200 })
            .then(page => { if (!cancelled) setCompaniesOptions(page.content ?? []); })
            .catch(() => {})
            .finally(() => { if (!cancelled) setLoadingCompaniesOptions(false); });
        return () => { cancelled = true; };
    }, [tabIndex]);

    useEffect(() => {
        let cancelled = false;
        if (!inspectCompanyId) { setInspectUsers([]); return; }
        setLoadingInspectUsers(true);
        fetchCompanyUsers(inspectCompanyId)
            .then(data => { if (!cancelled) setInspectUsers(Array.isArray(data) ? data : []); })
            .catch(() => { if (!cancelled) setInspectUsers([]); })
            .finally(() => { if (!cancelled) setLoadingInspectUsers(false); });
        return () => { cancelled = true; };
    }, [inspectCompanyId]);

    useEffect(() => {
        if (!inspectCompanyId) { setConfirmUnassignUser(null); setDuplicateWarning(null); }
    }, [inspectCompanyId]);

    useEffect(() => {
        let cancelled = false;
        if (tabIndex !== 1) return;
        setUserLoading(true);
        searchUsers({ page: 0, size: 8, search: userSearch })
            .then((resp) => { if (!cancelled) setAvailableUsersTab(resp.content ?? []); })
            .catch(() => { if (!cancelled) setAvailableUsersTab([]); })
            .finally(() => { if (!cancelled) setUserLoading(false); });
        return () => { cancelled = true; };
    }, [userSearch, tabIndex]);

    if (!canViewCompanies) return <AccessDeniedState />;

    return (
        <Box className="maintenance-companies-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Empresas"
                    description="Gestión de empresas y usuarios de empresas."
                    action={tabIndex === 0 && hasPermission(PERMISSIONS.MAINTENANCE.COMPANIES.MANAGE) ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={openCreateCompany} sx={{ px: '28px' }}>
                            Crear
                        </PrimaryButton>
                    ) : null}
                />

                <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 1 }}>
                    <Tabs value={tabIndex} onChange={(e, v) => setTabIndex(v)} variant="scrollable" scrollButtons="auto" allowScrollButtonsMobile>
                        <Tab label="Empresas" icon={<DomainOutlinedIcon sx={{ fontSize: 18 }} />} iconPosition="start" sx={{ textTransform: 'none', fontWeight: 700 }} />
                        <Tab label="Usuarios vinculados" icon={<GroupsOutlinedIcon sx={{ fontSize: 18 }} />} iconPosition="start" sx={{ textTransform: 'none', fontWeight: 700 }} />
                    </Tabs>
                </Box>

                <Box sx={{ pt: 3 }}>
                    {tabIndex === 0 && (
                        <CompanyTable
                            onEditCompany={openEditCompany}
                            onManageUsers={openCompanyUsers}
                        />
                    )}

                    {tabIndex === 1 && (
                        <Box>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                                <Typography variant="h6" sx={{ fontWeight: 700 }}>Ver usuarios vinculados</Typography>
                                <Box sx={{ flex: 1 }} />
                            </Box>

                            <Box sx={{ mb: 2 }}>
                                <SearchableSelect
                                    label="Selecciona una empresa"
                                    value={inspectCompanyId ?? ''}
                                    onChange={(id) => {
                                        setInspectCompanyId(id || null);
                                        const c = companiesOptions.find(x => x.id === id);
                                        setInspectCompanyName(c?.name ?? '');
                                    }}
                                    items={companiesOptions}
                                    getItemLabel={c => c.name}
                                    getItemValue={c => c.id}
                                    fullWidth
                                    size="small"
                                    disabled={loadingCompaniesOptions}
                                    externalSearch={''}
                                />
                            </Box>

                            <Box>
                                {inspectCompanyId ? (
                                    loadingInspectUsers ? (
                                        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}><CircularProgress /></Box>
                                    ) : (
                                        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                                            <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mb: 1 }}>
                                                <SearchableSelect
                                                    label="Buscar usuario"
                                                    value={selectedUserIdTab}
                                                    onChange={(id) => {
                                                        setSelectedUserIdTab(id);
                                                        setSelectedUserTab(availableUsersTab.find(x => x.id === id) ?? null);
                                                    }}
                                                    items={availableUsersTab}
                                                    getItemLabel={(u) => {
                                                        const name = getUserDisplayName(u);
                                                        return u.email ? `${name} · ${u.email}` : name;
                                                    }}
                                                    getItemValue={u => u.id}
                                                    fullWidth
                                                    size="small"
                                                    externalSearch={userSearch}
                                                    onSearchChange={(v) => setUserSearch(v)}
                                                    disabled={userLoading}
                                                />
                                                <Button
                                                    size="small"
                                                    variant="outlined"
                                                    disabled={assigningUser || !selectedUserTab}
                                                    onClick={async () => {
                                                        if (!inspectCompanyId || !selectedUserTab) return;
                                                        if (inspectUsers.find(x => x.id === selectedUserTab.id)) {
                                                            setDuplicateWarning(selectedUserTab);
                                                            return;
                                                        }
                                                        setAssigningUser(true);
                                                        try {
                                                            await assignCompanyUser(inspectCompanyId, selectedUserTab.id);
                                                            const updated = await fetchCompanyUsers(inspectCompanyId);
                                                            setInspectUsers(Array.isArray(updated) ? updated : []);
                                                            setSelectedUserIdTab('');
                                                            setSelectedUserTab(null);
                                                        } catch (err) {
                                                            alert(err?.response?.data?.message ?? err?.message ?? 'No se pudo vincular el usuario');
                                                        } finally {
                                                            setAssigningUser(false);
                                                        }
                                                    }}
                                                >
                                                    Vincular
                                                </Button>
                                            </Box>

                                            {inspectUsers.length === 0 ? (
                                                <Typography sx={{ color: 'text.secondary' }}>No hay usuarios vinculados a la empresa seleccionada.</Typography>
                                            ) : (
                                                inspectUsers.map((u) => (
                                                    <Box key={u.id} sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 1 }}>
                                                        <Box sx={{ minWidth: 0 }}>
                                                            <Typography sx={{ fontWeight: 800 }}>{getUserDisplayName(u)}</Typography>
                                                            <Typography sx={{ fontSize: 13, color: 'text.secondary' }}>{u.email || '—'}</Typography>
                                                            {u.username && (
                                                                <Typography sx={{ fontSize: 12, color: 'text.secondary' }}>{u.username}</Typography>
                                                            )}
                                                        </Box>
                                                        <Box sx={{ display: 'flex', gap: 1 }}>
                                                            <Button size="small" color="error" variant="outlined" disabled={processingAction} onClick={() => setConfirmUnassignUser(u)}>
                                                                Desasignar
                                                            </Button>
                                                        </Box>
                                                    </Box>
                                                ))
                                            )}
                                        </Box>
                                    )
                                ) : (
                                    <Typography sx={{ color: 'text.secondary' }}>Selecciona una empresa para ver sus usuarios vinculados.</Typography>
                                )}
                            </Box>
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

            <DialogModal
                open={!!confirmUnassignUser}
                type="delete"
                title="Desasignar usuario"
                message={`¿Seguro que deseas desasignar a "${confirmUnassignUser ? getUserDisplayName(confirmUnassignUser) : ''}" de ${inspectCompanyName}?`}
                onClose={() => setConfirmUnassignUser(null)}
                onConfirm={async () => {
                    if (!inspectCompanyId || !confirmUnassignUser) return;
                    setProcessingAction(true);
                    try {
                        await unassignCompanyUser(inspectCompanyId, confirmUnassignUser.id);
                        const updated = await fetchCompanyUsers(inspectCompanyId);
                        setInspectUsers(Array.isArray(updated) ? updated : []);
                        setConfirmUnassignUser(null);
                    } catch (err) {
                        setConfirmUnassignUser(null);
                        alert(err?.response?.data?.message ?? err?.message ?? 'No se pudo desasignar');
                    } finally {
                        setProcessingAction(false);
                    }
                }}
                confirmLabel="Desasignar"
            />

            <DialogModal
                open={!!duplicateWarning}
                type="warning"
                title="Usuario ya vinculado"
                message={`"${duplicateWarning ? getUserDisplayName(duplicateWarning) : ''}" ya está vinculado a esta empresa.`}
                onClose={() => setDuplicateWarning(null)}
                confirmLabel="Entendido"
                cancelLabel="Cerrar"
            />
        </Box>
    );
}