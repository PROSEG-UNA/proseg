import { createBrowserRouter, Navigate } from 'react-router-dom';
import LoginPage from '../features/auth/pages/LoginPage';
import RegisterPage from '../features/auth/pages/RegisterPage';
import ResetPasswordPage from '../features/auth/pages/ResetPasswordPage';
import HomePage from '../features/home/pages/HomePage.jsx';
import AssetPage from '../features/inventory/pages/AssetPage';
import MaintenancePage from '../features/maintenance/pages/MaintenancePage.jsx';
import SecurityPage from '../features/security/pages/SecurityPage';
import UserPage from '../features/security/pages/UserPage';
import RolePage from '../features/security/pages/RolePage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <LoginPage />,
  },
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/registro',
    element: <RegisterPage />,
  },
  {
    path: '/forgot-password',
    element: <ResetPasswordPage />,
  },
  {
    path: '/reset-password',
    element: <ResetPasswordPage />,
  },
  {
    path: '/home',
    element: <HomePage />,
  },
  {
    path: '/inventario',
    element: <Navigate to="/home" replace />,
  },
  {
    path: '/inventario/activos',
    element: <AssetPage />,
  },
  {
    path: '/mantenimiento',
    element: <MaintenancePage />,
  },
  {
    path: '/seguridad',
    element: <SecurityPage />,
  },
  {
    path: '/seguridad/usuarios',
    element: <UserPage />,
  },
  {
    path: '/seguridad/roles',
    element: <RolePage />,
  },
]);
