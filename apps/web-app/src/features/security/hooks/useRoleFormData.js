import { useState, useEffect } from 'react';
import { fetchAllPrivileges, fetchPermissionsByRole, createRole, updateRole } from '../services/rolesService';

export function useRoleFormData(role) {
    const isEditMode = !!role;

    const [allPrivileges, setAllPrivileges] = useState([]);
    const [selectedIds, setSelectedIds] = useState([]);
    const [roleName, setRoleName] = useState('');
    const [description, setDescription] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const load = async () => {
        try {
            setLoading(true);
            if (isEditMode) {
                const [privileges, assigned] = await Promise.all([
                    fetchAllPrivileges(),
                    fetchPermissionsByRole(role.name),
                ]);
                setAllPrivileges(privileges.map((p) => ({ id: p.id, name: p.name, description: p.description || '—' })));
                setSelectedIds(assigned.map((p) => p.id));
                setRoleName(role.name);
                setDescription(role.description || '');
            } else {
                const privileges = await fetchAllPrivileges();
                setAllPrivileges(privileges.map((p) => ({ id: p.id, name: p.name, description: p.description || '—' })));
                setSelectedIds([]);
                setRoleName('');
                setDescription('');
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Error al cargar privilegios');
        } finally {
            setLoading(false);
        }
    };

    const save = async () => {
        const privilegeNames = selectedIds
            .map((id) => allPrivileges.find((p) => p.id === id)?.name)
            .filter(Boolean);

        if (isEditMode) {
            await updateRole(role.name, roleName.trim(), description.trim() || null, privilegeNames);
        } else {
            await createRole(roleName.trim(), description.trim() || null, privilegeNames);
        }
    };

    useEffect(() => {
        void load();
    }, [role]);

    return { allPrivileges, selectedIds, setSelectedIds, roleName, setRoleName, description, setDescription, loading, error, save, isEditMode };
}