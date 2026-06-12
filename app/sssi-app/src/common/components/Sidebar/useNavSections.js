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
import PlaceIcon from '@mui/icons-material/Place';
import ApartmentIcon from '@mui/icons-material/Apartment';
import EmailIcon from '@mui/icons-material/Email';
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
    PERMISSIONS.MAINTENANCE.REQUESTS.MANAGE,
    PERMISSIONS.MAINTENANCE.REQUESTS.DELETE,
    PERMISSIONS.MAINTENANCE.COMPANY_USERS.READ,
    PERMISSIONS.MAINTENANCE.COMPANY_USERS.MANAGE,
    PERMISSIONS.MAINTENANCE.COMPANY_USERS.DELETE,
];

const registerPermissions = [
    PERMISSIONS.MAINTENANCE.REGISTERS.READ,
    PERMISSIONS.MAINTENANCE.REGISTERS.MANAGE,
    PERMISSIONS.MAINTENANCE.REGISTERS.HISTORY,
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

    const sections = useMemo(() => {
        const result = [];

        if (canViewInventorySection) {
            result.push({
                key: 'inventory',
                icon: WarehouseIcon,
                label: 'Inventarios',
                items: [
                    { key: 'assets', icon: AppsIcon, label: 'Activos', path: '/inventario/activos' },
                ],
            });
        }

        if (canViewLocationsSection) {
            result.push({
                key: 'locations',
                icon: PlaceIcon,
                label: 'Ubicaciones',
                items: [
                    { key: 'campus', icon: BusinessIcon, label: 'Campus', path: '/ubicaciones/campus' },
                    { key: 'buildings', icon: ApartmentIcon, label: 'Edificios', path: '/ubicaciones/edificios' },
                    { key: 'locations', icon: PlaceIcon, label: 'Locaciones', path: '/ubicaciones/locaciones' },
                    { key: 'emails', icon: EmailIcon, label: 'Correos', path: '/ubicaciones/correos' },
                ],
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

        if (canViewMaintenanceSection) {
            result.push({
                key: 'maintenance',
                icon: BuildIcon,
                label: 'Mantenimiento',
                items: [
                    { key: 'companies', icon: BusinessIcon, label: 'Empresas', path: '/mantenimiento/empresas' },
                    { key: 'requests', icon: ConstructionIcon, label: 'Solicitudes de mantenimiento', path: '/mantenimiento/solicitudes' },
                    canViewRegistersSubmodule ? { key: 'registers', icon: AssignmentIcon, label: 'Registros de mantenimiento', path: '/mantenimiento/registros' } : null,
                ].filter(Boolean),
            });
        }

        return result;
    }, [canViewInventorySection, canViewLocationsSection, canViewSecuritySection, canViewUsersSubmodule, canViewRolesSubmodule, canViewMaintenanceSection, canViewRegistersSubmodule]);

    return { sections };
}

export default useNavSections;
