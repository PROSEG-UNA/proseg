import { useCallback, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
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
import { useDriverOptions, useTourOptions, useVehicleOptions } from '../../hooks/useTransportOptions';
import { useQueryAlert } from '../../../../common/hooks/index.js';
import { ASSIGNMENT_STATUS_OPTIONS, assignmentStatusLabel, formatDateTime, getStatusChipColor } from '../../transportUtils';
import { keepPreviousPage, queryKeys } from '../../../../common/query';

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

const EMPTY_LIST = [];

function mapAssignmentToRow(assignment) {
    return {
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
    };
}

export default function AssignmentPanel() {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [editingAssignment, setEditingAssignment] = useState(null);
    const [formValues, setFormValues] = useState(INITIAL_VALUES);
    const queryClient = useQueryClient();
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

    const drivers = useDriverOptions();
    const vehicles = useVehicleOptions();
    const tours = useTourOptions();

    const driverOptions = drivers.options;
    const vehicleOptions = vehicles.options;
    const tourOptions = tours.options;
    const loadingReferences = drivers.loading || vehicles.loading || tours.loading;

    const referencesError = drivers.error ?? vehicles.error ?? tours.error;

    const { alert, setAlert, closeAlert } = useQueryAlert(
        referencesError ? getFriendlyApiErrorMessage(referencesError, 'No se pudieron cargar referencias de asignación') : null
    );

    const assignmentsRequestParams = {
        page: pagination.pageIndex,
        size: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
    };

    const listQueryKey = queryKeys.transport.assignmentList(assignmentsRequestParams);

    const assignmentsQuery = useQuery({
        queryKey: listQueryKey,
        placeholderData: keepPreviousPage(listQueryKey),
        queryFn: async () => {
            const response = await fetchAssignments(assignmentsRequestParams);
            return {
                rows: (response.content ?? []).map(mapAssignmentToRow),
                totalElements: response.totalElements ?? 0,
            };
        },
    });

    const rows = assignmentsQuery.data?.rows ?? EMPTY_LIST;
    const totalElements = assignmentsQuery.data?.totalElements ?? 0;
    const loading = assignmentsQuery.isLoading;
    const fetching = assignmentsQuery.isFetching;
    const error = assignmentsQuery.error
        ? getFriendlyApiErrorMessage(assignmentsQuery.error, 'Error al cargar asignaciones')
        : null;

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

    const invalidateAssignments = () => queryClient.invalidateQueries({
        queryKey: queryKeys.transport.assignment(),
    });

    const generateAssignmentsMutation = useMutation({
        mutationFn: () => generateAssignments({}),
        onSuccess: async () => {
            setAlert({ type: 'success', message: 'Asignaciones generadas correctamente' });
            await invalidateAssignments();
        },
        onError: (generateError) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(generateError, 'No se pudieron generar las asignaciones') });
        },
    });

    const updateAssignmentMutation = useMutation({
        mutationFn: ({ assignmentId, payload }) => updateAssignment(assignmentId, payload),
        onSuccess: async () => {
            setEditingAssignment(null);
            setAlert({ type: 'success', message: 'Asignación actualizada correctamente' });
            await invalidateAssignments();
        },
        onError: (updateError) => {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(updateError, 'No se pudo actualizar la asignación') });
        },
    });

    const generating = generateAssignmentsMutation.isPending;
    const saving = updateAssignmentMutation.isPending;

    const handleGenerateAssignments = useCallback(() => {
        generateAssignmentsMutation.mutate();
    }, [generateAssignmentsMutation]);

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

    const handleSaveAssignment = useCallback(() => {
        if (!editingAssignment) return;
        updateAssignmentMutation.mutate({
            assignmentId: editingAssignment.id,
            payload: {
                driverId: formValues.driverId.trim() || null,
                vehicleId: formValues.vehicleId.trim() || null,
                tourId: formValues.tourId.trim() || null,
                status: formValues.status,
                notes: formValues.notes.trim() || null,
            },
        });
    }, [editingAssignment, formValues, updateAssignmentMutation]);

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
                fetching={fetching}
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
                onClose={closeAlert}
            />
        </>
    );
}
