import { useState } from 'react';
import {
  Box,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Container,
  TextField,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import { Header } from '../../../common/components/Header';
import { NavDrawer } from '../../../common/components/NavDrawer';
import { FilterAccordion } from '../../../common/components/FilterAccordion';
import '../css/UserPage.css';

const mockUsers = [
  { id: 1, name: 'Juan Pérez', email: 'juan@example.com', role: 'Administrador' },
  { id: 2, name: 'María García', email: 'maria@example.com', role: 'Usuario' },
  { id: 3, name: 'Carlos López', email: 'carlos@example.com', role: 'Usuario' },
];

export function UserPage() {
  const [users] = useState(mockUsers);
  const [filters, setFilters] = useState({
    nameFilter: '',
    emailFilter: '',
  });
  const [drawerOpen, setDrawerOpen] = useState(false);
  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

  const handleFilterChange = (field, value) => {
    setFilters(prev => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleClearFilters = () => {
    setFilters({
      nameFilter: '',
      emailFilter: '',
    });
  };

  const handleSearch = () => {
    console.log('Buscando con filtros:', filters);
  };

  return (
    <Box className="user-page">
        <Header
          title="Gestión de Usuarios"
          onMenuClick={isMediumOrDown ? () => setDrawerOpen(true) : undefined}
        />
        <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />

        <Container maxWidth="lg" className="user-content">
          <Box sx={{ mb: 3 }}>
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

          <Box className="user-header-section" sx={{ display: 'flex', gap: 2, alignItems: 'flex-start', mb: 2, flexDirection: { xs: 'column', sm: 'row' }, width: '100%' }}>
            <FilterAccordion title="Filtros">
              <TextField
                label="Nombre"
                size="small"
                value={filters.nameFilter}
                onChange={(e) => handleFilterChange('nameFilter', e.target.value)}
                placeholder="Buscar por nombre"
                sx={{ width: '100%' }}
              />
              <TextField
                label="Email"
                size="small"
                value={filters.emailFilter}
                onChange={(e) => handleFilterChange('emailFilter', e.target.value)}
                placeholder="Buscar por email"
                sx={{ width: '100%' }}
              />
              <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center', width: '100%', mt: 1 }}>
                <Button
                  variant="outlined"
                  size="small"
                  onClick={handleClearFilters}
                  sx={{ color: 'text.secondary', borderColor: 'divider' }}
                >
                  Limpiar
                </Button>
                <Button
                  variant="contained"
                  size="small"
                  onClick={handleSearch}
                  sx={{ backgroundColor: 'primary.main' }}
                >
                  Buscar
                </Button>
              </Box>
            </FilterAccordion>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              sx={{ backgroundColor: 'primary.main', textTransform: 'none', mt: 0 }}
            >
              Agregar
            </Button>
          </Box>

          <TableContainer
            component={Paper}
            className="user-table-container"
            sx={{ backgroundColor: 'background.paper', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)' }}
          >
            <Table sx={{ '& .MuiTableCell-body': { borderColor: 'grey.300' } }}>
              <TableHead sx={{ backgroundColor: 'background.paper' }}>
                <TableRow>
                  <TableCell sx={{ fontWeight: 'bold', borderBottom: '2px solid', borderBottomColor: 'primary.main' }}>Nombre</TableCell>
                  <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid', borderBottomColor: 'primary.main' }}>
                    Email
                  </TableCell>
                  <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid', borderBottomColor: 'primary.main' }}>
                    Rol
                  </TableCell>
                  <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid', borderBottomColor: 'primary.main' }}>
                    Acciones
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {users.map(user => (
                  <TableRow key={user.id} hover>
                    <TableCell>{user.name}</TableCell>
                    <TableCell align="center">{user.email}</TableCell>
                    <TableCell align="center">{user.role}</TableCell>
                    <TableCell align="center">
                      <IconButton size="small" sx={{ color: 'primary.icon' }}>
                        <EditIcon />
                      </IconButton>
                      <IconButton size="small" sx={{ color: 'primary.icon' }}>
                        <DeleteIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Container>
    </Box>
  );
}

export default UserPage;