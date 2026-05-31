import { useEffect, useState } from 'react';
import { Box, Button, Chip, Divider, Typography } from '@mui/material';
import PeopleIcon from '@mui/icons-material/People';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../../common/components/DialogModal.jsx';
import { useDebounce } from '../../../../common/hooks/useDebounce.js';
import { fetchCompanyUsers, unassignCompanyUser, assignCompanyUsersBulk } from '../../services/companiesService';
import { searchUsers } from '../../../security/services/usersService';
import SearchableSelect from '../../../../common/components/SearchableSelect.jsx';

function userLabel(user) {
    const name = [user.firstName, user.lastName].filter(Boolean).join(' ');
    return name || user.username || user.id || 'Usuario';
}

export default function CompanyUsersModal({ open, companyId, companyName, onClose, onSaved }) {
    const [users, setUsers] = useState([]);
    const [availableUsers, setAvailableUsers] = useState([]);
    const [search, setSearch] = useState('');
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [selectedUserId, setSelectedUserId] = useState('');
    const [selectedUser, setSelectedUser] = useState(null);
    const [selectedUsers, setSelectedUsers] = useState({});
    const [confirmAssign, setConfirmAssign] = useState(false);
    const [alert, setAlert] = useState(null);
    const [confirmRemove, setConfirmRemove] = useState(null);
    const [duplicateWarning, setDuplicateWarning] = useState(null);
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

        searchUsers({ page: 0, size: 8, search: debouncedSearch })
            .then((response) => {
                if (cancelled) return;
                setAvailableUsers(response.content ?? []);
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
    }, [open, debouncedSearch]);

    useEffect(() => {
        if (!open) {
            setSelectedUserId('');
            setSelectedUser(null);
            setSelectedUsers({});
            setSearch('');
            setAlert(null);
            setConfirmRemove(null);
            setDuplicateWarning(null);
            setUsers([]);
            setAvailableUsers([]);
        }
    }, [open]);

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

    const handleAddSelected = () => {
        if (!selectedUser) {
            setAlert({ type: 'warning', message: 'Selecciona un usuario de la lista' });
            return;
        }
        if (users.some((user) => user.id === selectedUser.id) || selectedUsers[selectedUser.id]) {
            setDuplicateWarning(selectedUser);
            return;
        }
        setSelectedUsers((prev) => ({ ...prev, [selectedUser.id]: selectedUser }));
        setSelectedUserId('');
        setSelectedUser(null);
    };

    const handleRemoveSelected = (userId) => {
        setSelectedUsers((prev) => {
            const copy = { ...prev };
            delete copy[userId];
            return copy;
        });
    };

    const handleAssign = async () => {
        // bulk assign selectedUsers
        const ids = Object.keys(selectedUsers);
        if (!companyId || ids.length === 0) {
            setAlert({ type: 'warning', message: 'Selecciona al menos un usuario para vincular' });
            return;
        }
        const alreadyLinked = ids.some((id) => users.some((user) => user.id === id));
        if (alreadyLinked) {
            setAlert({ type: 'warning', message: 'No puedes vincular usuarios que ya están en la empresa' });
            return;
        }

        setConfirmAssign(false);
        setSaving(true);
        try {
            await assignCompanyUsersBulk(companyId, ids);
            setSelectedUsers({});
            const data = await fetchCompanyUsers(companyId);
            setUsers(Array.isArray(data) ? data : []);
            onSaved?.();
        } catch (error) {
            setAlert({ type: 'error', message: error?.response?.data?.message ?? error?.message ?? 'No se pudo vincular los usuarios' });
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
                primaryButton={{ label: saving ? 'Vinculando…' : 'Vincular', onClick: () => setConfirmAssign(true), disabled: saving }}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 3, display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                        <SearchableSelect
                            label="Usuarios del sistema"
                            value={selectedUserId}
                            onChange={(nextId) => {
                                setSelectedUserId(nextId);
                                setSelectedUser(availableUsers.find((user) => user.id === nextId) ?? null);
                            }}
                            onBlur={() => {}}
                            items={availableUsers}
                            getItemLabel={(user) => `${userLabel(user)} · ${user.email || '—'}`}
                            getItemValue={(user) => user.id}
                            fullWidth
                            size="small"
                            disabled={saving}
                            helperText={selectedUser ? `ID: ${selectedUser.id} · Correo: ${selectedUser.email || '—'}` : 'Busca y selecciona un usuario del sistema'}
                            externalSearch={search}
                            onSearchChange={(value) => {
                                setSearch(value);
                            }}
                            pageSize={8}
                        />

                        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                            <Button size="small" variant="outlined" onClick={handleAddSelected} disabled={saving || !selectedUser} sx={{ textTransform: 'none' }}>Agregar</Button>
                            <Typography sx={{ color: 'text.secondary', fontSize: 12 }}>Usuarios seleccionados:</Typography>
                        </Box>

                        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                            {Object.values(selectedUsers).length === 0 ? (
                                <Typography sx={{ color: 'text.secondary', fontSize: 13.5 }}>No hay usuarios seleccionados.</Typography>
                            ) : (
                                Object.values(selectedUsers).map((u) => (
                                    <Chip key={u.id} label={`${userLabel(u)} · ${u.email || '—'}`} onDelete={() => handleRemoveSelected(u.id)} />
                                ))
                            )}
                        </Box>

                        {/* Pagination removed from modal - handled by SearchableSelect dropdown */}
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
                open={!!duplicateWarning}
                type="warning"
                title="Usuario ya vinculado"
                message={`No puedes vincular a "${duplicateWarning ? `${duplicateWarning.firstName || ''} ${duplicateWarning.lastName || ''}`.trim() || duplicateWarning.username || duplicateWarning.id : ''}" porque ya está en la empresa.`}
                onClose={() => setDuplicateWarning(null)}
                confirmLabel="Entendido"
                cancelLabel="Cerrar"
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

            <DialogModal
                open={!!confirmAssign}
                title="Vincular usuarios"
                message={`¿Deseas vincular ${Object.keys(selectedUsers).length} usuario(s) a la empresa?`}
                onClose={() => setConfirmAssign(false)}
                onConfirm={handleAssign}
                confirmLabel="Vincular"
            />
        </>
    );
}

