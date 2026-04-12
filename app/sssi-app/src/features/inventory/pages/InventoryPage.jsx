import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Container,
  Card,
  CardContent,
  CardActions,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import StorageIcon from '@mui/icons-material/Storage';
import { alpha } from '@mui/material/styles';
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
              <Card
                sx={(theme) => ({
                  width: { xs: '100%', sm: '90%', md: '400px' },
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  transition: 'box-shadow 0.3s ease',
                  '&:hover': {
                    boxShadow: `0 8px 16px ${alpha(theme.palette.primary.main, 0.15)}`,
                  },
                })}
              >
                <CardContent sx={{ flex: 1, textAlign: 'center' }}>
                  <StorageIcon sx={{ fontSize: { xs: 50, sm: 60 }, color: 'primary.main', mb: 2 }} />
                  <Typography
                    variant="h6"
                    sx={{ fontWeight: 'bold', mb: 1, color: 'text.primary' }}
                  >
                    Gestión de Activos
                  </Typography>
                  <Typography
                    variant="body2"
                    sx={{ color: 'text.secondary', fontSize: { xs: '0.85rem', sm: '0.875rem' } }}
                  >
                    Administra todos los activos de la organización
                  </Typography>
                </CardContent>
                <CardActions sx={{ justifyContent: 'center', pb: 2 }}>
                  <Button
                    variant="contained"
                    sx={{
                      backgroundColor: 'primary.main',
                      '&:hover': { backgroundColor: 'primary.dark' },
                      textTransform: 'none',
                      fontWeight: 600,
                    }}
                    onClick={() => navigate('/inventario/activos')}
                  >
                    Ir a Activos
                  </Button>
                </CardActions>
              </Card>
            </Box>
          </Box>
        </Container>
    </Box>
  );
}

export default InventoryPage;