import { useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Button,
  Container,
  Card,
  CardContent,
  CardActions,
} from '@mui/material';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import { Header } from '../../../common/components/Header';
import '../css/SecurityPage.css';

export function SecurityPage() {
  const navigate = useNavigate();

  const handleLogout = () => {
    navigate('/login');
  };

  return (
    <Box className="security-page">
        <Header
          title="Gestión de Seguridad"
          navButtons={[{ label: 'Inventario', onClick: () => navigate('/inventario/activos') }]}
          onLogout={handleLogout}
        />

        <Container maxWidth="lg" className="security-content">
          <Box sx={{ py: 4, display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
            <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 3 }}>
              Bienvenido a Gestión de Seguridad
            </Typography>
            <Typography variant="body1" sx={{ color: 'text.secondary', mb: 4, maxWidth: '600px' }}>
              Administra usuarios, roles y permisos de tu organización.
            </Typography>

            <Box sx={{ display: 'flex', justifyContent: 'center', gap: 3, flexWrap: 'wrap' }}>
              <Card sx={{ width: { xs: '100%', sm: '45%', md: '400px' }, display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flex: 1, textAlign: 'center' }}>
                  <PeopleIcon sx={{ fontSize: 60, color: 'primary.main', mb: 2 }} />
                  <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1 }}>
                    Gestión de Usuarios
                  </Typography>
                  <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                    Administra los usuarios de la organización
                  </Typography>
                </CardContent>
                <CardActions sx={{ justifyContent: 'center' }}>
                  <Button
                    variant="contained"
                    sx={{ backgroundColor: 'primary.main' }}
                    onClick={() => navigate('/seguridad/usuarios')}
                  >
                    Ir a Usuarios
                  </Button>
                </CardActions>
              </Card>

              <Card sx={{ width: { xs: '100%', sm: '45%', md: '400px' }, display: 'flex', flexDirection: 'column' }}>
                <CardContent sx={{ flex: 1, textAlign: 'center' }}>
                  <VerifiedUserIcon sx={{ fontSize: 60, color: 'primary.main', mb: 2 }} />
                  <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 1 }}>
                    Gestión de Roles
                  </Typography>
                  <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                    Administra los roles y permisos del sistema
                  </Typography>
                </CardContent>
                <CardActions sx={{ justifyContent: 'center' }}>
                  <Button
                    variant="contained"
                    sx={{ backgroundColor: 'primary.main' }}
                    onClick={() => navigate('/seguridad/roles')}
                  >
                    Ir a Roles
                  </Button>
                </CardActions>
              </Card>
            </Box>
          </Box>
        </Container>
    </Box>
  );
}

export default SecurityPage;