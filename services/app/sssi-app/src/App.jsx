import './App.css';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from './common/theme';
import { SidebarProvider } from './common/context/SidebarContext';
import LoginPage from './features/auth/pages/LoginPage';
import InventoryPage from './features/inventory/pages/InventoryPage';
import SecurityPage from './features/security/pages/SecurityPage';
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
            <Route
              path="/inventory"
              element={
                <DashboardLayout>
                  <InventoryPage />
                </DashboardLayout>
              }
            />
            <Route
              path="/security"
              element={
                <DashboardLayout>
                  <SecurityPage />
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
