import { useContext, useMemo } from 'react';
import { AuthContext } from '../context/AuthContext';

export function usePermissions() {
  const { permissions = [] } = useContext(AuthContext);

  const normalizedPermissions = useMemo(
    () => new Set((permissions ?? []).filter(Boolean)),
    [permissions]
  );

  const hasPermission = (permission) => normalizedPermissions.has(permission);

  const hasAnyPermission = (permissionList = []) =>
    permissionList.some((permission) => normalizedPermissions.has(permission));

  const hasAllPermissions = (permissionList = []) =>
    permissionList.every((permission) => normalizedPermissions.has(permission));

  return {
    permissions: Array.from(normalizedPermissions),
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
  };
}