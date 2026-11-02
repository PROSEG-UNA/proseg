import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourProcessingAssignmentsCell from '../TourProcessingAssignmentsCell.jsx';

const neverMissing = () => false;

function renderCell(props) {
    render(
        <AppTheme defaultColorScheme="light">
            <TourProcessingAssignmentsCell isMissing={neverMissing} {...props} />
        </AppTheme>,
    );
}

afterEach(cleanup);

describe('TourProcessingAssignmentsCell', () => {
    it('muestra el primer viaje y un chip neutro cuando todos los viajes estan asignados', () => {
        renderCell({
            kind: 'driver',
            assignments: [{ driver: 'Ana Mora' }, { driver: 'Beto Vega' }],
        });

        expect(screen.getByText('Ana Mora')).toBeInTheDocument();
        const chip = screen.getByRole('img', { name: '2 viajes' });
        expect(chip).toHaveTextContent('+1');
        expect(chip).not.toHaveClass('MuiChip-colorWarning');
    });

    it('marca en ambar el chip cuando un viaje oculto no tiene chofer', () => {
        renderCell({
            kind: 'driver',
            assignments: [{ driver: 'Ana Mora' }, { driver: '' }],
        });

        expect(screen.getByText('Ana Mora')).toBeInTheDocument();
        const chip = screen.getByRole('img', { name: '2 viajes. 1 de 2 viajes sin chofer' });
        expect(chip).toHaveTextContent('+1');
        expect(chip).toHaveClass('MuiChip-colorWarning');
    });

    it('cuenta en el tooltip los viajes sin asignar y lista cada viaje', async () => {
        renderCell({
            kind: 'driver',
            assignments: [{ driver: 'Ana Mora' }, { driver: 'Beto Vega' }, { driver: null }],
        });

        await userEvent.hover(screen.getByRole('img', { name: /3 viajes/ }));

        expect(await screen.findByText('1 de 3 viajes sin chofer')).toBeInTheDocument();
        expect(screen.getByText('Viaje 1')).toBeInTheDocument();
        expect(screen.getByText('Beto Vega')).toBeInTheDocument();
        expect(screen.getByText('Sin chofer')).toBeInTheDocument();
    });

    it('muestra guion y chip ambar cuando el primer viaje no tiene chofer pero el segundo si', () => {
        renderCell({
            kind: 'driver',
            assignments: [{ driver: '' }, { driver: 'Beto Vega' }],
        });

        expect(screen.getByText('—')).toBeInTheDocument();
        expect(screen.queryByText('Beto Vega')).not.toBeInTheDocument();
        expect(screen.getByRole('img', { name: '2 viajes. 1 de 2 viajes sin chofer' })).toBeInTheDocument();
    });

    it('usa la placa del vehiculo prestado y cuenta los viajes sin vehiculo', () => {
        renderCell({
            kind: 'vehicle',
            assignments: [
                { vehicle: null, borrowedVehiclePlate: 'PRE-123', borrowedVehicleId: 'b1', resourceType: 'BORROWED_VEHICLE' },
                { vehicle: '' },
            ],
        });

        expect(screen.getByText('PRE-123')).toBeInTheDocument();
        expect(screen.getByRole('img', { name: '2 viajes. 1 de 2 viajes sin vehículo' })).toBeInTheDocument();
    });

    it('marca en ambar el chip cuando un viaje oculto tiene un chofer que no esta en el catalogo', () => {
        renderCell({
            kind: 'driver',
            isMissing: (value) => value === 'Chofer Nuevo',
            assignments: [{ driver: 'Ana Mora' }, { driver: 'Chofer Nuevo' }],
        });

        expect(screen.getByRole('img', { name: '2 viajes. 1 chofer no está en el sistema' })).toHaveClass('MuiChip-colorWarning');
    });

    it('no cuenta como faltante un chofer externo ni un vehiculo prestado', () => {
        renderCell({
            kind: 'vehicle',
            isMissing: () => true,
            assignments: [
                { vehicle: 'EXT-1', resourceType: 'EXTERNAL' },
                { vehicle: null, borrowedVehiclePlate: 'PRE-123', resourceType: 'BORROWED_VEHICLE' },
            ],
        });

        expect(screen.getByRole('img', { name: '2 viajes' })).not.toHaveClass('MuiChip-colorWarning');
    });
});
