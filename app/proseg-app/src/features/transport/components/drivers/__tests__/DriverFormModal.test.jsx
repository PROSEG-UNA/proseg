import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import DriverFormModal from '../DriverFormModal.jsx';
import {
    createDriver,
    deleteDriver,
    fetchDriverById,
    updateDriver,
    updateDriverMonthBusinessDays,
    updateDriverMonthSchedule,
} from '../../../services/drivers/driversService.js';

vi.mock('../../../services/drivers/driversService.js', () => ({
    createDriver: vi.fn(),
    deleteDriver: vi.fn(),
    fetchDriverById: vi.fn(),
    updateDriver: vi.fn(),
    updateDriverMonthBusinessDays: vi.fn(),
    updateDriverMonthSchedule: vi.fn(),
}));

const MAY_PERIOD = { year: 2026, month: 5, label: 'Mayo 26' };
const BUSINESS_DAYS_FIELD = { name: /Días hábiles de Mayo 26/ };
const MONTH_SCHEDULE_FIELD = { name: 'Horario ordinario de Mayo 26' };

const PERIPHERAL_SWITCH_LABEL = /Es conductor de giras Periféricas/;
const SCHEDULE_FIELD_LABEL = 'Horario ordinario';

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
    updateDriverMonthBusinessDays.mockReset();
    updateDriverMonthBusinessDays.mockResolvedValue({ driverId: 'driver-1' });
    updateDriverMonthSchedule.mockReset();
    updateDriverMonthSchedule.mockResolvedValue({ driverId: 'driver-1' });
    fetchDriverById.mockReset();
});

afterEach(cleanup);

