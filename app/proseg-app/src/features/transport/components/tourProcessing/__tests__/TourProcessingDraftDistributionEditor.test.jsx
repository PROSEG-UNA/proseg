import { useState } from 'react';
import { cleanup, render, screen } from '@testing-library/react';
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
    it('manera 1: crea los viajes con el mismo chofer y vehículo y sugiere el mínimo de viajes', async () => {
        render(<Harness initial={[trip({ passengers: '75', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '75' })]} passengers={150} />);

        await userEvent.click(screen.getByRole('button', { name: 'Un chofer y vehículo' }));
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
            initial={[trip({ passengers: '90', driver: 'Ana', vehicle: '301-1' }), trip({ passengers: '90', driver: 'Beto', vehicle: '301-2' })]}
            passengers={180}
        />);

        await userEvent.click(screen.getByRole('button', { name: 'Varios choferes' }));
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
});
