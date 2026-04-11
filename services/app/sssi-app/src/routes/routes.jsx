import { createBrowserRouter } from 'react-router-dom';
import LoginPage from '../features/auth/pages/LoginPage';
import InventoryPage from '../features/inventory/pages/InventoryPage';
import SecurityPage from '../features/security/pages/SecurityPage';

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
    path: '/inventory',
    element: <InventoryPage />,
  },
  {
    path: '/security',
    element: <SecurityPage />,
  },
]);
