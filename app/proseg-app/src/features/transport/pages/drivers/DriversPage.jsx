import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Alert, Box, Container, LinearProgress, ListItemIcon, ListItemText, Menu, MenuItem, Stack, Tab, Tabs } from '@mui/material';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import PeopleAltOutlinedIcon from '@mui/icons-material/PeopleAltOutlined';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import TuneOutlinedIcon from '@mui/icons-material/TuneOutlined';
import { MonthPicker, PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { PrimaryButton } from '../../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import DriversTable from '../../components/drivers/DriversTable.jsx';
import DriverFormModal from '../../components/drivers/DriverFormModal.jsx';
import DriverSurplusSummary from '../../components/drivers/DriverSurplusSummary.jsx';
import { shortPeriodLabel } from '../../components/tourProcessing/tourProcessingDraftLabels.js';
import { usePersistedMonth } from '../../hooks/usePersistedMonth.js';
import { useTransportDriverMonthsData } from '../../hooks/useTransportDriverMonthsData.js';
import { useTransportDriverMonthSurplusCap } from '../../hooks/useTransportDriverMonthSurplusCap.js';
import { exportDriverOvertimeRequest } from '../../services/drivers/driversService';
import { exportMonthTourControl } from '../../services/tourProcessing/tourProcessingService';
import { downloadBlob } from '../../transportUtils.js';
import { queryKeys } from '../../../../common/query';

const SELECTED_MONTH_STORAGE_KEY = 'transport-drivers-selected-month';

const DRIVER_SUB_TABS = [
    { key: 'own', label: 'Choferes de la unidad', unit: 'GENERAL_SERVICES', icon: BadgeOutlinedIcon },
    { key: 'other', label: 'Choferes de otra unidad', unit: 'OTHER_UNIT', icon: PeopleAltOutlinedIcon },
];

export default function DriversPage() {
    const [driverSubTab, setDriverSubTab] = useState('own');
    const activeDriverSubTab = DRIVER_SUB_TABS.find((tab) => tab.key === driverSubTab);
    const [driverFormOpen, setDriverFormOpen] = useState(false);
    const [driverFormId, setDriverFormId] = useState(null);
    const [selectedMonth, setSelectedMonth] = usePersistedMonth(SELECTED_MONTH_STORAGE_KEY);
    const [optionsAnchor, setOptionsAnchor] = useState(null);
    const [alert, setAlert] = useState(null);
    const queryClient = useQueryClient();
    const { hasPermission, hasAnyPermission } = usePermissions();

    const canViewDrivers = hasAnyPermission([
        PERMISSIONS.TRANSPORT.DRIVERS.READ,
        PERMISSIONS.TRANSPORT.DRIVERS.MANAGE,
        PERMISSIONS.TRANSPORT.DRIVERS.DELETE,
    ]);

    const year = selectedMonth?.year();
    const month = selectedMonth ? selectedMonth.month() + 1 : undefined;
    const monthData = useTransportDriverMonthsData(year, month);
    const { businessDays: defaultBusinessDays, surplusHoursPerDriver } = useTransportDriverMonthSurplusCap(year, month);
    const showEmptyMonthNotice = monthData.enabled && !monthData.loading && !monthData.hasAnyRecord;
    const canReadDrivers = hasPermission(PERMISSIONS.TRANSPORT.DRIVERS.READ);
    const canManageDrivers = hasPermission(PERMISSIONS.TRANSPORT.DRIVERS.MANAGE);
    const canDeleteDrivers = hasPermission(PERMISSIONS.TRANSPORT.DRIVERS.DELETE);
    const periodLabel = monthData.enabled ? shortPeriodLabel(year, month) : null;
    const canExportOvertime = Boolean(periodLabel) && canReadDrivers;
    const canExportTourControl = Boolean(periodLabel) && hasPermission(PERMISSIONS.TRANSPORT.TOURS.EXPORT);

    const overtimeRequestMutation = useMutation({
        mutationFn: () => exportDriverOvertimeRequest(year, month),
        onSuccess: ({ blob, fileName }) => downloadBlob(blob, fileName),
        onError: (error) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo generar la solicitud de horas extra') });
        },
    });
    const tourControlMutation = useMutation({
        mutationFn: () => exportMonthTourControl(year, month),
        onSuccess: ({ blob, fileName }) => downloadBlob(blob, fileName),
        onError: (error) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo generar el control de giras') });
        },
    });
    const exportingOvertimeRequest = overtimeRequestMutation.isPending;
    const exportingTourControl = tourControlMutation.isPending;

    const closeOptions = () => setOptionsAnchor(null);

    const handleExportTourControl = () => {
        closeOptions();
        if (!tourControlMutation.isPending) tourControlMutation.mutate();
    };

    const handleExportOvertimeRequest = () => {
        closeOptions();
        if (!overtimeRequestMutation.isPending) overtimeRequestMutation.mutate();
    };

    const openCreateDriver = () => {
        closeOptions();
        setDriverFormId(null);
        setDriverFormOpen(true);
    };

    const openEditDriver = (driver) => {
        setDriverFormId(driver.id);
        setDriverFormOpen(true);
    };

    const refreshDrivers = () => {
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.drivers() });
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessingDrafts() });
    };

    const refreshDriverMonth = () => {
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.driverMonths(year, month) });
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessingDrafts() });
    };

    const editingDriverMonth = driverFormId ? monthData.byDriverId?.get(driverFormId) : undefined;

    if (!canViewDrivers) return <AccessDeniedState />;

    return (
        <Box className="transport-drivers-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Choferes"
                    description="Gestión de choferes."
                    action={canManageDrivers || canExportOvertime || canExportTourControl ? (
                        <>
                            <PrimaryButton
                                onClick={(event) => setOptionsAnchor(event.currentTarget)}
                                startIcon={<TuneOutlinedIcon />}
                                endIcon={<KeyboardArrowDownIcon />}
                                aria-haspopup="menu"
                                aria-expanded={Boolean(optionsAnchor)}
                                sx={{ px: '28px' }}
                            >
                                Opciones
                            </PrimaryButton>
                            <Menu
                                anchorEl={optionsAnchor}
                                open={Boolean(optionsAnchor)}
                                onClose={closeOptions}
                                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                            >
                                {canManageDrivers && (
                                    <MenuItem onClick={openCreateDriver}>
                                        <ListItemIcon><AddOutlinedIcon fontSize="small" /></ListItemIcon>
                                        <ListItemText>Crear chofer</ListItemText>
                                    </MenuItem>
                                )}
                                {canExportTourControl && (
                                    <MenuItem onClick={handleExportTourControl} disabled={exportingTourControl}>
                                        <ListItemIcon><DownloadOutlinedIcon fontSize="small" /></ListItemIcon>
                                        <ListItemText>{`Descargar 'Control de Giras ${periodLabel}'`}</ListItemText>
                                    </MenuItem>
                                )}
                                {canExportOvertime && (
                                    <MenuItem onClick={handleExportOvertimeRequest} disabled={exportingOvertimeRequest}>
                                        <ListItemIcon><DownloadOutlinedIcon fontSize="small" /></ListItemIcon>
                                        <ListItemText>{`Descargar 'Solicitud de Horas Extra ${periodLabel}'`}</ListItemText>
                                    </MenuItem>
                                )}
                            </Menu>
                        </>
                    ) : null}
                />

                {(exportingOvertimeRequest || exportingTourControl) && <LinearProgress sx={{ mt: 2 }} />}

                <Box sx={{ borderBottom: 1, borderColor: 'divider', mt: 3 }}>
                    <Tabs
                        value={driverSubTab}
                        onChange={(_, value) => setDriverSubTab(value)}
                        variant="scrollable"
                        scrollButtons="auto"
                        allowScrollButtonsMobile
                    >
                        {DRIVER_SUB_TABS.map((tab) => {
                            const Icon = tab.icon;
                            return (
                                <Tab
                                    key={tab.key}
                                    value={tab.key}
                                    label={tab.label}
                                    icon={<Icon sx={{ fontSize: 18 }} />}
                                    iconPosition="start"
                                    sx={{ textTransform: 'none', fontWeight: 700 }}
                                />
                            );
                        })}
                    </Tabs>
                </Box>

                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={3}
                    alignItems={{ xs: 'stretch', sm: 'center' }}
                    flexWrap="wrap"
                    useFlexGap
                    sx={{ pt: 3 }}
                >
                    <Box sx={{ width: { xs: '100%', sm: 220 }, flexShrink: 0 }}>
                        <MonthPicker
                            label="Mes"
                            ariaLabel="Ver datos de un mes especifico"
                            value={selectedMonth}
                            onChange={setSelectedMonth}
                            clearable
                        />
                    </Box>
                    {monthData.enabled && surplusHoursPerDriver !== null && (
                        <DriverSurplusSummary surplusCapPerDriver={surplusHoursPerDriver} />
                    )}
                    {showEmptyMonthNotice && (
                        <Alert severity="info" variant="outlined" icon={<InfoOutlinedIcon fontSize="small" />} sx={{ py: 0 }}>
                            No hay ningún registro para este mes todavía.
                        </Alert>
                    )}
                </Stack>

                <Box sx={{ pt: 3 }}>
                    <DriversTable
                        key={driverSubTab}
                        unit={activeDriverSubTab.unit}
                        onEditDriver={openEditDriver}
                        monthData={monthData}
                    />
                </Box>
            </Container>

            {driverFormOpen ? (
                <DriverFormModal
                    open={driverFormOpen}
                    driverId={driverFormId}
                    canEdit={canManageDrivers}
                    canDelete={canDeleteDrivers}
                    onClose={() => setDriverFormOpen(false)}
                    onSaved={refreshDrivers}
                    onBusinessDaysSaved={refreshDriverMonth}
                    period={monthData.enabled ? { year, month, label: periodLabel } : null}
                    monthBusinessDays={editingDriverMonth?.businessDays ?? null}
                    defaultBusinessDays={defaultBusinessDays ?? null}
                    businessDaysCustomized={Boolean(editingDriverMonth?.businessDaysCustomized)}
                    onDeleted={() => {
                        refreshDrivers();
                        setAlert({ type: 'success', message: 'Chofer eliminado correctamente' });
                    }}
                />
            ) : null}

            <DialogModal
                open={Boolean(alert)}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </Box>
    );
}
