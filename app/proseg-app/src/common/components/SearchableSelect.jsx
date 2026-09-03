import { useState, useEffect, useRef } from 'react';
import {
    TextField, MenuItem, ListSubheader, IconButton,
    Typography, Divider, Button, Box, useTheme,
} from '@mui/material';
import AddCircleOutlinedIcon from '@mui/icons-material/AddCircleOutlined';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import NavigateBeforeIcon from '@mui/icons-material/NavigateBefore';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import CheckBoxOutlineBlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import CheckBoxIcon from '@mui/icons-material/CheckBox';

export default function SearchableSelect({
    value,
    onChange,
    onBlur,
    items = [],
    getItemLabel,
    getItemValue,
    onCreate,
    createLabel,
    onEdit,
    editLabel,
    pageSize = 5,
    label,
    required,
    disabled,
    error,
    helperText,
    fullWidth,
    size,
    sx,
    clearable = false,
    externalSearch,
    onSearchChange,
    hideSearch = false,
    multiple = false,
}) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const [internalSearch, setInternalSearch] = useState('');
    const [page, setPage] = useState(0);
    const [open, setOpen] = useState(false);
    const search = typeof externalSearch === 'string' ? externalSearch : internalSearch;
    const searchInputRef = useRef(null);

    const normalize = str => str.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
    const filtered = items.filter(item =>
        normalize(getItemLabel(item)).includes(normalize(search))
    );
    const totalPages = Math.ceil(filtered.length / pageSize) || 1;
    const pageItems = filtered.slice(page * pageSize, (page + 1) * pageSize);
    const visibleCount = filtered.length === 0 ? 1 : pageItems.length;
    const placeholderCount = Math.max(0, pageSize - visibleCount);

    const selectedItem = value ? items.find(i => getItemValue(i) === value) : null;
    const selectedOnPage = selectedItem ? pageItems.some(i => getItemValue(i) === value) : true;

    useEffect(() => { setPage(0); }, [search]);

    return (
        <TextField
            select
            label={label}
            value={multiple ? (Array.isArray(value) ? value : []) : (value ?? '')}
            onChange={(e) => {
                const v = e.target.value;
                if (multiple) {
                    if (Array.isArray(v) && v.includes('__CREATE__')) { onCreate?.(search); return; }
                    if (Array.isArray(v) && v.includes('__EDIT__')) { onEdit?.(); return; }
                    onChange((Array.isArray(v) ? v : []).filter(x => x !== '__CREATE__' && x !== '__EDIT__' && x !== ''));
                    return;
                }
                if (v === '__CREATE__') { onCreate?.(search); return; }
                if (v === '__EDIT__') { onEdit?.(); return; }
                onChange(v);
            }}
            onBlur={onBlur}
            required={required}
            disabled={disabled}
            error={error}
            helperText={helperText}
            fullWidth={fullWidth}
            size={size}
            sx={sx}
            slotProps={{
                select: {
                    multiple,
                    renderValue: (val) => {
                        if (multiple) {
                            const arr = Array.isArray(val) ? val : [];
                            if (arr.length === 0) return '';
                            return arr
                                .map(v => {
                                    const item = items.find(i => getItemValue(i) === v);
                                    return item ? getItemLabel(item) : v;
                                })
                                .join(', ');
                        }
                        if (!val) return '';
                        const item = items.find(i => getItemValue(i) === val);
                        return item ? getItemLabel(item) : '';
                    },
                    open,
                    onOpen: () => { setOpen(true); setTimeout(() => searchInputRef.current?.focus(), 50); },
                    onClose: () => { setOpen(false); setInternalSearch(''); setPage(0); },
                    MenuProps: {
                        PaperProps: { sx: { maxHeight: 'none' } },
                    },
                },
            }}
        >
            {!hideSearch && (
                <ListSubheader sx={{ px: 1.5, pt: 1, pb: 1.5, bgcolor: 'background.paper', lineHeight: 'normal' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <TextField
                        inputRef={searchInputRef}
                        size="small"
                        fullWidth
                        placeholder="Buscar..."
                        value={search}
                        onChange={(e) => {
                            const v = e.target.value;
                            if (typeof onSearchChange === 'function') onSearchChange(v);
                            else setInternalSearch(v);
                        }}
                        onKeyDown={(e) => e.stopPropagation()}
                        sx={{
                            '& .MuiOutlinedInput-root': {
                                borderRadius: '8px',
                                '& fieldset': { borderColor: 'divider'},
                                '&.Mui-focused fieldset': { borderColor: accentColor },
                            },
                            '& .MuiInputBase-input': { fontSize: 13.5, py: '6px' },
                        }}
                    />
                </Box>
            </ListSubheader>
            )}

            {!multiple && clearable && (
                <MenuItem value="" sx={{ fontSize: 13.5, color: 'text.secondary', fontStyle: 'italic' }}>
                    — Ninguno —
                </MenuItem>
            )}

            {pageItems.map(item => {
                const checked = multiple && (Array.isArray(value) ? value : []).includes(getItemValue(item));
                return (
                    <MenuItem
                        key={getItemValue(item)}
                        value={getItemValue(item)}
                        selected={checked}
                        sx={{ fontSize: 13.5, gap: 1 }}
                    >
                        {multiple && (checked
                            ? <CheckBoxIcon sx={{ fontSize: 18, color: accentColor }} />
                            : <CheckBoxOutlineBlankIcon sx={{ fontSize: 18, color: 'text.disabled' }} />)}
                        {getItemLabel(item)}
                    </MenuItem>
                );
            })}

            {filtered.length === 0 && (
                <MenuItem disabled sx={{ fontSize: 13, color: 'text.disabled', justifyContent: 'center' }}>
                    Sin resultados
                </MenuItem>
            )}

            {!multiple && selectedItem && !selectedOnPage && (
                <MenuItem key={`__sel__${getItemValue(selectedItem)}`} value={getItemValue(selectedItem)} sx={{ display: 'none' }}>
                    {getItemLabel(selectedItem)}
                </MenuItem>
            )}

            {multiple && (Array.isArray(value) ? value : [])
                .filter(v => !pageItems.some(i => getItemValue(i) === v))
                .map(v => {
                    const item = items.find(i => getItemValue(i) === v);
                    return (
                        <MenuItem key={`__sel__${v}`} value={v} sx={{ display: 'none' }}>
                            {item ? getItemLabel(item) : v}
                        </MenuItem>
                    );
                })}

            {Array.from({ length: placeholderCount }, (_, i) => (
                <MenuItem key={`__ph_${i}`} sx={{ visibility: 'hidden', pointerEvents: 'none', fontSize: 13.5 }}>
                    &nbsp;
                </MenuItem>
            ))}

            <ListSubheader
                sx={{
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    gap: 1, py: 0.5, bgcolor: 'background.paper', lineHeight: 'normal',
                }}
            >
                <IconButton
                    size="small"
                    disabled={page === 0}
                    onMouseDown={(e) => e.stopPropagation()}
                    onClick={(e) => { e.stopPropagation(); setPage(p => p - 1); }}
                >
                    <NavigateBeforeIcon fontSize="small" />
                </IconButton>
                <Typography sx={{ fontSize: 12, color: 'text.secondary', minWidth: 50, textAlign: 'center' }}>
                    {page + 1} / {totalPages}
                </Typography>
                <IconButton
                    size="small"
                    disabled={page === totalPages - 1}
                    onMouseDown={(e) => e.stopPropagation()}
                    onClick={(e) => { e.stopPropagation(); setPage(p => p + 1); }}
                >
                    <NavigateNextIcon fontSize="small" />
                </IconButton>
            </ListSubheader>

            {(onCreate || (onEdit && selectedItem)) && <Divider />}
            {onCreate && (
                <MenuItem
                    value="__CREATE__"
                    sx={{ color: accentColor, fontWeight: 600, fontSize: 13.5, gap: 1, justifyContent: 'center' }}
                >
                    <AddCircleOutlinedIcon sx={{ fontSize: 16 }} />
                    {typeof createLabel === 'function' ? createLabel(search) : createLabel}
                </MenuItem>
            )}
            {onEdit && selectedItem && (
                <MenuItem
                    value="__EDIT__"
                    sx={{ color: accentColor, fontWeight: 600, fontSize: 13.5, gap: 1, justifyContent: 'center' }}
                >
                    <EditOutlinedIcon sx={{ fontSize: 16 }} />
                    {typeof editLabel === 'function' ? editLabel(selectedItem) : editLabel}
                </MenuItem>
            )}

            {multiple && <Divider />}
            {multiple && (
                <ListSubheader sx={{ display: 'flex', justifyContent: 'center', py: 0.75, bgcolor: 'background.paper', lineHeight: 'normal' }}>
                    <Button
                        size="small"
                        variant="contained"
                        fullWidth
                        onClick={() => {
                            setOpen(false);
                            if (typeof onSearchChange === 'function') onSearchChange(''); else setInternalSearch('');
                            setPage(0);
                        }}
                        sx={{ textTransform: 'none', fontSize: 13, fontWeight: 600 }}
                    >
                        Aceptar
                    </Button>
                </ListSubheader>
            )}
        </TextField>
    );
}
