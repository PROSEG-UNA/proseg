import { useState } from 'react';
import {
  Box,
  Typography,
  Container,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { Header } from '../../../common/components/Header';
import { NavDrawer } from '../../../common/components/NavDrawer';
import UsersTable from '../components/UsersTable.jsx';
import '../css/UserPage.css';

export function UserPage() {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

  return (
    <Box className="user-page">
        <Header
          title="Gestión de Usuarios"
          onMenuClick={isMediumOrDown ? () => setDrawerOpen(true) : undefined}
        />
        <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />

        <Container maxWidth="lg" className="user-content">
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
            <Typography
              variant="h5"
              sx={{
                fontWeight: 700,
                color: 'primary.icon',
                fontSize: '1.8rem',
                letterSpacing: '0.3px',
              }}
            >
              Lista de Usuarios
            </Typography>
          </Box>

          <UsersTable />
        </Container>
    </Box>
  );
}

export default UserPage;