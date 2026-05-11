import { useMemo, useState } from 'react';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import SettingsIcon from '@mui/icons-material/Settings';

export default function RowActionsMenu({ actions = [], tooltip = 'Ver acción' }) {
    const [anchorEl, setAnchorEl] = useState(null);
    const open = Boolean(anchorEl);

    const visibleActions = useMemo(
        () => (actions ?? []).filter((action) => !action?.hidden),
        [actions]
    );

    const handleOpen = (event) => {
        event.stopPropagation();
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    if (visibleActions.length === 0) {
        return null;
    }

    return (
        <>
            <Tooltip title={tooltip}>
                <IconButton size="small" onClick={handleOpen}>
                    <SettingsIcon fontSize="small" sx={{ color: 'primary.icon' }} />
                </IconButton>
            </Tooltip>

            <Menu
                anchorEl={anchorEl}
                open={open}
                onClose={handleClose}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
            >
                {visibleActions.map((action) => {
                    const itemKey = action.key ?? action.label;
                    const itemColorSx = action.color === 'error' ? { color: 'error.main' } : undefined;

                    return (
                        <MenuItem
                            key={itemKey}
                            onClick={() => {
                                handleClose();
                                action.onClick?.();
                            }}
                            disabled={action.disabled}
                            sx={itemColorSx}
                        >
                            {action.icon ? (
                                <ListItemIcon sx={itemColorSx}>
                                    {action.icon}
                                </ListItemIcon>
                            ) : null}
                            <ListItemText>{action.label}</ListItemText>
                        </MenuItem>
                    );
                })}
            </Menu>
        </>
    );
}