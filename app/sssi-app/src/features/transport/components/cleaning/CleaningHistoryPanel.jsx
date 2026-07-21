import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Box, Chip, Stack, Typography } from '@mui/material';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import TableBase from '../../../../common/components/TablaBase.jsx';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { formatDateTime } from '../../transportUtils.js';
import { getFriendlyApiErrorMessage } from '../../../../common/utils/index.js';
import { fetchCleaningHistory, fetchCleaningHistoryById } from '../../services/cleaning/cleaningService.js';

const COLUMN_TO_BACKEND_KEY = {
    executedAt: 'executionDate',
    executedBy: 'executedBy',
    fileName: 'fileName',
    finalStatus: 'finalStatus',
};

function statusLabel(value) {
    if (!value) return '—';
    if (value === 'SUCCESS') return 'Éxito';
    if (value === 'FAILED') return 'Fallido';
    if (value === 'PARTIAL') return 'Parcial';
    return value;
}

function statusColor(value) {
    if (value === 'SUCCESS') return 'success';
    if (value === 'FAILED') return 'error';
    if (value === 'PARTIAL') return 'warning';
    return 'default';
}

function formatDurationMs(value) {
    if (value === null || value === undefined) return '—';
    const numeric = Number(value);
    if (Number.isNaN(numeric)) return String(value);
    if (numeric < 1000) return `${numeric} ms`;
    return `${(numeric / 1000).toFixed(2)} s`;
}

function countNew(record) {
    return (record?.createdDrivers ?? 0) + (record?.createdVehicles ?? 0) + (record?.createdTours ?? 0);
}

function countUpdated(record) {
    return (record?.updatedDrivers ?? 0) + (record?.updatedVehicles ?? 0) + (record?.updatedTours ?? 0);
}

