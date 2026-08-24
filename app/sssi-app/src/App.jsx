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
import MaintenancePage from './features/maintenance/pages/MaintenancePage.jsx';
import DashboardLayout from './layouts/DashboardLayout';
import CompaniesPage from "./features/maintenance/pages/company/CompaniesPage.jsx";
import RequestsPage from "./features/maintenance/pages/request/RequestsPage.jsx";
import MaintenanceRegisterPage from "./features/maintenance/pages/register/RegisterPage.jsx";
import CampusPage from './features/locations/pages/CampusPage';
import BuildingPage from './features/locations/pages/BuildingPage';
import LocationPage from './features/locations/pages/LocationPage';
import EmailPage from './features/locations/pages/EmailPage';
import TicketsPage from './features/maintenance/pages/TicketsPage.jsx';
import TransportPage from './features/transport/pages/TransportPage.jsx';
import DriversPage from './features/transport/pages/drivers/DriversPage.jsx';
import VehiclesPage from './features/transport/pages/vehicles/VehiclesPage.jsx';
import VehicleMaintenancePage from './features/transport/pages/maintenance/VehicleMaintenancePage.jsx';
import ToursPage from './features/transport/pages/tours/ToursPage.jsx';
import AssignmentPage from './features/transport/pages/assignment/AssignmentPage.jsx';
import CleaningPage from './features/transport/pages/cleaning/CleaningPage.jsx';

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
                                                <DashboardLayout title="Inicio" pageTitle="Inicio">
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
                                                <DashboardLayout title="Gestión de Activos" pageTitle="Activos">
                                                    <AssetPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route path="/ubicaciones" element={<Navigate to="/ubicaciones/campus" replace />} />
                                <Route
                                    path="/ubicaciones/campus"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Gestión de Campus" pageTitle="Campus">
                                                    <CampusPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/ubicaciones/edificios"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Gestión de Edificios" pageTitle="Edificios">
                                                    <BuildingPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/ubicaciones/locaciones"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Gestión de Detalles de Ubicación" pageTitle="Detalles de Ubicación">
                                                    <LocationPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/ubicaciones/correos"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Gestión de Correos" pageTitle="Correos">
                                                    <EmailPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/mantenimiento"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Gestión de Mantenimiento" pageTitle="Mantenimiento">
                                                    <MaintenancePage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/mantenimiento/tecnicos"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Gestión de Técnicos" pageTitle="Técnicos">
                                                    <MaintenancePage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/transporte"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Gestión de Transporte" pageTitle="Transporte">
                                                    <TransportPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/transporte/depuracion"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Depuración de Giras" pageTitle="Depuración">
                                                    <CleaningPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/transporte/choferes"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Gestión de Choferes" pageTitle="Choferes">
                                                    <DriversPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/transporte/vehiculos"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Gestión de Vehículos" pageTitle="Vehículos">
                                                    <VehiclesPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/transporte/mantenimiento"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Mantenimiento de Vehículos" pageTitle="Mantenimiento">
                                                    <VehicleMaintenancePage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/transporte/giras"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Gestión de Giras" pageTitle="Giras">
                                                    <ToursPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/transporte/asignaciones"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Gestión de Asignaciones" pageTitle="Asignaciones">
                                                    <AssignmentPage />
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
                                                <DashboardLayout title="Gestión de Seguridad" pageTitle="Seguridad">
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
                                                <DashboardLayout title="Gestión de Usuarios" pageTitle="Usuarios">
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
                                                <DashboardLayout title="Gestión de Roles" pageTitle="Roles">
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
                                                <DashboardLayout title="Gestión de Empresas" pageTitle="Empresas">
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
                                                <DashboardLayout title="Gestión de Solicitudes" pageTitle="Solicitudes">
                                                    <RequestsPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/mantenimiento/registros"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Gestión de Registros" pageTitle="Registros">
                                                    <MaintenanceRegisterPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/mantenimiento/tickets"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Gestión de Tickets" pageTitle="Tickets">
                                                    <TicketsPage />
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