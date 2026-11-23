import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MenuItem, TextField } from '@mui/material';
import AppTheme from '../../../../common/theme/AppTheme.jsx';
import AssetFormModal from './AssetFormModal.jsx';
import {
    checkAssetNumber,
    createAsset,
    fetchAssetById,
    updateAsset,
} from '../../services/assetsService.js';
import { fetchAssetArchives } from '../../services/assetArchiveService.js';
import { fetchAssetComponents } from '../../services/assetComponentsService.js';

vi.mock('../../hooks/useAssetCatalogOptions.js', () => ({
    useAssetCatalogOptions: () => ({
        options: {
            brands: [{ id: 'brand-1', name: 'Marca de prueba' }],
            types: [{ id: 'type-1', name: 'Computadora', requiresNetworkInterface: false }],
            models: [{ id: 'model-1', name: 'Modelo de prueba', brand: { id: 'brand-1' }, type: { id: 'type-1' } }],
            campuses: [{ id: 'campus-1', name: 'Campus Principal' }],
            buildings: [{ id: 'building-1', name: '-', campus: { id: 'campus-1' } }],
            locations: [{
                id: 'location-1',
                description: '-',
                floor: { name: '1', building: { id: 'building-1', campus: { id: 'campus-1' } } },
            }],
            executingUnits: [],
            employees: [],
        },
        upsertOption: vi.fn(),
        loadingOptions: false,
    }),
}));

vi.mock('../../../../common/hooks/usePermissions', () => ({
    usePermissions: () => ({ hasPermission: () => true }),
}));

vi.mock('../../../../common/components/SearchableSelect.jsx', () => ({
    default: ({ label, value, onChange, items, getItemLabel, getItemValue }) => (
        <TextField select label={label} value={value ?? ''} onChange={(event) => onChange(event.target.value)} fullWidth size="small">
            {items.map((item) => (
                <MenuItem key={getItemValue(item)} value={getItemValue(item)}>{getItemLabel(item)}</MenuItem>
            ))}
        </TextField>
    ),
}));

vi.mock('@mui/x-date-pickers/DatePicker', () => ({
    DatePicker: ({ label }) => <TextField label={label} value="" fullWidth size="small" />,
}));

vi.mock('./CoordinateMapPicker.jsx', () => ({
    default: () => <div aria-label="Selector de coordenadas" />,
}));

vi.mock('../../services/assetsService.js', () => ({
    checkAssetNumber: vi.fn(),
    createAsset: vi.fn(),
    fetchAssetById: vi.fn(),
    fetchLastKnownNetworkInterface: vi.fn(),
    updateAsset: vi.fn(),
}));

vi.mock('../../services/assetArchiveService.js', () => ({
    deleteArchive: vi.fn(),
    fetchAssetArchives: vi.fn(),
    registerArchive: vi.fn(),
    uploadPhoto: vi.fn(),
}));

vi.mock('../../services/assetComponentsService.js', () => ({
    createAssetComponent: vi.fn(),
    deleteAssetComponent: vi.fn(),
    fetchAssetComponents: vi.fn(),
    updateAssetComponent: vi.fn(),
}));

vi.mock('../../services/catalogService.js', () => ({
    createCatalogItem: vi.fn(),
}));

const existingAsset = {
    id: 'asset-1',
    assetNumber: 'A-1',
    serialNumber: 'SERIE-1',
    status: 'APROBADO',
    acquisitionDate: null,
    warrantyEndDate: null,
    firmwareSupportEndDate: null,
    decommissionDate: null,
    latitude: null,
    longitude: null,
    model: {
        id: 'model-1',
        name: 'Modelo de prueba',
        brand: { id: 'brand-1', name: 'Marca de prueba' },
        type: { id: 'type-1', name: 'Computadora', requiresNetworkInterface: false },
    },
    location: {
        id: 'location-1',
        description: '-',
        floor: {
            name: '1',
            building: {
                id: 'building-1',
                name: '-',
                campus: { id: 'campus-1', name: 'Campus Principal' },
            },
        },
    },
};

