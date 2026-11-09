import { useState } from 'react';
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import TourProcessingCatalogField from '../TourProcessingCatalogField.jsx';
import { queryKeys } from '../../../../../common/query';

const mocks = vi.hoisted(() => ({ catalog: null }));

vi.mock('../../../hooks/useTourProcessingCatalogMatch.js', () => ({
    useTourProcessingCatalogMatch: () => mocks.catalog,
}));

vi.mock('../../drivers/DriverFormModal.jsx', () => ({
    default: ({ initialValues, onSaved, onClose }) => (
        <div>
            <span>{`formulario-chofer:${initialValues.name}`}</span>
            <span>{`unidad:${initialValues.organizationalUnit ?? ''}`}</span>
            <button type="button" onClick={() => { onSaved({ id: 'driver-3', name: 'Juan Chacón' }); onClose(); }}>
                guardar-chofer
            </button>
        </div>
    ),
}));

vi.mock('../../vehicles/VehicleFormModal.jsx', () => ({
    default: ({ initialValues }) => <span>{`formulario-vehiculo:${initialValues.plate}`}</span>,
}));

const DRIVER_ENTRIES = [
    { label: 'Jorge Francisco Mena', unit: 'GENERAL_SERVICES', secondary: 'Servicios Generales · 07:30 a 16:30', searchText: 'Jorge Francisco Mena Servicios Generales 07:30 a 16:30' },
    { label: 'Miguel Fernandez', unit: 'GENERAL_SERVICES', secondary: 'Servicios Generales · 07:30 a 16:30', searchText: 'Miguel Fernandez Servicios Generales 07:30 a 16:30' },
    { label: 'Pedro Otra Unidad', unit: 'OTHER_UNIT', secondary: 'Otra Unidad · 07:30 a 16:30', searchText: 'Pedro Otra Unidad' },
    { label: 'Juan Pérez', unit: 'OTHER_UNIT', secondary: 'Otra Unidad · 07:30 a 16:30', searchText: 'Juan Pérez' },
];

const VEHICLE_ENTRIES = [
    { label: '301-627', secondary: 'Microbús · 36 pasajeros', searchText: '301-627 Microbús 36 pasajeros' },
    { label: '301-481', secondary: 'Sedán · 4 pasajeros', badge: { label: 'No disponible', color: 'error' }, searchText: '301-481 Sedán 4 pasajeros No disponible' },
];

function buildCatalog({ canAddDriver = true, missingDrivers = ['Juan Chacsn'] } = {}) {
    return {
        driver: {
            options: DRIVER_ENTRIES,
            isMissing: (value) => missingDrivers.includes(value),
            canAdd: canAddDriver,
        },
        vehicle: {
            options: VEHICLE_ENTRIES,
            isMissing: (value) => ['999-999', 'Todo Terreno'].includes(value),
            canAdd: true,
        },
    };
}

