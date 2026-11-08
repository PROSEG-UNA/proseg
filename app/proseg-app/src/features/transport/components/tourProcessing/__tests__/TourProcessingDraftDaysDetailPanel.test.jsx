import { cleanup, render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourProcessingDraftDaysDetailPanel from '../TourProcessingDraftDaysDetailPanel.jsx';

const DAY = { date: '2026-05-04', startTime: '07:30:00', endTime: '17:00:00', ordinaryHours: 9, surplusHours: 0.5, overtimeHours: 0 };

function renderPanel(props) {
    render(
        <AppTheme defaultColorScheme="light">
            <TourProcessingDraftDaysDetailPanel {...props} />
        </AppTheme>,
    );
}

afterEach(cleanup);

describe('TourProcessingDraftDaysDetailPanel', () => {
    it('muestra una sola lista cuando la gira tiene un solo vehiculo', () => {
        renderPanel({ days: [DAY], assignments: [{ rowId: 'a', driver: 'Ana', vehicle: '301-1', passengers: 10, days: [DAY] }] });

        expect(screen.getAllByRole('list')).toHaveLength(1);
        expect(screen.queryByRole('region')).not.toBeInTheDocument();
    });

    it('separa el detalle del mismo dia por cada vehiculo', () => {
        renderPanel({
            days: [DAY],
            assignments: [
                { rowId: 'a', driver: 'Ana Mora', vehicle: '301-1', passengers: 36, days: [DAY] },
                { rowId: 'b', driver: 'Beto Vega', vehicle: '301-2', passengers: 35, days: [DAY] },
            ],
        });

        const first = screen.getByRole('region', { name: 'Vehículo 1 · 301-1 · Ana Mora' });
        const second = screen.getByRole('region', { name: 'Vehículo 2 · 301-2 · Beto Vega' });
        expect(within(first).getByText('04/05/2026')).toBeInTheDocument();
        expect(within(second).getByText('04/05/2026')).toBeInTheDocument();
        expect(within(first).getByText('36 pasajeros')).toBeInTheDocument();
        expect(within(second).getByText('35 pasajeros')).toBeInTheDocument();
        expect(screen.getByText(/2 vehículos/)).toBeInTheDocument();
    });
});
