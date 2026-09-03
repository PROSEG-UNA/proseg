import { useCallback, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchAllPrivileges, fetchPermissionsByRole, createRole, updateRole } from '../services/rolesService';
import { queryKeys } from '../../../common/query';

const NEW_ROLE_KEY = '__new_role__';

const EMPTY_OVERRIDES = { roleName: null, description: null, selectedIds: null };

function mapPrivilege(privilege) {
    return {
        id: privilege.id,
        name: privilege.name,
        description: privilege.description || '—',
        domain: privilege.domain ?? null,
    };
}

export function useRoleFormData(role) {
    const isEditMode = !!role;
    const roleKey = role?.name ?? NEW_ROLE_KEY;

    const { data, isPending, error } = useQuery({
        queryKey: queryKeys.security.roleForm(roleKey),
        queryFn: async () => {
            if (isEditMode) {
                const [privileges, assigned] = await Promise.all([
                    fetchAllPrivileges(),
                    fetchPermissionsByRole(role.name),
                ]);

                return {
                    allPrivileges: privileges.map(mapPrivilege),
                    assignedIds: assigned.map((privilege) => privilege.id),
                    roleName: role.name,
                    description: role.description || '',
                };
            }

            const privileges = await fetchAllPrivileges();
            return {
                allPrivileges: privileges.map(mapPrivilege),
                assignedIds: [],
                roleName: '',
                description: '',
            };
        },
    });

    const [overrides, setOverrides] = useState({ roleKey, ...EMPTY_OVERRIDES });

    const activeOverrides = overrides.roleKey === roleKey ? overrides : EMPTY_OVERRIDES;

    const updateOverride = useCallback((field, value) => {
        setOverrides((current) => {
            const base = current.roleKey === roleKey ? current : { roleKey, ...EMPTY_OVERRIDES };
            const nextValue = typeof value === 'function' ? value(base[field]) : value;
            return { ...base, roleKey, [field]: nextValue };
        });
    }, [roleKey]);

    const allPrivileges = data?.allPrivileges ?? [];
    const roleName = activeOverrides.roleName ?? data?.roleName ?? '';
    const description = activeOverrides.description ?? data?.description ?? '';
    const selectedIds = activeOverrides.selectedIds ?? data?.assignedIds ?? [];

    const setRoleName = useCallback((value) => updateOverride('roleName', value), [updateOverride]);

    const setDescription = useCallback((value) => updateOverride('description', value), [updateOverride]);

    const setSelectedIds = useCallback((value) => {
        setOverrides((current) => {
            const base = current.roleKey === roleKey ? current : { roleKey, ...EMPTY_OVERRIDES };
            const currentSelection = base.selectedIds ?? data?.assignedIds ?? [];
            const nextSelection = typeof value === 'function' ? value(currentSelection) : value;
            return { ...base, roleKey, selectedIds: nextSelection };
        });
    }, [roleKey, data]);

    const resetDraft = useCallback(() => {
        setOverrides({ roleKey, ...EMPTY_OVERRIDES });
    }, [roleKey]);

    const save = async () => {
        const privilegeNames = selectedIds
            .map((id) => allPrivileges.find((privilege) => privilege.id === id)?.name)
            .filter(Boolean);

        if (isEditMode) {
            await updateRole(role.name, roleName.trim(), description.trim() || null, privilegeNames);
        } else {
            await createRole(roleName.trim(), description.trim() || null, privilegeNames);
        }
    };

    return {
        allPrivileges,
        selectedIds,
        setSelectedIds,
        roleName,
        setRoleName,
        description,
        setDescription,
        loading: isPending,
        error: error ? (error instanceof Error ? error.message : 'Error al cargar privilegios') : null,
        save,
        resetDraft,
        isEditMode,
    };
}
