import { useCallback, useEffect, useMemo, useState } from 'react';
import { Box, Chip, MenuItem, TextField, Typography } from '@mui/material';
import AutorenewOutlinedIcon from '@mui/icons-material/AutorenewOutlined';
import AssignmentTurnedInOutlinedIcon from '@mui/icons-material/AssignmentTurnedInOutlined';
import EditIcon from '@mui/icons-material/Edit';
import TableBase from '../../../../common/components/TablaBase.jsx';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import { PrimaryButton } from '../../../../common/components/PrimaryButton.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { fetchAssignments, generateAssignments, updateAssignment } from '../../services/assignment/assignmentService';
import { fetchDrivers } from '../../services/drivers/driversService';
import { fetchVehicles } from '../../services/vehicles/vehiclesService';
import { fetchTours } from '../../services/tours/toursService';
import { ASSIGNMENT_STATUS_OPTIONS, assignmentStatusLabel, formatDateTime, getStatusChipColor } from '../../transportUtils';

const COLUMN_TO_BACKEND_KEY = {
    driverName: 'driver.name',
    vehiclePlate: 'vehicle.plate',
    tourName: 'tour.name',
    statusRaw: 'status',
};

const INITIAL_VALUES = {
    driverId: '',
    vehicleId: '',
    tourId: '',
    status: 'PENDING',
    notes: '',
};

