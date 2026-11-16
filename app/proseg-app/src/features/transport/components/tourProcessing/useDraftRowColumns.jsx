import { useMemo } from 'react';
import { Box, Chip, Tooltip, Typography } from '@mui/material';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import TableSelectFilter from '../../../../common/components/TableSelectFilter.jsx';
import { formatDate } from '../../transportUtils.js';
import { useTourProcessingCatalogMatch } from '../../hooks/useTourProcessingCatalogMatch.js';
import CatalogValueCell from './CatalogValueCell.jsx';
import DuplicateChip from './DuplicateChip.jsx';
import TourObservationMatchIndicator from './TourObservationMatchIndicator.jsx';
import TourProblemsIndicator from './TourProblemsIndicator.jsx';
import TourProcessingAssignmentsCell from './TourProcessingAssignmentsCell.jsx';
import {
    APPROVAL_STATUS_FILTER_OPTIONS,
    approvalStatusColor,
    approvalStatusLabel,
} from './tourProcessingDraftLabels.js';
import { vehicleDistributionWarning } from './vehicleDistributionWarning.js';
import { checksDriverCatalog } from './tourProcessingCatalogMatch.js';

export const DRAFT_ROWS_FILTERABLE_COLUMNS = new Set([
    'rowNumber', 'number', 'approvalStatus', 'driver', 'vehicle', 'passengers', 'executingUnit', 'responsible',
    'destination', 'durationDays', 'priority', 'modality', 'observations',
]);

