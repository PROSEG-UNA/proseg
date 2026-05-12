import { useState, useEffect, useRef } from 'react';
import {
    TextField, MenuItem, ListSubheader, IconButton,
    Typography, Divider, useTheme,
} from '@mui/material';
import AddCircleOutlinedIcon from '@mui/icons-material/AddCircleOutlined';
import NavigateBeforeIcon from '@mui/icons-material/NavigateBefore';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';

export default function SearchableSelect({
    value,
    onChange,
    onBlur,
    items = [],
    getItemLabel,
    getItemValue,
    onCreate,
    createLabel,
    pageSize = 5,
    label,
    required,
    disabled,
    error,
    helperText,
    fullWidth,
    size,
    sx,
}) {
    const theme = useTheme();
    const accentColor = theme.vars.palette.tones.rose.fg;

    const [search, setSearch] = useState('');
    const [page, setPage] = useState(0);
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
            value={value ?? ''}
            onChange={(e) => {
                const v = e.target.value;
                if (v === '__CREATE__') { onCreate?.(); return; }
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
            SelectProps={{
                renderValue: (val) => {
                    if (!val) return '';
                    const item = items.find(i => getItemValue(i) === val);
                    return item ? getItemLabel(item) : '';
                },
                onOpen: () => setTimeout(() => searchInputRef.current?.focus(), 50),
                onClose: () => { setSearch(''); setPage(0); },
                MenuProps: {
                    PaperProps: { sx: { maxHeight: 'none' } },
                },
            }}
        >
            <ListSubheader sx={{ px: 1.5, pt: 1, pb: 1.5, bgcolor: 'background.paper', lineHeight: 'normal' }}>
                <TextField
                    inputRef={searchInputRef}
                    size="small"
                    fullWidth
                    placeholder="Buscar..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
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
            </ListSubheader>

            {pageItems.map(item => (
                <MenuItem key={getItemValue(item)} value={getItemValue(item)} sx={{ fontSize: 13.5 }}>
                    {getItemLabel(item)}
                </MenuItem>
            ))}

            {filtered.length === 0 && (
                <MenuItem disabled sx={{ fontSize: 13, color: 'text.disabled', justifyContent: 'center' }}>
                    Sin resultados
                </MenuItem>
            )}

            {selectedItem && !selectedOnPage && (
                <MenuItem key={`__sel__${getItemValue(selectedItem)}`} value={getItemValue(selectedItem)} sx={{ display: 'none' }}>
                    {getItemLabel(selectedItem)}
                </MenuItem>
            )}

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

            {onCreate && <Divider />}
            {onCreate && (
                <MenuItem
                    value="__CREATE__"
                    sx={{ color: accentColor, fontWeight: 600, fontSize: 13.5, gap: 1 }}
                >
                    <AddCircleOutlinedIcon sx={{ fontSize: 16 }} />
                    {createLabel}
                </MenuItem>
            )}
        </TextField>
    );
}
