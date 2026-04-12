import './App.css';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from './common/theme';
import { SidebarProvider } from './common/context/SidebarContext';
import LoginPage from './features/auth/pages/LoginPage';
import RegisterPage from './features/auth/pages/RegisterPage';
import InventoryPage from './features/inventory/pages/InventoryPage';
import AssetPage from './features/inventory/pages/AssetPage';
import UserPage from './features/security/pages/UserPage';
import RolePage from './features/security/pages/RolePage';
import DashboardLayout from './layouts/DashboardLayout';

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <SidebarProvider>
        <Router>
          <Routes>
            <Route path="/" element={<Navigate to="/login" replace />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/registro" element={<RegisterPage />} />
            <Route
              path="/inventario"
              element={
                <DashboardLayout>
                  <InventoryPage />
                </DashboardLayout>
              }
            />
            <Route
              path="/inventario/activos"
              element={
                <DashboardLayout>
                  <AssetPage />
                </DashboardLayout>
              }
            />
            <Route
              path="/seguridad/usuarios"
              element={
                <DashboardLayout>
                  <UserPage />
                </DashboardLayout>
              }
            />
            <Route
              path="/seguridad/roles"
              element={
                <DashboardLayout>
                  <RolePage />
                </DashboardLayout>
              }
            />
          </Routes>
        </Router>
      </SidebarProvider>
    </ThemeProvider>
  );
}

export default App;
