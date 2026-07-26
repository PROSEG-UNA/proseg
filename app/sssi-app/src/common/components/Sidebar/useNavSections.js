import { useMemo } from 'react';
import WarehouseIcon from '@mui/icons-material/Warehouse';
import ShieldIcon from '@mui/icons-material/Shield';
import BuildIcon from '@mui/icons-material/Build';
import AppsIcon from '@mui/icons-material/Apps';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import BusinessIcon from '@mui/icons-material/Business';
import ConstructionIcon from '@mui/icons-material/Construction';
import AssignmentIcon from '@mui/icons-material/Assignment';
import PushPinIcon from '@mui/icons-material/PushPin';
import PlaceIcon from '@mui/icons-material/Place';
import ApartmentIcon from '@mui/icons-material/Apartment';
import EmailIcon from '@mui/icons-material/Email';
import ConfirmationNumberIcon from '@mui/icons-material/ConfirmationNumber';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import DirectionsCarFilledOutlinedIcon from '@mui/icons-material/DirectionsCarFilledOutlined';
import BuildCircleOutlinedIcon from '@mui/icons-material/BuildCircleOutlined';
import AltRouteOutlinedIcon from '@mui/icons-material/AltRouteOutlined';
import AssignmentTurnedInOutlinedIcon from '@mui/icons-material/AssignmentTurnedInOutlined';
import CleaningServicesOutlinedIcon from '@mui/icons-material/CleaningServicesOutlined';
import { PERMISSIONS } from '../../constants/permissions';
import { usePermissions } from '../../hooks';

const userPermissions = [
    PERMISSIONS.USERS.CREATE,
    PERMISSIONS.USERS.READ,
    PERMISSIONS.USERS.READ_ALL,
    PERMISSIONS.USERS.READ_ROLES,
    PERMISSIONS.USERS.APPROVE,
    PERMISSIONS.USERS.ASSIGN_ROLE,
    PERMISSIONS.USERS.REMOVE_ROLE,
    PERMISSIONS.USERS.READ_INVITATIONS,
    PERMISSIONS.ROLES.READ_USERS_BY_ROLE,
];

const rolePermissions = [
    PERMISSIONS.ROLES.READ_BASE,
    PERMISSIONS.ROLES.READ_COMPOSITE,
    PERMISSIONS.ROLES.READ_ROLE_COMPOSITES,
    PERMISSIONS.ROLES.CREATE,
    PERMISSIONS.ROLES.UPDATE,
    PERMISSIONS.ROLES.DELETE,
    PERMISSIONS.ROLES.READ_USERS_BY_ROLE,
];

const inventoryPermissions = [
    PERMISSIONS.INVENTORY.READ,
    PERMISSIONS.INVENTORY.MANAGE,
    PERMISSIONS.INVENTORY.DELETE,
    PERMISSIONS.INVENTORY.LOCATIONS.READ,
    PERMISSIONS.INVENTORY.LOCATIONS.MANAGE,
    PERMISSIONS.INVENTORY.LOCATIONS.DELETE,
];

const locationsPermissions = [
    PERMISSIONS.INVENTORY.LOCATIONS.READ,
    PERMISSIONS.INVENTORY.LOCATIONS.MANAGE,
    PERMISSIONS.INVENTORY.LOCATIONS.DELETE,
    PERMISSIONS.INVENTORY.CATALOG.READ,
    PERMISSIONS.INVENTORY.CATALOG.MANAGE,
    PERMISSIONS.INVENTORY.CATALOG.DELETE,
];

const maintenancePermissions = [
    PERMISSIONS.MAINTENANCE.COMPANIES.READ,
    PERMISSIONS.MAINTENANCE.COMPANIES.MANAGE,
    PERMISSIONS.MAINTENANCE.COMPANIES.DELETE,
    PERMISSIONS.MAINTENANCE.REQUESTS.READ,
    PERMISSIONS.MAINTENANCE.REQUESTS.UPDATE,
    PERMISSIONS.MAINTENANCE.REQUESTS.DELETE,
    PERMISSIONS.MAINTENANCE.COMPANY_USERS.READ,
    PERMISSIONS.MAINTENANCE.COMPANY_USERS.MANAGE,
    PERMISSIONS.MAINTENANCE.COMPANY_USERS.DELETE,
    PERMISSIONS.MAINTENANCE.TICKETS.READ,
    PERMISSIONS.MAINTENANCE.TICKETS.CREATE,
    PERMISSIONS.MAINTENANCE.TICKETS.EDIT,
    PERMISSIONS.MAINTENANCE.TICKETS.SET_PRIORITY,
    PERMISSIONS.MAINTENANCE.TICKETS.VIEW_ALL,
    PERMISSIONS.MAINTENANCE.TICKETS.COMMENT,
    PERMISSIONS.MAINTENANCE.TICKETS.DELETE,
];

