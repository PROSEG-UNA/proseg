import { useMemo } from 'react';
import {
    MaterialReactTable,
    useMaterialReactTable,
} from 'material-react-table';
import { useTheme } from '@mui/material/styles';
import { MRT_Localization_ES } from 'material-react-table/locales/es';

export default function TableBase({
    columns,
    data,
    loading = false,
    error = null,
    enableRowActions = false,
    renderRowActions,
    enableRowSelection = false,
    rowSelection,
    onRowSelectionChange,
    enablePagination = true,
    enableColumnFilters = true,
    enableGlobalFilter = true,
    enableDensityToggle = true,
    enableFullScreenToggle = true,
    enableColumnActions = true,
    enableHiding = true,
    enableStickyHeader = true,
    enableStickyFooter = true,
    maxHeight,
    tableOptions = {},
}) {
    const theme = useTheme();

    const stableColumns = useMemo(() => columns, [columns]);

    const table = useMaterialReactTable({
        columns: stableColumns,
        data: data ?? [],
        getRowId: (row) => row.id,
        state: {
            isLoading: loading,
            showAlertBanner: !!error,
            ...(enableRowSelection && rowSelection != null
                ? { rowSelection }
                : {}),
            ...tableOptions.state,
        },

        enableRowActions,
        renderRowActions,
        positionActionsColumn: 'last',
        enableRowSelection,
        onRowSelectionChange,
        enablePagination,
        enableColumnFilters,
        columnFilterDisplayMode: 'subheader',
        enableGlobalFilter,
        enableDensityToggle,
        enableFullScreenToggle,
        enableColumnActions,
        enableHiding,
        enableSorting: true,
        enableStickyHeader,
        enableStickyFooter,
        enableTopToolbar: true,
        enableBottomToolbar: enablePagination,

        muiTableContainerProps: {
            sx: maxHeight
                ? { maxHeight, overflow: 'auto' }
                : { height: '600px', overflow: 'auto' },
        },

        paginationDisplayMode: 'pages',
        initialState: {
            density: 'compact',
            pagination: { pageIndex: 0, pageSize: 10 },
            showColumnFilters: false,
            ...tableOptions.initialState,
        },

        localization: MRT_Localization_ES,

        mrtTheme: (muiTheme) => ({
            baseBackgroundColor: muiTheme.palette.background.default,
        }),

        muiTableProps: {
            sx: { backgroundColor: 'background.default' },
        },

        muiTablePaperProps: {
            elevation: 0,
            sx: {
                backgroundColor: 'background.default',
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: `${theme.shape.borderRadius}px`,
                overflow: 'hidden',
                '& .Mui-TableHeadCell-Content-Actions': {
                    transition: 'opacity 150ms ease',
                    marginLeft: '4px',
                },
                '& th:hover .Mui-TableHeadCell-Content-Actions': {
                    opacity: 1,
                },
            },
        },

        muiTopToolbarProps: {
            sx: { backgroundColor: 'background.paper' },
        },

        muiBottomToolbarProps: {
            sx: { backgroundColor: 'background.paper' },
        },

        muiTableHeadCellProps: {
            sx: {
                backgroundColor: 'background.paper',
                fontWeight: 600,
                fontSize: '0.8125rem',
                border: 'none',
                '&:focus, &:focus-within': { outline: 'none' },
            },
        },

        muiTableBodyCellProps: {
            sx: {
                backgroundColor: 'background.default',
                fontSize: '0.8125rem',
                borderTop: '1px solid',
                borderTopColor: 'divider',
                '&:focus, &:focus-within': { outline: 'none' },
            },
        },

        muiTableBodyRowProps: {
            sx: {
                backgroundColor: 'background.default',
                '&:hover td': { backgroundColor: 'action.hover' },
                '&:last-of-type td': {
                    borderBottom: '1px solid',
                    borderBottomColor: 'divider',
                },
            },
        },

        muiSearchTextFieldProps: {
            variant: 'outlined',
            size: 'small',
        },

        muiPaginationProps: {
            rowsPerPageOptions: [10, 20, 50],
            shape: 'rounded',
            variant: 'outlined',
            size: 'small',
        },

        muiToolbarAlertBannerProps: error
            ? { color: 'error', children: error }
            : undefined,

        displayColumnDefOptions: {
            'mrt-row-actions': {
                header: 'Acciones',
                size: 120,
                grow: false,
                muiTableHeadCellProps: { align: 'center' },
                muiTableBodyCellProps: { align: 'center' },
            },
        },

        ...tableOptions,
    });

    return <MaterialReactTable table={table} />;
}
