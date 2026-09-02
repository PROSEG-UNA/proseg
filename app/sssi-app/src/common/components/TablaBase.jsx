import { useMemo, useState } from 'react';
import {
    MaterialReactTable,
    useMaterialReactTable,
    MRT_ToggleFiltersButton,
    MRT_ShowHideColumnsButton,
    MRT_ToggleDensePaddingButton,
    MRT_ToggleFullScreenButton,
    MRT_ToolbarAlertBanner,
    MRT_LinearProgressBar,
    MRT_BottomToolbar,
} from 'material-react-table';
import { Box, IconButton, TextField, useMediaQuery } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import SearchIcon from '@mui/icons-material/Search';
import CloseIcon from '@mui/icons-material/Close';
import { MRT_Localization_ES } from 'material-react-table/locales/es';
import { headerSurfaceSx } from '../theme/sxStyles';
import TableHorizontalScrollbar from './TableHorizontalScrollbar.jsx';
import { useFillToBottom, TOP_GAP, BOTTOM_GAP } from '../hooks/useFillToBottom.js';

export default function TableBase({
                                      columns,
                                      data,
                                      loading = false,
                                      fetching = false,
                                      error = null,
                                      enableRowActions = false,
                                      renderRowActions,
                                      renderDetailPanel,
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
                                      fillToBottom = true,
                                      tableOptions = {},
                                  }) {
    const theme = useTheme();
    const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
    const isTouch = useMediaQuery('(pointer: coarse)');
    const [isMobileSearchOpen, setIsMobileSearchOpen] = useState(false);
    const [isDesktopSearchOpen, setIsDesktopSearchOpen] = useState(false);
    const fillsToBottom = fillToBottom && !maxHeight;

    const stableColumns = useMemo(() => columns, [columns]);
    const tableOptionsState = tableOptions.state ?? {};
    const tableOptionsInitialState = tableOptions.initialState ?? {};

    const mergedState = {
        ...tableOptionsState,
        isLoading: loading,
        showLoadingOverlay: false,
        showProgressBars: loading || fetching,
        showAlertBanner: !!error,
        ...(enableRowSelection && rowSelection != null ? { rowSelection } : {}),
    };

    const table = useMaterialReactTable({
        ...tableOptions,
        columns: stableColumns,
        data: data ?? [],
        getRowId: (row) => row.id,
        state: mergedState,

        enableRowActions,
        renderRowActions,
        renderDetailPanel: renderDetailPanel
            ? (props) => {
                const detailContent = renderDetailPanel(props);
                if (!detailContent) return null;

                return (
                    <Box
                        sx={{
                            width: '100%',
                            borderBottom: '1px solid',
                            borderBottomColor: 'divider',
                            borderTop: '1px solid',
                            borderTopColor: 'divider',
                        }}
                    >
                        {detailContent}
                    </Box>
                );
            }
            : undefined,
        positionActionsColumn: tableOptions.positionActionsColumn ?? 'last',
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

        muiTableContainerProps: ({ table }) => ({
            sx: {
                overflowX: isTouch ? 'auto' : 'hidden',
                overflowY: 'auto',
                ...(maxHeight
                    ? { maxHeight }
                    : fillsToBottom
                        ? { flex: '1 1 auto', minHeight: 0, height: 'auto', maxHeight: 'none' }
                        : {
                            height: table.getState().isFullScreen
                                ? 'calc(100vh - 120px)'
                                : 'calc(100vh - 315px)',
                        }),
            },
        }),

        paginationDisplayMode: 'pages',
        initialState: {
            density: 'compact',
            pagination: { pageIndex: 0, pageSize: 10 },
            showColumnFilters: false,
            ...tableOptionsInitialState,
        },

        localization: MRT_Localization_ES,

        mrtTheme: (muiTheme) => ({
            baseBackgroundColor: muiTheme.palette.background.paperWarm,
        }),

        muiTableProps: {
            sx: { backgroundColor: 'background.paperWarm' },
        },

        muiTablePaperProps: ({ table }) => ({
            elevation: 0,
            sx: {
                backgroundColor: 'background.paperWarm',
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: `${theme.shape.borderRadius}px`,
                overflow: table.getState().isFullScreen ? 'hidden' : 'clip',
                marginBottom: table.getState().isFullScreen || fillsToBottom ? 0 : '2rem',
                ...(fillsToBottom
                    ? {
                        display: 'flex',
                        flexDirection: 'column',
                        position: 'relative',
                        boxSizing: 'border-box',
                        ...(table.getState().isFullScreen ? {} : { height: '100%', minHeight: 0 }),
                    }
                    : {}),
                '& .Mui-TableHeadCell-Content-Actions': {
                    transition: 'opacity 150ms ease',
                    marginLeft: '4px',
                },
                '& th:hover .Mui-TableHeadCell-Content-Actions': {
                    opacity: 1,
                },
            },
        }),

        renderTopToolbar: ({ table }) => {
            const toolbarSx = (t) => ({ ...headerSurfaceSx(t), position: 'relative' });
            const actionIcons = (
                <>
                    {enableColumnFilters && <MRT_ToggleFiltersButton table={table} />}
                    {enableHiding && <MRT_ShowHideColumnsButton table={table} />}
                    {enableDensityToggle && <MRT_ToggleDensePaddingButton table={table} />}
                    {enableFullScreenToggle && <MRT_ToggleFullScreenButton table={table} />}
                </>
            );
            if (isMobile && isMobileSearchOpen) {
                return (
                    <Box sx={toolbarSx}>
                        <MRT_LinearProgressBar isTopToolbar table={table} />
                        <Box sx={{ display: 'flex', flexDirection: 'column', p: 1, gap: 1 }}>
                            <TextField
                                autoFocus
                                fullWidth
                                size="small"
                                variant="outlined"
                                placeholder="Buscar..."
                                value={table.getState().globalFilter ?? ''}
                                onChange={(e) => table.setGlobalFilter(e.target.value)}
                                slotProps={{
                                    input: {
                                        endAdornment: (
                                            <IconButton
                                                size="small"
                                                edge="end"
                                                onClick={() => {
                                                    table.setGlobalFilter('');
                                                    setIsMobileSearchOpen(false);
                                                }}
                                            >
                                                <CloseIcon fontSize="small" />
                                            </IconButton>
                                        ),
                                    },
                                }}
                            />
                            <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                                {actionIcons}
                            </Box>
                        </Box>
                        <MRT_ToolbarAlertBanner stackAlertBanner table={table} />
                    </Box>
                );
            }
            if (isMobile) {
                return (
                    <Box sx={toolbarSx}>
                        <MRT_LinearProgressBar isTopToolbar table={table} />
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', px: 1, py: 0.5 }}>
                            {enableGlobalFilter && (
                                <IconButton size="small" onClick={() => setIsMobileSearchOpen(true)}>
                                    <SearchIcon fontSize="small" />
                                </IconButton>
                            )}
                            {actionIcons}
                        </Box>
                        <MRT_ToolbarAlertBanner stackAlertBanner table={table} />
                    </Box>
                );
            }
            return (
                <Box sx={toolbarSx}>
                    <MRT_LinearProgressBar isTopToolbar table={table} />
                    <Box sx={{ display: 'flex', alignItems: 'center', px: 2, py: 0.5 }}>
                        <Box sx={{ ml: 'auto', display: 'flex', alignItems: 'center' }}>
                            {enableGlobalFilter && (
                                isDesktopSearchOpen ? (
                                    <TextField
                                        autoFocus
                                        size="small"
                                        variant="outlined"
                                        placeholder="Buscar..."
                                        value={table.getState().globalFilter ?? ''}
                                        onChange={(e) => table.setGlobalFilter(e.target.value)}
                                        sx={{ width: 250 }}
                                        slotProps={{
                                            input: {
                                                endAdornment: (
                                                    <IconButton
                                                        size="small"
                                                        edge="end"
                                                        onClick={() => {
                                                            table.setGlobalFilter('');
                                                            setIsDesktopSearchOpen(false);
                                                        }}
                                                    >
                                                        <CloseIcon fontSize="small" />
                                                    </IconButton>
                                                ),
                                            },
                                        }}
                                    />
                                ) : (
                                    <IconButton size="small" onClick={() => setIsDesktopSearchOpen(true)}>
                                        <SearchIcon fontSize="small" />
                                    </IconButton>
                                )
                            )}
                            {actionIcons}
                        </Box>
                    </Box>
                    <MRT_ToolbarAlertBanner stackAlertBanner table={table} />
                </Box>
            );
        },

        renderBottomToolbar: enablePagination
            ? ({ table }) => (
                <Box
                    sx={(t) => ({
                        ...headerSurfaceSx(t),
                        zIndex: 3,
                        ...(fillsToBottom
                            ? {
                                flex: '0 0 auto',
                                position: 'relative',
                                borderTop: '1px solid',
                                borderTopColor: 'divider',
                            }
                            : { position: 'sticky', bottom: 0 }),
                    })}
                >
                    {!isTouch && <TableHorizontalScrollbar containerRef={table.refs.tableContainerRef} />}
                    <MRT_BottomToolbar table={table} />
                </Box>
            )
            : undefined,

        muiBottomToolbarProps: {
            sx: (t) => ({
                ...headerSurfaceSx(t),
                ...(fillsToBottom ? { position: 'relative', boxShadow: 'none' } : {}),
            }),
        },

        muiTableHeadCellProps: {
            sx: (t) => ({
                ...headerSurfaceSx(t),
                fontWeight: 600,
                fontSize: '0.9rem',
                border: 'none',
                '&:focus, &:focus-within': { outline: 'none' },
            }),
        },

        muiTableBodyCellProps: {
            sx: {
                backgroundColor: 'background.paperWarm',
                fontSize: '0.9rem',
                borderTop: '1px solid',
                borderTopColor: 'divider',
                '&:focus, &:focus-within': { outline: 'none' },
            },
        },

        muiTableBodyRowProps: {
            sx: (t) => ({
                backgroundColor: 'background.paperWarm',
                '&:hover td': { backgroundColor: 'action.hover' },
                '&:last-of-type td': {
                    borderBottom: '1px solid',
                    borderBottomColor: 'divider',
                },
                '&:nth-of-type(even) td': {
                    backgroundColor: 'rgba(0,0,0,0.015)',
                    ...t.applyStyles('dark', {
                        backgroundColor: 'rgba(255,255,255,0.04)',
                    }),
                },
            }),
        },

        muiDetailPanelProps: {
            sx: {
                backgroundColor: 'background.paper',
                padding: 0,
            },
        },

        muiExpandButtonProps: ({ row }) => ({
            sx: {
                '& svg': {
                    transition: 'transform 200ms ease',
                    transform: row.getIsExpanded() ? 'rotate(-90deg) !important' : 'rotate(0deg) !important',
                },
            },
        }),

        muiLinearProgressProps: ({ isTopToolbar }) => ({
            sx: { display: isTopToolbar ? undefined : 'none' },
        }),

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
            'mrt-row-expand': {
                muiTableBodyCellProps: {
                    sx: { borderTop: 'none' },
                },
            },
            'mrt-row-actions': {
                header: 'Acción',
                size: 120,
                grow: false,
                muiTableHeadCellProps: { align: 'center' },
                muiTableBodyCellProps: { align: 'center' },
                ...(tableOptions.displayColumnDefOptions?.['mrt-row-actions'] ?? {}),
            },
            ...(tableOptions.displayColumnDefOptions ?? {}),
        },

    });

    const isFullScreen = table.getState().isFullScreen;
    const { slotRef, paneRef } = useFillToBottom(fillsToBottom && !isFullScreen);

    if (!fillsToBottom) return <MaterialReactTable table={table} />;

    return (
        <Box
            ref={slotRef}
            sx={{
                height: `calc(100dvh - ${TOP_GAP + BOTTOM_GAP}px)`,
                marginBottom: `${BOTTOM_GAP}px`,
            }}
        >
            <Box
                ref={paneRef}
                sx={isFullScreen ? { height: '100%' } : { position: 'sticky', top: `${TOP_GAP}px` }}
            >
                <MaterialReactTable table={table} />
            </Box>
        </Box>
    );
}
