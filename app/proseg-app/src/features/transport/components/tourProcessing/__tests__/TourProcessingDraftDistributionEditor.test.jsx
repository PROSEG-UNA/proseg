import { useState } from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourProcessingDraftDistributionEditor from '../TourProcessingDraftDistributionEditor.jsx';

vi.mock('../TourProcessingCatalogField.jsx', () => ({
    default: ({ kind, value, onChange }) => (
        <input aria-label={kind} value={value} onChange={(event) => onChange(event.target.value)} />
    ),
}));

const VEHICLES = [{ label: '301-1', capacity: 30 }, { label: '301-2', capacity: 30 }];

const trip = (overrides = {}) => ({
    passengers: '', driver: '', vehicle: '', resourceType: 'INTERNAL', borrowedVehicleId: '', ...overrides,
});

function Harness({ initial, passengers }) {
    const [assignments, setAssignments] = useState(initial);
    return (
        <AppTheme defaultColorScheme="light">
            <TourProcessingDraftDistributionEditor
                assignments={assignments}
                totalPassengers={String(passengers)}
                onChange={setAssignments}
                isEdit
                saving={false}
                fieldSx={{}}
                borrowedVehicles={{ options: [], loading: false }}
                vehicleOptions={VEHICLES}
            />
        </AppTheme>
    );
}

afterEach(cleanup);

const valuesOf = (label) => screen.getAllByLabelText(label).map((input) => input.value);
const passengersOfTrips = () => screen.getAllByRole('textbox', { name: 'Pasajeros' }).map((input) => input.value);

async function setTripCount(count) {
    const field = screen.getByRole('textbox', { name: 'Cantidad de viajes' });
    await userEvent.clear(field);
    await userEvent.type(field, String(count));
}

