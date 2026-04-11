import { createBrowserRouter } from 'react-router-dom';
import LoginPage from '../features/auth/pages/LoginPage';
import InventoryPage from '../features/inventory/pages/InventoryPage';
import AssetPage from '../features/inventory/pages/AssetPage';
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
    path: '/inventario',
    element: <InventoryPage />,
  },
  {
    path: '/inventario/activos',
    element: <AssetPage />,
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