describe('DriverFormModal con horario ordinario y giras Periféricas', () => {
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

    it('crea el chofer con el horario ordinario estandar y sin marcarlo como de Periféricas', async () => {
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

    it('desmarca y bloquea las giras Periféricas al seleccionar Otra Unidad', async () => {
        renderModal();

        await userEvent.type(screen.getByRole('textbox', { name: /Nombre/ }), 'Jorge Mena');
        const peripheralSwitch = screen.getByRole('switch', { name: PERIPHERAL_SWITCH_LABEL });
        await userEvent.click(peripheralSwitch);
        expect(peripheralSwitch).toBeChecked();

        await userEvent.click(screen.getByRole('combobox', { name: 'Unidad a la que pertenece' }));
        await userEvent.click(await screen.findByRole('option', { name: 'Otra Unidad' }));
        expect(peripheralSwitch).not.toBeChecked();
        expect(peripheralSwitch).toBeDisabled();

        await userEvent.click(screen.getByRole('button', { name: 'Crear' }));
        await waitFor(() => expect(createDriver).toHaveBeenCalledWith(expect.objectContaining({
            organizationalUnit: 'OTHER_UNIT',
            ulloaShifts: [],
        })));
    });

    it('no pide turnos al marcar al chofer como conductor de giras Periféricas', async () => {
        renderModal();

        await userEvent.type(screen.getByRole('textbox', { name: /Nombre/ }), 'Ricardo Jovane');
        await userEvent.click(screen.getByRole('switch', { name: PERIPHERAL_SWITCH_LABEL }));
        expect(screen.queryByRole('combobox', { name: /Turnos/ })).not.toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Crear' }));

        await waitFor(() => expect(createDriver).toHaveBeenCalledTimes(1));
        expect(createDriver).toHaveBeenCalledWith(expect.objectContaining({
            ulloaShifts: ['SHORT_MORNING', 'MORNING', 'AFTERNOON'],
        }));
    });

    it('solo ofrece los dos horarios ordinarios válidos y guarda el elegido', async () => {
        renderModal();

        await userEvent.type(screen.getByRole('textbox', { name: /Nombre/ }), 'Ricardo Jovane');
        await userEvent.click(screen.getByRole('combobox', { name: SCHEDULE_FIELD_LABEL }));
        const options = await screen.findAllByRole('option');
        expect(options.map((option) => option.textContent)).toEqual(['07:30 a 16:30', '12:00 a 20:00']);
        await userEvent.click(screen.getByRole('option', { name: '12:00 a 20:00' }));
        await userEvent.click(screen.getByRole('button', { name: 'Crear' }));

        await waitFor(() => expect(createDriver).toHaveBeenCalledTimes(1));
        expect(createDriver).toHaveBeenCalledWith(expect.objectContaining({
            ordinaryStart: '12:00',
            ordinaryEnd: '20:00',
        }));
    });

    it('al editar carga el horario y la marca de Periféricas del chofer y permite cambiar el horario', async () => {
        fetchDriverById.mockResolvedValue({
            id: 'driver-1',
            name: 'Daniel Zuniga',
            status: 'ACTIVE',
            allowedVehicleTypes: ['MICROBUS'],
            ordinaryStart: '12:00:00',
            ordinaryEnd: '20:00:00',
            ulloaShifts: ['AFTERNOON'],
        });
        renderModal({ driverId: 'driver-1' });

        await waitFor(() => expect(screen.getByRole('switch', { name: PERIPHERAL_SWITCH_LABEL })).toBeChecked());
        expect(screen.getByRole('combobox', { name: SCHEDULE_FIELD_LABEL })).toHaveTextContent('12:00 a 20:00');
        await userEvent.click(screen.getByRole('combobox', { name: SCHEDULE_FIELD_LABEL }));
        await userEvent.click(await screen.findByRole('option', { name: '07:30 a 16:30' }));
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateDriver).toHaveBeenCalledTimes(1));
        expect(updateDriver).toHaveBeenCalledWith('driver-1', expect.objectContaining({
            name: 'Daniel Zuniga',
            ordinaryStart: '07:30',
            ordinaryEnd: '16:30',
            ulloaShifts: ['SHORT_MORNING', 'MORNING', 'AFTERNOON'],
        }));
    });

    it('lleva un horario guardado que empieza desde las 10:00 al horario de 12:00 a 20:00 y el resto a 07:30 a 16:30', async () => {
        fetchDriverById.mockResolvedValue({
            id: 'driver-1',
            name: 'Daniel Zuniga',
            status: 'ACTIVE',
            allowedVehicleTypes: ['MICROBUS'],
            ordinaryStart: '11:00:00',
            ordinaryEnd: '20:00:00',
            ulloaShifts: [],
        });
        renderModal({ driverId: 'driver-1' });

        await waitFor(() => expect(screen.getByRole('combobox', { name: SCHEDULE_FIELD_LABEL })).toHaveTextContent('12:00 a 20:00'));
        cleanup();

        fetchDriverById.mockResolvedValue({
            id: 'driver-1',
            name: 'Daniel Zuniga',
            status: 'ACTIVE',
            allowedVehicleTypes: ['MICROBUS'],
            ordinaryStart: '06:30:00',
            ordinaryEnd: '17:30:00',
            ulloaShifts: [],
        });
        renderModal({ driverId: 'driver-1' });

        await waitFor(() => expect(screen.getByRole('combobox', { name: SCHEDULE_FIELD_LABEL })).toHaveTextContent('07:30 a 16:30'));
    });
});

