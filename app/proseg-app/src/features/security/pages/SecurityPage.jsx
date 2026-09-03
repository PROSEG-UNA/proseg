import { useNavigate } from 'react-router-dom';
import { Box, Typography, Container } from '@mui/material';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import { FeatureCard } from '../../../common/components/FeatureCard';
import '../css/SecurityPage.css';

export function SecurityPage() {
  const navigate = useNavigate();

  return (
    <Box className="security-page">


      <Container maxWidth="lg" className="security-content">
          <Box sx={{ py: 4, display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
            <Typography variant="h4" sx={{ fontWeight: 'bold', mb: 3 }}>
              Bienvenido a Gestión de Seguridad
            </Typography>
            <Typography variant="body1" sx={{ color: 'text.secondary', mb: 4, maxWidth: '600px' }}>
              Administra usuarios, roles y permisos de tu organización.
            </Typography>

            <Box sx={{ display: 'flex', justifyContent: 'center', gap: 3, flexWrap: 'wrap' }}>
              <FeatureCard
                icon={<PeopleIcon fontSize="inherit" />}
                title="Gestión de Usuarios"
                description="Administra los usuarios de la organización"
                buttonLabel="Ir a Usuarios"
                onNavigate={() => navigate('/seguridad/usuarios')}
              />
              <FeatureCard
                icon={<VerifiedUserIcon fontSize="inherit" />}
                title="Gestión de Roles"
                description="Administra los roles y permisos del sistema"
                buttonLabel="Ir a Roles"
                onNavigate={() => navigate('/seguridad/roles')}
              />
            </Box>
          </Box>
        </Container>
    </Box>
  );
}

export default SecurityPage;