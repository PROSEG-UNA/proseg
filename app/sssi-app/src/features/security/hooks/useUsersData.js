import { useState, useEffect } from 'react';
import { fetchUsers } from '../services/usersService';
import { getFriendlyApiErrorMessage } from '../../../common/utils';

function buildFullName(firstName, lastName) {
    return [firstName, lastName].filter(Boolean).join(' ').trim() || '—';
}

function mapStatusToSpanish(status) {
    switch (status) {
        case 'INVITED':
            return 'Invitado';
        case 'APPROVED':
            return 'Activo';
        case 'REJECTED':
            return 'Inactivo';
        case 'PENDING':
        default:
            return 'Pendiente';
    }
}

export function useUsersData({ pageIndex = 0, pageSize = 10, refreshKey = 0 } = {}) {
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [totalElements, setTotalElements] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    useEffect(() => {
        let ignore = false;

        const loadUsers = async () => {
            try {
                setLoading(true);
                setError(null);

                const response = await fetchUsers({ page: pageIndex, size: pageSize });

                if (ignore) return;

                const mappedRows = (response.content ?? []).map((user) => {
                    const statusRaw = user.status || 'PENDING';
                    return {
                        id: user.id,
                        username: user.username || '—',
                        email: user.email || '—',
                        fullName: buildFullName(user.firstName, user.lastName),
                        status: mapStatusToSpanish(statusRaw),
                        statusRaw,
                    };
                });

                setRows(mappedRows);
                setTotalElements(response.totalElements ?? 0);
                setTotalPages(response.totalPages ?? 0);
            } catch (err) {
                if (!ignore) {
                    setError(getFriendlyApiErrorMessage(err, 'Error al cargar usuarios'));
                }
            } finally {
                if (!ignore) {
                    setLoading(false);
                }
            }
        };

        void loadUsers();

        return () => {
            ignore = true;
        };
    }, [pageIndex, pageSize, refreshKey]);

    return { rows, loading, error, totalElements, totalPages };
}
