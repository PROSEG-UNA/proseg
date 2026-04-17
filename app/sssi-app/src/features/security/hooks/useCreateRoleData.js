import { useState, useEffect } from 'react';
import { fetchAllPrivileges, createRole } from '../services/rolesService';

export function useCreateRoleData() {
    const [allPrivileges, setAllPrivileges] = useState([]);
    const [selectedIds, setSelectedIds] = useState([]);
    const [roleName, setRoleName] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const load = async () => {
        try {
            setLoading(true);
            const privileges = await fetchAllPrivileges();
            setAllPrivileges(
                privileges.map((p) => ({
                    id: p.id,
                    name: p.name,
                    description: p.description || '—',
                }))
            );
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
        await createRole(roleName.trim(), privilegeNames);
    };

    useEffect(() => {
        void load();
    }, []);

    return { allPrivileges, selectedIds, setSelectedIds, roleName, setRoleName, loading, error, save };
}