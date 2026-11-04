import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourProcessingAutoAssignmentInfoModal from '../TourProcessingAutoAssignmentInfoModal.jsx';

afterEach(cleanup);

describe('información de la autoasignación', () => {
    it('permite consultar las etapas de vehículos y volver a choferes', () => {
        const onClose = vi.fn();
        render(
            <AppTheme defaultColorScheme="light">
                <TourProcessingAutoAssignmentInfoModal open onClose={onClose} />
            </AppTheme>,
        );

        expect(within(screen.getByRole('tabpanel')).getByText('Autoasignación de choferes')).toBeInTheDocument();
        fireEvent.click(screen.getByRole('tab', { name: 'Vehículos' }));
        const vehiclePanel = screen.getByRole('tabpanel');
        expect(within(vehiclePanel).getByText('Autoasignación de vehículos')).toBeInTheDocument();
        expect(within(vehiclePanel).getByText(/Primero se prueban los vehículos libres/)).toBeInTheDocument();
        expect(within(vehiclePanel).getByText(/cambiar vehículos elegidos durante esta autoasignación/)).toBeInTheDocument();
        expect(within(vehiclePanel).getByText(/quedan pendientes con su motivo/)).toBeInTheDocument();
        expect(within(vehiclePanel).getByText(/aparecen en Requieren revisión/)).toBeInTheDocument();
        expect(within(vehiclePanel).getByText(/Este recorrido se hace una sola vez/)).toBeInTheDocument();
        expect(within(vehiclePanel).getByText(/no agrega horas al chofer/)).toBeInTheDocument();
        fireEvent.click(screen.getByRole('tab', { name: 'Choferes' }));
        expect(within(screen.getByRole('tabpanel')).getByText('Autoasignación de choferes')).toBeInTheDocument();
    });
});