describe('DriverFormModal con los días hábiles del mes', () => {
    const loadDriver = () => fetchDriverById.mockResolvedValue({ id: 'driver-1', name: 'Daniel Zuniga', status: 'ACTIVE' });

    it('muestra los días hábiles del chofer en el mes y los guarda aparte del chofer', async () => {
        loadDriver();
        const onBusinessDaysSaved = vi.fn();
        const onClose = vi.fn();
        renderModal({
            driverId: 'driver-1',
            period: MAY_PERIOD,
            monthBusinessDays: 28,
            defaultBusinessDays: 28,
            onBusinessDaysSaved,
            onClose,
        });

        await waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
        const field = screen.getByRole('textbox', BUSINESS_DAYS_FIELD);
        expect(field).toHaveValue('28');
        expect(screen.getByText('Valor del mes. Puedes cambiarlo solo para este chofer.')).toBeInTheDocument();
        await userEvent.clear(field);
        await userEvent.type(field, '21');
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateDriverMonthBusinessDays).toHaveBeenCalledWith('driver-1', 2026, 5, 21));
        expect(updateDriver).toHaveBeenCalledTimes(1);
        expect(onBusinessDaysSaved).toHaveBeenCalledTimes(1);
        expect(onBusinessDaysSaved).toHaveBeenCalledWith({
            driverName: 'Daniel Zuniga',
            previousBusinessDays: 28,
            businessDays: 21,
            reset: false,
        });
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('no llama al cambio de días hábiles cuando el valor no cambió', async () => {
        loadDriver();
        const onBusinessDaysSaved = vi.fn();
        renderModal({ driverId: 'driver-1', period: MAY_PERIOD, monthBusinessDays: 21, defaultBusinessDays: 28, businessDaysCustomized: true, onBusinessDaysSaved });

        await waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
        expect(screen.getByRole('textbox', BUSINESS_DAYS_FIELD)).toHaveValue('21');
        expect(screen.getByText('Personalizado para este chofer. El valor del mes es 28.')).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateDriver).toHaveBeenCalledTimes(1));
        expect(updateDriverMonthBusinessDays).not.toHaveBeenCalled();
        expect(onBusinessDaysSaved).not.toHaveBeenCalled();
    });

    it('rechaza más días que los del mes y no guarda nada', async () => {
        loadDriver();
        renderModal({ driverId: 'driver-1', period: MAY_PERIOD, monthBusinessDays: 28, defaultBusinessDays: 28 });

        await waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
        const field = screen.getByRole('textbox', BUSINESS_DAYS_FIELD);
        await userEvent.clear(field);
        await userEvent.type(field, '32');

        expect(await screen.findByText('Debe estar entre 0 y 31')).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
        expect(updateDriver).not.toHaveBeenCalled();
        expect(updateDriverMonthBusinessDays).not.toHaveBeenCalled();
    });

    it('exige un valor y acepta cero', async () => {
        loadDriver();
        renderModal({ driverId: 'driver-1', period: MAY_PERIOD, monthBusinessDays: 28, defaultBusinessDays: 28 });

        await waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
        const field = screen.getByRole('textbox', BUSINESS_DAYS_FIELD);
        await userEvent.clear(field);
        expect(await screen.findByText('Este campo es requerido')).toBeInTheDocument();
        await userEvent.type(field, '0');
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateDriverMonthBusinessDays).toHaveBeenCalledWith('driver-1', 2026, 5, 0));
    });

    it('restablece el valor del mes enviando null', async () => {
        loadDriver();
        renderModal({ driverId: 'driver-1', period: MAY_PERIOD, monthBusinessDays: 21, defaultBusinessDays: 28, businessDaysCustomized: true });

        await waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
        await userEvent.click(screen.getByRole('button', { name: 'Usar el valor del mes (28)' }));
        expect(screen.getByRole('textbox', BUSINESS_DAYS_FIELD)).toHaveValue('28');
        expect(screen.getByText('Se usará el valor del mes (28) al guardar.')).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateDriverMonthBusinessDays).toHaveBeenCalledWith('driver-1', 2026, 5, null));
    });

    it('no ofrece restablecer cuando el chofer usa el valor del mes', async () => {
        loadDriver();
        renderModal({ driverId: 'driver-1', period: MAY_PERIOD, monthBusinessDays: 28, defaultBusinessDays: 28 });

        await waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
        expect(screen.queryByRole('button', { name: /Usar el valor del mes/ })).not.toBeInTheDocument();
    });

    it('no muestra el campo sin mes seleccionado, sin días del mes ni al crear un chofer', async () => {
        loadDriver();
        renderModal({ driverId: 'driver-1', period: null, monthBusinessDays: 28 });
        await waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
        expect(screen.queryByRole('textbox', BUSINESS_DAYS_FIELD)).not.toBeInTheDocument();
        cleanup();

        loadDriver();
        renderModal({ driverId: 'driver-1', period: MAY_PERIOD, monthBusinessDays: null });
        await waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
        expect(screen.queryByRole('textbox', BUSINESS_DAYS_FIELD)).not.toBeInTheDocument();
        cleanup();

        renderModal({ period: MAY_PERIOD, monthBusinessDays: 28 });
        expect(screen.queryByRole('textbox', BUSINESS_DAYS_FIELD)).not.toBeInTheDocument();
    });

    it('muestra el campo deshabilitado sin permiso de edición', async () => {
        loadDriver();
        renderModal({ driverId: 'driver-1', canEdit: false, period: MAY_PERIOD, monthBusinessDays: 28, defaultBusinessDays: 28 });

        await waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
        expect(screen.getByRole('textbox', BUSINESS_DAYS_FIELD)).toBeDisabled();
    });

    it('avisa que el chofer se guardó cuando falla el cambio de días hábiles y no cierra', async () => {
        loadDriver();
        updateDriverMonthBusinessDays.mockRejectedValue({ response: { status: 409, data: { message: 'No se puede editar una depuración terminada. Devuélvela a borrador' } } });
        const onSaved = vi.fn();
        const onClose = vi.fn();
        const onBusinessDaysSaved = vi.fn();
        renderModal({ driverId: 'driver-1', period: MAY_PERIOD, monthBusinessDays: 28, defaultBusinessDays: 28, onSaved, onClose, onBusinessDaysSaved });

        await waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
        const field = screen.getByRole('textbox', BUSINESS_DAYS_FIELD);
        await userEvent.clear(field);
        await userEvent.type(field, '21');
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText(/El chofer se guardó, pero no se pudieron cambiar los días hábiles/)).toBeInTheDocument();
        expect(screen.getByText(/No se puede editar una depuración terminada/)).toBeInTheDocument();
        expect(onSaved).toHaveBeenCalledTimes(1);
        expect(onBusinessDaysSaved).not.toHaveBeenCalled();
        expect(onClose).not.toHaveBeenCalled();
    });
});

