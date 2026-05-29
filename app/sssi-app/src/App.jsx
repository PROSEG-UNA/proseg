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
import ResetPasswordPage from './features/auth/pages/ResetPasswordPage';
import SetPasswordPage from './features/security/pages/SetPasswordPage';
import HomePage from './features/home/pages/HomePage.jsx';
import AssetPage from './features/inventory/pages/AssetPage';
import SecurityPage from './features/security/pages/SecurityPage';
import UserPage from './features/security/pages/UserPage';
import RolePage from './features/security/pages/RolePage';
import DashboardLayout from './layouts/DashboardLayout';
import CompaniesPage from "./features/maintenance/pages/CompaniesPage.jsx";
import RequestsPage from "./features/maintenance/pages/RequestsPage.jsx";

function App() {
    return (
        <AuthProvider>
            <AppTheme defaultColorScheme="light">
                <CssBaseline />
                <Box className="spsg-root">
                    <SidebarProvider>
                        <Router>
                            <Routes>
                                <Route path="/" element={<Navigate to="/login" replace />} />
                                <Route path="/login" element={<PublicRoute element={<LoginPage />} />} />
                                <Route path="/registro" element={<PublicRoute element={<RegisterPage />} />} />
                                <Route path="/forgot-password" element={<PublicRoute element={<ResetPasswordPage />} />} />
                                <Route path="/reset-password" element={<PublicRoute element={<ResetPasswordPage />} />} />
                                <Route path="/set-password" element={<PublicRoute element={<SetPasswordPage />} />} />
                                <Route
                                    path="/home"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout>
                                                    <HomePage />
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
                                <Route
                                    path="/mantenimiento/empresas"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout>
                                                    <CompaniesPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/mantenimiento/solicitudes"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout>
                                                    <RequestsPage />
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