const ticketPermissions = [
    PERMISSIONS.MAINTENANCE.TICKETS.READ,
    PERMISSIONS.MAINTENANCE.TICKETS.CREATE,
    PERMISSIONS.MAINTENANCE.TICKETS.EDIT,
    PERMISSIONS.MAINTENANCE.TICKETS.SET_PRIORITY,
    PERMISSIONS.MAINTENANCE.TICKETS.VIEW_ALL,
    PERMISSIONS.MAINTENANCE.TICKETS.COMMENT,
    PERMISSIONS.MAINTENANCE.TICKETS.DELETE,
];

const registerPermissions = [
    PERMISSIONS.MAINTENANCE.REGISTERS.READ,
    PERMISSIONS.MAINTENANCE.REGISTERS.MANAGE,
    PERMISSIONS.MAINTENANCE.REGISTERS.HISTORY,
];

const transportDriverPermissions = [
    PERMISSIONS.TRANSPORT.DRIVERS.READ,
    PERMISSIONS.TRANSPORT.DRIVERS.MANAGE,
    PERMISSIONS.TRANSPORT.DRIVERS.DELETE,
];

const transportVehiclePermissions = [
    PERMISSIONS.TRANSPORT.VEHICLES.READ,
    PERMISSIONS.TRANSPORT.VEHICLES.MANAGE,
    PERMISSIONS.TRANSPORT.VEHICLES.DELETE,
];

const transportMaintenancePermissions = [
    PERMISSIONS.TRANSPORT.MAINTENANCE.READ,
    PERMISSIONS.TRANSPORT.MAINTENANCE.MANAGE,
    PERMISSIONS.TRANSPORT.MAINTENANCE.DELETE,
];

const transportTourPermissions = [
    PERMISSIONS.TRANSPORT.TOURS.READ,
    PERMISSIONS.TRANSPORT.TOURS.MANAGE,
    PERMISSIONS.TRANSPORT.TOURS.DELETE,
];

const transportAssignmentPermissions = [
    PERMISSIONS.TRANSPORT.ASSIGNMENT.GENERATE,
    PERMISSIONS.TRANSPORT.ASSIGNMENT.UPDATE,
];

