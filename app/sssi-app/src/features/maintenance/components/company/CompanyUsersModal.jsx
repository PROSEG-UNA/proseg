import { useEffect, useState } from 'react';
import { Box, Button, Chip, Divider, IconButton, InputAdornment, MenuItem, TextField, Typography } from '@mui/material';
import PeopleIcon from '@mui/icons-material/People';
import SearchIcon from '@mui/icons-material/Search';
import NavigateBeforeIcon from '@mui/icons-material/NavigateBefore';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { assignCompanyUser, fetchCompanyUsers, unassignCompanyUser } from '../../services/companiesService';
import { searchUsers } from '../../../security/services/usersService';

function userLabel(user) {
    const name = [user.firstName, user.lastName].filter(Boolean).join(' ');
    return name || user.username || user.keycloakUserId || 'Usuario';
}

export default function CompanyUsersModal({ open, companyId, companyName, onClose, onSaved }) {
    const [users, setUsers] = useState([]);
    const [availableUsers, setAvailableUsers] = useState([]);
    const [totalPages, setTotalPages] = useState(0);
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [selectedUserId, setSelectedUserId] = useState('');
    const [selectedUser, setSelectedUser] = useState(null);
    const [alert, setAlert] = useState(null);
    const [confirmRemove, setConfirmRemove] = useState(null);
    const debouncedSearch = useDebounce(search, 350);

    const loadUsers = () => {
        if (!open || !companyId) return;
        let cancelled = false;
        setLoading(true);

        fetchCompanyUsers(companyId)
            .then((data) => {
                if (!cancelled) setUsers(Array.isArray(data) ? data : []);
            })
            .catch((error) => {
                if (!cancelled) {
                    setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudieron cargar los usuarios' });
                }
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => {
            cancelled = true;
        };
    };

    const loadAvailableUsers = () => {
        if (!open) return;
        let cancelled = false;
        setLoading(true);

        searchUsers({ page, size: 8, search: debouncedSearch })
            .then((response) => {
                if (cancelled) return;
                setAvailableUsers(response.content ?? []);
                setTotalPages(response.totalPages ?? 0);
            })
            .catch((error) => {
                if (!cancelled) {
                    setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo buscar usuarios' });
                }
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => {
            cancelled = true;
        };
    };

    useEffect(loadUsers, [open, companyId]);

    useEffect(() => {
        return loadAvailableUsers();
    }, [open, page, debouncedSearch]);

    useEffect(() => {
        if (!open) {
            setSelectedUserId('');
            setSelectedUser(null);
            setSearch('');
            setPage(0);
            setAlert(null);
            setConfirmRemove(null);
            setUsers([]);
            setAvailableUsers([]);
            setTotalPages(0);
        }
    }, [open]);

    useEffect(() => {
        setPage(0);
    }, [search]);

    useEffect(() => {
        if (!selectedUserId) {
            setSelectedUser(null);
            return;
        }

        const selectedFromPage = availableUsers.find((user) => user.id === selectedUserId);
        if (selectedFromPage) {
            setSelectedUser(selectedFromPage);
        }
    }, [availableUsers, selectedUserId]);

    const handleAssign = async () => {
        if (!companyId || !selectedUser) {
            setAlert({ type: 'warning', message: 'Selecciona un usuario de la lista' });
            return;
        }

        setSaving(true);
        try {
            await assignCompanyUser(companyId, selectedUser.id);
            setSelectedUserId('');
            const data = await fetchCompanyUsers(companyId);
            setUsers(Array.isArray(data) ? data : []);
            onSaved?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo vincular el usuario' });
        } finally {
            setSaving(false);
        }
    };

    const handleRemove = async () => {
        if (!confirmRemove || !companyId) return;
        setSaving(true);
        try {
            await unassignCompanyUser(companyId, confirmRemove.id);
            const data = await fetchCompanyUsers(companyId);
            setUsers(Array.isArray(data) ? data : []);
            setConfirmRemove(null);
            onSaved?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo desasignar el usuario' });
        } finally {
            setSaving(false);
        }
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="sm"
                icon={PeopleIcon}
                title={companyName ? `Usuarios de ${companyName}` : 'Usuarios de la empresa'}
                subtitle="Gestiona las cuentas vinculadas a esta empresa"
                loading={loading || saving}
                secondaryButton={{ label: 'Cerrar', onClick: onClose, disabled: saving }}
                primaryButton={{ label: saving ? 'Vinculando…' : 'Vincular', onClick: handleAssign, disabled: saving }}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                        <TextField
                            label="Buscar usuario"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            fullWidth
                            size="small"
                            disabled={saving}
                            placeholder="Busca por nombre, correo o usuario"
                            InputProps={{
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <SearchIcon fontSize="small" />
                                    </InputAdornment>
                                ),
                            }}
                            helperText="Selecciona el usuario deseado desde los resultados paginados"
                        />

                        <TextField
                            select
                            label="Usuarios encontrados"
                            value={selectedUserId}
                            onChange={(e) => {
                                const nextId = e.target.value;
                                setSelectedUserId(nextId);
                                setSelectedUser(availableUsers.find((user) => user.id === nextId) ?? null);
                            }}
                            fullWidth
                            size="small"
                            disabled={saving}
                            helperText={selectedUser ? `ID: ${selectedUser.id} · Correo: ${selectedUser.email || '—'}` : 'Elige un usuario de la lista'}
                        >
                            {availableUsers.length === 0 ? (
                                <MenuItem disabled value="">
                                    Sin resultados
                                </MenuItem>
                            ) : (
                                availableUsers.map((user) => (
                                    <MenuItem key={user.id} value={user.id}>
                                        {userLabel(user)} · {user.email || '—'}
                                    </MenuItem>
                                ))
                            )}
                            {selectedUser && !availableUsers.some((user) => user.id === selectedUser.id) ? (
                                <MenuItem value={selectedUser.id} sx={{ display: 'none' }}>
                                    {userLabel(selectedUser)} · {selectedUser.email || '—'}
                                </MenuItem>
                            ) : null}
                        </TextField>

                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1 }}>
                            <IconButton size="small" disabled={page === 0 || saving} onClick={() => setPage((p) => Math.max(0, p - 1))}>
                                <NavigateBeforeIcon fontSize="small" />
                            </IconButton>
                            <Typography sx={{ fontSize: 12, color: 'text.secondary', minWidth: 60, textAlign: 'center' }}>
                                {totalPages === 0 ? '0 / 0' : `${page + 1} / ${totalPages}`}
                            </Typography>
                            <IconButton size="small" disabled={totalPages === 0 || page >= totalPages - 1 || saving} onClick={() => setPage((p) => p + 1)}>
                                <NavigateNextIcon fontSize="small" />
                            </IconButton>
                        </Box>
                    </Box>

                    <Divider />

                    <Box>
                        <Typography sx={{ fontSize: 11.5, fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'text.secondary', mb: 1.5 }}>
                            Usuarios vinculados
                        </Typography>

                        {users.length === 0 ? (
                            <Typography sx={{ color: 'text.secondary', fontSize: 13.5 }}>
                                No hay usuarios vinculados.
                            </Typography>
                        ) : (
                            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                                {users.map((user) => (
                                    <Box
                                        key={user.id}
                                        sx={{
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            alignItems: 'center',
                                            gap: 2,
                                            p: 1.5,
                                            border: '1px solid',
                                            borderColor: 'divider',
                                            borderRadius: '12px',
                                        }}
                                    >
                                        <Box sx={{ minWidth: 0 }}>
                                            <Typography sx={{ fontWeight: 700, fontSize: 13.5 }} noWrap>
                                                {userLabel(user)}
                                            </Typography>
                                            <Typography sx={{ color: 'text.secondary', fontSize: 12.5 }} noWrap>
                                                {user.email || user.id}
                                            </Typography>
                                        </Box>
                                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                            <Chip label={user.status || '—'} size="small" variant="outlined" color="primary" />
                                            <Button
                                                size="small"
                                                color="error"
                                                variant="outlined"
                                                onClick={() => setConfirmRemove(user)}
                                                disabled={saving}
                                                sx={{ textTransform: 'none' }}
                                            >
                                                Quitar
                                            </Button>
                                        </Box>
                                    </Box>
                                ))}
                            </Box>
                        )}
                    </Box>
                </Box>
            </GeneralModal>

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />

            <DialogModal
                type="delete"
                open={!!confirmRemove}
                title="Desasignar usuario"
                message={`¿Seguro que deseas desasignar el usuario "${confirmRemove?.username || confirmRemove?.id || ''}" de esta empresa?`}
                onClose={() => setConfirmRemove(null)}
                onConfirm={handleRemove}
                confirmLabel="Desasignar"
            />
        </>
    );
}

