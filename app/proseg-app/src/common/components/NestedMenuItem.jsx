import { useEffect, useRef, useState } from 'react';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';

const CLOSE_DELAY_MS = 150;

export default function NestedMenuItem({ label, icon, disabled, children }) {
    const [itemElement, setItemElement] = useState(null);
    const closeTimer = useRef(null);
    const [open, setOpen] = useState(false);
    const [openedByKeyboard, setOpenedByKeyboard] = useState(false);

    const cancelClose = () => clearTimeout(closeTimer.current);

    useEffect(() => () => clearTimeout(closeTimer.current), []);

    const openSubmenu = (byKeyboard = false) => {
        cancelClose();
        setOpenedByKeyboard(byKeyboard);
        setOpen(true);
    };

    const scheduleClose = () => {
        cancelClose();
        closeTimer.current = setTimeout(() => setOpen(false), CLOSE_DELAY_MS);
    };

    const closeSubmenu = () => {
        cancelClose();
        setOpen(false);
        if (openedByKeyboard) itemElement?.focus();
    };

    const handleItemKeyDown = (event) => {
        if (['ArrowLeft', 'Enter', ' '].includes(event.key)) {
            event.preventDefault();
            event.stopPropagation();
            openSubmenu(true);
        }
    };

    const handleMenuKeyDown = (event) => {
        if (event.key === 'ArrowRight' || event.key === 'Escape') {
            event.preventDefault();
            closeSubmenu();
        }
        event.stopPropagation();
    };

    return (
        <>
            <MenuItem
                ref={setItemElement}
                disabled={disabled}
                aria-haspopup="menu"
                aria-expanded={open}
                onMouseEnter={() => openSubmenu()}
                onMouseLeave={scheduleClose}
                onClick={() => openSubmenu()}
                onKeyDown={handleItemKeyDown}
                sx={open ? { bgcolor: 'action.hover' } : undefined}
            >
                <ListItemIcon>{icon}</ListItemIcon>
                <ChevronLeftIcon sx={{ fontSize: 16, mr: 0.5, color: 'text.secondary' }} />
                <ListItemText>{label}</ListItemText>
            </MenuItem>
            <Menu
                anchorEl={itemElement}
                open={open}
                onClose={closeSubmenu}
                anchorOrigin={{ vertical: 'top', horizontal: 'left' }}
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                hideBackdrop
                disableScrollLock
                disableAutoFocus={!openedByKeyboard}
                disableEnforceFocus
                disableRestoreFocus
                autoFocus={openedByKeyboard}
                onKeyDown={handleMenuKeyDown}
                sx={{ pointerEvents: 'none' }}
                slotProps={{
                    paper: {
                        onMouseEnter: cancelClose,
                        onMouseLeave: scheduleClose,
                        sx: { pointerEvents: 'auto', mt: 0, mr: '4px' },
                    },
                }}
            >
                {children}
            </Menu>
        </>
    );
}
