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
    { label: '301-481', secondary: 'Sedán · 4 pasajeros', searchText: '301-481 Sedán 4 pasajeros' },
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
            isUnavailable: (value) => value === '301-900',
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

    it('avisa y ofrece agregar cuando el chofer no esta en el sistema', () => {
        renderField();

        expect(screen.getByText(/Este chofer no se encuentra en el sistema, agrégalo antes de continuar/)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Agregar chofer al sistema' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Seleccionar otro chofer' })).not.toBeInTheDocument();
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

    it('abre la lista del sistema desde el campo para seleccionar otro chofer', async () => {
        const { onChange } = renderField();

        await userEvent.click(screen.getByRole('combobox', { name: 'Chofer' }));

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
        expect(screen.queryByText('No disponible')).not.toBeInTheDocument();
    });

    it('muestra No disponible junto a la placa asignada aunque no este en las opciones', () => {
        renderField({ kind: 'vehicle', value: '301-900', isEdit: false });

        expect(screen.getByDisplayValue('301-900')).toBeInTheDocument();
        expect(screen.getByText('No disponible')).toBeInTheDocument();
        expect(screen.queryByRole('option', { name: /301-900/ })).not.toBeInTheDocument();
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

    it('en interno avisa que no existe un chofer que es de otra unidad y no ofrece agregarlo', () => {
        renderField({ value: 'Pedro Otra Unidad', generalServicesOnly: true });

        expect(screen.getByText(/Este chofer no se encuentra en el sistema, agrégalo antes de continuar/)).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Agregar chofer al sistema' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Seleccionar otro chofer' })).not.toBeInTheDocument();
    });

    it('en interno no avisa de un chofer de Servicios Generales', () => {
        renderField({ value: 'Jorge Francisco Mena', generalServicesOnly: true });

        expect(screen.queryByText(/Este chofer no se encuentra en el sistema/)).not.toBeInTheDocument();
    });

    it('en prestamo no avisa de un chofer de otra unidad', () => {
        renderField({ value: 'Pedro Otra Unidad', unitTabs: true });

        expect(screen.queryByText(/Este chofer no se encuentra en el sistema/)).not.toBeInTheDocument();
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
        expect(screen.queryByRole('button', { name: 'Seleccionar otro chofer' })).not.toBeInTheDocument();
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
        expect(screen.queryByRole('button', { name: 'Seleccionar otro vehículo' })).not.toBeInTheDocument();
    });

    it('en interno no muestra pestañas de unidad', async () => {
        renderField({ value: '', generalServicesOnly: true });

        await userEvent.click(screen.getByRole('combobox', { name: 'Chofer' }));

        expect(await screen.findByRole('option', { name: /Jorge Francisco Mena/ })).toBeInTheDocument();
        expect(screen.queryByRole('tab', { name: 'Otra Unidad' })).not.toBeInTheDocument();
    });

    it('en prestamo abre en la pestaña Otra Unidad y solo lista choferes de otra unidad', async () => {
        renderField({ value: '', unitTabs: true });

        await userEvent.click(screen.getByRole('combobox', { name: 'Chofer' }));

        expect(await screen.findByRole('tab', { name: 'Otra Unidad', selected: true })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /Pedro Otra Unidad/ })).toBeInTheDocument();
        expect(screen.queryByRole('option', { name: /Jorge Francisco Mena/ })).not.toBeInTheDocument();
    });

    it('en prestamo la pestaña Servicios Generales lista los choferes de la unidad', async () => {
        renderField({ value: '', unitTabs: true });

        await userEvent.click(screen.getByRole('combobox', { name: 'Chofer' }));
        await userEvent.click(await screen.findByRole('tab', { name: 'Servicios Generales' }));

        expect(await screen.findByRole('option', { name: /Jorge Francisco Mena/ })).toBeInTheDocument();
        expect(screen.queryByRole('option', { name: /Pedro Otra Unidad/ })).not.toBeInTheDocument();
    });

    it('muestra el icono de advertencia en modo lectura', () => {
        renderField({ isEdit: false });

        expect(screen.getByDisplayValue('Juan Chacsn')).toHaveAttribute('readonly');
        expect(screen.getByRole('img', { hidden: false })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Seleccionar otro chofer' })).not.toBeInTheDocument();
    });
});
