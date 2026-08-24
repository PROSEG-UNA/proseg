import { useCallback, useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Radio, Typography, useTheme } from '@mui/material';
import ManageAccountsIcon from '@mui/icons-material/ManageAccounts';
import GeneralModal from '../../../common/components/GeneralModal.jsx';
import TableBase from '../../../common/components/TablaBase.jsx';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import DialogModal from '../../../common/components/DialogModal.jsx';
import { fetchRoles } from '../services/rolesService';
import { queryKeys } from '../../../common/query';
import { assignSingleRoleToUser, fetchRolesByUserId } from '../services/usersService';
import { getFriendlyApiErrorMessage, formatRoleName } from '../../../common/utils/index.js';

const roleColumns = [
    { accessorKey: 'name', header: 'Rol', size: 180, grow: true, Cell: ({ cell }) => formatRoleName(cell.getValue()) },
    { accessorKey: 'description', header: 'Descripción', size: 260, grow: 2 },
];

const ROLE_CATALOG_PARAMS = { size: 200 };
const ROLE_CATALOG_STALE_TIME = 10 * 60_000;
const EMPTY_ROLES = [];
const NO_SELECTION_OVERRIDE = { userId: null, roleId: undefined };

export default function AssignUserRolesModal({ open, user, onClose, onSaved }) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const [selectionOverride, setSelectionOverride] = useState(NO_SELECTION_OVERRIDE);
    const [saving, setSaving] = useState(false);
    const [alert, setAlert] = useState(null);

    const queryClient = useQueryClient();
    const modalReady = Boolean(open && user?.id);

    const rolesQuery = useQuery({
        queryKey: queryKeys.security.roleCatalog(ROLE_CATALOG_PARAMS),
        queryFn: async () => {
            const response = await fetchRoles(ROLE_CATALOG_PARAMS);
            return (response.content ?? []).map((role) => ({
                id: role.id,
                name: role.name,
                description: role.description || '—',
            }));
        },
        enabled: modalReady,
        staleTime: ROLE_CATALOG_STALE_TIME,
    });

    const userRolesQuery = useQuery({
        queryKey: queryKeys.security.userRoles(user?.id ?? null),
        queryFn: () => fetchRolesByUserId(user.id),
        enabled: modalReady,
    });

    const roles = rolesQuery.data ?? EMPTY_ROLES;

    const initialRoleId = useMemo(() => {
        const assignedRoleIds = new Set((userRolesQuery.data ?? []).map((role) => role.id));
        return roles.find((role) => assignedRoleIds.has(role.id))?.id ?? null;
    }, [roles, userRolesQuery.data]);

    const selectedRoleId = selectionOverride.userId === user?.id && selectionOverride.roleId !== undefined
        ? selectionOverride.roleId
        : initialRoleId;

    const setSelectedRoleId = useCallback(
        (roleId) => setSelectionOverride({ userId: user?.id ?? null, roleId }),
        [user?.id]
    );

    const loading = rolesQuery.isLoading || userRolesQuery.isLoading;
    const queryError = rolesQuery.error ?? userRolesQuery.error;
    const error = queryError ? (queryError.message || 'Error al cargar roles') : null;

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
        [selectedRoleId, setSelectedRoleId]
    );

    const handleClose = useCallback(() => {
        if (saving) return;
        setSelectionOverride(NO_SELECTION_OVERRIDE);
        onClose?.();
    }, [saving, onClose]);

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
            setSelectionOverride(NO_SELECTION_OVERRIDE);
            await queryClient.invalidateQueries({ queryKey: queryKeys.security.userRoles(user.id) });
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
                onClose={handleClose}
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
                secondaryButton={{ label: 'Cancelar', onClick: handleClose }}
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
                        fillToBottom={false}
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
