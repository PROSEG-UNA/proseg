import { startTransition, useEffect, useId, useMemo, useRef, useState, useSyncExternalStore } from 'react';
import {
    MaterialReactTable,
    useMaterialReactTable,
    MRT_ShowHideColumnsButton,
    MRT_ToggleDensePaddingButton,
    MRT_ToggleFullScreenButton,
    MRT_ToolbarAlertBanner,
    MRT_LinearProgressBar,
    MRT_BottomToolbar,
} from 'material-react-table';
import { Box, IconButton, TextField, Tooltip, useMediaQuery } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import SearchIcon from '@mui/icons-material/Search';
import CloseIcon from '@mui/icons-material/Close';
import { MRT_Localization_ES } from 'material-react-table/locales/es';
import { headerSurfaceSx } from '../theme/sxStyles';
import TableHorizontalScrollbar from './TableHorizontalScrollbar.jsx';
import { useFillToBottom, TOP_GAP, BOTTOM_GAP } from '../hooks/useFillToBottom.js';

const GLOBAL_FILTER_DEBOUNCE_MS = 300;
const INTERACTIVE_ELEMENTS_SELECTOR = 'button, a, input, textarea, select, label, [role="button"], [role="checkbox"], [role="menuitem"]';

function applyGlobalFilter(table, value) {
    const nextValue = value || undefined;
    if (nextValue === (table.getState().globalFilter || undefined)) return;
    table.setGlobalFilter(nextValue);
}

function DebouncedSearchField({ table, onRequestClose, ...textFieldProps }) {
    const [value, setValue] = useState(() => table.getState().globalFilter ?? '');

    useEffect(() => {
        const timeoutId = setTimeout(() => applyGlobalFilter(table, value), GLOBAL_FILTER_DEBOUNCE_MS);
        return () => clearTimeout(timeoutId);
    }, [value, table]);

    return (
        <TextField
            autoFocus
            size="small"
            variant="outlined"
            placeholder="Buscar..."
            value={value}
            onChange={(e) => setValue(e.target.value)}
            slotProps={{
                input: {
                    endAdornment: (
                        <IconButton
                            size="small"
                            edge="end"
                            onClick={() => {
                                setValue('');
                                applyGlobalFilter(table, '');
                                onRequestClose?.();
                            }}
                        >
                            <CloseIcon fontSize="small" />
                        </IconButton>
                    ),
                },
            }}
            {...textFieldProps}
        />
    );
}

function createValueStore(initialValue) {
    let value = initialValue;
    const listeners = new Set();
    return {
        get: () => value,
        set: (nextValue) => {
            if (nextValue === value) return;
            value = nextValue;
            listeners.forEach((listener) => listener());
        },
        subscribe: (listener) => {
            listeners.add(listener);
            return () => listeners.delete(listener);
        },
    };
}

function useStoreValue(store) {
    return useSyncExternalStore(store.subscribe, store.get);
}

function FiltersToggleButton({ table, store, onToggle }) {
    const isVisible = useStoreValue(store);
    const { icons: { FilterListIcon, FilterListOffIcon }, localization } = table.options;

    return (
        <Tooltip title={localization.showHideFilters}>
            <IconButton aria-label={localization.showHideFilters} onClick={() => onToggle(!isVisible)}>
                {isVisible ? <FilterListOffIcon /> : <FilterListIcon />}
            </IconButton>
        </Tooltip>
    );
}

function HiddenFiltersStyle({ store, scopeId }) {
    const isVisible = useStoreValue(store);
    if (isVisible) return null;
    return <style>{`[data-table-scope="${scopeId}"] thead th > .MuiCollapse-root { display: none; }`}</style>;
}

function useSearchOpenState(table) {
    return useState(() => Boolean(table.getState().globalFilter));
}

function DesktopToolbarSearch({ table }) {
    const [isOpen, setIsOpen] = useSearchOpenState(table);

    if (isOpen) {
        return <DebouncedSearchField table={table} sx={{ width: 250 }} onRequestClose={() => setIsOpen(false)} />;
    }
    return (
        <IconButton size="small" onClick={() => setIsOpen(true)}>
            <SearchIcon fontSize="small" />
        </IconButton>
    );
}

