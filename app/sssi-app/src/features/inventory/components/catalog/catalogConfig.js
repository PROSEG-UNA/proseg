import BusinessIcon from '@mui/icons-material/Business';
import ApartmentIcon from '@mui/icons-material/Apartment';
import PlaceIcon from '@mui/icons-material/Place';
import CategoryIcon from '@mui/icons-material/Category';
import LabelIcon from '@mui/icons-material/Label';
import DevicesIcon from '@mui/icons-material/Devices';
import { INVENTORY_ENDPOINTS } from '../../services/endpoints';

export const CATALOG_CONFIG = {
    campus: {
        title: 'Campus',
        pluralTitle: 'Campus',
        baseUrl: INVENTORY_ENDPOINTS.campuses,
        icon: BusinessIcon,
        columnToBackendKey: { name: 'name' },
        columns: [
            { accessorKey: 'name', header: 'Nombre', size: 200, grow: true },
        ],
        formFields: [
            { key: 'name', label: 'Nombre', type: 'text', required: true },
        ],
    },
    building: {
        title: 'Edificio',
        pluralTitle: 'Edificios',
        baseUrl: INVENTORY_ENDPOINTS.buildings,
        icon: ApartmentIcon,
        columnToBackendKey: { name: 'name', campus: 'campus.name' },
        columns: [
            { accessorKey: 'name', header: 'Nombre', size: 160, grow: true },
            {
                id: 'campus',
                header: 'Campus',
                accessorFn: (row) => row.campus?.name ?? '-',
                size: 140,
                grow: 1,
            },
        ],
        formFields: [
            { key: 'name', label: 'Nombre', type: 'text', required: true },
            {
                key: 'campusId',
                label: 'Campus',
                type: 'select',
                required: true,
                optionsUrl: INVENTORY_ENDPOINTS.campuses,
                getOptionLabel: (opt) => opt.name,
                getOptionValue: (opt) => opt.id,
                getInitialValue: (row) => row?.campus?.id ?? '',
            },
        ],
    },
    location: {
        title: 'Locación',
        pluralTitle: 'Locaciones',
        baseUrl: INVENTORY_ENDPOINTS.locations,
        icon: PlaceIcon,
        columnToBackendKey: {
            campus: 'floor.building.campus.name',
            building: 'floor.building.name',
            floor: 'floor.name',
            description: 'description',
        },
        columns: [
            { accessorKey: 'description', header: 'Descripción', size: 180, grow: 2 },
            {
                id: 'campus',
                header: 'Campus',
                accessorFn: (row) => row.floor?.building?.campus?.name ?? '-',
                size: 130,
                grow: 1,
            },
            {
                id: 'building',
                header: 'Edificio',
                accessorFn: (row) => row.floor?.building?.name ?? '-',
                size: 130,
                grow: 1,
            },
            {
                id: 'floor',
                header: 'Piso',
                accessorFn: (row) => row.floor?.name ?? '-',
                size: 80,
                grow: 0,
            },
        ],
        formFields: [
            {
                key: 'campusId',
                label: 'Campus',
                type: 'select',
                required: true,
                optionsUrl: INVENTORY_ENDPOINTS.campuses,
                getOptionLabel: (opt) => opt.name,
                getOptionValue: (opt) => opt.id,
                getInitialValue: (row) => row?.floor?.building?.campus?.id ?? '',
            },
            {
                key: 'buildingId',
                label: 'Edificio',
                type: 'select',
                required: true,
                optionsUrl: INVENTORY_ENDPOINTS.buildings,
                getOptionLabel: (opt) => opt.name,
                getOptionValue: (opt) => opt.id,
                getInitialValue: (row) => row?.floor?.building?.id ?? '',
                dependsOn: 'campusId',
                filterBy: (opt, campusId) => opt.campus?.id === campusId,
            },
            {
                key: 'floorNumber',
                label: 'Número de piso',
                type: 'number',
                required: true,
                getInitialValue: (row) => row?.floor?.name ? parseInt(row.floor.name) : '',
            },
            { key: 'description', label: 'Descripción', type: 'textarea', required: true },
        ],
    },
    type: {
        title: 'Tipo',
        pluralTitle: 'Tipos',
        baseUrl: INVENTORY_ENDPOINTS.types,
        icon: CategoryIcon,
        columnToBackendKey: { name: 'name', description: 'description' },
        columns: [
            { accessorKey: 'name', header: 'Nombre', size: 160, grow: true },
            { accessorKey: 'description', header: 'Descripción', size: 200, grow: 2 },
            {
                accessorKey: 'requiresNetworkInterface',
                header: 'IP y MAC',
                size: 130,
                grow: false,
                enableColumnFilter: false,
                enableSorting: false,
                Cell: ({ cell }) => (cell.getValue() ? 'Sí' : 'No'),
            },
        ],
        formFields: [
            { key: 'name', label: 'Nombre', type: 'text', required: true },
            { key: 'description', label: 'Descripción', type: 'textarea', required: false },
            {
                key: 'requiresNetworkInterface',
                label: 'Requiere IP y MAC',
                type: 'boolean',
                deleteWarning: 'Al desactivar esta opción se eliminará la IP y MAC de todos los activos asociados a este tipo. ¿Deseas continuar?',
            },
        ],
    },
    brand: {
        title: 'Marca',
        pluralTitle: 'Marcas',
        baseUrl: INVENTORY_ENDPOINTS.brands,
        icon: LabelIcon,
        columnToBackendKey: { name: 'name' },
        columns: [
            { accessorKey: 'name', header: 'Nombre', size: 200, grow: true },
        ],
        formFields: [
            { key: 'name', label: 'Nombre', type: 'text', required: true },
        ],
    },
    model: {
        title: 'Modelo',
        pluralTitle: 'Modelos',
        baseUrl: INVENTORY_ENDPOINTS.models,
        icon: DevicesIcon,
        columnToBackendKey: { name: 'name', brand: 'brand.name', type: 'type.name' },
        columns: [
            { accessorKey: 'name', header: 'Nombre', size: 160, grow: true },
            {
                id: 'brand',
                header: 'Marca',
                accessorFn: (row) => row.brand?.name ?? '-',
                size: 140,
                grow: 1,
            },
            {
                id: 'type',
                header: 'Tipo',
                accessorFn: (row) => row.type?.name ?? '-',
                size: 140,
                grow: 1,
            },
        ],
        formFields: [
            { key: 'name', label: 'Nombre', type: 'text', required: true },
            {
                key: 'brandId',
                label: 'Marca',
                type: 'select',
                required: true,
                optionsUrl: INVENTORY_ENDPOINTS.brands,
                getOptionLabel: (opt) => opt.name,
                getOptionValue: (opt) => opt.id,
                getInitialValue: (row) => row?.brand?.id ?? '',
            },
            {
                key: 'typeId',
                label: 'Tipo',
                type: 'select',
                required: true,
                optionsUrl: INVENTORY_ENDPOINTS.types,
                getOptionLabel: (opt) => opt.name,
                getOptionValue: (opt) => opt.id,
                getInitialValue: (row) => row?.type?.id ?? '',
            },
        ],
    },
};
