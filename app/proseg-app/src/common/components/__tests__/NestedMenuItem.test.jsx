import { useState } from 'react';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import Button from '@mui/material/Button';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import AppTheme from '../../theme/AppTheme';
import NestedMenuItem from '../NestedMenuItem.jsx';

function Harness({ onSelect }) {
    const [anchor, setAnchor] = useState(null);
    return (
        <AppTheme>
            <Button onClick={(event) => setAnchor(event.currentTarget)}>abrir</Button>
            <Menu anchorEl={anchor} open={Boolean(anchor)} onClose={() => setAnchor(null)}>
                <MenuItem>Crear gira</MenuItem>
                <NestedMenuItem label="Autoasignar">
                    <MenuItem onClick={() => { setAnchor(null); onSelect('choferes'); }}>Autoasignar choferes</MenuItem>
                    <MenuItem onClick={() => { setAnchor(null); onSelect('vehiculos'); }}>Autoasignar vehículos</MenuItem>
                </NestedMenuItem>
            </Menu>
        </AppTheme>
    );
}

describe('NestedMenuItem', () => {
    afterEach(cleanup);

    it('muestra las opciones al pasar el mouse y dispara la acción elegida', async () => {
        const onSelect = vi.fn();
        const user = userEvent.setup();
        render(<Harness onSelect={onSelect} />);

        await user.click(screen.getByRole('button', { name: 'abrir' }));
        expect(screen.queryByText('Autoasignar choferes')).toBeNull();

        await user.hover(screen.getByText('Autoasignar'));
        const option = await screen.findByText('Autoasignar vehículos');
        expect(screen.getByText('Autoasignar choferes')).toBeTruthy();

        await user.click(option);
        expect(onSelect).toHaveBeenCalledWith('vehiculos');
    });

    it('abre el submenú con clic y lo cierra al salir con el mouse', async () => {
        const user = userEvent.setup();
        render(<Harness onSelect={vi.fn()} />);

        await user.click(screen.getByRole('button', { name: 'abrir' }));
        await user.click(screen.getByText('Autoasignar'));
        expect(await screen.findByText('Autoasignar choferes')).toBeTruthy();

        await user.hover(screen.getByText('Crear gira'));
        await waitFor(() => expect(screen.queryByText('Autoasignar choferes')).toBeNull());
    });
});
