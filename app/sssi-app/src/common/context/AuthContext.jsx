import { createContext, useEffect, useState } from 'react';
import axios from 'axios';
import { getCurrentUser } from '../../features/auth/services/authService';

export const AuthContext = createContext({
  isAuthenticated: false,
  user: null,
  loading: true,
  setIsAuthenticated: () => {},
  logout: () => {},
});

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const verifyAuth = async () => {
      try {
        const apiResp = await getCurrentUser();
        const userData = apiResp?.data;
        if (apiResp && apiResp.status >= 200 && apiResp.status < 300 && userData) {
          setUser(userData);
          setIsAuthenticated(true);
        } else {
          setUser(null);
          setIsAuthenticated(false);
        }
      } catch (error) {
        setUser(null);
        setIsAuthenticated(false);
      } finally {
        setLoading(false);
      }
    };

    verifyAuth();
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
    <AuthContext.Provider value={{ isAuthenticated, user, loading, setIsAuthenticated, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