export function useNavSections() {
    const { hasAnyPermission } = usePermissions();

    const canViewInventorySection = hasAnyPermission(inventoryPermissions);
    const canViewLocationsSection = hasAnyPermission(locationsPermissions);
    const canViewUsersSubmodule = hasAnyPermission(userPermissions);
    const canViewRolesSubmodule = hasAnyPermission(rolePermissions);
    const canViewSecuritySection = canViewUsersSubmodule || canViewRolesSubmodule;
    const canViewMaintenanceSection = hasAnyPermission(maintenancePermissions);
    const canViewRegistersSubmodule = hasAnyPermission(registerPermissions);
    const canViewTicketsSubmodule = hasAnyPermission(ticketPermissions);
    const canViewDriversSubmodule = hasAnyPermission(transportDriverPermissions);
    const canViewVehiclesSubmodule = hasAnyPermission(transportVehiclePermissions);
    const canViewTransportMaintenanceSubmodule = hasAnyPermission(transportMaintenancePermissions);
    const canViewToursSubmodule = hasAnyPermission(transportTourPermissions);
    const canViewAssignmentSubmodule = hasAnyPermission(transportAssignmentPermissions);
    const canViewCleaningSubmodule = canViewToursSubmodule || canViewAssignmentSubmodule;
    const canViewTransportSection = canViewCleaningSubmodule || canViewDriversSubmodule || canViewVehiclesSubmodule || canViewTransportMaintenanceSubmodule || canViewToursSubmodule || canViewAssignmentSubmodule;

    const sections = useMemo(() => {
        const result = [];

        if (canViewInventorySection) {
            result.push({
                key: 'inventory',
                icon: WarehouseIcon,
                label: 'Inventario',
                items: [
                    { key: 'assets', icon: AppsIcon, label: 'Activos', path: '/inventario/activos' },
                ],
            });
        }

        if (canViewLocationsSection) {
            result.push({
                key: 'locations',
                icon: PushPinIcon,
                label: 'Ubicaciones',
                items: [
                    { key: 'campus', icon: BusinessIcon, label: 'Campus', path: '/ubicaciones/campus' },
                    { key: 'buildings', icon: ApartmentIcon, label: 'Edificios', path: '/ubicaciones/edificios' },
                    { key: 'locations', icon: PlaceIcon, label: 'Locaciones', path: '/ubicaciones/locaciones' },
                    { key: 'emails', icon: EmailIcon, label: 'Correos', path: '/ubicaciones/correos' },
                ],
            });
        }

        if (canViewMaintenanceSection) {
            result.push({
                key: 'maintenance',
                icon: BuildIcon,
                label: 'Mantenimiento',
                items: [
                    { key: 'companies', icon: BusinessIcon, label: 'Empresas', path: '/mantenimiento/empresas' },
                    { key: 'requests', icon: ConstructionIcon, label: 'Solicitud de mantenimiento', path: '/mantenimiento/solicitudes' },
                    canViewRegistersSubmodule ? { key: 'registers', icon: AssignmentIcon, label: 'Registro de mantenimiento', path: '/mantenimiento/registros' } : null,
                    canViewTicketsSubmodule ? { key: 'tickets', icon: ConfirmationNumberIcon, label: 'Tickets', path: '/mantenimiento/tickets' } : null,
                ].filter(Boolean),
            });
        }

        if (canViewTransportSection) {
            result.push({
                key: 'transport',
                icon: LocalShippingIcon,
                label: 'Transporte',
                items: [
                    canViewCleaningSubmodule ? { key: 'cleaning', icon: CleaningServicesOutlinedIcon, label: 'Depuración', path: '/transporte/depuracion' } : null,
                    canViewDriversSubmodule ? { key: 'drivers', icon: BadgeOutlinedIcon, label: 'Choferes', path: '/transporte/choferes' } : null,
                    canViewVehiclesSubmodule ? { key: 'vehicles', icon: DirectionsCarFilledOutlinedIcon, label: 'Vehículos', path: '/transporte/vehiculos' } : null,
                    canViewTransportMaintenanceSubmodule ? { key: 'transport-maintenance', icon: BuildCircleOutlinedIcon, label: 'Mantenimiento', path: '/transporte/mantenimiento' } : null,
                    canViewToursSubmodule ? { key: 'tours', icon: AltRouteOutlinedIcon, label: 'Giras', path: '/transporte/giras' } : null,
                    canViewAssignmentSubmodule ? { key: 'assignment', icon: AssignmentTurnedInOutlinedIcon, label: 'Asignaciones', path: '/transporte/asignaciones' } : null,
                ].filter(Boolean),
            });
        }

        if (canViewSecuritySection) {
            result.push({
                key: 'security',
                icon: ShieldIcon,
                label: 'Seguridad',
                items: [
                    canViewUsersSubmodule ? { key: 'users', icon: PeopleIcon, label: 'Usuarios', path: '/seguridad/usuarios' } : null,
                    canViewRolesSubmodule ? { key: 'roles', icon: VerifiedUserIcon, label: 'Roles', path: '/seguridad/roles' } : null,
                ].filter(Boolean),
            });
        }

        return result;
    }, [canViewInventorySection, canViewLocationsSection, canViewSecuritySection, canViewUsersSubmodule, canViewRolesSubmodule, canViewMaintenanceSection, canViewRegistersSubmodule, canViewTicketsSubmodule, canViewCleaningSubmodule, canViewDriversSubmodule, canViewVehiclesSubmodule, canViewTransportMaintenanceSubmodule, canViewToursSubmodule, canViewAssignmentSubmodule, canViewTransportSection]);

    return { sections };
}

export default useNavSections;
