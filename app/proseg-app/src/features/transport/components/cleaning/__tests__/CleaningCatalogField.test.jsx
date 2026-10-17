import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import CleaningCatalogField from '../CleaningCatalogField.jsx';

const mocks = vi.hoisted(() => ({ catalog: null }));

vi.mock('../../../hooks/useCleaningCatalogMatch.js', () => ({
    useCleaningCatalogMatch: () => mocks.catalog,
}));

vi.mock('../../drivers/DriverFormModal.jsx', () => ({
    default: ({ initialValues, onSaved, onClose }) => (
        <div>
            <span>{`formulario-chofer:${initialValues.name}`}</span>
            <button type="button" onClick={() => { onSaved({ name: 'Juan Chacón' }); onClose(); }}>
                guardar-chofer
            </button>
        </div>
    ),
}));

vi.mock('../../vehicles/VehicleFormModal.jsx', () => ({
    default: ({ initialValues }) => <span>{`formulario-vehiculo:${initialValues.plate}`}</span>,
}));

const DRIVER_NAMES = ['Jorge Francisco Mena', 'Miguel Fernandez'];

function buildCatalog({ canAddDriver = true, missingDrivers = ['Juan Chacsn'] } = {}) {
    return {
        driver: {
            options: DRIVER_NAMES,
            isMissing: (value) => missingDrivers.includes(value),
            canAdd: canAddDriver,
        },
        vehicle: {
            options: ['301-627'],
            isMissing: (value) => ['999-999', 'Todo Terreno'].includes(value),
            canAdd: true,
        },
    };
}

function renderField(props = {}) {
    const onChange = vi.fn();
    const queryClient = new QueryClient();
    render(
        <AppTheme defaultColorScheme="light">
            <QueryClientProvider client={queryClient}>
                <CleaningCatalogField
                    kind="driver"
                    value="Juan Chacsn"
                    onChange={onChange}
                    onBlur={vi.fn()}
                    isEdit
                    disabled={false}
                    {...props}
                />
            </QueryClientProvider>
        </AppTheme>,
    );
    return { onChange };
}

beforeEach(() => {
    mocks.catalog = buildCatalog();
});

afterEach(cleanup);

describe('CleaningCatalogField', () => {
    it('no ofrece opciones cuando el chofer ya esta en el sistema', () => {
        renderField({ value: 'Jorge Francisco Mena' });

        expect(screen.getByRole('combobox', { name: 'Chofer' })).toHaveValue('Jorge Francisco Mena');
        expect(screen.queryByRole('button', { name: 'Seleccionar otro chofer' })).not.toBeInTheDocument();
    });

    it('avisa y ofrece agregar o seleccionar otro cuando el chofer no esta en el sistema', () => {
        renderField();

        expect(screen.getByText(/Este chofer no se encuentra en el sistema, agrégalo antes de continuar/)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Agregar chofer al sistema' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Seleccionar otro chofer' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Dejar vacío' })).not.toBeInTheDocument();
    });

    it('deja el campo vacio con la X del input', async () => {
        const { onChange } = renderField();

        await userEvent.click(screen.getByTitle('Clear'));

        expect(onChange).toHaveBeenCalledWith('');
    });

    it('agrega el chofer al sistema con el nombre importado y lo deja seleccionado', async () => {
        const { onChange } = renderField();

        await userEvent.click(screen.getByRole('button', { name: 'Agregar chofer al sistema' }));
        expect(screen.getByText('formulario-chofer:Juan Chacsn')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'guardar-chofer' }));

        await waitFor(() => expect(onChange).toHaveBeenCalledWith('Juan Chacón'));
    });

    it('abre la lista del sistema para seleccionar otro chofer', async () => {
        const { onChange } = renderField();

        await userEvent.click(screen.getByRole('button', { name: 'Seleccionar otro chofer' }));

        expect(screen.getByRole('combobox', { name: 'Chofer' })).toHaveFocus();
        expect(await screen.findByRole('option', { name: 'Jorge Francisco Mena' })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: 'Miguel Fernandez' })).toBeInTheDocument();

        await userEvent.click(screen.getByRole('option', { name: 'Miguel Fernandez' }));

        expect(onChange).toHaveBeenCalledWith('Miguel Fernandez');
    });

    it('oculta agregar al sistema sin permiso de gestionar', () => {
        mocks.catalog = buildCatalog({ canAddDriver: false });
        renderField();

        expect(screen.queryByRole('button', { name: 'Agregar chofer al sistema' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Seleccionar otro chofer' })).toBeInTheDocument();
    });

    it('usa la placa en las opciones del vehiculo', async () => {
        renderField({ kind: 'vehicle', value: '999-999' });

        expect(screen.getByText(/Este vehículo no se encuentra en el sistema, agrégalo antes de continuar/)).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Agregar vehículo al sistema' }));

        expect(screen.getByText('formulario-vehiculo:999-999')).toBeInTheDocument();
    });

    it('no ofrece agregar un tipo de vehiculo porque solo se agregan placas', () => {
        renderField({ kind: 'vehicle', value: 'Todo Terreno' });

        expect(screen.getByText(/Este valor no es una placa/)).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Agregar vehículo al sistema' })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Seleccionar otro vehículo' })).toBeInTheDocument();
    });

    it('muestra el icono de advertencia en modo lectura', () => {
        renderField({ isEdit: false });

        expect(screen.getByDisplayValue('Juan Chacsn')).toHaveAttribute('readonly');
        expect(screen.getByRole('img', { hidden: false })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Seleccionar otro chofer' })).not.toBeInTheDocument();
    });
});
