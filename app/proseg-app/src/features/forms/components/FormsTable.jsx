import { useEffect, useMemo, useState } from 'react';
import TableBase from '../../../common/components/TablaBase.jsx';
import { useFormRecordsData } from '../hooks/useFormRecordsData';
import { formTypeLabel, formatDateTime } from '../formsUtils';

export default function FormsTable({ selectedFormTypeId = '' }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });

    useEffect(() => {
        setPagination((prev) => (prev.pageIndex === 0 ? prev : { ...prev, pageIndex: 0 }));
    }, [selectedFormTypeId]);

    const { rows, loading, fetching, error, totalElements } = useFormRecordsData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        filters: selectedFormTypeId ? { formTypeId: selectedFormTypeId } : {},
        sort: ['createdAt,desc'],
    });

    const columns = useMemo(() => [
        {
            accessorKey: 'formTypeName',
            header: 'Tipo',
            size: 240,
            grow: true,
            Cell: ({ row }) => row.original.formTypeName || formTypeLabel(row.original.formTypeCode),
        },
        {
            accessorKey: 'createdByName',
            header: 'Creado por',
            size: 220,
            grow: true,
            Cell: ({ row }) => row.original.createdByName || row.original.createdBy || '—',
        },
        {
            accessorKey: 'createdAt',
            header: 'Creado',
            size: 180,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
        {
            accessorKey: 'updatedAt',
            header: 'Actualizado',
            size: 180,
            grow: false,
            enableColumnFilter: false,
            Cell: ({ cell }) => formatDateTime(cell.getValue()),
        },
    ], []);

    return (
        <TableBase
            columns={columns}
            data={rows}
            loading={loading}
            fetching={fetching}
            error={error}
            enableGlobalFilter={false}
            tableOptions={{
                manualPagination: true,
                enableSorting: false,
                rowCount: totalElements,
                onPaginationChange: setPagination,
                state: { pagination },
            }}
        />
    );
}
