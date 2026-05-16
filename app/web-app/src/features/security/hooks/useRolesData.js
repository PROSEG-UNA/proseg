import { useState, useEffect } from 'react';
import { fetchRoles, fetchPermissionsByRole } from '../services/rolesService';

export function useRolesData({ pageIndex = 0, pageSize = 10, refreshKey = 0 } = {}) {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [totalElements, setTotalElements] = useState(0);

    const loadRoles = async (page, size, ignore) => {
        try {
            setLoading(true);
            setError(null);

            const response = await fetchRoles({ page, size });

            if (ignore) return;

            const rolesWithPermissions = await Promise.all(
                (response.content ?? []).map(async (role) => {
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
                setTotalElements(response.totalElements ?? 0);
            }
        } catch (err) {
            if (!ignore) setError(err.message);
        } finally {
            if (!ignore) setLoading(false);
        }
    };

    useEffect(() => {
        let ignore = false;
        void loadRoles(pageIndex, pageSize, ignore);
        return () => { ignore = true; };
    }, [pageIndex, pageSize, refreshKey]);

    return { rows, loading, error, totalElements };
}