import { useNavigate } from 'react-router-dom';
import { Box, Typography, Container } from '@mui/material';
import StorageIcon from '@mui/icons-material/Storage';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import { FeatureCard } from '../../../common/components/FeatureCard.jsx';

export function HomePage() {
    const navigate = useNavigate();

    return (
        <Box sx={{ minHeight: '100dvh', display: 'flex', flexDirection: 'column' }}>

            <Container maxWidth="lg" sx={{ pt: { xs: 6, md: 9 }, pb: { xs: 4, md: 5 } }}>
                <Box sx={{ maxWidth: 580 }}>
                    <Typography
                        sx={{
                            fontWeight: 800,
                            fontSize: { xs: '2rem', sm: '2.6rem', md: '3rem' },
                            letterSpacing: '-0.03em',
                            lineHeight: 1.1,
                            color: 'text.primary',
                            mb: 2,
                        }}
                    >
                        Bienvenido al{' '}
                        <Box
                            component="span"
                            sx={(t) => ({ color: t.vars.palette.tones.rose.fg })}
                        >
                            SPSG
                        </Box>
                    </Typography>
                    <Typography
                        sx={{
                            fontSize: { xs: '0.9rem', sm: '1rem' },
                            color: 'text.secondary',
                            lineHeight: 1.75,
                            maxWidth: 520,
                        }}
                    >
                        Plataforma institucional para digitalizar la gestión de activos y seguridad
                        de la UNA, centralizando usuarios, roles y trazabilidad de movimientos para
                        mejorar el control operativo y la toma de decisiones.
                    </Typography>
                </Box>
            </Container>

            <Container maxWidth="lg" sx={{ pb: { xs: 6, md: 8 }, flex: 1 }}>
                <Box sx={{ mb: 2.5, display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <Box
                        sx={(t) => ({
                            width: 20, height: 2, borderRadius: '2px',
                            background: t.vars.palette.tones.rose.fg,
                            flexShrink: 0,
                        })}
                    />
                    <Typography
                        sx={{
                            fontSize: '0.72rem',
                            fontWeight: 800,
                            letterSpacing: '0.1em',
                            textTransform: 'uppercase',
                            color: 'text.disabled',
                        }}
                    >
                        Módulos disponibles
                    </Typography>
                </Box>

                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2.5 }}>
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
            </Container>
        </Box>
    );
}

export default HomePage;
