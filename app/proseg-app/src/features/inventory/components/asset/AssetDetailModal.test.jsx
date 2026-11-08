import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { useQuery } from '@tanstack/react-query';
import AppTheme from '../../../../common/theme/AppTheme.jsx';
import AssetDetailModal from './AssetDetailModal.jsx';

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...await importOriginal(),
    useQuery: vi.fn(),
}));

vi.mock('./CoordinateMapPicker.jsx', () => ({
    default: ({ disabled }) => <div aria-label="Mapa del activo" data-disabled={disabled} />,
}));

const asset = {
    assetNumber: 'A-310',
    serialNumber: 'SERIE-310',
    model: {
        type: { name: 'Computadora' },
        brand: { name: 'Marca de prueba' },
        name: 'Modelo de prueba',
    },
    location: {
        description: 'Oficina 12',
        floor: {
            name: '2',
            building: {
                name: 'Edificio Central',
                campus: { name: 'Campus Principal' },
            },
        },
    },
    status: 'APROBADO',
    acquisitionDate: '2025-01-15',
    warrantyEndDate: null,
    firmwareSupportEndDate: null,
    createdAt: '2025-01-15T10:00:00Z',
    updatedAt: '2025-02-15T10:00:00Z',
    latitude: 9.935,
    longitude: -84.08,
    executingUnit: { name: 'Tecnología' },
    employee: { name: 'Persona de prueba', identification: 'ID-310' },
    networkInterface: { ipAddress: '192.168.1.10', macAddress: 'AA:BB:CC:DD:EE:FF' },
};

const loadedQuery = {
    data: {
        asset,
        archives: [{ id: 'image-1', imageUrl: '/asset.png', caption: 'Etiqueta' }],
        components: [{
            id: 'component-1',
            name: 'Memoria',
            quantity: 2,
            location: 'Interior',
            observations: 'Componente de prueba',
        }],
        networkInterface: asset.networkInterface,
    },
    isPending: false,
    error: null,
    refetch: vi.fn(),
};

function renderModal(props = {}) {
    return render(
        <AppTheme>
            <AssetDetailModal
                open
                assetId="asset-310"
                onClose={vi.fn()}
                {...props}
            />
        </AppTheme>,
    );
}

describe('AssetDetailModal', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        useQuery.mockReturnValue(loadedQuery);
    });

    it('shows asset fields and extra information using read-only controls', () => {
        renderModal();

        expect(screen.getByRole('textbox', { name: 'Número de activo' })).toHaveValue('A-310');
        expect(screen.getByRole('textbox', { name: 'Número de serie' })).toHaveValue('SERIE-310');
        expect(screen.getByRole('textbox', { name: 'Marca' })).toHaveValue('Marca de prueba');
        expect(screen.getByRole('textbox', { name: 'Campus' })).toHaveValue('Campus Principal');
        expect(screen.getByRole('textbox', { name: 'Fecha de adquisición' })).toHaveValue('15/01/2025');
        expect(screen.getByRole('textbox', { name: 'Dirección IP' })).toHaveValue('192.168.1.10');
        expect(screen.getByText('Componente de prueba')).toBeInTheDocument();
        expect(screen.getByAltText('Etiqueta')).toHaveAttribute('src', '/asset.png');
        expect(screen.getByLabelText('Mapa del activo')).toHaveAttribute('data-disabled', 'true');

        screen.getAllByRole('textbox').forEach((input) => {
            expect(input).toHaveAttribute('readonly');
        });
        expect(screen.queryByRole('button', { name: /guardar|crear/i })).not.toBeInTheDocument();
    });

    it('uses the same section headings as the edit form and shows an edit transition only when permitted', () => {
        const onEdit = vi.fn();
        renderModal({ canEdit: true, onEdit });

        ['Identificación', 'Clasificación', 'Ubicación', 'Estado', 'IP y MAC', 'Fechas', 'Coordenadas', 'Responsable', 'Componentes Asociados', 'Fotos']
            .forEach((title) => expect(screen.getAllByText(title).length).toBeGreaterThan(0));
        fireEvent.click(screen.getByRole('button', { name: 'Editar activo' }));
        expect(onEdit).toHaveBeenCalledOnce();

        expect(screen.getByRole('button', { name: 'Cerrar' })).toBeInTheDocument();
    });

    it('does not expose the edit transition without permission', () => {
        renderModal({ canEdit: false, onEdit: vi.fn() });

        expect(screen.queryByRole('button', { name: 'Editar activo' })).not.toBeInTheDocument();
    });

    it('shows loading, error with retry, and empty-data states', () => {
        useQuery.mockReturnValue({ ...loadedQuery, data: undefined, isPending: true });
        const { rerender } = renderModal();
        expect(document.querySelectorAll('.MuiSkeleton-root')).not.toHaveLength(0);

        useQuery.mockReturnValue({ ...loadedQuery, data: undefined, error: new Error('load failed') });
        rerender(
            <AppTheme>
                <AssetDetailModal open assetId="asset-310" onClose={vi.fn()} />
            </AppTheme>,
        );
        expect(screen.getByText('No se pudo cargar el detalle del activo.')).toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
        expect(loadedQuery.refetch).toHaveBeenCalledOnce();

        useQuery.mockReturnValue({ ...loadedQuery, data: { asset: null }, error: null });
        rerender(
            <AppTheme>
                <AssetDetailModal open assetId="asset-310" onClose={vi.fn()} />
            </AppTheme>,
        );
        expect(screen.getByText('No se encontró información para este activo.')).toBeInTheDocument();
    });
});
