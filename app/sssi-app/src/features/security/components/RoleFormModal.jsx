import { useState, useMemo } from 'react';
import {
    Dialog, DialogContent,
    IconButton, Button, TextField, Typography,
    Box, Checkbox, Chip, Tooltip, LinearProgress,
    Collapse, Divider, alpha, useTheme, useMediaQuery,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ShieldIcon from '@mui/icons-material/Shield';
import PeopleIcon from '@mui/icons-material/People';
import ManageAccountsIcon from '@mui/icons-material/ManageAccounts';
import VisibilityIcon from '@mui/icons-material/Visibility';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import AlertModal from '../../../common/components/AlertModal.jsx';
import { useRoleFormData } from '../hooks/useRoleFormData';

const RED = {
    50:  '#fff1f2',
    100: '#ffe4e6',
    200: '#fecdd3',
    400: '#f87171',
    500: '#ef4444',
    600: '#dc2626',
    700: '#b91c1c',
    800: '#991b1b',
    900: '#7f1d1d',
};

const DOMAIN_META = {
    Usuarios: {
        lightColor: RED[600],
        darkColor:  RED[400],
        lightBg:    RED[50],
        darkBg:     alpha(RED[900], 0.35),
        icon: PeopleIcon,
        description: 'Gestión y registro de usuarios',
    },
    Roles: {
        lightColor: '#b45309',
        darkColor:  '#fbbf24',
        lightBg:    '#fffbeb',
        darkBg:     alpha('#78350f', 0.35),
        icon: ShieldIcon,
        description: 'Configuración de roles del sistema',
    },
    'Roles de Usuario': {
        lightColor: '#0f766e',
        darkColor:  '#2dd4bf',
        lightBg:    '#f0fdfa',
        darkBg:     alpha('#134e4a', 0.35),
        icon: ManageAccountsIcon,
        description: 'Asignación de roles a usuarios',
    },
};

const ROLE_PRESETS = [
    {
        id: 'solo-lectura',
        name: 'Solo Lectura',
        description: 'Consulta usuarios, roles y composites sin modificar nada',
        color: '#0f766e',
        darkColor: '#2dd4bf',
        icon: VisibilityIcon,
        privileges: ['LEER_USUARIOS','LEER_USUARIO','LEER_ROLES_BASE','LEER_ROLES_COMPUESTOS','LEER_COMPOSITES_ROL','LEER_ROLES_USUARIO','LEER_USUARIOS_POR_ROL'],
    },
    {
        id: 'gestion-usuarios',
        name: 'Gestión de Usuarios',
        description: 'Registra, aprueba y consulta usuarios del sistema',
        color: RED[600],
        darkColor: RED[400],
        icon: PeopleIcon,
        privileges: ['LEER_USUARIOS','LEER_USUARIO','REGISTRAR_USUARIO','APROBAR_USUARIO','LEER_ROLES_USUARIO','LEER_USUARIOS_POR_ROL'],
    },
    {
        id: 'gestion-roles',
        name: 'Gestión de Roles',
        description: 'CRUD completo sobre roles y sus composites',
        color: '#b45309',
        darkColor: '#fbbf24',
        icon: ShieldIcon,
        privileges: ['CREAR_ROL','EDITAR_ROL','ELIMINAR_ROL','LEER_ROLES_BASE','LEER_ROLES_COMPUESTOS','LEER_COMPOSITES_ROL','ASIGNAR_ROL_USUARIO','REMOVER_ROL_USUARIO'],
    },
    {
        id: 'administrador',
        name: 'Administrador',
        description: 'Acceso total — todos los privilegios del sistema',
        color: RED[800],
        darkColor: RED[200],
        icon: AdminPanelSettingsIcon,
        privileges: ['LEER_ROLES_BASE','REMOVER_ROL_USUARIO','LEER_ROLES_COMPUESTOS','APROBAR_USUARIO','ELIMINAR_ROL','ASIGNAR_ROL_USUARIO','LEER_COMPOSITES_ROL','LEER_USUARIO','CREAR_ROL','LEER_USUARIOS_POR_ROL','LEER_ROLES_USUARIO','REGISTRAR_USUARIO','EDITAR_ROL','LEER_USUARIOS'],
    },
];

const groupPrivilegesByDomain = (privileges) => {
    const groups = { Usuarios: [], Roles: [], 'Roles de Usuario': [] };
    const USUARIOS_SET = new Set(['LEER_USUARIOS','LEER_USUARIO','REGISTRAR_USUARIO','APROBAR_USUARIO','LEER_USUARIOS_POR_ROL']);
    const ROLES_SET    = new Set(['CREAR_ROL','EDITAR_ROL','ELIMINAR_ROL','LEER_ROLES_BASE','LEER_ROLES_COMPUESTOS','LEER_COMPOSITES_ROL']);
    const ROL_USR_SET  = new Set(['ASIGNAR_ROL_USUARIO','REMOVER_ROL_USUARIO','LEER_ROLES_USUARIO']);
    privileges.forEach(p => {
        if (USUARIOS_SET.has(p.name))     groups.Usuarios.push(p);
        else if (ROLES_SET.has(p.name))   groups.Roles.push(p);
        else if (ROL_USR_SET.has(p.name)) groups['Roles de Usuario'].push(p);
        else                              groups.Usuarios.push(p);
    });
    return groups;
};

function SectionLabel({ children, noMargin }) {
    return (
        <Typography sx={{ fontSize: 10.5, fontWeight: 800, color: 'text.disabled', letterSpacing: '0.08em', textTransform: 'uppercase', mb: noMargin ? 0 : 1.5 }}>
            {children}
        </Typography>
    );
}

function SelectionBadge({ selected, total, color }) {
    const pct = total === 0 ? 0 : Math.round((selected / total) * 100);
    return (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
            {selected === total && total > 0 && <CheckCircleIcon sx={{ fontSize: 14, color }} />}
            <Typography variant="caption" sx={{ fontWeight: 700, color: selected > 0 ? color : 'text.disabled', fontSize: 11 }}>
                {selected}/{total}
            </Typography>
            <Box sx={{ width: 40, height: 3, borderRadius: 2, bgcolor: 'divider', overflow: 'hidden' }}>
                <Box sx={{ width: `${pct}%`, height: '100%', background: color, borderRadius: 2, transition: 'width 0.25s ease' }} />
            </Box>
        </Box>
    );
}

function PresetChip({ preset, onApply, active, isDark }) {
    const color = isDark ? preset.darkColor : preset.color;
    const PresetIcon = preset.icon;
    return (
        <Tooltip title={preset.description} arrow placement="top">
            <Box
                onClick={() => onApply(preset)}
                sx={{
                    border: '1.5px solid',
                    borderColor: active ? color : 'divider',
                    borderWidth: active ? '2px' : '1.5px',
                    borderRadius: '10px',
                    px: 1.5, py: 0.875,
                    cursor: 'pointer',
                    bgcolor: active ? alpha(color, isDark ? 0.15 : 0.07) : 'background.paper',
                    transition: 'all 0.15s ease',
                    display: 'flex', alignItems: 'center', gap: 0.75,
                    '&:hover': {
                        borderColor: color,
                        bgcolor: alpha(color, isDark ? 0.12 : 0.05),
                        transform: 'translateY(-1px)',
                        boxShadow: `0 4px 12px ${alpha(color, 0.22)}`,
                    },
                }}
            >
                <PresetIcon sx={{ fontSize: 13, color }} />
                <Typography sx={{ fontSize: 11.5, fontWeight: 700, color: active ? color : 'text.secondary', whiteSpace: 'nowrap' }}>
                    {preset.name}
                </Typography>
            </Box>
        </Tooltip>
    );
}

function PrivilegeRow({ privilege, checked, onChange, domainColor }) {
    return (
        <Box
            onClick={onChange}
            sx={{
                display: 'flex', alignItems: 'flex-start', gap: 1.5,
                px: 1.5, py: 0.875, borderRadius: '8px', cursor: 'pointer',
                transition: 'background 0.1s',
                '&:hover': { bgcolor: 'action.hover' },
            }}
        >
            <Checkbox
                checked={checked}
                onChange={onChange}
                onClick={e => e.stopPropagation()}
                size="small"
                sx={{ p: 0, mt: '2px', color: 'text.disabled', '&.Mui-checked': { color: domainColor } }}
            />
            <Box>
                <Typography sx={{ fontSize: 12.5, fontWeight: 700, color: 'text.primary', lineHeight: 1.3, fontFamily: '"Roboto Mono", monospace', letterSpacing: '0.01em' }}>
                    {privilege.name}
                </Typography>
                {privilege.description && (
                    <Typography sx={{ fontSize: 11, color: 'text.secondary', mt: 0.25, lineHeight: 1.45 }}>
                        {privilege.description}
                    </Typography>
                )}
            </Box>
        </Box>
    );
}

function DomainPanel({ domainName, privileges, selectedIds, onToggleAll, onToggle, isDark, defaultExpanded = false }) {
    const [expanded, setExpanded] = useState(defaultExpanded);
    const fallback = { lightColor: '#6b7280', darkColor: '#9ca3af', lightBg: '#f9fafb', darkBg: 'rgba(0,0,0,0.2)', icon: ShieldIcon, description: '' };
    const meta = DOMAIN_META[domainName] ?? fallback;
    const DomainIcon = meta.icon;
    const color  = isDark ? meta.darkColor : meta.lightColor;
    const iconBg = isDark ? meta.darkBg    : meta.lightBg;

    const selectedInDomain = privileges.filter(p => selectedIds.includes(p.id)).length;
    const allSelected  = selectedInDomain === privileges.length && privileges.length > 0;
    const someSelected = selectedInDomain > 0 && !allSelected;

    return (
        <Box sx={{ border: '1.5px solid', borderColor: expanded ? color : 'divider', borderRadius: '12px', overflow: 'hidden', transition: 'border-color 0.2s', mb: 1.25 }}>
            <Box
                sx={{
                    display: 'flex', alignItems: 'center', px: 2, py: 1.5,
                    bgcolor: expanded ? alpha(color, isDark ? 0.1 : 0.04) : 'background.paper',
                    cursor: 'pointer', transition: 'background 0.15s',
                    '&:hover': { bgcolor: alpha(color, isDark ? 0.1 : 0.04) },
                }}
                onClick={() => setExpanded(v => !v)}
            >
                <Box sx={{ width: 34, height: 34, borderRadius: '9px', background: iconBg, display: 'flex', alignItems: 'center', justifyContent: 'center', mr: 1.5, flexShrink: 0 }}>
                    <DomainIcon sx={{ fontSize: 17, color }} />
                </Box>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography sx={{ fontSize: 13.5, fontWeight: 700, color: 'text.primary' }}>{domainName}</Typography>
                    <Typography sx={{ fontSize: 11.5, color: 'text.secondary' }}>{meta.description}</Typography>
                </Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, mr: 0.5 }}>
                    <SelectionBadge selected={selectedInDomain} total={privileges.length} color={color} />
                    <Tooltip title={allSelected ? 'Deseleccionar todos' : 'Seleccionar todos'}>
                        <Checkbox
                            checked={allSelected}
                            indeterminate={someSelected}
                            onChange={e => { e.stopPropagation(); onToggleAll(); }}
                            onClick={e => e.stopPropagation()}
                            size="small"
                            sx={{ p: 0, color: 'text.disabled', '&.Mui-checked, &.MuiCheckbox-indeterminate': { color } }}
                        />
                    </Tooltip>
                    <IconButton size="small" sx={{ p: 0.25, color: 'text.secondary' }}>
                        {expanded ? <ExpandLessIcon sx={{ fontSize: 17 }} /> : <ExpandMoreIcon sx={{ fontSize: 17 }} />}
                    </IconButton>
                </Box>
            </Box>

            <Collapse in={expanded}>
                <Divider />
                <Box sx={{ py: 0.75, px: 0.5, bgcolor: 'background.default' }}>
                    {privileges.map(p => (
                        <PrivilegeRow
                            key={p.id}
                            privilege={p}
                            checked={selectedIds.includes(p.id)}
                            onChange={() => onToggle(p.id)}
                            domainColor={color}
                        />
                    ))}
                </Box>
            </Collapse>
        </Box>
    );
}

