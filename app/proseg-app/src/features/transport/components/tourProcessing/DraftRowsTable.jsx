import { memo, useCallback, useMemo, useState } from 'react';
import { Box, Typography } from '@mui/material';
import TableBase from '../../../../common/components/TablaBase.jsx';
import { usePersistedExpansion } from '../../hooks/usePersistedExpansion.js';
import TourProcessingDraftDaysDetailPanel from './TourProcessingDraftDaysDetailPanel.jsx';

const NO_DUPLICATES_HEADER = (
    <Box component="span" sx={{ display: 'flex', flexDirection: 'column' }}>
        Repetición
        <Typography component="span" sx={{ fontSize: 11.5, color: 'text.disabled' }}>
            No hay giras duplicadas
        </Typography>
    </Box>
);

const DraftRowsTable = memo(function DraftRowsTable({
    columns,
    rows,
    loading,
    fetching,
    error,
    totalElements,
    tableOptions,
    canEdit,
    hasDuplicates,
    onOpenRow,
    fitContainer = false,
    virtualizeRows = true,
    expansionStorageKey = 'tour-processing-draft-rows-expand-all',
}) {
    const [duplicateColumnHidden, setDuplicateColumnHidden] = useState(false);
    const { tableOptions: expansionOptions } = usePersistedExpansion(expansionStorageKey);

    if (!hasDuplicates && duplicateColumnHidden) setDuplicateColumnHidden(false);

    const tableColumns = useMemo(() => columns.map((column) => (
        column.id === 'duplicate' && !hasDuplicates
            ? { ...column, enableHiding: false, header: NO_DUPLICATES_HEADER }
            : column
    )), [columns, hasDuplicates]);

    const columnVisibility = useMemo(() => ({
        ...tableOptions.state.columnVisibility,
        duplicate: hasDuplicates && !duplicateColumnHidden,
    }), [tableOptions.state.columnVisibility, hasDuplicates, duplicateColumnHidden]);

    const { onColumnVisibilityChange } = tableOptions;
    const handleColumnVisibilityChange = useCallback((updater) => {
        const nextVisibility = typeof updater === 'function' ? updater(columnVisibility) : updater;
        const { duplicate, ...otherColumns } = nextVisibility;
        if (hasDuplicates) setDuplicateColumnHidden(duplicate === false);
        onColumnVisibilityChange(otherColumns);
    }, [columnVisibility, hasDuplicates, onColumnVisibilityChange]);

    const handleRowClick = useCallback(
        (row) => onOpenRow(canEdit ? 'edit' : 'view', row.original),
        [canEdit, onOpenRow],
    );

    const mergedTableOptions = useMemo(() => ({
        ...tableOptions,
        ...expansionOptions,
        enableRowVirtualization: virtualizeRows,
        onColumnVisibilityChange: handleColumnVisibilityChange,
        state: { ...tableOptions.state, ...expansionOptions.state, columnVisibility, density: 'compact' },
        rowCount: totalElements,
        muiPaginationProps: { rowsPerPageOptions: [10, 20, 50] },
    }), [tableOptions, expansionOptions, handleColumnVisibilityChange, columnVisibility, totalElements, virtualizeRows]);

    return (
        <TableBase
            columns={tableColumns}
            data={rows}
            loading={loading}
            fetching={fetching}
            error={error}
            enableDensityToggle={false}
            compactFullScreen
            fillToBottom={!fitContainer}
            fitParent={fitContainer}
            fitColumnsToWidth={fitContainer}
            onRowClick={handleRowClick}
            renderDetailPanel={({ row }) => row.original.days?.length
                ? <TourProcessingDraftDaysDetailPanel days={row.original.days} assignments={row.original.assignments} />
                : null}
            tableOptions={mergedTableOptions}
        />
    );
});

export default DraftRowsTable;
