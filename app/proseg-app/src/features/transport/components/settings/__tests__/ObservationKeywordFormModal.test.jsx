import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import { createObservationKeyword } from '../../../services/observationKeywords/observationKeywordsService';
import ObservationKeywordFormModal from '../ObservationKeywordFormModal.jsx';

vi.mock('../../../services/observationKeywords/observationKeywordsService', () => ({
    createObservationKeyword: vi.fn(),
    fetchObservationKeywordById: vi.fn(),
    updateObservationKeyword: vi.fn(),
}));

afterEach(() => cleanup());

describe('formulario de palabras clave', () => {
    it('crea la palabra con Enter y deja Cancelar al lado izquierdo', async () => {
        createObservationKeyword.mockResolvedValue({ id: 'keyword-1', text: 'requiere' });
        const onSaved = vi.fn();
        const onClose = vi.fn();
        render(
            <AppTheme defaultColorScheme="light">
                <ObservationKeywordFormModal open onClose={onClose} onSaved={onSaved} />
            </AppTheme>,
        );

        const cancelButton = screen.getByRole('button', { name: 'Cancelar' });
        const addButton = screen.getByRole('button', { name: 'Agregar' });
        expect(cancelButton.parentElement.firstElementChild).toBe(cancelButton);
        expect(cancelButton.parentElement.contains(addButton)).toBe(true);

        await userEvent.type(screen.getByRole('textbox', { name: /Palabra clave/ }), 'requiere{Enter}');

        await waitFor(() => expect(createObservationKeyword).toHaveBeenCalledWith({ text: 'requiere' }));
        expect(onSaved).toHaveBeenCalledWith({ id: 'keyword-1', text: 'requiere' });
        expect(onClose).toHaveBeenCalledOnce();
    });
});
