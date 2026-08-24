import CategoryIcon from '@mui/icons-material/Category';
import LabelIcon from '@mui/icons-material/Label';
import DevicesIcon from '@mui/icons-material/Devices';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import BadgeIcon from '@mui/icons-material/Badge';
import { INVENTORY_ENDPOINTS } from '../../services/endpoints';

export const CATALOG_CONFIG = {
    type: {
        title: 'Tipo de Activo',
        pluralTitle: 'Tipos de Activos',
        gender: 'm',
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
        gender: 'm',
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
                header: 'Tipo de Activo',
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
                label: 'Tipo de Activo',
                type: 'select',
                required: true,
                optionsUrl: INVENTORY_ENDPOINTS.types,
                getOptionLabel: (opt) => opt.name,
                getOptionValue: (opt) => opt.id,
                getInitialValue: (row) => row?.type?.id ?? '',
            },
        ],
    },
    executingUnit: {
        title: 'Unidad Ejecutora',
        pluralTitle: 'Unidades Ejecutoras',
        gender: 'f',
        baseUrl: INVENTORY_ENDPOINTS.executingUnits,
        icon: AccountBalanceIcon,
        columnToBackendKey: { name: 'name' },
        columns: [
            { accessorKey: 'name', header: 'Nombre', size: 220, grow: true },
        ],
        formFields: [
            { key: 'name', label: 'Nombre', type: 'text', required: true },
        ],
    },
    employee: {
        title: 'Funcionario',
        pluralTitle: 'Funcionarios',
        gender: 'm',
        baseUrl: INVENTORY_ENDPOINTS.employees,
        icon: BadgeIcon,
        columnToBackendKey: { name: 'name', identification: 'identification' },
        columns: [
            { accessorKey: 'name', header: 'Nombre', size: 220, grow: true },
            { accessorKey: 'identification', header: 'Identificación', size: 160, grow: false },
        ],
        formFields: [
            { key: 'name', label: 'Nombre del funcionario', type: 'text', required: true },
            { key: 'identification', label: 'Identificación', type: 'text', required: false },
        ],
    },
    employeeWithIdentification: {
        title: 'Identificación',
        pluralTitle: 'Identificaciones',
        gender: 'f',
        baseUrl: INVENTORY_ENDPOINTS.employees,
        icon: BadgeIcon,
        columnToBackendKey: { identification: 'identification' },
        columns: [],
        formFields: [
            { key: 'identification', label: 'Identificación', type: 'text', required: true },
            { key: 'name', label: 'Nombre del funcionario', type: 'text', required: true },
        ],
    },
    employeeIdentification: {
        title: 'Identificación',
        pluralTitle: 'Identificaciones',
        gender: 'f',
        baseUrl: INVENTORY_ENDPOINTS.employees,
        icon: BadgeIcon,
        columnToBackendKey: { identification: 'identification' },
        columns: [],
        formFields: [
            { key: 'identification', label: 'Identificación', type: 'text', required: true },
            { key: 'name', label: 'Nombre del funcionario', type: 'text', required: true, readOnly: true },
        ],
    },
};
