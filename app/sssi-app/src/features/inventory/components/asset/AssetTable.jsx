import { useMemo, useState, useCallback } from 'react';
import TableBase from '../../../../common/components/TablaBase.jsx';
import AlertModal from '../../../../common/components/AlertModal.jsx';
import { useAssetsData } from '../../hooks/useAssetsData';
import { getAssetsColumns, renderAssetActions } from './assetColumns.jsx';
import AssetDetailPanel from './AssetDetailPanel.jsx';

export default function AssetTable({ refreshKey = 0 }) {
    const [pagination, setPagination] = useState({ pageIndex: 0, pageSize: 10 });
    const [alert, setAlert] = useState(null);

    const { rows, loading, error, totalElements } = useAssetsData({
        pageIndex: pagination.pageIndex,
        pageSize: pagination.pageSize,
        refreshKey,
    });

    const columns = useMemo(
        () =>
            getAssetsColumns().map((column) => ({
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

    const handleEdit = useCallback(() => {}, []);
    const handleDelete = useCallback(() => {}, []);

    return (
        <>
            <TableBase
                columns={columns}
                data={rows}
                loading={loading}
                error={error}
                enableRowActions
                renderRowActions={renderAssetActions({
                    onEdit: handleEdit,
                    onDelete: handleDelete,
                })}
                tableOptions={{
                    positionActionsColumn: 'last',
                    manualPagination: true,
                    rowCount: totalElements,
                    onPaginationChange: setPagination,
                    state: { pagination },
                    displayColumnDefOptions: {
                        'mrt-row-expand': {
                            muiTableBodyCellProps: {
                                sx: { borderTop: 'none', borderBottom: '1px solid', borderBottomColor: 'divider' },
                            },
                        },
                        'mrt-row-actions': {
                            muiTableBodyCellProps: {
                                sx: { py: 1.15, borderBottom: '1px solid', borderBottomColor: 'divider' },
                            },
                        },
                    },
                }}
                enableGlobalFilter
                renderDetailPanel={({ row }) => <AssetDetailPanel assetId={row.original.id} />}
            />

            <AlertModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}