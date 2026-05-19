import { useContext, useMemo } from 'react';
import { AuthContext } from '../context/AuthContext';
import { PERMISSIONS } from '../constants/permissions';

export function usePermissions() {
  const { permissions = [] } = useContext(AuthContext);
  const isSuperAdmin = permissions.includes(PERMISSIONS.SUPER_ADMIN);

  const normalizedPermissions = useMemo(
    () => new Set((permissions ?? []).filter(Boolean)),
    [permissions]
  );

  const hasPermission = (permission) => isSuperAdmin || normalizedPermissions.has(permission);

  const hasAnyPermission = (permissionList = []) =>
    isSuperAdmin || permissionList.some((permission) => normalizedPermissions.has(permission));

  const hasAllPermissions = (permissionList = []) =>
    isSuperAdmin || permissionList.every((permission) => normalizedPermissions.has(permission));

  return {
    permissions: Array.from(normalizedPermissions),
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
  };
}