import BusinessIcon from '@mui/icons-material/Business';
import ApartmentIcon from '@mui/icons-material/Apartment';
import PlaceIcon from '@mui/icons-material/Place';
import { LOCATION_ENDPOINTS } from '../../services/endpoints';

export const LOCATION_CONFIG = {
    campus: {
        title: 'Campus',
        pluralTitle: 'Campus',
        baseUrl: LOCATION_ENDPOINTS.campuses,
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
        baseUrl: LOCATION_ENDPOINTS.buildings,
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
                optionsUrl: LOCATION_ENDPOINTS.campuses,
                getOptionLabel: (opt) => opt.name,
                getOptionValue: (opt) => opt.id,
                getInitialValue: (row) => row?.campus?.id ?? '',
            },
        ],
    },
    location: {
        title: 'Detalle de Ubicación',
        pluralTitle: 'Detalles de Ubicación',
        baseUrl: LOCATION_ENDPOINTS.locations,
        icon: PlaceIcon,
        columnToBackendKey: {
            campus: 'floor.building.campus.name',
            building: 'floor.building.name',
            floor: 'floor.name',
            description: 'description',
        },
        columns: [
            { accessorKey: 'description', header: 'Detalle de Ubicación', size: 180, grow: 2 },
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
                optionsUrl: LOCATION_ENDPOINTS.campuses,
                getOptionLabel: (opt) => opt.name,
                getOptionValue: (opt) => opt.id,
                getInitialValue: (row) => row?.floor?.building?.campus?.id ?? '',
            },
            {
                key: 'buildingId',
                label: 'Edificio',
                type: 'select',
                required: true,
                optionsUrl: LOCATION_ENDPOINTS.buildings,
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
                min: 1,
                required: true,
                getInitialValue: (row) => row?.floor?.name ? parseInt(row.floor.name) : '',
            },
            { key: 'description', label: 'Detalle de Ubicación', type: 'textarea', required: true },
        ],
    },
};
