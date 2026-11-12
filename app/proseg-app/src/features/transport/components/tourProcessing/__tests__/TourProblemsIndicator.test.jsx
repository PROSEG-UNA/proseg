import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourProblemsIndicator from '../TourProblemsIndicator.jsx';

const problems = [
    { title: 'Vehículos no disponibles asignados a giras', detail: 'Gira 100 (04/05/2026): vehículo 301-295 no está disponible' },
    { title: 'Sin chofer o sin vehículo', detail: 'La gira está aprobada pero ninguno de sus viajes tiene chofer y vehículo' },
];

const renderIndicator = (value, props = {}) => render(
    <AppTheme defaultColorScheme="light">
        <TourProblemsIndicator problems={value} {...props} />
    </AppTheme>,
);

afterEach(() => cleanup());

describe('ícono de problemas de la gira', () => {
    it('no muestra nada si la gira no tiene problemas', () => {
        const { container, rerender } = renderIndicator(undefined);
        expect(container).toBeEmptyDOMElement();

        rerender(
            <AppTheme defaultColorScheme="light">
                <TourProblemsIndicator problems={[]} />
            </AppTheme>,
        );
        expect(screen.queryByRole('button')).toBeNull();
    });

    it('el cursor lista cada problema con su detalle', async () => {
        renderIndicator(problems);

        const icon = screen.getByRole('button', {
            name: 'Problemas de la gira: Vehículos no disponibles asignados a giras, Sin chofer o sin vehículo',
        });
        await userEvent.hover(icon);

        const tooltip = await screen.findByRole('tooltip');
        expect(tooltip).toHaveTextContent('Vehículos no disponibles asignados a giras');
        expect(tooltip).toHaveTextContent('Gira 100 (04/05/2026): vehículo 301-295 no está disponible');
        expect(tooltip).toHaveTextContent('Sin chofer o sin vehículo');
    });

    it('el clic no se propaga a la fila', async () => {
        const onRowClick = vi.fn();
        render(
            <AppTheme defaultColorScheme="light">
                <div onClick={onRowClick}>
                    <TourProblemsIndicator problems={problems} />
                </div>
            </AppTheme>,
        );

        await userEvent.click(screen.getByRole('button'));

        expect(onRowClick).not.toHaveBeenCalled();
    });
});
