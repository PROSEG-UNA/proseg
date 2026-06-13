import { useEffect, useState, useMemo } from 'react';
import {
    Box, Typography, Checkbox, Chip, Tooltip,
    Collapse, Divider, IconButton, useTheme, useColorScheme,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ShieldIcon from '@mui/icons-material/Shield';
import PeopleIcon from '@mui/icons-material/People';
import ManageAccountsIcon from '@mui/icons-material/ManageAccounts';
import VisibilityIcon from '@mui/icons-material/Visibility';
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings';
import InventoryIcon from '@mui/icons-material/Inventory';
import FolderIcon from '@mui/icons-material/Folder';
import BusinessIcon from '@mui/icons-material/Business';
import ConstructionIcon from '@mui/icons-material/Construction';
import AssignmentIcon from '@mui/icons-material/Assignment';
import PushPinIcon from '@mui/icons-material/PushPin';
import ConfirmationNumberIcon from '@mui/icons-material/ConfirmationNumber';
import GeneralModal from '../../../common/components/GeneralModal.jsx';
import DialogModal from '../../../common/components/DialogModal.jsx';
import { useRoleFormData } from '../hooks/useRoleFormData';
import { ValidatedTextField } from '../../../common/components/ValidatedTextField';
import { getValidationRule, validateField } from '../../../common/utils/validationRegex';
import { formatRoleName } from '../../../common/utils/index.js';

const DOMAIN_META = {
    Usuarios: {
        lightColor: '#dc2626',
        darkColor:  '#f87171',
        lightBg:    '#fff1f2',
        darkBg:     'rgba(127,29,29,0.35)',
        icon: PeopleIcon,
        description: 'Gestión y registro de usuarios',
    },
    Roles: {
        lightColor: '#b45309',
        darkColor:  '#fbbf24',
        lightBg:    '#fffbeb',
        darkBg:     'rgba(120,53,15,0.35)',
        icon: ShieldIcon,
        description: 'Configuración de roles del sistema',
    },
    'Roles de Usuario': {
        lightColor: '#0f766e',
        darkColor:  '#2dd4bf',
        lightBg:    '#f0fdfa',
        darkBg:     'rgba(19,78,74,0.35)',
        icon: ManageAccountsIcon,
        description: 'Asignación de roles a usuarios',
    },
    Inventario: {
        lightColor: '#1d4ed8',
        darkColor:  '#60a5fa',
        lightBg:    '#eff6ff',
        darkBg:     'rgba(30,58,95,0.35)',
        icon: InventoryIcon,
        description: 'Gestión de activos',
    },
    Ubicaciones: {
        lightColor: '#be185d',
        darkColor:  '#f472b6',
        lightBg:    '#fdf2f8',
        darkBg:     'rgba(131,24,67,0.35)',
        icon: PushPinIcon,
        description: 'Gestión de campus, edificios y locaciones',
    },
    Archivos: {
        lightColor: '#7c3aed',
        darkColor:  '#c084fc',
        lightBg:    '#faf5ff',
        darkBg:     'rgba(76,29,149,0.35)',
        icon: FolderIcon,
        description: 'Gestión y acceso a archivos del sistema',
    },
    Empresas: {
        lightColor: '#059669',
        darkColor:  '#34d399',
        lightBg:    '#ecfdf5',
        darkBg:     'rgba(6,78,59,0.35)',
        icon: BusinessIcon,
        description: 'Empresas de mantenimiento y sus usuarios',
    },
    'Solicitud de mantenimiento': {
        lightColor: '#ea580c',
        darkColor:  '#fb923c',
        lightBg:    '#fff7ed',
        darkBg:     'rgba(124,45,18,0.35)',
        icon: ConstructionIcon,
        description: 'Solicitudes de mantenimiento',
    },
    'Registro de mantenimiento': {
        lightColor: '#0284c7',
        darkColor:  '#38bdf8',
        lightBg:    '#f0f9ff',
        darkBg:     'rgba(12,74,110,0.35)',
        icon: AssignmentIcon,
        description: 'Registros, historial y técnicos de mantenimiento',
    },
    Tickets: {
        lightColor: '#4f46e5',
        darkColor:  '#818cf8',
        lightBg:    '#eef2ff',
        darkBg:     'rgba(49,46,129,0.35)',
        icon: ConfirmationNumberIcon,
        description: 'Tickets de mantenimiento y su seguimiento',
    },
};

const ROLE_PRESETS = [
    {
        id: 'solo-lectura',
        name: 'Solo Lectura',
        description: 'Consulta todos los módulos (usuarios, roles, inventario, ubicaciones, archivos, empresas, mantenimiento y tickets) sin modificar nada',
        color: '#0f766e',
        darkColor: '#2dd4bf',
        icon: VisibilityIcon,
        filter: name => name.startsWith('LEER_'),
    },
    {
        id: 'gestion-usuarios',
        name: 'Gestión de Usuarios',
        description: 'Registra, aprueba y consulta usuarios del sistema',
        color: '#dc2626',
        darkColor: '#f87171',
        icon: PeopleIcon,
        privileges: ['LEER_USUARIOS','LEER_USUARIO','CREAR_USUARIO','APROBAR_USUARIO','LEER_ROLES_USUARIO','LEER_USUARIOS_POR_ROL'],
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
        color: '#991b1b',
        darkColor: '#fecdd3',
        icon: AdminPanelSettingsIcon,
        selectAll: true,
    },
];

const groupPrivilegesByDomain = (privileges) => {
    const groups = {
        Usuarios: [],
        Roles: [],
        'Roles de Usuario': [],
        Inventario: [],
        Ubicaciones: [],
        Archivos: [],
        Empresas: [],
        'Solicitud de mantenimiento': [],
        'Registro de mantenimiento': [],
        Tickets: [],
    };
    privileges.forEach(p => {
        const target = p.domain && groups[p.domain] !== undefined ? p.domain : 'Usuarios';
        groups[target].push(p);
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
                    bgcolor: active ? `color-mix(in srgb, ${color} ${isDark ? 15 : 7}%, transparent)` : 'background.paper',
                    transition: 'all 0.15s ease',
                    display: 'flex', alignItems: 'center', gap: 0.75,
                    '&:hover': {
                        borderColor: color,
                        bgcolor: `color-mix(in srgb, ${color} ${isDark ? 12 : 5}%, transparent)`,
                        transform: 'translateY(-1px)',
                        boxShadow: `0 4px 12px color-mix(in srgb, ${color} 22%, transparent)`,
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
                    {formatRoleName(privilege.name)}
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
                    bgcolor: expanded ? `color-mix(in srgb, ${color} ${isDark ? 10 : 4}%, transparent)` : 'background.paper',
                    cursor: 'pointer', transition: 'background 0.15s',
                    '&:hover': { bgcolor: `color-mix(in srgb, ${color} ${isDark ? 10 : 4}%, transparent)` },
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
    const theme = useTheme();
    const { mode, systemMode } = useColorScheme();
    const isDark = (mode === 'system' ? systemMode : mode) === 'dark';
    const accentColor = theme.vars.palette.tones.rose.fg;

    const { allPrivileges, selectedIds, setSelectedIds, roleName, setRoleName, description, setDescription, save, isEditMode } = useRoleFormData(role);

    const [alert, setAlert]               = useState(null);
    const [saving, setSaving]             = useState(false);
    const [activePreset, setActivePreset] = useState(null);
    const [touched, setTouched]           = useState({ roleName: false, roleDescription: false });
    const [errors, setErrors]             = useState({});

    useEffect(() => {
        setTouched({ roleName: false, roleDescription: false });
        setErrors({});
        setActivePreset(null);
    }, [role, open]);

    const grouped         = useMemo(() => groupPrivilegesByDomain(allPrivileges), [allPrivileges]);
    const totalPrivileges = allPrivileges.length;
    const selectedCount   = selectedIds.length;
    const completionPct   = totalPrivileges === 0 ? 0 : Math.round((selectedCount / totalPrivileges) * 100);

    const fieldSx = {
        '& .MuiOutlinedInput-root': {
            borderRadius: '10px',
            '& fieldset': { borderColor: 'divider' },
            '&:hover fieldset': { borderColor: `color-mix(in srgb, ${accentColor} 50%, transparent)` },
            '&.Mui-focused fieldset': { borderColor: accentColor },
        },
        '& .MuiInputLabel-root.Mui-focused': { color: accentColor },
        '& .MuiInputBase-input': { color: 'text.primary' },
    };

    const applyPreset = (preset) => {
        const ids = preset.selectAll
            ? allPrivileges.map(p => p.id)
            : preset.filter
                ? allPrivileges.filter(p => preset.filter(p.name)).map(p => p.id)
                : allPrivileges.filter(p => preset.privileges.includes(p.name)).map(p => p.id);
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

    const handleRoleNameChange = (event) => {
        setRoleName(event.target.value);
        if (touched.roleName) {
            const result = validateField(event.target.value, getValidationRule('roleName'));
            setErrors((prev) => ({ ...prev, roleName: result.isValid ? '' : result.error }));
        }
    };

    const handleDescriptionChange = (event) => {
        setDescription(event.target.value);
        if (touched.roleDescription) {
            const result = validateField(event.target.value, getValidationRule('roleDescription'));
            setErrors((prev) => ({ ...prev, roleDescription: result.isValid ? '' : result.error }));
        }
    };

    const handleFieldBlur = (fieldName, value) => {
        setTouched((prev) => ({ ...prev, [fieldName]: true }));
        const rule = getValidationRule(fieldName);
        if (rule) {
            const result = validateField(value, rule);
            setErrors((prev) => ({ ...prev, [fieldName]: result.isValid ? '' : result.error }));
        }
    };

    const handleSave = async () => {
        const roleNameRule        = getValidationRule('roleName');
        const roleDescriptionRule = getValidationRule('roleDescription');
        const roleNameCheck        = validateField(roleName || '', roleNameRule);
        const roleDescriptionCheck = validateField(description || '', roleDescriptionRule);

        setTouched({ roleName: true, roleDescription: true });
        setErrors({
            roleName:        roleNameCheck.isValid ? '' : roleNameCheck.error,
            roleDescription: roleDescriptionCheck.isValid ? '' : roleDescriptionCheck.error,
        });

        if (!roleNameCheck.isValid || !roleDescriptionCheck.isValid) {
            setAlert({ type: 'warning', message: 'Revisa los datos del rol antes de continuar' });
            return;
        }
        if (!selectedIds.length) { setAlert({ type: 'warning', message: 'Seleccioná al menos un privilegio' }); return; }

        setSaving(true);
        try {
            await save();
            setAlert({ type: 'success', message: isEditMode ? `Rol "${roleName}" actualizado correctamente` : `Rol "${roleName}" creado correctamente` });
            onSaved?.();
        } catch (e) {
            setAlert({ type: 'error', message: getFriendlyApiErrorMessage(e, 'Error al guardar') });
        } finally {
            setSaving(false);
        }
    };

    return (
        <>
            <GeneralModal
                open={open}
                onClose={onClose}
                maxWidth="md"
                icon={AdminPanelSettingsIcon}
                title={isEditMode ? 'Editar rol' : 'Nuevo rol'}
                subtitle={isEditMode ? 'Modificá nombre, descripción y privilegios' : 'Definí los permisos para este rol'}
                loading={saving}
                contentSx={{
                    '&::-webkit-scrollbar': { width: '5px' },
                    '&::-webkit-scrollbar-track': { background: 'transparent' },
                    '&::-webkit-scrollbar-thumb': { background: `color-mix(in srgb, ${accentColor} 25%, transparent)`, borderRadius: '4px' },
                    '&::-webkit-scrollbar-thumb:hover': { background: `color-mix(in srgb, ${accentColor} 45%, transparent)` },
                }}
                footerLeft={
                    <Typography sx={{ fontSize: 11.5, color: 'text.disabled', fontWeight: 500 }}>
                        {selectedCount === 0
                            ? 'Sin privilegios asignados'
                            : `${selectedCount} privilegio${selectedCount !== 1 ? 's' : ''} asignado${selectedCount !== 1 ? 's' : ''}`}
                    </Typography>
                }
                secondaryButton={{ label: 'Cancelar', onClick: onClose }}
                primaryButton={{
                    label: saving ? 'Guardando…' : isEditMode ? 'Guardar cambios' : 'Crear rol',
                    onClick: handleSave,
                    disabled: saving,
                    loading: saving,
                }}
            >
                <Box sx={{ px: { xs: 2.5, sm: 3 }, pt: 2.5, pb: 2, flexShrink: 0 }}>
                    <SectionLabel>Información básica</SectionLabel>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                        <ValidatedTextField
                            fieldName="roleName"
                            label="Nombre del rol"
                            name="roleName"
                            value={roleName}
                            onChange={handleRoleNameChange}
                            onBlur={() => handleFieldBlur('roleName', roleName)}
                            fullWidth
                            size="small"
                            disabled={saving}
                            error={touched.roleName && !!errors.roleName}
                            helperText={touched.roleName && errors.roleName}
                            sx={fieldSx}
                        />
                        <ValidatedTextField
                            fieldName="roleDescription"
                            label="Descripción"
                            name="roleDescription"
                            value={description}
                            onChange={handleDescriptionChange}
                            onBlur={() => handleFieldBlur('roleDescription', description)}
                            fullWidth
                            size="small"
                            multiline
                            rows={2}
                            disabled={saving}
                            error={touched.roleDescription && !!errors.roleDescription}
                            helperText={touched.roleDescription && errors.roleDescription}
                            sx={fieldSx}
                        />
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
                        <SectionLabel noMargin>Privilegios por dominio</SectionLabel>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.875 }}>
                            <Chip
                                label={`${selectedCount} seleccionados`}
                                size="small"
                                sx={{
                                    fontSize: 11, height: 20, fontWeight: 700,
                                    bgcolor: selectedCount > 0 ? `color-mix(in srgb, ${accentColor} ${isDark ? 20 : 10}%, transparent)` : 'action.hover',
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
            </GeneralModal>

            <DialogModal open={!!alert} type={alert?.type} message={alert?.message} onClose={() => setAlert(null)} />
        </>
    );
}
