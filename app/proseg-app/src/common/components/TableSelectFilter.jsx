import { useState } from 'react';
import { ButtonBase, Menu, MenuItem } from '@mui/material';
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';

export default function TableSelectFilter({ column, label, options, clearLabel = 'Sin filtro' }) {
    const [anchorEl, setAnchorEl] = useState(null);
    const value = column.getFilterValue();
    const selectedLabel = options.find((option) => option.value === value)?.label ?? `Filtrar por ${label}`;

    const selectValue = (event, nextValue) => {
        event.stopPropagation();
        column.setFilterValue(nextValue || undefined);
        setAnchorEl(null);
    };

    return (
        <>
            <ButtonBase
                aria-label={`Filtrar por ${label}`}
                aria-haspopup="menu"
                aria-expanded={Boolean(anchorEl)}
                onClick={(event) => {
                    event.stopPropagation();
                    setAnchorEl(event.currentTarget);
                }}
                onKeyDown={(event) => event.stopPropagation()}
                sx={{
                    width: 'calc(100% + 4px)',
                    minHeight: 29,
                    ml: '-2px',
                    px: '2px',
                    justifyContent: 'space-between',
                    borderBottom: '1px solid',
                    borderColor: value ? 'primary.main' : 'text.disabled',
                    color: value ? 'text.primary' : 'text.secondary',
                    fontSize: '0.875rem',
                    fontWeight: 400,
                    whiteSpace: 'nowrap',
                    '&:hover': { borderBottomWidth: 2, borderColor: 'text.primary' },
                    '&.Mui-focusVisible': { borderBottomWidth: 2, borderColor: 'primary.main' },
                }}
            >
                <span>{selectedLabel}</span>
                <ArrowDropDownIcon fontSize="small" />
            </ButtonBase>
            <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
                <MenuItem selected={!value} onClick={(event) => selectValue(event, undefined)}>{clearLabel}</MenuItem>
                {options.map((option) => (
                    <MenuItem
                        key={option.value}
                        selected={value === option.value}
                        onClick={(event) => selectValue(event, option.value)}
                    >
                        {option.label}
                    </MenuItem>
                ))}
            </Menu>
        </>
    );
}
