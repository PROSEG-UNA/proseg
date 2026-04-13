import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
import '../css/RolePage.css';

const mockRoles = [
  { id: 1, name: 'Administrador', description: 'Acceso total al sistema', permissions: 15 },
  { id: 2, name: 'Usuario', description: 'Acceso limitado', permissions: 5 },
  { id: 3, name: 'Invitado', description: 'Solo lectura', permissions: 2 },
];

export function RolePage() {
  const navigate = useNavigate();
  const [roles] = useState(mockRoles);
  const [filters, setFilters] = useState({
    nameFilter: '',
    permissionsFilter: '',
  });
  const [drawerOpen, setDrawerOpen] = useState(false);
  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

  const handleLogout = () => {
    navigate('/login');
  };

  const handleFilterChange = (field, value) => {
    setFilters(prev => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleClearFilters = () => {
    setFilters({
      nameFilter: '',
      permissionsFilter: '',
    });
  };

  const handleSearch = () => {
    console.log('Buscando con filtros:', filters);
  };

  return (
      <Box className="role-page">
        <Header
          title="Gestión de Roles"
          onMenuClick={isMediumOrDown ? () => setDrawerOpen(true) : undefined}
          onLogout={handleLogout}
        />
        <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} onLogout={handleLogout} />

        <Container maxWidth="lg" className="role-content">
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
              Lista de Roles
            </Typography>
          </Box>

          <Box className="role-header-section" sx={{ display: 'flex', gap: 2, alignItems: 'flex-start', mb: 2, flexDirection: { xs: 'column', sm: 'row' }, width: '100%' }}>
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
                  label="Permisos"
                  size="small"
                  value={filters.permissionsFilter}
                  onChange={(e) => handleFilterChange('permissionsFilter', e.target.value)}
                  placeholder="Buscar por permisos"
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
              className="role-table-container"
              sx={{ backgroundColor: 'background.paper', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)' }}
          >
            <Table sx={{ '& .MuiTableCell-body': { borderColor: 'divider' } }}>
              <TableHead sx={{ backgroundColor: 'background.paper' }}>
                <TableRow>
                  <TableCell sx={{ fontWeight: 'bold', borderBottom: '2px solid', borderBottomColor: 'primary.main' }}>Nombre</TableCell>
                  <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid', borderBottomColor: 'primary.main' }}>
                    Descripción
                  </TableCell>
                  <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid', borderBottomColor: 'primary.main' }}>
                    Permisos
                  </TableCell>
                  <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid', borderBottomColor: 'primary.main' }}>
                    Acciones
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {roles.map(role => (
                    <TableRow key={role.id} hover>
                      <TableCell>{role.name}</TableCell>
                      <TableCell align="center">{role.description}</TableCell>
                      <TableCell align="center">{role.permissions}</TableCell>
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

export default RolePage;