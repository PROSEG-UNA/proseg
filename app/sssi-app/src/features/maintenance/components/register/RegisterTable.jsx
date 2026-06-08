import { useEffect, useMemo, useState } from 'react';
import TableBase from '../../../../common/components/TablaBase.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { useMaintenanceRegisterData } from '../../hooks/register/useMaintenanceRegisterData';
import { getRegisterColumns, renderRegisterActions } from './registerColumns.jsx';
import RegisterDetailPanel from './RegisterDetailPanel.jsx';

const COLUMN_TO_BACKEND_KEY = {
    companyName: 'company.name',
    companyLegalId: 'company.legalId',
    statusRaw: 'status',
    startDate: 'startDate',
};

export default function RegisterTable({ refreshKey = 0, onOpenRegister }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [globalFilter, setGlobalFilter] = useState('');
    const [columnFilters, setColumnFilters] = useState([]);
    const [sorting, setSorting] = useState([]);

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

    useEffect(() => {
        setPagination((prev) => (prev.pageIndex === 0 ? prev : { ...prev, pageIndex: 0 }));
    }, [debouncedGlobalFilter, backendFilters, backendSort]);

    const { rows, loading, error, totalElements } = useMaintenanceRegisterData({
        mode: 'history',
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        search: debouncedGlobalFilter,
        filters: backendFilters,
        sort: backendSort,
        refreshKey,
    });

    const columns = useMemo(() => getRegisterColumns(), []);

    return (
        <TableBase
            columns={columns}
            data={rows}
            loading={loading}
            error={error}
            enableRowActions
            renderRowActions={renderRegisterActions({ onOpen: onOpenRegister })}
            renderDetailPanel={({ row }) => (
                <RegisterDetailPanel requestId={row.original.id} />
            )}
            tableOptions={{
                positionActionsColumn: 'last',
                manualPagination: true,
                manualFiltering: true,
                manualSorting: true,
                rowCount: totalElements,
                onPaginationChange: setPagination,
                onGlobalFilterChange: setGlobalFilter,
                onColumnFiltersChange: setColumnFilters,
                onSortingChange: setSorting,
                state: { pagination, globalFilter, columnFilters, sorting },
                initialState: {
                    columnVisibility: {
                        companyLegalId: false,
                        createdAt: false,
                    },
                },
            }}
            enableGlobalFilter
        />
    );
}