function renderForm(props = {}) {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const onClose = vi.fn();
    const onSaved = vi.fn();
    render(
        <AppTheme>
            <QueryClientProvider client={queryClient}>
                <AssetFormModal open onClose={onClose} onSaved={onSaved} {...props} />
            </QueryClientProvider>
        </AppTheme>,
    );
    return { onClose, onSaved };
}

describe('AssetFormModal', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        checkAssetNumber.mockResolvedValue(false);
        createAsset.mockResolvedValue({ id: 'asset-created' });
        updateAsset.mockResolvedValue({});
        fetchAssetById.mockResolvedValue(existingAsset);
        fetchAssetArchives.mockResolvedValue([]);
        fetchAssetComponents.mockResolvedValue([]);
    });

    it('keeps the shared section structure editable and validates required fields before saving', async () => {
        const { onClose, onSaved } = renderForm();

        ['Identificación', 'Clasificación', 'Ubicación', 'Estado', 'Fechas', 'Coordenadas', 'Responsable', 'Componentes asociados', 'Fotos']
            .forEach((title) => expect(screen.getAllByText(title).length).toBeGreaterThan(0));
        const serialNumber = screen.getByRole('textbox', { name: 'Número de serie' });
        expect(serialNumber).not.toHaveAttribute('readonly');
        fireEvent.change(serialNumber, { target: { value: 'SERIE-NUEVA' } });
        expect(serialNumber).toHaveValue('SERIE-NUEVA');

        fireEvent.click(screen.getByRole('button', { name: 'Crear Activo' }));
        expect(await screen.findByText('Revisa los datos antes de continuar')).toBeInTheDocument();
        expect(createAsset).not.toHaveBeenCalled();
        expect(onSaved).not.toHaveBeenCalled();
        expect(onClose).not.toHaveBeenCalled();

        fireEvent.click(screen.getByRole('button', { name: 'Aceptar' }));
        fireEvent.click(await screen.findByRole('button', { name: 'Cancelar' }));
        expect(onClose).toHaveBeenCalledOnce();
    });

    it('loads the asset for editing and saves changes through the existing update flow', async () => {
        const { onClose, onSaved } = renderForm({ assetId: 'asset-1' });

        const serialNumber = await screen.findByRole('textbox', { name: 'Número de serie' });
        await waitFor(() => expect(serialNumber).toHaveValue('SERIE-1'));
        fireEvent.change(serialNumber, { target: { value: 'SERIE-ACTUALIZADA' } });
        fireEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

        await waitFor(() => expect(updateAsset).toHaveBeenCalledWith(
            'asset-1',
            expect.objectContaining({ serialNumber: 'SERIE-ACTUALIZADA' }),
        ));
        expect(createAsset).not.toHaveBeenCalled();
        expect(onSaved).toHaveBeenCalledOnce();
        expect(onClose).toHaveBeenCalledOnce();
    });

    it('creates an asset with the existing required fields and catalog selections', async () => {
        const user = userEvent.setup();
        const { onClose, onSaved } = renderForm();

        fireEvent.change(screen.getByRole('textbox', { name: 'Número de activo' }), {
            target: { value: 'A-NUEVO' },
        });
        for (const [label, option] of [
            ['Marca', 'Marca de prueba'],
            ['Tipo de activo', 'Computadora'],
            ['Modelo', 'Modelo de prueba'],
            ['Campus', 'Campus Principal'],
            ['Estado', 'Aprobado'],
        ]) {
            await user.click(screen.getByRole('combobox', { name: label }));
            await user.click(await screen.findByRole('option', { name: option }));
        }
        fireEvent.click(screen.getByRole('button', { name: 'Crear Activo' }));

        await waitFor(() => expect(createAsset).toHaveBeenCalledWith(
            expect.objectContaining({
                assetNumber: 'A-NUEVO',
                modelId: 'model-1',
                locationId: 'location-1',
                status: 'APROBADO',
            }),
        ));
        expect(onSaved).toHaveBeenCalledOnce();
        expect(onClose).toHaveBeenCalledOnce();
    });
});