function MobileToolbarContent({ table, enableGlobalFilter, actionIcons }) {
    const [isOpen, setIsOpen] = useSearchOpenState(table);

    if (isOpen) {
        return (
            <Box sx={{ display: 'flex', flexDirection: 'column', p: 1, gap: 1 }}>
                <DebouncedSearchField table={table} fullWidth onRequestClose={() => setIsOpen(false)} />
                <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                    {actionIcons}
                </Box>
            </Box>
        );
    }
    return (
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', px: 1, py: 0.5 }}>
            {enableGlobalFilter && (
                <IconButton size="small" onClick={() => setIsOpen(true)}>
                    <SearchIcon fontSize="small" />
                </IconButton>
            )}
            {actionIcons}
        </Box>
    );
}

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
                                      onRowClick,
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
    const tableElRef = useRef(null);
    const tableScopeId = useId();
    const fillsToBottom = fillToBottom && !maxHeight;

    const stableColumns = useMemo(() => columns, [columns]);
    const tableOptionsState = tableOptions.state ?? {};
    const tableOptionsInitialState = tableOptions.initialState ?? {};

    const initialFiltersVisible = Boolean(tableOptionsState.showColumnFilters ?? tableOptionsInitialState.showColumnFilters);
    const [filtersVisibilityStore] = useState(() => createValueStore(initialFiltersVisible));
    const [areFiltersMounted, setAreFiltersMounted] = useState(initialFiltersVisible);

    useEffect(() => {
        if (enableColumnFilters) startTransition(() => setAreFiltersMounted(true));
    }, [enableColumnFilters]);

    const setFiltersVisible = (isVisible) => {
        filtersVisibilityStore.set(isVisible);
        if (isVisible) setAreFiltersMounted(true);
        if (tableOptions.onShowColumnFiltersChange) {
            startTransition(() => tableOptions.onShowColumnFiltersChange(isVisible));
        }
    };

    const mergedState = {
        ...tableOptionsState,
        isLoading: loading,
        showLoadingOverlay: false,
        showProgressBars: loading || fetching,
        showAlertBanner: !!error,
        showColumnFilters: enableColumnFilters && areFiltersMounted,
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
        onShowColumnFiltersChange: (updater) => {
            setFiltersVisible(typeof updater === 'function' ? updater(filtersVisibilityStore.get()) : updater);
        },
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
        enableBottomToolbar: enablePagination || !isTouch,

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
            ref: tableElRef,
            sx: {
                backgroundColor: 'background.paperWarm',
                willChange: isTouch ? undefined : 'transform',
            },
        },

        muiTablePaperProps: ({ table }) => ({
            elevation: 0,
            'data-table-scope': tableScopeId,
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
            const hiddenFiltersStyle = enableColumnFilters && (
                <HiddenFiltersStyle store={filtersVisibilityStore} scopeId={tableScopeId} />
            );
            const actionIcons = (
                <>
                    {enableColumnFilters && (
                        <FiltersToggleButton table={table} store={filtersVisibilityStore} onToggle={setFiltersVisible} />
                    )}
                    {enableHiding && <MRT_ShowHideColumnsButton table={table} />}
                    {enableDensityToggle && <MRT_ToggleDensePaddingButton table={table} />}
                    {enableFullScreenToggle && <MRT_ToggleFullScreenButton table={table} />}
                </>
            );
            if (isMobile) {
                return (
                    <Box sx={toolbarSx}>
                        {hiddenFiltersStyle}
                        <MRT_LinearProgressBar isTopToolbar table={table} />
                        <MobileToolbarContent
                            table={table}
                            enableGlobalFilter={enableGlobalFilter}
                            actionIcons={actionIcons}
                        />
                        <MRT_ToolbarAlertBanner stackAlertBanner table={table} />
                    </Box>
                );
            }
            return (
                <Box sx={toolbarSx}>
                    {hiddenFiltersStyle}
                    <MRT_LinearProgressBar isTopToolbar table={table} />
                    <Box sx={{ display: 'flex', alignItems: 'center', px: 2, py: 0.5 }}>
                        <Box sx={{ ml: 'auto', display: 'flex', alignItems: 'center' }}>
                            {enableGlobalFilter && <DesktopToolbarSearch table={table} />}
                            {actionIcons}
                        </Box>
                    </Box>
                    <MRT_ToolbarAlertBanner stackAlertBanner table={table} />
                </Box>
            );
        },

        renderBottomToolbar: (enablePagination || !isTouch)
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
                    {!isTouch && (
                        <TableHorizontalScrollbar
                            containerRef={table.refs.tableContainerRef}
                            tableElRef={tableElRef}
                        />
                    )}
                    {enablePagination && <MRT_BottomToolbar table={table} />}
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

        muiTableBodyRowProps: ({ row }) => ({
            onClick: onRowClick
                ? (event) => {
                    const clickedInsideRow = event.currentTarget.contains(event.target);
                    const clickedInteractive = event.target.closest?.(INTERACTIVE_ELEMENTS_SELECTOR);
                    if (clickedInsideRow && !clickedInteractive) onRowClick(row);
                }
                : undefined,
            sx: (t) => ({
                cursor: onRowClick ? 'pointer' : 'default',
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
        }),

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
            ...(tableOptions.muiPaginationProps ?? {}),
        },

        muiToolbarAlertBannerProps: error
            ? { color: 'error', children: error }
            : {
                sx: (t) => ({
                    backgroundColor: t.vars.palette.tones.rose.soft,
                    color: t.vars.palette.tones.rose.fg,
                    border: `1px solid ${t.vars.palette.tones.rose.ring}`,
                    '& .MuiAlert-icon': { color: t.vars.palette.tones.rose.fg },
                    '& .MuiButtonBase-root': { color: t.vars.palette.tones.rose.fg },
                }),
            },

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
