import { useEffect, useMemo, useState } from 'react';
import BuildIcon from '@mui/icons-material/Build';
import HistoryIcon from '@mui/icons-material/History';
import TableBase from '../../../../common/components/TablaBase.jsx';
import RowActionsMenu from '../../../../common/components/RowActionsMenu.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { useRegisterAssetsData } from '../../hooks/register/useRegisterAssetsData';
import { getRegisterAssetColumns } from './registerAssetColumns.jsx';
import RecordFormModal from './RecordFormModal.jsx';
import AssetRecordsModal from './AssetRecordsModal.jsx';

const STORAGE_KEY = 'register-assets-table-column-visibility';

const DEFAULT_COLUMN_VISIBILITY = {
    executingUnit: false,
    responsibleEmployee: false,
    responsibleEmployeeId: false,
    status: false,
    acquisitionDate: false,
    warrantyEndDate: false,
    firmwareSupportEndDate: false,
    decommissionDate: false,
    latitude: false,
    longitude: false,
};

const COLUMN_TO_BACKEND_KEY = {
    assetNumber: 'assetNumber',
    serialNumber: 'serialNumber',
    type: 'model.type.name',
    brand: 'model.brand.name',
    model: 'model.name',
    campus: 'location.floor.building.campus.name',
    building: 'location.floor.building.name',
    floor: 'location.floor.name',
    location: 'location.description',
    executingUnit: 'executingUnit',
    responsibleEmployee: 'responsibleEmployee',
    responsibleEmployeeId: 'responsibleEmployeeId',
    status: 'status',
    acquisitionDate: 'acquisitionDate',
    warrantyEndDate: 'warrantyEndDate',
    firmwareSupportEndDate: 'firmwareSupportEndDate',
};

export default function RegisterAssetsTable({ registerId, canRegister }) {
    const [columnVisibility, setColumnVisibility] = useState(() => {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved) {
            try { return JSON.parse(saved); } catch { return DEFAULT_COLUMN_VISIBILITY; }
        }
        return DEFAULT_COLUMN_VISIBILITY;
    });
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [recordTarget, setRecordTarget] = useState(null);
    const [historyTarget, setHistoryTarget] = useState(null);
    const [recordsRefresh, setRecordsRefresh] = useState(0);

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

    const persistColumnVisibility = () => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(columnVisibility));
    };

    useEffect(persistColumnVisibility, [columnVisibility]);

    const resetPageOnFilterChange = () => {
        setPagination((prev) => (prev.pageIndex === 0 ? prev : { ...prev, pageIndex: 0 }));
    };

    useEffect(resetPageOnFilterChange, [debouncedGlobalFilter, backendFilters, backendSort]);

    const { rows, loading, error, totalElements } = useRegisterAssetsData({
        registerId,
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
    });

    const columns = useMemo(
        () =>
            getRegisterAssetColumns().map((column) => ({
                ...column,
                muiTableBodyCellProps: {
                    ...(column.muiTableBodyCellProps ?? {}),
                    sx: {
                        ...(column.muiTableBodyCellProps?.sx ?? {}),
                        py: 1.15,
                        borderBottom: '1px solid',
                        borderBottomColor: 'divider',
                    },
                },
            })),
        []
    );

    const renderActions = ({ row }) => {
        const actions = [
            {
                key: 'register',
                label: 'Registrar mantenimiento',
                icon: <BuildIcon fontSize="small" />,
                hidden: !canRegister,
                onClick: () => setRecordTarget(row.original),
            },
            {
                key: 'history',
                label: 'Ver historial',
                icon: <HistoryIcon fontSize="small" />,
                onClick: () => setHistoryTarget(row.original),
            },
        ];

        return <RowActionsMenu actions={actions} tooltip="Ver acción" />;
    };

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                error={error}
                enableRowActions
                renderRowActions={renderActions}
                fillToBottom={false}
                tableOptions={{
                    positionActionsColumn: 'first',
                    manualPagination: true,
                    manualFiltering: true,
                    manualSorting: true,
                    rowCount: totalElements,
                    onPaginationChange: setPagination,
                    onGlobalFilterChange: setGlobalFilter,
                    onColumnFiltersChange: setColumnFilters,
                    onSortingChange: setSorting,
                    onColumnVisibilityChange: setColumnVisibility,
                    state: { pagination, globalFilter, columnFilters, sorting, columnVisibility },
                    displayColumnDefOptions: {
                        'mrt-row-actions': {
                            muiTableBodyCellProps: {
                                sx: { py: 1.15, borderBottom: '1px solid', borderBottomColor: 'divider' },
                            },
                        },
                    },
                }}
                enableGlobalFilter
            />

            <RecordFormModal
                open={!!recordTarget}
                registerId={registerId}
                asset={recordTarget}
                onClose={() => setRecordTarget(null)}
                onSaved={() => setRecordsRefresh((v) => v + 1)}
            />

            <AssetRecordsModal
                open={!!historyTarget}
                registerId={registerId}
                asset={historyTarget}
                refreshKey={recordsRefresh}
                onClose={() => setHistoryTarget(null)}
            />
        </>
    );
}
