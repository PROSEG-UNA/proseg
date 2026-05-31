import { useEffect, useMemo, useState } from 'react';
import { Radio, Typography, useTheme } from '@mui/material';
import ManageAccountsIcon from '@mui/icons-material/ManageAccounts';
import GeneralModal from '../../../common/components/GeneralModal.jsx';
import TableBase from '../../../common/components/TablaBase.jsx';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import DialogModal from '../../../common/components/DialogModal.jsx';
import { fetchRoles } from '../services/rolesService';
import { assignSingleRoleToUser, fetchRolesByUserId } from '../services/usersService';
import { getFriendlyApiErrorMessage, formatRoleName } from '../../../common/utils/index.js';

const roleColumns = [
    { accessorKey: 'name', header: 'Rol', size: 180, grow: true, Cell: ({ cell }) => formatRoleName(cell.getValue()) },
    { accessorKey: 'description', header: 'Descripción', size: 260, grow: 2 },
];

export default function AssignUserRolesModal({ open, user, onClose, onSaved }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const [roles, setRoles] = useState([]);
    const [selectedRoleId, setSelectedRoleId] = useState(null);
    const [initialRoleId, setInitialRoleId] = useState(null);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);
    const [alert, setAlert] = useState(null);

    useEffect(() => {
        let ignore = false;

        const loadRoles = async () => {
            if (!open || !user?.id) return;
            try {
                setLoading(true);
                setError(null);
                setSelectedRoleId(null);

                const [rolesResponse, assignedRoles] = await Promise.all([
                    fetchRoles({ size: 200 }),
                    fetchRolesByUserId(user.id),
                ]);
                if (ignore) return;

                const mappedRoles = (rolesResponse.content ?? []).map((role) => ({
                    id: role.id,
                    name: role.name,
                    description: role.description || '—',
                }));
                setRoles(mappedRoles);

                const assignedRoleIds = new Set((assignedRoles ?? []).map((role) => role.id));
                const preselectedRoleId = mappedRoles.find((role) => assignedRoleIds.has(role.id))?.id ?? null;
                setSelectedRoleId(preselectedRoleId);
                setInitialRoleId(preselectedRoleId);
            } catch (err) {
                if (!ignore) setError(err?.message || 'Error al cargar roles');
            } finally {
                if (!ignore) setLoading(false);
            }
        };

        void loadRoles();
        return () => { ignore = true; };
    }, [open, user?.id]);

    const columns = useMemo(
        () => [
            {
                id: 'selection',
                header: 'Seleccionar',
                size: 90,
                grow: false,
                Cell: ({ row }) => (
                    <Radio
                        checked={selectedRoleId === row.original.id}
                        onChange={() => setSelectedRoleId(row.original.id)}
                        value={row.original.id}
                        inputProps={{ 'aria-label': `Seleccionar rol ${formatRoleName(row.original.name)}` }}
                    />
                ),
                enableColumnFilter: false,
                enableSorting: false,
            },
            ...roleColumns,
        ],
        [selectedRoleId]
    );

    const handleAssign = async () => {
        if (!user?.id) {
            setAlert({ type: 'error', message: 'No se encontró el usuario seleccionado.' });
            return;
        }
        if (!selectedRoleId) {
            setAlert({ type: 'error', message: 'Debes seleccionar un rol para el usuario.' });
            return;
        }
        if (selectedRoleId === initialRoleId) {
            setAlert({ type: 'info', message: 'No hay cambios por guardar.' });
            return;
        }

        setSaving(true);
        try {
            await assignSingleRoleToUser(user.id, selectedRoleId);
            setInitialRoleId(selectedRoleId);
            setAlert({ type: 'success', message: 'Rol actualizado correctamente.' });
            onSaved?.();
        } catch (err) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(err, 'Error al actualizar el rol del usuario.') });
        } finally {
            setSaving(false);
        }
    };

    const isAccessDeniedError = error && (error.includes('permisos') || error.includes('403') || error.includes('Acceso denegado'));

    return (
        <>
            <GeneralModal
                open={open}
                onClose={() => !saving && onClose?.()}
                maxWidth="md"
                fullScreenAt="md"
                icon={ManageAccountsIcon}
                title="Asignar rol"
                subtitle="Seleccione el rol que tendrá el usuario"
                loading={saving}
                footerLeft={
                    <Typography sx={{ fontSize: 11.5, color: 'text.disabled', fontWeight: 500 }}>
                        {selectedRoleId ? 'Rol seleccionado' : 'Sin rol seleccionado'}
                    </Typography>
                }
                secondaryButton={{ label: 'Cancelar', onClick: onClose }}
                primaryButton={{ label: saving ? 'Guardando…' : 'Guardar cambios', onClick: handleAssign, disabled: saving, loading: saving }}
            >
                {isAccessDeniedError ? (
                    <AccessDeniedState />
                ) : (
                    <TableBase
                        columns={columns}
                        data={roles}
                        loading={loading}
                        error={error}
                        tableOptions={{ positionToolbarAlertBanner: 'none' }}
                    />
                )}
            </GeneralModal>

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </>
    );
}
