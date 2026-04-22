import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Typography, Container, useMediaQuery, useTheme } from '@mui/material';
import StorageIcon from '@mui/icons-material/Storage';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import { FeatureCard } from '../../../common/components/FeatureCard';
import { Header } from '../../../common/components/Header';
import { NavDrawer } from '../../../common/components/NavDrawer';
import '../css/InventoryPage.css';

export function InventoryPage() {
  const navigate = useNavigate();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

  return (
    <Box className="inventory-page">
        <Header
          title="Sistema de Sección de Seguridad Institucional"
          onMenuClick={isMediumOrDown ? () => setDrawerOpen(true) : undefined}
        />
        <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />

        <Container maxWidth="lg" className="inventory-content">
          <Box sx={{ py: 4, display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
            <Typography
              variant="h4"
              sx={{
                fontWeight: 'bold',
                mb: 3,
                color: 'primary.icon',
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
                maxWidth: '760px',
                fontSize: { xs: '0.9rem', sm: '1rem' },
              }}
            >
              El SSSI es una plataforma institucional para digitalizar la gestión de activos y seguridad de la Universidad Nacional de Costa Rica, centralizando usuarios, roles y trazabilidad de movimientos, mantenimiento y estado de los recursos para mejorar el control operativo y la toma de decisiones.
            </Typography>

            <Box sx={{ display: 'flex', justifyContent: 'center', gap: 3, width: '100%', flexWrap: 'wrap' }}>
              <FeatureCard
                icon={<StorageIcon fontSize="inherit" />}
                title="Gestión de Activos"
                description="Administra todos los activos de la organización"
                buttonLabel="Ir a Activos"
                onNavigate={() => navigate('/inventario/activos')}
              />
              <FeatureCard
                icon={<PeopleIcon fontSize="inherit" />}
                title="Gestión de Usuarios"
                description="Administra usuarios y su estado dentro del sistema"
                buttonLabel="Ir a Usuarios"
                onNavigate={() => navigate('/seguridad/usuarios')}
              />
              <FeatureCard
                icon={<VerifiedUserIcon fontSize="inherit" />}
                title="Gestión de Roles"
                description="Administra roles y permisos de acceso"
                buttonLabel="Ir a Roles"
                onNavigate={() => navigate('/seguridad/roles')}
              />
            </Box>
          </Box>
        </Container>
    </Box>
  );
}

export default InventoryPage;