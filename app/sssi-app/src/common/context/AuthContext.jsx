import { createContext, useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { getCurrentUser } from '../../features/auth/services/authService';
import { PERMISSIONS } from '../constants/permissions';

export const AuthContext = createContext({
  isAuthenticated: false,
  user: null,
  permissions: [],
  hasPermission: () => false,
  hasAnyPermission: () => false,
  loading: true,
  setIsAuthenticated: () => {},
  refreshAuth: async () => null,
  logout: () => {},
});

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const isUserLikePayload = (value) =>
    Boolean(
      value &&
      typeof value === 'object' &&
      (Object.prototype.hasOwnProperty.call(value, 'id') ||
        Object.prototype.hasOwnProperty.call(value, 'permissions') ||
        Object.prototype.hasOwnProperty.call(value, 'authorities') ||
        Object.prototype.hasOwnProperty.call(value, 'username'))
    );

  const normalizeUserPayload = (payload) => {
    if (!payload) return null;

    let current = payload;
    let depth = 0;

    while (current && typeof current === 'object' && depth < 4) {
      if (isUserLikePayload(current)) {
        return current;
      }

      if (isUserLikePayload(current.data)) {
        return current.data;
      }

      if (!Object.prototype.hasOwnProperty.call(current, 'data')) {
        break;
      }

      current = current.data;
      depth += 1;
    }

    return isUserLikePayload(current) ? current : null;
  };

  const permissions = useMemo(() => {
    const source = user?.permissions ?? user?.authorities ?? [];
    return Array.from(new Set(source.filter(Boolean)));
  }, [user]);

  const isSuperAdmin = permissions.includes(PERMISSIONS.SUPER_ADMIN);

  const hasPermission = (permission) => isSuperAdmin || permissions.includes(permission);

  const hasAnyPermission = (permissionList = []) =>
    isSuperAdmin || permissionList.some((permission) => permissions.includes(permission));

  const refreshAuth = async () => {
    try {
      const apiResp = await getCurrentUser();
      const userData = normalizeUserPayload(apiResp);

      if (userData) {
        const resolvedPermissions = userData?.permissions ?? userData?.authorities ?? userData?.roles ?? [];
        setUser(userData);
        setIsAuthenticated(true);
        return userData;
      }

      setUser(null);
      setIsAuthenticated(false);
      return null;
    } catch (error) {
      setUser(null);
      setIsAuthenticated(false);
      return null;
    }
  };

  useEffect(() => {
    void refreshAuth().finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const responseInterceptor = axios.interceptors.response.use(
      response => response,
      error => {
        if (error.response?.status === 401) {
          setIsAuthenticated(false);
          setUser(null);
          const publicPaths = ['/login', '/forgot-password', '/reset-password'];
          if (!publicPaths.includes(window.location.pathname)) {
            window.location.href = '/login';
          }
        }
        return Promise.reject(error);
      }
    );

    return () => {
      axios.interceptors.response.eject(responseInterceptor);
    };
  }, []);

  const logout = async () => {
    setIsAuthenticated(false);
    setUser(null);

    try {
      const expire = 'Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; SameSite=Strict;';
      document.cookie = `auth_token=; ${expire}`;
      document.cookie = `refresh_token=; ${expire}`;
      try { localStorage.removeItem('auth_token'); } catch (e) {}
      try { sessionStorage.removeItem('auth_token'); } catch (e) {}
    } catch (e) {
    }
  };

  return (
    <AuthContext.Provider
      value={{ isAuthenticated, user, permissions, hasPermission, hasAnyPermission, loading, setIsAuthenticated, refreshAuth, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}
