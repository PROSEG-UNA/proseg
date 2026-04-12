import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, Container, useMediaQuery, useTheme } from '@mui/material';
import StorageIcon from '@mui/icons-material/Storage';
import { FeatureCard } from '../../../common/components/FeatureCard';
import { Header } from '../../../common/components/Header';
import { NavDrawer } from '../../../common/components/NavDrawer';
import '../css/InventoryPage.css';

export function InventoryPage() {
  const navigate = useNavigate();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

  const handleLogout = () => {
    navigate('/login');
  };

  return (
    <Box className="inventory-page">
        <Header
          title="Gestión de Inventario"
          onMenuClick={isMediumOrDown ? () => setDrawerOpen(true) : undefined}
          onLogout={handleLogout}
        />
        <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} onLogout={handleLogout} />

        <Container maxWidth="lg" className="inventory-content">
          <Box sx={{ py: 4, display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
            <Typography
              variant="h4"
              sx={{
                fontWeight: 'bold',
                mb: 3,
                color: 'primary.main',
                fontSize: { xs: '1.8rem', sm: '2.2rem' },
                letterSpacing: '0.3px',
              }}
            >
              Bienvenido
            </Typography>
            <Typography
              variant="body1"
              sx={{
                color: 'text.secondary',
                mb: 4,
                maxWidth: '600px',
                fontSize: { xs: '0.9rem', sm: '1rem' },
              }}
            >
              Selecciona una opción para comenzar a gestionar los activos de tu organización.
            </Typography>

            <Box sx={{ display: 'flex', justifyContent: 'center', width: '100%' }}>
              <FeatureCard
                icon={<StorageIcon fontSize="inherit" />}
                title="Gestión de Activos"
                description="Administra todos los activos de la organización"
                buttonLabel="Ir a Activos"
                onNavigate={() => navigate('/inventario/activos')}
              />
            </Box>
          </Box>
        </Container>
    </Box>
  );
}

export default InventoryPage;