export function useDraftRowColumns({ draftId, canReviewObservations, problemsByRowId, onObservationReviewed, compact = false }) {
    const {
        driver: { isMissing: isDriverMissing },
        vehicle: { isMissing: isVehicleMissing, isUnavailable: isVehicleUnavailable },
    } = useTourProcessingCatalogMatch();

    return useMemo(() => [
        { accessorKey: 'rowNumber', header: 'Fila', size: 90, grow: false, meta: { fullScreenSize: 70 } },
        {
            accessorKey: 'number',
            header: 'Numero',
            size: 150,
            grow: false,
            meta: { fullScreenSize: 90 },
            Cell: ({ cell, row }) => (
                <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.25, maxWidth: '100%' }}>
                    <TourProblemsIndicator problems={problemsByRowId[row.original.id]} />
                    {cell.getValue()}
                </Box>
            ),
        },
        {
            id: 'duplicate',
            header: 'Repetición',
            size: 120,
            grow: false,
            meta: { fullScreenSize: 110 },
            enableSorting: false,
            enableColumnFilter: false,
            accessorFn: (row) => row.duplicateRank,
            Cell: ({ row }) => <DuplicateChip row={row.original} />,
        },
        {
            accessorKey: 'approvalStatus',
            header: 'Estado',
            size: 120,
            grow: false,
            meta: { fullScreenSize: 100 },
            filterVariant: 'select',
            filterSelectOptions: APPROVAL_STATUS_FILTER_OPTIONS,
            Filter: ({ column }) => (
                <TableSelectFilter column={column} label="Estado" options={APPROVAL_STATUS_FILTER_OPTIONS} clearLabel="Todas" />
            ),
            Cell: ({ cell, table }) => {
                const isFullScreen = table.getState().isFullScreen || compact;
                return (
                    <Chip
                        label={approvalStatusLabel(cell.getValue())}
                        color={approvalStatusColor(cell.getValue())}
                        size="small"
                        variant="outlined"
                        sx={{
                            width: isFullScreen ? '100%' : 104,
                            maxWidth: 104,
                            justifyContent: 'center',
                            ...(isFullScreen && { height: 20, fontSize: 'inherit' }),
                        }}
                    />
                );
            },
        },
        {
            accessorKey: 'driver',
            header: 'Chofer',
            size: 210,
            grow: false,
            meta: { fullScreenSize: 170 },
            Cell: ({ cell, row }) => {
                if (row.original.assignments?.length > 1) {
                    return <TourProcessingAssignmentsCell kind="driver" assignments={row.original.assignments} isMissing={isDriverMissing} />;
                }
                return <CatalogValueCell
                    kind="driver"
                    value={cell.getValue()}
                    missing={checksDriverCatalog(row.original.resourceType) && isDriverMissing(cell.getValue())}
                />;
            },
        },
        {
            accessorKey: 'vehicle',
            header: 'Vehículo',
            size: 220,
            grow: false,
            meta: { fullScreenSize: 100 },
            Cell: ({ cell, row }) => {
                const warning = vehicleDistributionWarning(row.original);
                const ownVehicle = row.original.resourceType !== 'EXTERNAL' && row.original.resourceType !== 'BORROWED_VEHICLE';
                if (row.original.assignments?.length > 1) {
                    return (
                        <TourProcessingAssignmentsCell
                            kind="vehicle"
                            assignments={row.original.assignments}
                            isMissing={isVehicleMissing}
                            isUnavailable={isVehicleUnavailable}
                        />
                    );
                }
                return (
                    <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.75, maxWidth: '100%' }}>
                        <CatalogValueCell
                            kind="vehicle"
                            value={row.original.resourceType === 'BORROWED_VEHICLE' ? row.original.borrowedVehiclePlate : cell.getValue()}
                            missing={ownVehicle && isVehicleMissing(cell.getValue())}
                        />
                        {warning && (
                            <Tooltip title={warning} placement="top">
                                <WarningAmberOutlinedIcon
                                    role="img"
                                    aria-label={warning}
                                    sx={{ fontSize: 18, color: 'warning.main', flexShrink: 0, cursor: 'default' }}
                                />
                            </Tooltip>
                        )}
                    </Box>
                );
            },
        },
        { accessorKey: 'passengers', header: 'Pasajeros', size: 120, grow: false, meta: { fullScreenSize: 100 } },
        { accessorKey: 'executingUnit', header: 'Unidad ejecutora', size: 240, grow: false, meta: { fullScreenSize: 136, wrapInFullScreen: true } },
        { accessorKey: 'responsible', header: 'Responsable', size: 190, grow: false, meta: { fullScreenSize: 116, wrapInFullScreen: true } },
        { accessorKey: 'destination', header: 'Destinos', size: 180, grow: false, meta: { fullScreenSize: 100, wrapInFullScreen: true } },
        { accessorKey: 'durationDays', header: 'Duración', size: 110, grow: false, meta: { fullScreenSize: 92 } },
        { accessorKey: 'priority', header: 'Prioridad', size: 110, grow: false, meta: { fullScreenSize: 96 } },
        { accessorKey: 'modality', header: 'Modalidad', size: 120, grow: false, meta: { fullScreenSize: 102 }, Cell: ({ cell }) => cell.getValue() ?? '—' },
        {
            accessorKey: 'departureDate',
            header: 'Fecha salida',
            size: 140,
            grow: false,
            meta: { fullScreenSize: 112 },
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDate(cell.getValue()),
        },
        { accessorKey: 'departureTime', header: 'Hora salida', size: 120, grow: false, meta: { fullScreenSize: 106 }, enableColumnFilter: false },
        {
            accessorKey: 'returnDate',
            header: 'Fecha regreso',
            size: 140,
            grow: false,
            meta: { fullScreenSize: 122 },
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDate(cell.getValue()),
        },
        { accessorKey: 'returnTime', header: 'Hora regreso', size: 130, grow: false, meta: { fullScreenSize: 114 }, enableColumnFilter: false },
        {
            accessorKey: 'observations',
            header: 'Observaciones',
            size: 320,
            grow: false,
            meta: { fullScreenSize: 170, wrapInFullScreen: true },
            Cell: ({ cell, row, table }) => {
                const value = cell.getValue();
                if (!value) return '—';
                const isFullScreen = table.getState().isFullScreen || compact;
                return (
                    <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.25, maxWidth: '100%' }}>
                        <TourObservationMatchIndicator
                            draftId={draftId}
                            row={row.original}
                            canReview={canReviewObservations}
                            onReviewed={onObservationReviewed}
                        />
                        <Tooltip title={value} placement="top-start">
                            <Typography
                                noWrap={!isFullScreen}
                                sx={{
                                    fontSize: 'inherit',
                                    minWidth: 0,
                                    maxWidth: isFullScreen ? '100%' : 300,
                                    ...(isFullScreen && {
                                        display: '-webkit-box',
                                        WebkitLineClamp: 2,
                                        WebkitBoxOrient: 'vertical',
                                        overflow: 'hidden',
                                    }),
                                }}
                            >
                                {value}
                            </Typography>
                        </Tooltip>
                    </Box>
                );
            },
        },
    ], [isDriverMissing, isVehicleMissing, isVehicleUnavailable, draftId, canReviewObservations, onObservationReviewed, problemsByRowId, compact]);
}
