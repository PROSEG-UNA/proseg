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
import AssetExpirationPage from './features/inventory/pages/AssetExpirationPage';
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
import TicketsDashboard from './features/maintenance/pages/TicketsDashboard.jsx';
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
                                                <DashboardLayout title="Inicio" pageTitle="Inicio" description="Consulta el resumen de servicios y el acceso a los módulos institucionales.">
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
                                                <DashboardLayout title="Gestión de Activos" pageTitle="Activos" description="Registra, consulta y actualiza los activos institucionales.">
                                                    <AssetPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />
                                <Route
                                    path="/inventario/vencimientos"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Panel de Vencimientos" pageTitle="Panel de vencimientos" description="Revisa vencimientos de garantías y soporte de firmware de los activos.">
                                                    <AssetExpirationPage />
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
                                                <DashboardLayout title="Gestión de Campus" pageTitle="Campus" description="Administra los campus y sedes de la universidad.">
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
                                                <DashboardLayout title="Gestión de Edificios" pageTitle="Edificios" description="Registra y consulta los edificios de cada campus.">
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
                                                <DashboardLayout title="Gestión de Detalles de Ubicación" pageTitle="Detalles de Ubicación" description="Define espacios y detalles de ubicación dentro de los edificios.">
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
                                                <DashboardLayout title="Gestión de Correos" pageTitle="Correos" description="Gestiona los correos asociados a los edificios.">
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
                                                <DashboardLayout title="Gestión de Tickets" pageTitle="Mantenimiento" description="Administra empresas y solicitudes del mantenimiento institucional.">
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
                                                <DashboardLayout title="Gestión de Técnicos" pageTitle="Técnicos" description="Administra los técnicos del mantenimiento institucional.">
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
                                                <DashboardLayout title="Gestión de Transporte" pageTitle="Transporte" description="Consulta choferes, vehículos, giras y asignaciones del transporte.">
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
                                                <DashboardLayout title="Depuración de Giras" pageTitle="Depuración" description="Depura giras y consulta el historial de depuración del transporte.">
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
                                                <DashboardLayout title="Gestión de Choferes" pageTitle="Choferes" description="Registra y consulta los choferes del transporte institucional.">
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
                                                <DashboardLayout title="Gestión de Vehículos" pageTitle="Vehículos" description="Administra el parque de vehículos institucionales.">
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
                                                <DashboardLayout title="Mantenimiento de Vehículos" pageTitle="Mantenimiento de vehículos" description="Registra y da seguimiento al mantenimiento de los vehículos.">
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
                                                <DashboardLayout title="Gestión de Giras" pageTitle="Giras" description="Planifica y consulta las giras de transporte.">
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
                                                <DashboardLayout title="Gestión de Asignaciones" pageTitle="Asignaciones" description="Asigna vehículos y choferes a las giras de transporte.">
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
                                                <DashboardLayout title="Gestión de Seguridad" pageTitle="Seguridad" description="Accede a la administración de usuarios, roles y permisos.">
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
                                                <DashboardLayout title="Gestión de Usuarios" pageTitle="Usuarios" description="Crea y administra los usuarios de la organización.">
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
                                                <DashboardLayout title="Gestión de Roles" pageTitle="Roles" description="Define roles y permisos de acceso al sistema.">
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
                                                <DashboardLayout title="Gestión de Empresas" pageTitle="Empresas" description="Registra las empresas proveedoras de mantenimiento.">
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
                                                <DashboardLayout title="Gestión de Solicitudes" pageTitle="Solicitudes" description="Crea y consulta las solicitudes de mantenimiento.">
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
                                                <DashboardLayout title="Gestión de Registros" pageTitle="Registros" description="Consulta el historial de registros de mantenimiento.">
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
                                                <DashboardLayout title="Gestión de Tickets" pageTitle="Tickets" description="Crea, consulta y da seguimiento a los tickets de mantenimiento.">
                                                    <TicketsPage />
                                                </DashboardLayout>
                                            }
                                        />
                                    }
                                />

                                <Route
                                    path="/mantenimiento/dashboard"
                                    element={
                                        <ProtectedRoute
                                            element={
                                                <DashboardLayout title="Dashboard de Tickets" pageTitle="Dashboard" description="Visualiza indicadores y el estado de los tickets de mantenimiento.">
                                                    <TicketsDashboard />
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