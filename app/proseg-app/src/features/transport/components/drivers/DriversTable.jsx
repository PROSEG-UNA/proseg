import { useCallback, useMemo, useState } from 'react';
import TableBase from '../../../../common/components/TablaBase.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePersistedColumnVisibility } from '../../hooks/usePersistedColumnVisibility';
import { usePersistedPagination } from '../../hooks/usePersistedPagination.js';
import { useTransportDriversData } from '../../hooks/useTransportDriversData';
import { getDriverColumns } from './driverColumns.jsx';

const COLUMN_VISIBILITY_STORAGE_KEY = 'drivers-table-column-visibility';

const COLUMN_TO_BACKEND_KEY = {
    name: 'name',
    status: 'status',
    organizationalUnit: 'organizationalUnit',
};

export default function DriversTable({ onEditDriver, monthData, surplusHoursPerDriver }) {
    const [pagination, setPagination] = usePersistedPagination('drivers-table-page-size');
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);
    const [columnVisibility, setColumnVisibility] = usePersistedColumnVisibility(COLUMN_VISIBILITY_STORAGE_KEY);
    const { hasPermission } = usePermissions();

    const canEdit = hasPermission(PERMISSIONS.TRANSPORT.DRIVERS.MANAGE);
    const canDelete = hasPermission(PERMISSIONS.TRANSPORT.DRIVERS.DELETE);

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
    }, [setPagination]);

    const handleColumnFiltersChange = useCallback((updater) => {
        setColumnFilters((currentValue) => (typeof updater === 'function' ? updater(currentValue) : updater));
        setPagination((previousValue) => (previousValue.pageIndex === 0 ? previousValue : { ...previousValue, pageIndex: 0 }));
    }, [setPagination]);

    const handleSortingChange = useCallback((updater) => {
        setSorting((currentValue) => (typeof updater === 'function' ? updater(currentValue) : updater));
        setPagination((previousValue) => (previousValue.pageIndex === 0 ? previousValue : { ...previousValue, pageIndex: 0 }));
    }, [setPagination]);

    const { rows, loading, fetching, error, totalElements } = useTransportDriversData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
    });

    const showMonthColumns = Boolean(monthData?.enabled);

    const rowsWithMonth = useMemo(() => {
        if (!showMonthColumns) return rows;
        return rows.map((row) => {
            const driverMonth = monthData.byDriverId.get(row.id);
            const surplusHoursAccrued = driverMonth?.surplusHoursAccrued ?? 0;
            return {
                ...row,
                ordinaryHoursAccrued: driverMonth?.ordinaryHoursAccrued ?? 0,
                surplusExecutionRatio: surplusHoursPerDriver > 0 ? surplusHoursAccrued / surplusHoursPerDriver : null,
                surplusHoursAccrued,
                overtimeHoursAccrued: driverMonth?.overtimeHoursAccrued ?? 0,
                weekendsWorked: driverMonth?.weekendsWorked ?? 0,
            };
        });
    }, [rows, showMonthColumns, monthData, surplusHoursPerDriver]);

    const columns = useMemo(() => getDriverColumns({ showMonthColumns }), [showMonthColumns]);

    return (
        <TableBase
            columns={columns}
            data={rowsWithMonth}
            loading={loading}
            fetching={fetching || Boolean(monthData?.loading)}
            error={error ?? monthData?.error}
            onRowClick={canEdit || canDelete ? (row) => onEditDriver(row.original) : undefined}
            tableOptions={{
                manualPagination: true,
                manualFiltering: true,
                manualSorting: true,
                rowCount: totalElements,
                onPaginationChange: setPagination,
                onGlobalFilterChange: handleGlobalFilterChange,
                onColumnFiltersChange: handleColumnFiltersChange,
                onSortingChange: handleSortingChange,
                onColumnVisibilityChange: setColumnVisibility,
                state: { pagination, globalFilter, columnFilters, sorting, columnVisibility },
            }}
            enableGlobalFilter
        />
    );
}
