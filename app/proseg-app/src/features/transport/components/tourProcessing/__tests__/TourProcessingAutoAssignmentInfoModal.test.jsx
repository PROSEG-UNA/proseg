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

    it('describe las siete etapas de choferes en el orden en que se ejecutan', () => {
        render(
            <AppTheme defaultColorScheme="light">
                <TourProcessingAutoAssignmentInfoModal open onClose={vi.fn()} />
            </AppTheme>,
        );

        const panel = screen.getByRole('tabpanel');
        expect(within(panel).getByText(/construye el horario de cada chofer en siete etapas/)).toBeInTheDocument();
        const titles = within(panel).getAllByRole('heading', { level: 4 }).map((heading) => heading.textContent);
        expect(titles.slice(0, 7)).toEqual([
            'Preparar las giras',
            'Asignar al solicitante',
            'Asignar giras Periféricas',
            'Cubrir las obligatorias de fin de semana',
            'Completar sobresueldo',
            'Asignar las giras pendientes',
            'Reacomodar y volver a intentar',
        ]);
    });

    it('explica el cubrimiento de las obligatorias, los topes y que nunca contrata', () => {
        render(
            <AppTheme defaultColorScheme="light">
                <TourProcessingAutoAssignmentInfoModal open onClose={vi.fn()} />
            </AppTheme>,
        );

        const panel = screen.getByRole('tabpanel');
        expect(within(panel).getByText(/giras de prioridad 1 con menos de 5 pasajeros; nunca se contratan/)).toBeInTheDocument();
        expect(within(panel).getByText(/reutiliza uno ya elegido mientras sus topes lo permitan/)).toBeInTheDocument();
        expect(within(panel).getByText(/En este paso no se reparte con equidad/)).toBeInTheDocument();
        expect(within(panel).getByText(/solo se conserva el cambio si el resultado es válido/)).toBeInTheDocument();
        expect(within(panel).getByText(/queda con una advertencia que indica el porcentaje de sobresueldo/)).toBeInTheDocument();
        expect(within(panel).getByText(/Una obligatoria solo queda sin chofer si nadie puede tomarla/)).toBeInTheDocument();
        expect(within(panel).getByText(/Cada chofer tiene un solo horario ordinario, que puede ser de 07:30 a 16:30 o de 12:00 a 20:00/)).toBeInTheDocument();
        expect(within(panel).getByText(/no pasa de 3 fines de semana trabajados si el mes tiene 5, o de 2 si tiene 4/)).toBeInTheDocument();
        expect(within(panel).getByText(/no supera las 60 horas extra del mes/)).toBeInTheDocument();
        expect(within(panel).getByText(/Nunca contrata transporte externo ni pide vehículos prestados/)).toBeInTheDocument();
        expect(within(panel).getByText(/Una gira de varios días se asigna completa a un solo chofer/)).toBeInTheDocument();
        expect(within(panel).queryByText(/conservar fines de semana libres/)).toBeNull();
    });
});
