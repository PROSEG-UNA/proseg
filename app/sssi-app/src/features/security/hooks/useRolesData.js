import { useState, useEffect } from 'react';
import { fetchRoles, fetchPermissionsByRole } from '../services/rolesService';

export function useRolesData(refreshKey = 0) {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        let ignore = false;

        async function loadRoles() {
            try {
                setLoading(true);
                const roles = await fetchRoles();

                const rolesWithPermissions = await Promise.all(
                    roles.map(async (role) => {
                        const permissions = await fetchPermissionsByRole(role.name);
                        return {
                            id: role.id,
                            name: role.name,
                            description: role.description || '—',
                            permissionCount: permissions.length,
                        };
                    })
                );

                if (!ignore) {
                    setRows(rolesWithPermissions);
                }
            } catch (err) {
                if (!ignore) setError(err.message);
            } finally {
                if (!ignore) setLoading(false);
            }
        }

        loadRoles();
        return () => { ignore = true; };
    }, [refreshKey]);

    return { rows, loading, error };
}