export default function AssignmentPanel({ refreshKey = 0, onRefresh }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [totalElements, setTotalElements] = useState(0);
    const [alert, setAlert] = useState(null);
    const [generating, setGenerating] = useState(false);
    const [internalRefresh, setInternalRefresh] = useState(0);
    const [editingAssignment, setEditingAssignment] = useState(null);
    const [formValues, setFormValues] = useState(INITIAL_VALUES);
    const [saving, setSaving] = useState(false);
    const [loadingReferences, setLoadingReferences] = useState(false);
    const [driverOptions, setDriverOptions] = useState([]);
    const [vehicleOptions, setVehicleOptions] = useState([]);
    const [tourOptions, setTourOptions] = useState([]);
    const { hasPermission } = usePermissions();

    const canGenerate = hasPermission(PERMISSIONS.TRANSPORT.ASSIGNMENT.GENERATE);
    const canUpdate = hasPermission(PERMISSIONS.TRANSPORT.ASSIGNMENT.UPDATE);

    const debouncedGlobalFilter = useDebounce(globalFilter, 350);
    const debouncedColumnFilters = useDebounce(columnFilters, 350);

    const backendFilters = useMemo(() => {
        const out = {};
        debouncedColumnFilters.forEach(({ id, value }) => {
            const key = COLUMN_TO_BACKEND_KEY[id];
            if (!key) return;
            if (value === null || value === undefined || value === '') return;
            out[key] = value;
        });
        return out;
    }, [debouncedColumnFilters]);

    const backendSort = useMemo(
        () => sorting
            .map(({ id, desc }) => {
                const key = COLUMN_TO_BACKEND_KEY[id];
                if (!key) return null;
                return `${key},${desc ? 'desc' : 'asc'}`;
            })
            .filter(Boolean),
        [sorting]
    );

    const handleGlobalFilterChange = useCallback((updater) => {
        setGlobalFilter((currentValue) => (typeof updater === 'function' ? updater(currentValue) : updater));
        setPagination((previousValue) => (previousValue.pageIndex === 0 ? previousValue : { ...previousValue, pageIndex: 0 }));
    }, []);

    const handleColumnFiltersChange = useCallback((updater) => {
        setColumnFilters((currentValue) => (typeof updater === 'function' ? updater(currentValue) : updater));
        setPagination((previousValue) => (previousValue.pageIndex === 0 ? previousValue : { ...previousValue, pageIndex: 0 }));
    }, []);

    const handleSortingChange = useCallback((updater) => {
        setSorting((currentValue) => (typeof updater === 'function' ? updater(currentValue) : updater));
        setPagination((previousValue) => (previousValue.pageIndex === 0 ? previousValue : { ...previousValue, pageIndex: 0 }));
    }, []);

    useEffect(() => {
        let ignore = false;

        const loadReferences = async () => {
            try {
                setLoadingReferences(true);
                const [driversResponse, vehiclesResponse, toursResponse] = await Promise.all([
                    fetchDrivers({ page: 0, size: 300 }),
                    fetchVehicles({ page: 0, size: 300 }),
                    fetchTours({ page: 0, size: 300 }),
                ]);
                if (ignore) return;

                setDriverOptions((driversResponse?.content ?? []).map((driver) => ({
                    id: driver.id,
                    label: [driver.firstName, driver.lastName].filter(Boolean).join(' ').trim() || 'Chofer',
                })));
                setVehicleOptions((vehiclesResponse?.content ?? []).map((vehicle) => ({
                    id: vehicle.id,
                    label: vehicle.plate ? `${vehicle.plate}${vehicle.model ? ` - ${vehicle.model}` : ''}` : 'Vehículo',
                })));
                setTourOptions((toursResponse?.content ?? []).map((tour) => ({
                    id: tour.id,
                    label: tour.name || 'Gira',
                })));
            } catch (error) {
                if (!ignore) {
                    setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron cargar referencias de asignación') });
                }
            } finally {
                if (!ignore) setLoadingReferences(false);
            }
        };

        void loadReferences();
        return () => {
            ignore = true;
        };
    }, []);

    useEffect(() => {
        let ignore = false;

        const load = async () => {
            try {
                setLoading(true);
                setError(null);
                const response = await fetchAssignments({
                    page: pagination.pageIndex,
                    size: pagination.pageSize,
                    search: debouncedGlobalFilter,
                    filters: backendFilters,
                    sort: backendSort,
                });
                if (ignore) return;

                setRows((response.content ?? []).map((assignment) => ({
                    id: assignment.id,
                    driverId: assignment.driver?.id ?? assignment.driverId ?? '',
                    driverName: [assignment.driver?.firstName, assignment.driver?.lastName].filter(Boolean).join(' ').trim() || assignment.driverName || '—',
                    vehicleId: assignment.vehicle?.id ?? assignment.vehicleId ?? '',
                    vehiclePlate: assignment.vehicle?.plate ?? assignment.vehiclePlate ?? '—',
                    tourId: assignment.tour?.id ?? assignment.tourId ?? '',
                    tourName: assignment.tour?.name ?? assignment.tourName ?? '—',
                    status: assignmentStatusLabel(assignment.status),
                    statusRaw: assignment.status ?? '',
                    notes: assignment.notes ?? '',
                    createdAt: assignment.createdAt ?? null,
                    updatedAt: assignment.updatedAt ?? null,
                })));
                setTotalElements(response.totalElements ?? 0);
            } catch (error) {
                if (!ignore) setError(getFriendlyApiErrorMessage(error, 'Error al cargar asignaciones'));
            } finally {
                if (!ignore) setLoading(false);
            }
        };

        void load();
        return () => {
            ignore = true;
        };
    }, [pagination.pageIndex, pagination.pageSize, debouncedGlobalFilter, backendFilters, backendSort, refreshKey, internalRefresh]);

    const columns = useMemo(() => [
        { accessorKey: 'tourName', header: 'Gira', size: 220, grow: true },
        { accessorKey: 'driverName', header: 'Chofer', size: 200, grow: true },
        { accessorKey: 'vehiclePlate', header: 'Vehículo', size: 150, grow: false },
        {
            accessorKey: 'status',
            header: 'Estado',
            size: 140,
            grow: false,
            Cell: ({ row, cell }) => (
                <Chip
                    label={cell.getValue() ?? '—'}
                    size="small"
                    color={getStatusChipColor(row.original.statusRaw)}
                    variant="outlined"
                />
            ),
        },
        {
            accessorKey: 'createdAt',
            header: 'Creada',
            size: 170,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
        {
            accessorKey: 'updatedAt',
            header: 'Actualizada',
            size: 170,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
    ], []);

    const handleGenerateAssignments = useCallback(async () => {
        setGenerating(true);
        try {
            await generateAssignments({});
            setInternalRefresh((value) => value + 1);
            onRefresh?.();
            setAlert({ type: 'success', message: 'Asignaciones generadas correctamente' });
        } catch (error) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudieron generar las asignaciones') });
        } finally {
            setGenerating(false);
        }
    }, [onRefresh]);

    const openEditAssignment = useCallback((assignment) => {
        setEditingAssignment(assignment);
        setFormValues({
            driverId: assignment.driverId ?? '',
            vehicleId: assignment.vehicleId ?? '',
            tourId: assignment.tourId ?? '',
            status: assignment.statusRaw || 'PENDING',
            notes: assignment.notes ?? '',
        });
    }, []);

    const handleSaveAssignment = useCallback(async () => {
        if (!editingAssignment) return;
        setSaving(true);
        try {
            await updateAssignment(editingAssignment.id, {
                driverId: formValues.driverId.trim() || null,
                vehicleId: formValues.vehicleId.trim() || null,
                tourId: formValues.tourId.trim() || null,
                status: formValues.status,
                notes: formValues.notes.trim() || null,
            });
            setEditingAssignment(null);
            setInternalRefresh((value) => value + 1);
            onRefresh?.();
            setAlert({ type: 'success', message: 'Asignación actualizada correctamente' });
        } catch (error) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(error, 'No se pudo actualizar la asignación') });
        } finally {
            setSaving(false);
        }
    }, [editingAssignment, formValues, onRefresh]);

    const renderRowActions = useMemo(() => {
        if (!canUpdate) return undefined;
        return ({ row }) => (
            <RowActionsMenu
                actions={[
                    {
                        key: 'edit',
                        label: 'Actualizar asignación',
                        icon: <EditIcon fontSize="small" />,
                        onClick: () => openEditAssignment(row.original),
                    },
                ]}
                tooltip="Ver acción"
            />
        );
    }, [canUpdate, openEditAssignment]);

    return (
        <>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2, alignItems: { xs: 'flex-start', md: 'center' }, flexDirection: { xs: 'column', md: 'row' }, mb: 2 }}>
                <Box>
                    <Typography variant="h6" sx={{ fontWeight: 700, color: 'primary.icon' }}>
                        Panel de asignaciones
                    </Typography>
                    <Typography sx={{ color: 'text.secondary', fontSize: 14 }}>
                        Genera y actualiza las asignaciones disponibles.
                    </Typography>
                </Box>
                {canGenerate ? (
                    <PrimaryButton
                        startIcon={<AutorenewOutlinedIcon />}
                        onClick={handleGenerateAssignments}
                        disabled={generating}
                        sx={{ px: 2.5 }}
                    >
                        {generating ? 'Generando...' : 'Generar asignaciones'}
                    </PrimaryButton>
                ) : null}
            </Box>

            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                error={error}
                enableRowActions={canUpdate}
                renderRowActions={renderRowActions}
                tableOptions={{
                    positionActionsColumn: 'last',
                    manualPagination: true,
                    manualFiltering: true,
                    manualSorting: true,
                    rowCount: totalElements,
                    onPaginationChange: setPagination,
                    onGlobalFilterChange: handleGlobalFilterChange,
                    onColumnFiltersChange: handleColumnFiltersChange,
                    onSortingChange: handleSortingChange,
                    state: { pagination, globalFilter, columnFilters, sorting },
                    initialState: {
                        columnVisibility: {
                            updatedAt: false,
                        },
                    },
                }}
                enableGlobalFilter
            />

            <GeneralModal
                open={!!editingAssignment}
                onClose={() => setEditingAssignment(null)}
                icon={AssignmentTurnedInOutlinedIcon}
                title="Actualizar asignación"
                subtitle={editingAssignment?.tourName}
                primaryButton={{
                    label: 'Guardar cambios',
                    onClick: handleSaveAssignment,
                    loading: saving,
                    disabled: saving || loadingReferences,
                }}
                secondaryButton={{
                    label: 'Cancelar',
                    onClick: () => setEditingAssignment(null),
                    disabled: saving,
                }}
            >
                <Box sx={{ p: 3, display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' } }}>
                    <TextField
                        select
                        label="Chofer"
                        value={formValues.driverId}
                        onChange={(event) => setFormValues((previousValue) => ({ ...previousValue, driverId: event.target.value }))}
                        fullWidth
                        disabled={loadingReferences}
                    >
                        <MenuItem value="">Sin chofer</MenuItem>
                        {driverOptions.map((option) => (
                            <MenuItem key={option.id} value={option.id}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        select
                        label="Vehículo"
                        value={formValues.vehicleId}
                        onChange={(event) => setFormValues((previousValue) => ({ ...previousValue, vehicleId: event.target.value }))}
                        fullWidth
                        disabled={loadingReferences}
                    >
                        <MenuItem value="">Sin vehículo</MenuItem>
                        {vehicleOptions.map((option) => (
                            <MenuItem key={option.id} value={option.id}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        select
                        label="Gira"
                        value={formValues.tourId}
                        onChange={(event) => setFormValues((previousValue) => ({ ...previousValue, tourId: event.target.value }))}
                        fullWidth
                        disabled={loadingReferences}
                    >
                        <MenuItem value="">Selecciona una gira</MenuItem>
                        {tourOptions.map((option) => (
                            <MenuItem key={option.id} value={option.id}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        select
                        label="Estado"
                        value={formValues.status}
                        onChange={(event) => setFormValues((previousValue) => ({ ...previousValue, status: event.target.value }))}
                        fullWidth
                    >
                        {ASSIGNMENT_STATUS_OPTIONS.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        label="Notas"
                        value={formValues.notes}
                        onChange={(event) => setFormValues((previousValue) => ({ ...previousValue, notes: event.target.value }))}
                        multiline
                        minRows={3}
                        fullWidth
                        sx={{ gridColumn: { md: '1 / -1' } }}
                    />
                </Box>
            </GeneralModal>

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}
