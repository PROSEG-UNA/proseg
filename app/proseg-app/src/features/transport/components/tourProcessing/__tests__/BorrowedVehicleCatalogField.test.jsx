import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AppTheme from '../../../../../common/theme/AppTheme';
import { queryKeys } from '../../../../../common/query';
import BorrowedVehicleCatalogField from '../BorrowedVehicleCatalogField.jsx';

vi.mock('../../vehicles/BorrowedVehicleFormModal.jsx', () => ({
    default: ({ initialValues, onSaved }) => (
        <div>
            <span>Placa inicial: {initialValues.plate}</span>
            <button onClick={() => onSaved({ id: 'b2', plate: initialValues.plate, capacity: 40 })}>Guardar vehículo</button>
        </div>
    ),
}));

afterEach(cleanup);

describe('BorrowedVehicleCatalogField', () => {
    it('advierte y permite agregar un vehículo prestado escrito en la gira', async () => {
        const queryClient = new QueryClient();
        queryClient.setQueryData(queryKeys.transport.borrowedVehicleOptions(), [{ id: 'b1', label: 'PRE-123' }]);
        const onChange = vi.fn();
        render(
            <AppTheme defaultColorScheme="light">
                <QueryClientProvider client={queryClient}>
                    <BorrowedVehicleCatalogField value="" options={[{ id: 'b1', label: 'PRE-123' }]} loading={false} onChange={onChange} />
                </QueryClientProvider>
            </AppTheme>,
        );

        await userEvent.type(screen.getByRole('combobox', { name: 'Vehículo prestado' }), 'No_Existe');

        expect(screen.getByText('Este vehículo prestado no se encuentra en el sistema, agrégalo antes de continuar')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Agregar vehículo al sistema' })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Seleccionar otro vehículo' })).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Agregar vehículo al sistema' }));
        expect(screen.getByText('Placa inicial: No_Existe')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', { name: 'Guardar vehículo' }));
        expect(onChange).toHaveBeenLastCalledWith('b2');
        expect(queryClient.getQueryData(queryKeys.transport.borrowedVehicleOptions())).toContainEqual({ id: 'b2', label: 'No_Existe', capacity: 40 });
    });

    it('muestra la placa de la gira aunque el vehículo aún no tenga una selección', () => {
        render(
            <AppTheme defaultColorScheme="light">
                <QueryClientProvider client={new QueryClient()}>
                    <BorrowedVehicleCatalogField value="" plate="No_Existe" options={[]} loading={false} onChange={vi.fn()} />
                </QueryClientProvider>
            </AppTheme>,
        );

        expect(screen.getByRole('combobox', { name: 'Vehículo prestado' })).toHaveValue('No_Existe');
        expect(screen.getByRole('button', { name: 'Agregar vehículo al sistema' })).toBeInTheDocument();
    });
});