export default function CleaningHistoryPanel() {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([{ id: 'executedAt', desc: true }]);
    const [rows, setRows] = useState([]);
    const [totalElements, setTotalElements] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [alert, setAlert] = useState(null);
    const [selectedId, setSelectedId] = useState(null);
    const [selectedDetail, setSelectedDetail] = useState(null);
    const [loadingDetail, setLoadingDetail] = useState(false);

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
                const key = id;
                if (!key) return null;
                return `${key},${desc ? 'desc' : 'asc'}`;
            })
            .filter(Boolean),
        [sorting]
    );

    const handleGlobalFilterChange = useCallback((updater) => {
        setGlobalFilter((currentValue) => (typeof updater === 'function' ? updater(currentValue) : updater));
        setPagination((currentValue) => (currentValue.pageIndex === 0 ? currentValue : { ...currentValue, pageIndex: 0 }));
    }, []);

    const handleColumnFiltersChange = useCallback((updater) => {
        setColumnFilters((currentValue) => (typeof updater === 'function' ? updater(currentValue) : updater));
        setPagination((currentValue) => (currentValue.pageIndex === 0 ? currentValue : { ...currentValue, pageIndex: 0 }));
    }, []);

    const handleSortingChange = useCallback((updater) => {
        setSorting((currentValue) => (typeof updater === 'function' ? updater(currentValue) : updater));
        setPagination((currentValue) => (currentValue.pageIndex === 0 ? currentValue : { ...currentValue, pageIndex: 0 }));
    }, []);

    useEffect(() => {
        let ignore = false;
        const load = async () => {
            try {
                setLoading(true);
                setError(null);
                const response = await fetchCleaningHistory({
                    page: pagination.pageIndex,
                    size: pagination.pageSize,
                    search: debouncedGlobalFilter,
                    filters: backendFilters,
                    sort: backendSort,
                });
                if (ignore) return;
                const items = response?.content ?? [];
                setRows(items.map((record) => ({
                    id: record.id,
                    executedAt: record.executedAt ?? null,
                    executedBy: record.executedByDisplay ?? record.executedBy ?? '—',
                    fileName: record.fileName ?? '—',
                    finalStatus: record.finalStatus ?? '',
                    totalReadRecords: record.totalReadRecords ?? 0,
                    duplicatesDetected: record.duplicatesDetected ?? 0,
                    invalidRecords: record.invalidRecords ?? 0,
                    durationMs: record.durationMs ?? 0,
                    newCount: countNew(record),
                    updatedCount: countUpdated(record),
                })));
                setTotalElements(response?.totalElements ?? 0);
            } catch (requestError) {
                if (!ignore) setError(getFriendlyApiErrorMessage(requestError, 'No se pudo cargar el historial de depuraciones'));
            } finally {
                if (!ignore) setLoading(false);
            }
        };
        void load();
        return () => {
            ignore = true;
        };
    }, [pagination.pageIndex, pagination.pageSize, debouncedGlobalFilter, backendFilters, backendSort]);

    useEffect(() => {
        if (!selectedId) return;
        let ignore = false;
        const loadDetail = async () => {
            try {
                setLoadingDetail(true);
                const response = await fetchCleaningHistoryById(selectedId);
                if (!ignore) setSelectedDetail(response);
            } catch (requestError) {
                if (!ignore) {
                    setAlert({ type: 'error', message: getFriendlyApiErrorMessage(requestError, 'No se pudo cargar el detalle de depuración') });
                    setSelectedId(null);
                }
            } finally {
                if (!ignore) setLoadingDetail(false);
            }
        };
        void loadDetail();
        return () => {
            ignore = true;
        };
    }, [selectedId]);

    const columns = useMemo(() => [
        {
            accessorKey: 'executedAt',
            header: 'Fecha',
            size: 170,
            grow: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
        { accessorKey: 'executedBy', header: 'Usuario', size: 180, grow: true },
        { accessorKey: 'fileName', header: 'Archivo', size: 220, grow: true },
        {
            accessorKey: 'finalStatus',
            header: 'Estado',
            size: 120,
            grow: false,
            Cell: ({ cell }) => (
                <Chip
                    label={statusLabel(cell.getValue())}
                    color={statusColor(cell.getValue())}
                    size="small"
                    variant="outlined"
                />
            ),
        },
        { accessorKey: 'totalReadRecords', header: 'Total registros', size: 120, grow: false },
        { accessorKey: 'newCount', header: 'Nuevos', size: 90, grow: false },
        { accessorKey: 'updatedCount', header: 'Actualizados', size: 110, grow: false },
        { accessorKey: 'duplicatesDetected', header: 'Duplicados', size: 100, grow: false },
        { accessorKey: 'invalidRecords', header: 'Inválidos', size: 100, grow: false },
        {
            accessorKey: 'durationMs',
            header: 'Duración',
            size: 120,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDurationMs(cell.getValue()),
        },
    ], []);

    const detailRows = selectedDetail?.details ?? [];
    const detailColumns = useMemo(() => [
        { accessorKey: 'originalRowNumber', header: 'Fila', size: 80, grow: false },
        { accessorKey: 'recordType', header: 'Tipo', size: 120, grow: false },
        { accessorKey: 'actionPerformed', header: 'Acción realizada', size: 180, grow: false },
        { accessorKey: 'processingResult', header: 'Resultado', size: 100, grow: false },
        { accessorKey: 'observations', header: 'Observaciones', size: 220, grow: true },
        { accessorKey: 'rejectionReason', header: 'Motivo rechazo', size: 220, grow: true },
        { accessorKey: 'validationErrors', header: 'Errores validación', size: 220, grow: true },
    ], []);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                error={error}
                enableRowActions
                renderRowActions={({ row }) => (
                    <RowActionsMenu
                        tooltip="Ver acción"
                        actions={[{
                            key: 'view',
                            label: 'Ver detalle',
                            icon: <VisibilityOutlinedIcon fontSize="small" />,
                            onClick: () => setSelectedId(row.original.id),
                        }]}
                    />
                )}
                tableOptions={{
                    manualPagination: true,
                    manualFiltering: true,
                    manualSorting: true,
                    rowCount: totalElements,
                    onPaginationChange: setPagination,
                    onGlobalFilterChange: handleGlobalFilterChange,
                    onColumnFiltersChange: handleColumnFiltersChange,
                    onSortingChange: handleSortingChange,
                    state: { pagination, globalFilter, columnFilters, sorting },
                }}
                enableGlobalFilter
            />

            <GeneralModal
                open={!!selectedId}
                onClose={() => {
                    setSelectedId(null);
                    setSelectedDetail(null);
                }}
                title="Detalle de depuración"
                subtitle={selectedDetail?.fileName ?? ''}
                maxWidth="xl"
                fillHeight
                primaryButton={{
                    label: 'Cerrar',
                    onClick: () => {
                        setSelectedId(null);
                        setSelectedDetail(null);
                    },
                }}
                loading={loadingDetail}
            >
                <Box sx={{ p: 3 }}>
                    {selectedDetail ? (
                        <Stack spacing={2}>
                            <Typography sx={{ fontWeight: 700, fontSize: 16 }}>Resumen</Typography>
                            <Box sx={{ display: 'grid', gap: 1.25, gridTemplateColumns: { xs: '1fr', md: 'repeat(3, minmax(0, 1fr))' } }}>
                                <Alert severity="info" variant="outlined">Usuario: {selectedDetail.executedByDisplay ?? selectedDetail.executedBy ?? '—'}</Alert>
                                <Alert severity="info" variant="outlined">Fecha: {formatDateTime(selectedDetail.executedAt)}</Alert>
                                <Alert severity={statusColor(selectedDetail.finalStatus)} variant="outlined">
                                    Estado: {statusLabel(selectedDetail.finalStatus)}
                                </Alert>
                                <Alert severity="info" variant="outlined">Archivo: {selectedDetail.fileName}</Alert>
                                <Alert severity="info" variant="outlined">Duración: {formatDurationMs(selectedDetail.durationMs)}</Alert>
                                <Alert severity="info" variant="outlined">Tipo archivo: {selectedDetail.fileType}</Alert>
                            </Box>

                            <Typography sx={{ fontWeight: 700, fontSize: 16 }}>Estadísticas</Typography>
                            <Box sx={{ display: 'grid', gap: 1.25, gridTemplateColumns: { xs: 'repeat(2, minmax(0, 1fr))', md: 'repeat(4, minmax(0, 1fr))' } }}>
                                <Alert severity="success" variant="outlined">Drivers creados: {selectedDetail.createdDrivers ?? 0}</Alert>
                                <Alert severity="info" variant="outlined">Drivers actualizados: {selectedDetail.updatedDrivers ?? 0}</Alert>
                                <Alert severity="success" variant="outlined">Vehicles creados: {selectedDetail.createdVehicles ?? 0}</Alert>
                                <Alert severity="info" variant="outlined">Vehicles actualizados: {selectedDetail.updatedVehicles ?? 0}</Alert>
                                <Alert severity="success" variant="outlined">Tours creados: {selectedDetail.createdTours ?? 0}</Alert>
                                <Alert severity="info" variant="outlined">Tours actualizados: {selectedDetail.updatedTours ?? 0}</Alert>
                                <Alert severity="warning" variant="outlined">Duplicados: {selectedDetail.duplicatesDetected ?? 0}</Alert>
                                <Alert severity="error" variant="outlined">Inválidos: {selectedDetail.invalidRecords ?? 0}</Alert>
                            </Box>

                            <Typography sx={{ fontWeight: 700, fontSize: 16 }}>Registros procesados</Typography>
                            <TableBase
                                columns={detailColumns}
                                data={detailRows.map((detail, index) => ({ id: detail.id ?? `${index}`, ...detail }))}
                                enableGlobalFilter
                                tableOptions={{
                                    initialState: { pagination: { pageIndex: 0, pageSize: 10 } },
                                }}
                                maxHeight="45vh"
                            />
                        </Stack>
                    ) : null}
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