export default function RoleFormModal({ open, onClose, onSaved, role = null }) {
    const theme    = useTheme();
    const isDark   = theme.palette.mode === 'dark';
    // En pantallas pequeñas (laptop 768–1024px) usamos fullScreen para aprovechar toda la altura
    const isSmall  = useMediaQuery(theme.breakpoints.down('md'));

    const { allPrivileges, selectedIds, setSelectedIds, roleName, setRoleName, description, setDescription, save, isEditMode } = useRoleFormData(role);

    const [alert, setAlert]               = useState(null);
    const [saving, setSaving]             = useState(false);
    const [activePreset, setActivePreset] = useState(null);

    const grouped         = useMemo(() => groupPrivilegesByDomain(allPrivileges), [allPrivileges]);
    const totalPrivileges = allPrivileges.length;
    const selectedCount   = selectedIds.length;
    const completionPct   = totalPrivileges === 0 ? 0 : Math.round((selectedCount / totalPrivileges) * 100);

    const headerGradient = isDark
        ? `linear-gradient(135deg, ${RED[900]} 0%, ${RED[800]} 100%)`
        : `linear-gradient(135deg, ${RED[600]} 0%, ${RED[800]} 100%)`;
    const accentColor = isDark ? RED[400] : RED[600];

    const fieldSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': { borderColor: 'divider' },
            '&:hover fieldset': { borderColor: alpha(accentColor, 0.5) },
            '&.Mui-focused fieldset': { borderColor: accentColor },
        },
        '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
        '& .MuiInputBase-input': { color: 'text.primary' },
    };

    const applyPreset = (preset) => {
        const ids = allPrivileges.filter(p => preset.privileges.includes(p.name)).map(p => p.id);
        setSelectedIds(ids);
        setActivePreset(preset.id);
    };

    const toggleDomain = (group) => {
        const ids = group.map(p => p.id);
        const allSelected = ids.every(id => selectedIds.includes(id));
        setSelectedIds(allSelected ? selectedIds.filter(id => !ids.includes(id)) : [...new Set([...selectedIds, ...ids])]);
        setActivePreset(null);
    };

    const togglePrivilege = (id) => {
        setSelectedIds(prev => prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]);
        setActivePreset(null);
    };

    const handleSave = async () => {
        if (!roleName.trim())    { setAlert({ type: 'warning', message: 'El nombre del rol es requerido' }); return; }
        if (!selectedIds.length) { setAlert({ type: 'warning', message: 'Seleccioná al menos un privilegio' }); return; }
        setSaving(true);
        try {
            await save();
            setAlert({ type: 'success', message: isEditMode ? `Rol "${roleName}" actualizado correctamente` : `Rol "${roleName}" creado correctamente` });
            onSaved?.();
        } catch (e) {
            setAlert({ type: 'error', message: e?.message || 'Error al guardar' });
        } finally {
            setSaving(false);
        }
    };

    return (
        <Dialog
            open={open}
            onClose={onClose}
            fullScreen={isSmall}
            maxWidth="md"
            fullWidth
            slotProps={{ backdrop: { sx: { backdropFilter: 'blur(3px)' } } }}
            PaperProps={{
                sx: {
                    maxHeight: isSmall ? '100vh' : '92vh',
                    height: isSmall ? '100vh' : 'auto',
                    borderRadius: isSmall ? 0 : '16px',
                    overflow: 'hidden',
                    display: 'flex',
                    flexDirection: 'column',
                    boxShadow: isDark
                        ? `0 24px 48px rgba(0,0,0,0.55), 0 0 0 1px ${alpha(RED[700], 0.3)}`
                        : '0 24px 48px rgba(0,0,0,0.14)',
                    bgcolor: 'background.paper',
                },
            }}
        >
            <Box sx={{ flexShrink: 0, background: headerGradient, px: { xs: 2.5, sm: 3 }, py: 2.25, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <Box sx={{ width: 38, height: 38, borderRadius: '10px', background: 'rgba(255,255,255,0.15)', border: '1px solid rgba(255,255,255,0.22)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <AdminPanelSettingsIcon sx={{ color: '#fff', fontSize: 20 }} />
                    </Box>
                    <Box>
                        <Typography sx={{ color: '#fff', fontWeight: 800, fontSize: { xs: 15, sm: 15.5 }, lineHeight: 1.2, letterSpacing: '-0.01em' }}>
                            {isEditMode ? 'Editar rol' : 'Nuevo rol'}
                        </Typography>
                        <Typography sx={{ color: 'rgba(255,255,255,0.65)', fontSize: { xs: 11, sm: 11.5 } }}>
                            {isEditMode ? 'Modificá nombre, descripción y privilegios' : 'Definí los permisos para este rol'}
                        </Typography>
                    </Box>
                </Box>
                <IconButton onClick={onClose} size="small" sx={{ color: 'rgba(255,255,255,0.75)', border: '1px solid rgba(255,255,255,0.2)', p: 0.625, '&:hover': { bgcolor: 'rgba(255,255,255,0.15)', color: '#fff' } }}>
                    <CloseIcon sx={{ fontSize: 16 }} />
                </IconButton>
            </Box>

            {saving && (
                <LinearProgress sx={{ flexShrink: 0, height: 2, bgcolor: alpha(accentColor, 0.15), '& .MuiLinearProgress-bar': { bgcolor: accentColor } }} />
            )}

            <DialogContent
                sx={{
                    p: 0,
                    flex: 1,
                    minHeight: 0,
                    overflowY: 'auto',
                    display: 'flex',
                    flexDirection: 'column',
                    bgcolor: 'background.paper',

                    '&::-webkit-scrollbar': { width: '5px' },
                    '&::-webkit-scrollbar-track': { background: 'transparent' },
                    '&::-webkit-scrollbar-thumb': { background: alpha(accentColor, 0.25), borderRadius: '4px' },
                    '&::-webkit-scrollbar-thumb:hover': { background: alpha(accentColor, 0.45) },
                }}
            >
                {/* INFO BÁSICA */}
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 2, flexShrink: 0 }}>
                    <SectionLabel>Información básica</SectionLabel>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                        <TextField label="Nombre del rol" value={roleName} onChange={e => setRoleName(e.target.value)} fullWidth size="small" sx={fieldSx} />
                        <TextField label="Descripción" value={description} onChange={e => setDescription(e.target.value)} fullWidth size="small" multiline rows={2} sx={fieldSx} />
                    </Box>
                </Box>

                <Divider sx={{ flexShrink: 0 }} />

                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2, pb: 2, flexShrink: 0 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.25 }}>
                        <SectionLabel noMargin>Plantillas rápidas</SectionLabel>
                        <Typography sx={{ fontSize: 10.5, color: 'text.disabled' }}>Podés ajustar después</Typography>
                    </Box>
                    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.875 }}>
                        {ROLE_PRESETS.map(preset => (
                            <PresetChip key={preset.id} preset={preset} onApply={applyPreset} active={activePreset === preset.id} isDark={isDark} />
                        ))}
                    </Box>
                </Box>

                <Divider sx={{ flexShrink: 0 }} />

                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2, pb: 3, flex: 1 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.75 }}>
                        <SectionLabel noMargin>Permisos por dominio</SectionLabel>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.875 }}>
                            <Chip
                                label={`${selectedCount} seleccionados`}
                                size="small"
                                sx={{
                                    fontSize: 11, height: 20, fontWeight: 700,
                                    bgcolor: selectedCount > 0 ? alpha(accentColor, isDark ? 0.2 : 0.1) : 'action.hover',
                                    color: selectedCount > 0 ? accentColor : 'text.disabled',
                                    border: 'none',
                                }}
                            />
                            <Typography sx={{ fontSize: 11, color: 'text.disabled', fontWeight: 600 }}>{completionPct}%</Typography>
                        </Box>
                    </Box>

                    {Object.entries(grouped).map(([domainName, privileges], i) =>
                        privileges.length > 0 ? (
                            <DomainPanel
                                key={domainName}
                                domainName={domainName}
                                privileges={privileges}
                                selectedIds={selectedIds}
                                onToggleAll={() => toggleDomain(privileges)}
                                onToggle={togglePrivilege}
                                isDark={isDark}
                                defaultExpanded={i === 0}
                            />
                        ) : null
                    )}
                </Box>
            </DialogContent>

            <Box
                sx={{
                    flexShrink: 0,
                    px: { xs: 2.5, sm: 3 }, py: 1.75,
                    borderTop: '1px solid', borderColor: 'divider',
                    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                    bgcolor: isDark ? alpha('#000', 0.25) : alpha(RED[50], 0.7),
                }}
            >
                <Typography sx={{ fontSize: 11.5, color: 'text.disabled', fontWeight: 500 }}>
                    {selectedCount === 0
                        ? 'Sin privilegios asignados'
                        : `${selectedCount} privilegio${selectedCount !== 1 ? 's' : ''} asignado${selectedCount !== 1 ? 's' : ''}`}
                </Typography>
                <Box sx={{ display: 'flex', gap: 1 }}>
                    <Button
                        onClick={onClose}
                        variant="outlined"
                        size="small"
                        sx={{ borderColor: 'divider', color: 'text.secondary', textTransform: 'none', fontWeight: 600, fontSize: 12.5, borderRadius: '8px', '&:hover': { borderColor: accentColor, color: accentColor, bgcolor: alpha(accentColor, 0.05) } }}
                    >
                        Cancelar
                    </Button>
                    <Button
                        onClick={handleSave}
                        disabled={saving}
                        variant="contained"
                        size="small"
                        sx={{
                            background: headerGradient,
                            textTransform: 'none', fontWeight: 700, fontSize: 12.5, borderRadius: '8px',
                            boxShadow: `0 4px 14px ${alpha(RED[600], isDark ? 0.4 : 0.3)}`,
                            px: 2.5, letterSpacing: '0.01em',
                            '&:hover': {
                                background: isDark
                                    ? `linear-gradient(135deg, ${RED[800]}, ${RED[700]})`
                                    : `linear-gradient(135deg, ${RED[700]}, ${RED[900]})`,
                                boxShadow: `0 6px 18px ${alpha(RED[600], 0.4)}`,
                            },
                            '&:disabled': { opacity: 0.55 },
                        }}
                    >
                        {saving ? 'Guardando…' : isEditMode ? 'Guardar cambios' : 'Crear rol'}
                    </Button>
                </Box>
            </Box>

            <AlertModal open={!!alert} type={alert?.type} message={alert?.message} onClose={() => setAlert(null)} />
        </Dialog>
    );
}