import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourObservationMatchIndicator from '../TourObservationMatchIndicator.jsx';
import { highlightObservation } from '../observationKeywordText.js';
import { updateTourObservationReviewed } from '../../../services/tourProcessing/tourProcessingService.js';

vi.mock('../../../services/tourProcessing/tourProcessingService.js', () => ({
    updateTourObservationReviewed: vi.fn(),
}));

const row = {
    id: 'row-1',
    observations: 'Revisar URGENTE hoy',
    matchedObservationKeywords: ['urgente'],
    observationReviewed: false,
};

function renderIndicator(current = row, props = {}) {
    return render(
        <AppTheme defaultColorScheme="light">
            <QueryClientProvider client={new QueryClient()}>
                <TourObservationMatchIndicator draftId="draft-1" row={current} canReview {...props} />
            </QueryClientProvider>
        </AppTheme>,
    );
}

afterEach(() => cleanup());

describe('resaltado de la observación', () => {
    it('marca la palabra aunque tenga mayúsculas o tildes', () => {
        expect(highlightObservation('Revisar URGENTE hoy', ['urgente']).filter((part) => part.match).map((part) => part.text)).toEqual(['URGENTE']);
        expect(highlightObservation('Revisar urgénte hoy', ['urgente']).filter((part) => part.match).map((part) => part.text)).toEqual(['urgénte']);
    });

    it('deja el texto plano si las coincidencias se cruzan', () => {
        expect(highlightObservation('urgente', ['urg', 'gente']).every((part) => !part.match)).toBe(true);
    });
});

describe('ícono de palabras clave', () => {
    beforeEach(() => {
        updateTourObservationReviewed.mockReset();
    });

    it('no muestra el ícono sin coincidencias o si ya está revisada', () => {
        const { rerender } = renderIndicator({ ...row, matchedObservationKeywords: [] });
        expect(screen.queryByRole('button', { name: /Coincidencias en la observación/ })).toBeNull();

        rerender(
            <AppTheme defaultColorScheme="light">
                <QueryClientProvider client={new QueryClient()}>
                    <TourObservationMatchIndicator draftId="draft-1" row={{ ...row, observationReviewed: true }} canReview />
                </QueryClientProvider>
            </AppTheme>,
        );
        expect(screen.queryByRole('button', { name: /Coincidencias en la observación/ })).toBeNull();
    });

    it('el cursor muestra la palabra y la observación, y el clic la marca como revisada', async () => {
        updateTourObservationReviewed.mockResolvedValue({ ...row, observationReviewed: true });
        const onReviewed = vi.fn();
        renderIndicator(row, { onReviewed });

        const icon = screen.getByRole('button', { name: 'Coincidencias en la observación: urgente' });
        await userEvent.hover(icon);
        expect(await screen.findByRole('tooltip')).toHaveTextContent('Coincidencias: urgente');
        expect(screen.getByRole('tooltip')).toHaveTextContent('Revisar URGENTE hoy');
        expect(screen.getByRole('tooltip').querySelector('mark')).toHaveTextContent('URGENTE');

        const reviewButton = screen.getByRole('button', { name: 'Marcar como revisada' });
        expect(reviewButton).toHaveClass('MuiButton-outlined');
        await userEvent.hover(reviewButton);
        await userEvent.click(reviewButton);

        expect(updateTourObservationReviewed).toHaveBeenCalledWith('draft-1', 'row-1', true);
        expect(onReviewed).toHaveBeenCalledWith(expect.objectContaining({ observationReviewed: true }));
    });

    it('abre la acción con el teclado', async () => {
        renderIndicator();
        const icon = screen.getByRole('button', { name: 'Coincidencias en la observación: urgente' });
        icon.focus();
        await userEvent.keyboard('{Enter}');
        expect(screen.getByRole('button', { name: 'Marcar como revisada' })).toBeInTheDocument();
    });

    it('permite volver a dejarla pendiente desde la observación revisada', async () => {
        updateTourObservationReviewed.mockResolvedValue({ ...row, observationReviewed: false });
        renderIndicator({ ...row, observationReviewed: true }, { allowReopen: true });

        await userEvent.click(screen.getByRole('button', { name: 'Volver a marcar como pendiente' }));
        expect(updateTourObservationReviewed).toHaveBeenCalledWith('draft-1', 'row-1', false);
    });
});
