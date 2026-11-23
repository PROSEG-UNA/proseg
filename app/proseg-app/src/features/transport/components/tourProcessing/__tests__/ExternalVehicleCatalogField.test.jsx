import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import { queryKeys } from '../../../../../common/query';
import ExternalVehicleCatalogField from '../ExternalVehicleCatalogField.jsx';

vi.mock('../../../../../common/hooks/index.js', () => ({
    usePermissions: () => ({ hasPermission: () => true, hasAnyPermission: () => true }),
}));

vi.mock('../../externalHiring/ExternalVehicleFormModal.jsx', () => ({
    default: ({ initialValues, onSaved }) => (
        <div>
            <span>Placa inicial: {initialValues.plate}</span>
            <button onClick={() => onSaved({ id: 'e9', provider: 'Proveedor Nuevo', plate: initialValues.plate, capacity: 20, available: true })}>
                Guardar vehículo externo
            </button>
        </div>
    ),
}));

const OPTIONS = [
    { id: 'e1', label: 'HB4380', provider: 'Anca', capacity: 13, available: true },
    { id: 'e2', label: 'GB3177', provider: 'Transvanu', capacity: 33, available: false },
];

afterEach(cleanup);

function renderField(props = {}, queryClient = new QueryClient()) {
    return render(
        <AppTheme defaultColorScheme="light">
            <QueryClientProvider client={queryClient}>
                <ExternalVehicleCatalogField
                    provider=""
                    plate=""
                    options={OPTIONS}
                    loading={false}
                    loaded
                    isEdit
                    onChange={vi.fn()}
                    {...props}
                />
            </QueryClientProvider>
        </AppTheme>,
    );
}

describe('ExternalVehicleCatalogField', () => {
    it('ofrece solo las placas disponibles con su proveedor y completa el proveedor al elegir una', async () => {
        const onChange = vi.fn();
        renderField({ onChange });

        await userEvent.click(screen.getByRole('combobox', { name: 'Placa' }));

        expect(screen.getByText('Anca · 13 pasajeros')).toBeInTheDocument();
        expect(screen.queryByText('GB3177')).not.toBeInTheDocument();

        await userEvent.click(screen.getByText('HB4380'));
        expect(onChange).toHaveBeenLastCalledWith({ driver: 'Anca', vehicle: 'HB4380' });
    });

    it('permite buscar la placa por el nombre del proveedor', async () => {
        renderField();

        await userEvent.type(screen.getByRole('combobox', { name: 'Placa' }), 'anca');

        expect(screen.getByText('Anca · 13 pasajeros')).toBeInTheDocument();
    });

    it('advierte de una placa que no está en Contratación Externa y permite agregarla', async () => {
        const queryClient = new QueryClient();
        queryClient.setQueryData(queryKeys.transport.externalVehicleOptions(), OPTIONS);
        const onChange = vi.fn();
        renderField({ plate: 'ZZ9999', onChange }, queryClient);

        expect(screen.getByText('Esta placa no se encuentra en Contratación Externa, agrégala antes de continuar')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Agregar vehículo externo al sistema' }));
        expect(screen.getByText('Placa inicial: ZZ9999')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Guardar vehículo externo' }));
        expect(onChange).toHaveBeenLastCalledWith({ driver: 'Proveedor Nuevo', vehicle: 'ZZ9999' });
        expect(queryClient.getQueryData(queryKeys.transport.externalVehicleOptions())).toContainEqual(
            expect.objectContaining({ id: 'e9', label: 'ZZ9999', provider: 'Proveedor Nuevo' }),
        );
    });

    it('no marca como faltante una placa registrada aunque esté no disponible, y avisa su estado', () => {
        renderField({ provider: 'Transvanu', plate: 'GB3177' });

        expect(screen.getByText('No disponible')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Agregar vehículo externo al sistema' })).not.toBeInTheDocument();
    });

    it('usa un solo campo y muestra el proveedor como texto de apoyo', () => {
        renderField({ provider: 'Anca', plate: 'HB4380' });

        expect(screen.getAllByRole('combobox')).toHaveLength(1);
        expect(screen.queryByLabelText('Proveedor')).not.toBeInTheDocument();
        expect(screen.getByText('Proveedor: Anca')).toBeInTheDocument();
    });

    it('no advierte nada mientras el catálogo no se ha cargado', () => {
        renderField({ plate: 'ZZ9999', loaded: false });

        expect(screen.queryByRole('button', { name: 'Agregar vehículo externo al sistema' })).not.toBeInTheDocument();
    });

    it('muestra proveedor y placa solo de lectura fuera de edición', () => {
        renderField({ provider: 'Anca', plate: 'HB4380', isEdit: false });

        expect(screen.getByLabelText('Placa')).toHaveValue('HB4380');
        expect(screen.getByText('Proveedor: Anca')).toBeInTheDocument();
        expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    });
});