describe('DriverFormModal con el calendario de horarios del mes', () => {
    const loadDriver = (start = '07:30:00', end = '16:30:00') => fetchDriverById.mockResolvedValue({
        id: 'driver-1',
        name: 'Daniel Zuniga',
        status: 'ACTIVE',
        ordinaryStart: start,
        ordinaryEnd: end,
    });
    const monthProps = { driverId: 'driver-1', period: MAY_PERIOD, monthBusinessDays: 28, defaultBusinessDays: 28, monthScheduleDays: [] };
    const waitForDriver = () => waitFor(() => expect(screen.getByRole('textbox', { name: /Nombre/ })).toHaveValue('Daniel Zuniga'));
    const afternoonDay = (date) => ({ date, ordinaryStart: '12:00:00', ordinaryEnd: '20:00:00' });

    it('muestra todos los días con el horario del chofer y deja los fines de semana aparte', async () => {
        loadDriver();
        renderModal(monthProps);

        await waitForDriver();
        expect(screen.getByText('Horario ordinario de Mayo 26')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Día 4, mañana' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Día 29, mañana' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /tarde/ })).not.toBeInTheDocument();
        expect(screen.getByLabelText('Día 2, fin de semana')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Día 2,/ })).not.toBeInTheDocument();
        expect(screen.getAllByRole('button', { name: /^Día \d+, mañana$/ })).toHaveLength(21);
    });

    it('selecciona el otro horario por defecto y guarda solo los días distintos del chofer', async () => {
        loadDriver();
        const onScheduleSaved = vi.fn();
        const onClose = vi.fn();
        renderModal({ ...monthProps, onScheduleSaved, onClose });

        await waitForDriver();
        await userEvent.click(screen.getByRole('button', { name: 'Día 18, mañana' }));
        await userEvent.click(screen.getByRole('button', { name: 'Día 19, mañana' }));
        expect(screen.getByRole('button', { name: 'Día 18, tarde' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Día 20, mañana' })).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateDriverMonthSchedule).toHaveBeenCalledWith('driver-1', 2026, 5, [
            { date: '2026-05-18', ordinaryStart: '12:00', ordinaryEnd: '20:00' },
            { date: '2026-05-19', ordinaryStart: '12:00', ordinaryEnd: '20:00' },
        ]));
        expect(updateDriver).toHaveBeenCalledTimes(1);
        expect(onScheduleSaved).toHaveBeenCalledWith({ driverName: 'Daniel Zuniga', changedDays: 2 });
        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('cambia el horario a seleccionar para poner días de mañana sobre días de tarde', async () => {
        loadDriver();
        renderModal({ ...monthProps, monthScheduleDays: [afternoonDay('2026-05-18'), afternoonDay('2026-05-19')] });

        await waitForDriver();
        expect(screen.getByRole('button', { name: 'Día 18, tarde' })).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Mañana 07:30 a 16:30' }));
        await userEvent.click(screen.getByRole('button', { name: 'Día 18, tarde' }));
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(screen.getByRole('button', { name: 'Día 18, mañana' })).toBeInTheDocument();
        await waitFor(() => expect(updateDriverMonthSchedule).toHaveBeenCalledWith('driver-1', 2026, 5, [
            { date: '2026-05-19', ordinaryStart: '12:00', ordinaryEnd: '20:00' },
        ]));
    });

    it('selecciona varios días arrastrando sobre el calendario', async () => {
        loadDriver();
        renderModal(monthProps);

        await waitForDriver();
        fireEvent.pointerDown(screen.getByRole('button', { name: 'Día 11, mañana' }));
        fireEvent.pointerOver(screen.getByRole('button', { name: 'Día 12, mañana' }));
        fireEvent.pointerOver(screen.getByRole('button', { name: 'Día 13, mañana' }));
        fireEvent.pointerUp(document);
        fireEvent.pointerOver(screen.getByRole('button', { name: 'Día 14, mañana' }));

        expect(screen.getByRole('button', { name: 'Día 11, tarde' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Día 12, tarde' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Día 13, tarde' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Día 14, mañana' })).toBeInTheDocument();
    });

    it('muestra los días con horario distinto que ya tiene el mes y no guarda si no se tocan', async () => {
        loadDriver();
        renderModal({ ...monthProps, monthScheduleDays: [afternoonDay('2026-05-18')] });

        await waitForDriver();
        expect(screen.getByRole('button', { name: 'Día 18, tarde' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Día 19, mañana' })).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateDriver).toHaveBeenCalledTimes(1));
        expect(updateDriverMonthSchedule).not.toHaveBeenCalled();
    });

    it('guarda una lista vacía cuando se vuelven a seleccionar todos los días con el horario del chofer', async () => {
        loadDriver();
        const onScheduleSaved = vi.fn();
        renderModal({ ...monthProps, monthScheduleDays: [afternoonDay('2026-05-18')], onScheduleSaved });

        await waitForDriver();
        await userEvent.click(screen.getByRole('button', { name: 'Mañana 07:30 a 16:30' }));
        await userEvent.click(screen.getByRole('button', { name: 'Día 18, tarde' }));
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateDriverMonthSchedule).toHaveBeenCalledWith('driver-1', 2026, 5, []));
        expect(onScheduleSaved).toHaveBeenCalledWith({ driverName: 'Daniel Zuniga', changedDays: 0 });
    });

    it('usa el horario de tarde del chofer como color por defecto', async () => {
        loadDriver('12:00:00', '20:00:00');
        renderModal(monthProps);

        await waitForDriver();
        await waitFor(() => expect(screen.getByRole('button', { name: 'Día 4, tarde' })).toBeInTheDocument());
        await userEvent.click(screen.getByRole('button', { name: 'Día 5, tarde' }));
        expect(screen.getByRole('button', { name: 'Día 5, mañana' })).toBeInTheDocument();
    });

    it('recolorea los días sin cambios cuando se cambia el horario del chofer y no llama al mes', async () => {
        loadDriver();
        renderModal(monthProps);

        await waitForDriver();
        await userEvent.click(screen.getByRole('combobox', { name: SCHEDULE_FIELD_LABEL }));
        await userEvent.click(await screen.findByRole('option', { name: '12:00 a 20:00' }));

        expect(screen.getByRole('button', { name: 'Día 4, tarde' })).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));
        await waitFor(() => expect(updateDriver).toHaveBeenCalledTimes(1));
        expect(updateDriverMonthSchedule).not.toHaveBeenCalled();
    });

    it('muestra el calendario aunque el mes no tenga borrador, sin pedir los días hábiles', async () => {
        loadDriver();
        const onScheduleSaved = vi.fn();
        renderModal({ ...monthProps, monthBusinessDays: null, defaultBusinessDays: null, onScheduleSaved });

        await waitForDriver();
        expect(screen.getByText('Horario ordinario de Mayo 26')).toBeInTheDocument();
        expect(screen.queryByRole('textbox', { name: /Días hábiles/ })).not.toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Día 18, mañana' }));
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateDriverMonthSchedule).toHaveBeenCalledWith('driver-1', 2026, 5, [
            { date: '2026-05-18', ordinaryStart: '12:00', ordinaryEnd: '20:00' },
        ]));
        expect(updateDriverMonthBusinessDays).not.toHaveBeenCalled();
        expect(onScheduleSaved).toHaveBeenCalledWith({ driverName: 'Daniel Zuniga', changedDays: 1 });
    });

    it('no muestra el calendario sin mes, sin datos del mes ni al crear un chofer', async () => {
        loadDriver();
        renderModal({ ...monthProps, period: null });
        await waitForDriver();
        expect(screen.queryByText('Horario ordinario de Mayo 26')).not.toBeInTheDocument();
        expect(screen.getByRole('combobox', { name: SCHEDULE_FIELD_LABEL })).toHaveTextContent('07:30 a 16:30');
        cleanup();

        loadDriver();
        renderModal({ ...monthProps, monthScheduleDays: null });
        await waitForDriver();
        expect(screen.queryByText('Horario ordinario de Mayo 26')).not.toBeInTheDocument();
        cleanup();

        renderModal({ period: MAY_PERIOD, monthBusinessDays: 28, monthScheduleDays: [] });
        expect(screen.queryByText('Horario ordinario de Mayo 26')).not.toBeInTheDocument();
    });

    it('deshabilita los días sin permiso de edición', async () => {
        loadDriver();
        renderModal({ ...monthProps, canEdit: false });

        await waitForDriver();
        expect(screen.getByRole('button', { name: 'Día 4, mañana' })).toBeDisabled();
    });

    it('avisa que el chofer se guardó cuando falla el cambio de horario y no cierra', async () => {
        loadDriver();
        updateDriverMonthSchedule.mockRejectedValue({ response: { status: 409, data: { message: 'No se puede editar una depuración terminada. Devuélvela a borrador' } } });
        const onSaved = vi.fn();
        const onClose = vi.fn();
        const onScheduleSaved = vi.fn();
        renderModal({ ...monthProps, onSaved, onClose, onScheduleSaved });

        await waitForDriver();
        await userEvent.click(screen.getByRole('button', { name: 'Día 18, mañana' }));
        await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        expect(await screen.findByText(/El chofer se guardó, pero no se pudo cambiar el horario del mes/)).toBeInTheDocument();
        expect(onSaved).toHaveBeenCalledTimes(1);
        expect(onScheduleSaved).not.toHaveBeenCalled();
        expect(onClose).not.toHaveBeenCalled();
    });
});
