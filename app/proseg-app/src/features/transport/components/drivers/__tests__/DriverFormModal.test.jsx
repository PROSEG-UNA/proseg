import { cleanup, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import DriverFormModal from '../DriverFormModal.jsx';
import { createDriver, deleteDriver, fetchDriverById, updateDriver } from '../../../services/drivers/driversService.js';

vi.mock('../../../services/drivers/driversService.js', () => ({
    createDriver: vi.fn(),
    deleteDriver: vi.fn(),
    fetchDriverById: vi.fn(),
    updateDriver: vi.fn(),
}));

const ULLOA_SWITCH_LABEL = /Es conductor de giras hacia Ulloa/;

function renderModal(props = {}) {
    render(
        <AppTheme defaultColorScheme="light">
            <LocalizationProvider dateAdapter={AdapterDayjs}>
                <DriverFormModal open onClose={vi.fn()} onSaved={vi.fn()} {...props} />
            </LocalizationProvider>
        </AppTheme>,
    );
}

beforeEach(() => {
    createDriver.mockReset();
    createDriver.mockResolvedValue({ id: 'driver-1' });
    updateDriver.mockReset();
    updateDriver.mockResolvedValue({ id: 'driver-1' });
    deleteDriver.mockReset();
    deleteDriver.mockResolvedValue();
    fetchDriverById.mockReset();
});

afterEach(cleanup);

describe('DriverFormModal con horario ordinario y giras a Ulloa', () => {
    it('elimina un chofer desde el modal de edición después de confirmar', async () => {
        fetchDriverById.mockResolvedValue({ id: 'driver-1', name: 'Daniel Zuniga' });
        const onDeleted = vi.fn();
        const onClose = vi.fn();
        renderModal({ driverId: 'driver-1', canDelete: true, onDeleted, onClose });

        await waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
        await userEvent.click(screen.getByRole('button', { name: 'Eliminar chofer' }));
        expect(screen.getByText(/Seguro que deseas eliminar al chofer "Daniel Zuniga"/)).toBeInTheDocument();
        expect(deleteDriver).not.toHaveBeenCalled();
        await userEvent.click(screen.getByRole('button', { name: 'Eliminar' }));

        await waitFor(() => expect(deleteDriver).toHaveBeenCalledWith('driver-1'));
        expect(onDeleted).toHaveBeenCalledTimes(1);
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('permite eliminar sin habilitar la edición cuando solo tiene permiso de eliminar', async () => {
        fetchDriverById.mockResolvedValue({ id: 'driver-1', name: 'Daniel Zuniga' });
        renderModal({ driverId: 'driver-1', canEdit: false, canDelete: true });

        await waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
        expect(screen.getByRole('textbox', { name: /Nombre/ })).toBeDisabled();
        expect(screen.queryByRole('button', { name: 'Guardar cambios' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Eliminar chofer' })).toBeEnabled();
    });

    it('crea el chofer con el horario ordinario estandar y sin turnos de Ulloa', async () => {
        renderModal();

        await userEvent.type(screen.getByRole('textbox', { name: /Nombre/ }), 'Ricardo Jovane');
        await userEvent.click(screen.getByRole('button', { name: 'Crear' }));

        await waitFor(() => expect(createDriver).toHaveBeenCalledTimes(1));
        expect(createDriver).toHaveBeenCalledWith(expect.objectContaining({
            name: 'Ricardo Jovane',
            organizationalUnit: 'GENERAL_SERVICES',
            ordinaryStart: '07:30',
            ordinaryEnd: '16:30',
            ulloaShifts: [],
        }));
    });

    it('permite crear un chofer de otra unidad', async () => {
        renderModal();

        await userEvent.type(screen.getByRole('textbox', { name: /Nombre/ }), 'Jorge Mena');
        await userEvent.click(screen.getByRole('combobox', { name: 'Unidad a la que pertenece' }));
        await userEvent.click(await screen.findByRole('option', { name: 'Otra Unidad' }));
        await userEvent.click(screen.getByRole('button', { name: 'Crear' }));

        await waitFor(() => expect(createDriver).toHaveBeenCalledWith(expect.objectContaining({
            name: 'Jorge Mena',
            organizationalUnit: 'OTHER_UNIT',
        })));
    });

    it('exige al menos un turno cuando el chofer es de Ulloa', async () => {
        renderModal();

        await userEvent.type(screen.getByRole('textbox', { name: /Nombre/ }), 'Ricardo Jovane');
        await userEvent.click(screen.getByRole('switch', { name: ULLOA_SWITCH_LABEL }));
        await userEvent.click(screen.getByRole('button', { name: 'Crear' }));

        expect(await screen.findByText('Selecciona al menos un turno')).toBeInTheDocument();
        expect(createDriver).not.toHaveBeenCalled();
    });

    it('guarda los turnos elegidos de Ulloa', async () => {
        renderModal();

        await userEvent.type(screen.getByRole('textbox', { name: /Nombre/ }), 'Ricardo Jovane');
        await userEvent.click(screen.getByRole('switch', { name: ULLOA_SWITCH_LABEL }));
        await userEvent.click(screen.getByRole('combobox', { name: 'Turnos de Ulloa que cubre' }));
        await userEvent.click(await screen.findByRole('option', { name: /Tarde, 12:00 a 22:00/ }));
        await userEvent.keyboard('{Escape}');
        await userEvent.click(screen.getByRole('button', { name: 'Crear' }));

        await waitFor(() => expect(createDriver).toHaveBeenCalledTimes(1));
        expect(createDriver).toHaveBeenCalledWith(expect.objectContaining({ ulloaShifts: ['AFTERNOON'] }));
    });

    it('al editar carga el horario y los turnos del chofer y ofrece el horario de la tarde', async () => {
        fetchDriverById.mockResolvedValue({
            id: 'driver-1',
            name: 'Daniel Zuniga',
            status: 'ACTIVE',
            allowedVehicleTypes: ['MICROBUS'],
            ordinaryStart: '07:30:00',
            ordinaryEnd: '16:30:00',
            ulloaShifts: ['AFTERNOON'],
        });
        renderModal({ driverId: 'driver-1' });

        await waitFor(() => expect(screen.getByRole('switch', { name: ULLOA_SWITCH_LABEL })).toBeChecked());
        await userEvent.click(screen.getByRole('button', { name: 'Usar 11:00 a 20:00' }));
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateDriver).toHaveBeenCalledTimes(1));
        expect(updateDriver).toHaveBeenCalledWith('driver-1', expect.objectContaining({
            name: 'Daniel Zuniga',
            ordinaryStart: '11:00',
            ordinaryEnd: '20:00',
            ulloaShifts: ['AFTERNOON'],
        }));
        expect(within(document.body).queryByRole('button', { name: 'Usar 11:00 a 20:00' })).not.toBeInTheDocument();
    });

    it('rechaza un horario ordinario que termina antes de empezar', async () => {
        fetchDriverById.mockResolvedValue({
            id: 'driver-1',
            name: 'Daniel Zuniga',
            status: 'ACTIVE',
            allowedVehicleTypes: ['MICROBUS'],
            ordinaryStart: '17:00:00',
            ordinaryEnd: '16:30:00',
            ulloaShifts: [],
        });
        renderModal({ driverId: 'driver-1' });

        await waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText('Debe terminar después del inicio')).toBeInTheDocument();
        expect(updateDriver).not.toHaveBeenCalled();
    });
});