describe('TourProcessingDraftDistributionEditor', () => {
    it('un solo chofer: crea los viajes con el mismo chofer y vehículo y sugiere el mínimo de viajes', async () => {
        render(<Harness initial={[trip({ passengers: '75', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '75' })]} passengers={150} />);

        await userEvent.click(screen.getByRole('button', { name: 'Choferes que repiten viajes' }));
        expect(screen.getByText('Mínimo 5 con 30 espacios')).toBeInTheDocument();
        await setTripCount(5);
        await userEvent.click(screen.getByRole('button', { name: 'Repartir pasajeros' }));

        expect(screen.getByText('Viaje 5')).toBeInTheDocument();
        expect(passengersOfTrips()).toEqual(['30', '30', '30', '30', '30']);
        expect(valuesOf('driver')).toEqual(['Ana', 'Ana', 'Ana', 'Ana', 'Ana']);
        expect(valuesOf('vehicle')).toEqual(['301-1', '301-1', '301-1', '301-1', '301-1']);
        expect(screen.getByText('5 viajes · 150 espacios')).toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('variante 1b: alterna los viajes entre dos choferes', async () => {
        render(<Harness
            initial={[trip({ passengers: '60', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '60', driver: 'Beto', vehicle: '301-2' }), trip({ passengers: '60', driver: 'Ana', vehicle: '301-1' })]}
            passengers={180}
        />);

        await setTripCount(6);
        await userEvent.click(screen.getByRole('button', { name: 'Repartir pasajeros' }));

        expect(valuesOf('driver')).toEqual(['Ana', 'Beto', 'Ana', 'Beto', 'Ana', 'Beto']);
        expect(passengersOfTrips()).toEqual(['30', '30', '30', '30', '30', '30']);
    });

    it('avisa cuántos pasajeros faltan por cubrir sin marcarlo como error', () => {
        render(<Harness
            initial={[trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' })]}
            passengers={100}
        />);

        expect(screen.getByText('Faltan 40 pasajeros por cubrir.')).toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('vehículos distintos: mantiene el primer viaje y deja libres los demás', async () => {
        render(<Harness initial={[trip({ passengers: '35', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '35' })]} passengers={70} />);

        await setTripCount(3);
        await userEvent.click(screen.getByRole('button', { name: 'Repartir pasajeros' }));

        expect(valuesOf('driver')).toEqual(['Ana', '', '']);
        expect(passengersOfTrips()).toEqual(['24', '23', '23']);
    });

    it('la cantidad de choferes se actualiza sola al agregar un chofer distinto', () => {
        render(<Harness initial={[trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' })]} passengers={90} />);

        expect(screen.getByText('1 chofer')).toBeInTheDocument();
        fireEvent.change(screen.getAllByLabelText('driver')[1], { target: { value: 'Beto' } });

        expect(valuesOf('driver')).toEqual(['Ana', 'Beto', 'Ana']);
        expect(screen.getByText('2 choferes')).toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('propone cambiar a choferes distintos cuando ningún chofer repite viaje', async () => {
        render(<Harness initial={[trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' })]} passengers={60} />);

        fireEvent.change(screen.getAllByLabelText('driver')[1], { target: { value: 'Beto' } });
        expect(screen.getByText(/ningún chofer repite viaje/)).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Sí, cambiar de modo' }));

        expect(valuesOf('driver')).toEqual(['Ana', 'Beto']);
        await waitFor(() => expect(screen.queryByText('2 choferes')).not.toBeInTheDocument());
    });

    it('en choferes distintos, repetir un chofer pide cambiar a choferes que repiten viajes', async () => {
        render(<Harness
            initial={[trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '30', driver: 'Beto', vehicle: '301-2' })]}
            passengers={60}
        />);

        fireEvent.change(screen.getAllByLabelText('driver')[1], { target: { value: 'Ana' } });
        expect(screen.getByText(/ya tiene otro viaje/)).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Sí, cambiar de modo' }));

        expect(valuesOf('driver')).toEqual(['Ana', 'Ana']);
        expect(await screen.findByText('1 chofer')).toBeInTheDocument();
    });

    it('en choferes distintos, no acepta repetir un chofer si se rechaza el cambio de modo', async () => {
        render(<Harness
            initial={[trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '30', driver: 'Beto', vehicle: '301-2' })]}
            passengers={60}
        />);

        fireEvent.change(screen.getAllByLabelText('driver')[1], { target: { value: 'Ana' } });
        await userEvent.click(screen.getByRole('button', { name: 'No, mantener' }));

        expect(valuesOf('driver')).toEqual(['Ana', 'Beto']);
        expect(screen.queryByText('1 chofer')).not.toBeInTheDocument();
    });

    it('al pasar a choferes distintos con choferes repetidos advierte y conserva solo la primera aparición', async () => {
        render(<Harness
            initial={[trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '30', driver: 'Beto', vehicle: '301-2' })]}
            passengers={90}
        />);

        await userEvent.click(screen.getByRole('button', { name: 'Choferes distintos' }));
        expect(screen.getByText(/solo se conservará cada chofer en su primera aparición/)).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Sí, cambiar de modo' }));

        expect(valuesOf('driver')).toEqual(['Ana', '', 'Beto']);
        expect(valuesOf('vehicle')).toEqual(['301-1', '', '301-2']);
    });

    it('si se rechaza el cambio a choferes distintos se mantiene el modo y los choferes', async () => {
        render(<Harness
            initial={[trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' })]}
            passengers={60}
        />);

        await userEvent.click(screen.getByRole('button', { name: 'Choferes distintos' }));
        await userEvent.click(screen.getByRole('button', { name: 'No, mantener' }));

        expect(valuesOf('driver')).toEqual(['Ana', 'Ana']);
        expect(await screen.findByText('1 chofer')).toBeInTheDocument();
    });

    it('al pasar a choferes que repiten viajes con un chofer distinto por viaje, repite el del Viaje 1', async () => {
        render(<Harness
            initial={[trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '30', driver: 'Beto', vehicle: '301-2' }), trip({ passengers: '30', driver: 'Caro', vehicle: '301-3' })]}
            passengers={90}
        />);

        await userEvent.click(screen.getByRole('button', { name: 'Choferes que repiten viajes' }));
        expect(screen.getByText(/ninguno repetiría viaje/)).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Sí, cambiar de modo' }));

        expect(valuesOf('driver')).toEqual(['Ana', 'Ana', 'Ana']);
        expect(valuesOf('vehicle')).toEqual(['301-1', '301-1', '301-1']);
        expect(await screen.findByText('1 chofer')).toBeInTheDocument();
    });

    it('si se rechaza el cambio a choferes que repiten viajes se mantiene el modo y los choferes', async () => {
        render(<Harness
            initial={[trip({ passengers: '30', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '30', driver: 'Beto', vehicle: '301-2' })]}
            passengers={60}
        />);

        await userEvent.click(screen.getByRole('button', { name: 'Choferes que repiten viajes' }));
        await userEvent.click(screen.getByRole('button', { name: 'No, mantener' }));

        expect(valuesOf('driver')).toEqual(['Ana', 'Beto']);
        expect(screen.queryByText(/choferes?$/)).not.toBeInTheDocument();
    });
});