function renderField(props = {}) {
    const onChange = vi.fn();
    const queryClient = new QueryClient();
    queryClient.setQueryData(queryKeys.transport.driverOptions(), [{ id: 'driver-1', label: 'Jorge Francisco Mena' }]);
    render(
        <AppTheme defaultColorScheme="light">
            <QueryClientProvider client={queryClient}>
                <TourProcessingCatalogField
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
    return { onChange, queryClient };
}

beforeEach(() => {
    mocks.catalog = buildCatalog();
});

afterEach(cleanup);

describe('TourProcessingCatalogField', () => {
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
        const { onChange, queryClient } = renderField();

        await userEvent.click(screen.getByRole('button', { name: 'Agregar chofer al sistema' }));
        expect(screen.getByText('formulario-chofer:Juan Chacsn')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'guardar-chofer' }));

        await waitFor(() => expect(onChange).toHaveBeenCalledWith('Juan Chacón'));
        expect(queryClient.getQueryData(queryKeys.transport.driverOptions())).toContainEqual(expect.objectContaining({ id: 'driver-3', label: 'Juan Chacón' }));
    });

    it('quita la marca de faltante al agregar la opción antes de actualizar el campo', async () => {
        const queryClient = new QueryClient();
        queryClient.setQueryData(queryKeys.transport.driverOptions(), [{ id: 'driver-1', label: 'Jorge Francisco Mena' }]);
        mocks.catalog.driver.isMissing = (value) => Boolean(value) && !queryClient.getQueryData(queryKeys.transport.driverOptions()).some((option) => option.label === value);

        function ControlledField() {
            const [value, setValue] = useState('Juan Chacsn');
            return <TourProcessingCatalogField kind="driver" value={value} onChange={setValue} onBlur={vi.fn()} isEdit disabled={false} />;
        }

        render(
            <AppTheme defaultColorScheme="light">
                <QueryClientProvider client={queryClient}><ControlledField /></QueryClientProvider>
            </AppTheme>,
        );

        expect(screen.getByText(/Este chofer no se encuentra en el sistema/)).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: 'Agregar chofer al sistema' }));
        await userEvent.click(screen.getByRole('button', { name: 'guardar-chofer' }));

        await waitFor(() => expect(screen.getByRole('combobox', { name: 'Chofer' })).toHaveValue('Juan Chacón'));
        expect(screen.queryByText(/Este chofer no se encuentra en el sistema/)).not.toBeInTheDocument();
    });

    it('abre la lista del sistema para seleccionar otro chofer', async () => {
        const { onChange } = renderField();

        await userEvent.click(screen.getByRole('button', { name: 'Seleccionar otro chofer' }));

        expect(screen.getByRole('combobox', { name: 'Chofer' })).toHaveFocus();
        expect(await screen.findByRole('option', { name: /Jorge Francisco Mena/ })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /Miguel Fernandez/ })).toBeInTheDocument();

        await userEvent.click(screen.getByRole('option', { name: /Miguel Fernandez/ }));

        expect(onChange).toHaveBeenCalledWith('Miguel Fernandez');
    });

    it('muestra los datos del vehiculo en cada opcion', async () => {
        renderField({ kind: 'vehicle', value: '301-627' });

        await userEvent.click(screen.getByRole('combobox', { name: 'Vehículo' }));

        expect(await screen.findByText('Microbús · 36 pasajeros')).toBeInTheDocument();
        expect(screen.getByText('Sedán · 4 pasajeros')).toBeInTheDocument();
        expect(screen.getByText('No disponible')).toBeInTheDocument();
    });

    it('busca el vehiculo por pasajeros o tipo', async () => {
        renderField({ kind: 'vehicle', value: '' });

        await userEvent.type(screen.getByRole('combobox', { name: 'Vehículo' }), 'sedán');

        expect(await screen.findByRole('option', { name: /301-481/ })).toBeInTheDocument();
        expect(screen.queryByRole('option', { name: /301-627/ })).not.toBeInTheDocument();
    });

    it('en interno solo lista choferes de Servicios Generales', async () => {
        renderField({ value: '', generalServicesOnly: true });

        await userEvent.click(screen.getByRole('combobox', { name: 'Chofer' }));

        expect(await screen.findByRole('option', { name: /Jorge Francisco Mena/ })).toBeInTheDocument();
        expect(screen.queryByRole('option', { name: /Pedro Otra Unidad/ })).not.toBeInTheDocument();
    });

    it('sin filtro lista choferes de todas las unidades', async () => {
        renderField({ value: '' });

        await userEvent.click(screen.getByRole('combobox', { name: 'Chofer' }));

        expect(await screen.findByRole('option', { name: /Pedro Otra Unidad/ })).toBeInTheDocument();
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

    function LoanField() {
        const [value, setValue] = useState('');
        return (
            <TourProcessingCatalogField
                kind="driver"
                otherUnitOnly
                value={value}
                onChange={setValue}
                onBlur={vi.fn()}
                isEdit
                disabled={false}
            />
        );
    }

    it('en prestamo deja Juan escrito aunque Juan Perez este en la lista', async () => {
        const queryClient = new QueryClient();
        render(
            <AppTheme defaultColorScheme="light">
                <QueryClientProvider client={queryClient}><LoanField /></QueryClientProvider>
            </AppTheme>,
        );

        await userEvent.type(screen.getByRole('combobox', { name: 'Chofer' }), 'Juan');
        await userEvent.tab();

        expect(screen.getByDisplayValue('Juan')).toBeInTheDocument();
        expect(screen.getByText('El chofer no existe en el sistema. Lo puede dejar así o lo puede ingresar al sistema tocando este botón.')).toBeInTheDocument();
        expect(screen.queryByRole('combobox', { name: 'Chofer' })).not.toBeInTheDocument();
    });

    it('en prestamo el boton abre el formulario con el nombre y otra unidad', async () => {
        const queryClient = new QueryClient();
        render(
            <AppTheme defaultColorScheme="light">
                <QueryClientProvider client={queryClient}><LoanField /></QueryClientProvider>
            </AppTheme>,
        );

        await userEvent.type(screen.getByRole('combobox', { name: 'Chofer' }), 'Juan');
        await userEvent.tab();
        await userEvent.click(screen.getByRole('button', { name: 'Ingresar al sistema' }));

        expect(screen.getByText('formulario-chofer:Juan')).toBeInTheDocument();
        expect(screen.getByText('unidad:OTHER_UNIT')).toBeInTheDocument();
    });

    it('en prestamo solo lista choferes de otra unidad', async () => {
        renderField({ value: '', otherUnitOnly: true });

        await userEvent.click(screen.getByRole('combobox', { name: 'Chofer' }));

        expect(await screen.findByRole('option', { name: /Pedro Otra Unidad/ })).toBeInTheDocument();
        expect(screen.queryByRole('option', { name: /Jorge Francisco Mena/ })).not.toBeInTheDocument();
    });

    it('en prestamo una coincidencia exacta sigue en el combobox', () => {
        renderField({ value: 'Pedro Otra Unidad', otherUnitOnly: true });

        expect(screen.getByRole('combobox', { name: 'Chofer' })).toHaveValue('Pedro Otra Unidad');
        expect(screen.queryByText(/El chofer no existe en el sistema/)).not.toBeInTheDocument();
    });

    it('en prestamo la lectura no muestra la advertencia de chofer inexistente', () => {
        renderField({ value: 'Juan', otherUnitOnly: true, isEdit: false });

        expect(screen.getByDisplayValue('Juan')).toBeInTheDocument();
        expect(screen.queryByRole('img')).not.toBeInTheDocument();
    });

    it('muestra el icono de advertencia en modo lectura', () => {
        renderField({ isEdit: false });

        expect(screen.getByDisplayValue('Juan Chacsn')).toHaveAttribute('readonly');
        expect(screen.getByRole('img', { hidden: false })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Seleccionar otro chofer' })).not.toBeInTheDocument();
    });
});
