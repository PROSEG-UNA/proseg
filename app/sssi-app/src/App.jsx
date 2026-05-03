import './App.css';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import CssBaseline from '@mui/material/CssBaseline';
import Box from '@mui/material/Box';
import AppTheme from './common/theme/AppTheme';
import { SidebarProvider } from './common/context/SidebarContext';
import { AuthProvider } from './common/context/AuthContext';
import { ProtectedRoute, PublicRoute } from './common/components/ProtectedRoute';
import LoginPage from './features/auth/pages/LoginPage';
import RegisterPage from './features/auth/pages/RegisterPage';
import InventoryPage from './features/inventory/pages/InventoryPage';
import AssetPage from './features/inventory/pages/AssetPage';
import SecurityPage from './features/security/pages/SecurityPage';
import UserPage from './features/security/pages/UserPage';
import RolePage from './features/security/pages/RolePage';
import DashboardLayout from './layouts/DashboardLayout';

function App() {
  return (
    <AuthProvider>
      <AppTheme defaultColorScheme="light">
        <CssBaseline />
        <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
          <SidebarProvider>
            <Router>
              <Routes>
                <Route path="/" element={<Navigate to="/login" replace />} />
                <Route path="/login" element={<PublicRoute element={<LoginPage />} />} />
                <Route path="/registro" element={<PublicRoute element={<RegisterPage />} />} />
                <Route
                  path="/home"
                  element={
                    <ProtectedRoute
                      element={
                        <DashboardLayout>
                          <InventoryPage />
                        </DashboardLayout>
                      }
                    />
                  }
                />
                <Route path="/inventario" element={<Navigate to="/home" replace />} />
                <Route
                  path="/inventario/activos"
                  element={
                    <ProtectedRoute
                      element={
                        <DashboardLayout>
                          <AssetPage />
                        </DashboardLayout>
                      }
                    />
                  }
                />
                <Route
                  path="/seguridad"
                  element={
                    <ProtectedRoute
                      element={
                        <DashboardLayout>
                          <SecurityPage />
                        </DashboardLayout>
                      }
                    />
                  }
                />
                <Route
                  path="/seguridad/usuarios"
                  element={
                    <ProtectedRoute
                      element={
                        <DashboardLayout>
                          <UserPage />
                        </DashboardLayout>
                      }
                    />
                  }
                />
                <Route
                  path="/seguridad/roles"
                  element={
                    <ProtectedRoute
                      element={
                        <DashboardLayout>
                          <RolePage />
                        </DashboardLayout>
                      }
                    />
                  }
                />
              </Routes>
            </Router>
          </SidebarProvider>
        </Box>
      </AppTheme>
    </AuthProvider>
  );
}

export default App;
