import { useState, useEffect } from 'react';
import { fetchAllPrivileges, fetchPermissionsByRole, updateRole } from '../services/rolesService';

export function useEditRoleData(role) {
    const [allPrivileges, setAllPrivileges] = useState([]);
    const [selectedIds, setSelectedIds] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!role) return;

        async function load() {
            try {
                setLoading(true);
                const [privileges, assigned] = await Promise.all([
                    fetchAllPrivileges(),
                    fetchPermissionsByRole(role.name),
                ]);

                setAllPrivileges(
                    privileges.map((p) => ({
                        id: p.id,
                        name: p.name,
                        description: p.description || '—',
                    }))
                );
                setSelectedIds(assigned.map((p) => p.id));
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        }

        load();
    }, [role]);

    const save = async () => {
        const privilegeNames = selectedIds
            .map((id) => allPrivileges.find((p) => p.id === id)?.name)
            .filter(Boolean);
        await updateRole(role.name, privilegeNames);
    };

    return { allPrivileges, selectedIds, setSelectedIds, loading, error, save };
}
