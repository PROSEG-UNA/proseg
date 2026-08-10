import TableBase from '../../../../common/components/TablaBase.jsx';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import LocationFormModal from './LocationFormModal.jsx';
import BuildingEmailsModal from '../email/BuildingEmailModal.jsx';
import CampusEmailsModal from '../email/CampusEmailModal.jsx';

export default function LocationTableView({ table, fillToBottom = true }) {
    const {
        config, title, columns,
        rows, loading, error, totalElements,
        pagination, setPagination,
        globalFilter, setGlobalFilter,
        columnFilters, setColumnFilters,
        sorting, setSorting,
        isAccessDeniedError, permissions, renderRowActions,
        handleConfirmDelete, handleFormSaved,
        formOpen, formRow, setFormOpen, setFormRow,
        deletingRow, deleting, setDeletingRow,
        buildingEmailsRow, setBuildingEmailsRow,
        campusEmailsRow, setCampusEmailsRow,
        alert, setAlert,
    } = table;

    const { canView, hasRowActions } = permissions;

    return (
        <>
            {!canView || isAccessDeniedError ? (
                <AccessDeniedState />
            ) : (
                <TableBase
                    columns={columns}
                    data={rows}
                    loading={loading}
                    error={error}
                    enableRowActions={hasRowActions}
                    renderRowActions={hasRowActions ? renderRowActions : undefined}
                    fillToBottom={fillToBottom}
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
                        displayColumnDefOptions: {
                            'mrt-row-actions': {
                                muiTableBodyCellProps: { sx: { py: 1.15 } },
                            },
                        },
                    }}
                    enableGlobalFilter
                />
            )}

            <LocationFormModal
                open={formOpen}
                onClose={() => { setFormOpen(false); setFormRow(null); }}
                onSaved={handleFormSaved}
                config={config}
                row={formRow}
            />

            <BuildingEmailsModal
                open={!!buildingEmailsRow}
                onClose={() => setBuildingEmailsRow(null)}
                building={buildingEmailsRow}
            />

            <CampusEmailsModal
                open={!!campusEmailsRow}
                onClose={() => setCampusEmailsRow(null)}
                campus={campusEmailsRow}
            />

            <DialogModal
                type="delete"
                open={!!deletingRow}
                title={`Eliminar ${title}`}
                message={`¿Estás seguro de que deseas eliminar "${deletingRow?.name}"?\nEsta acción no se puede deshacer.`}
                onClose={() => !deleting && setDeletingRow(null)}
                onConfirm={handleConfirmDelete}
                confirmLabel="Eliminar"
            />

            <DialogModal open={!!alert} type={alert?.type} message={alert?.message} onClose={() => setAlert(null)} />
        </>
    );
}
