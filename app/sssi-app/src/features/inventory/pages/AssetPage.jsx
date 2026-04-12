import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  AppBar,
  Toolbar,
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
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Collapse,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import MenuIcon from '@mui/icons-material/Menu';
import CloseIcon from '@mui/icons-material/Close';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import WarehouseIcon from '@mui/icons-material/Warehouse';
import PeopleIcon from '@mui/icons-material/People';
import VerifiedUserIcon from '@mui/icons-material/VerifiedUser';
import AppsIcon from '@mui/icons-material/Apps';
import { alpha } from '@mui/material/styles';
import { FilterAccordion } from '../../../common/components/FilterAccordion';
import '../css/AssetPage.css';

const mockAssets = [
  { id: 1, name: 'Laptop Dell', category: 'Electrónica', status: 'Activo' },
  { id: 2, name: 'Escritorio Madera', category: 'Mueble', status: 'Activo' },
  { id: 3, name: 'Monitor Samsung', category: 'Electrónica', status: 'Inactivo' },
];

export function AssetPage() {
  const navigate = useNavigate();
  const [assets] = useState(mockAssets);
  const [filters, setFilters] = useState({
    nameFilter: '',
    categoryFilter: '',
  });
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [expandedMenu, setExpandedMenu] = useState(null);
  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

  const handleLogout = () => {
    navigate('/login');
  };

  const toggleDrawer = (open) => (event) => {
    if (event.type === 'keydown' && (event.key === 'Tab' || event.key === 'Shift')) {
      return;
    }
    setDrawerOpen(open);
  };

  const toggleMenu = (menu) => {
    setExpandedMenu(expandedMenu === menu ? null : menu);
  };

  const handleNavigation = (path) => {
    navigate(path);
    setDrawerOpen(false);
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
      categoryFilter: '',
    });
  };

  const handleSearch = () => {
    console.log('Buscando con filtros:', filters);
  };

  return (
    <Box className="asset-page">
        <AppBar position="static" sx={{ backgroundColor: 'primary.main' }}>
          <Toolbar>
            {isMediumOrDown && (
              <IconButton
                edge="start"
                color="inherit"
                onClick={toggleDrawer(true)}
                sx={{ mr: 1 }}
              >
                <MenuIcon />
              </IconButton>
            )}
            <Typography
              variant="h5"
              sx={{
                flexGrow: 1,
                fontWeight: 700,
                fontSize: { xs: '1.2rem', sm: '1.5rem' },
                letterSpacing: '0.5px',
                textShadow: '0 2px 4px rgba(0, 0, 0, 0.2)',
                color: 'primary.contrastText',
              }}
            >
              Gestión de Activos
            </Typography>
            <IconButton color="inherit" onClick={handleLogout}>
              <LogoutIcon />
            </IconButton>
          </Toolbar>
        </AppBar>

        <Drawer anchor="left" open={drawerOpen} onClose={toggleDrawer(false)}>
          <Box
            sx={{ width: 280, backgroundColor: 'background.paper', height: '100%', display: 'flex', flexDirection: 'column' }}
            role="presentation"
          >
            <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid rgba(0,0,0,0.1)' }}>
              <img src="/logo_una.png" alt="Logo UNA" style={{ maxWidth: '50%', height: 'auto', maxHeight: '50px' }} />
              <IconButton onClick={toggleDrawer(false)} sx={{ color: 'primary.main' }}>
                <CloseIcon />
              </IconButton>
            </Box>
            <List sx={{ flex: 1, overflowY: 'auto', p: 0 }}>
              <ListItem disablePadding sx={{ display: 'block' }}>
                <ListItemButton
                  onClick={() => toggleMenu('inventory')}
                  sx={(theme) => ({ borderBottom: '1px solid rgba(0,0,0,0.1)', color: 'text.primary', '&:hover': { backgroundColor: alpha(theme.palette.primary.main, 0.05) } })}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                      <WarehouseIcon sx={{ mr: 2, fontSize: 20 }} />
                      <ListItemText primary="Gestión Inventarios" />
                    </Box>
                    {expandedMenu === 'inventory' ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                  </Box>
                </ListItemButton>
                <Collapse in={expandedMenu === 'inventory'} timeout="auto" unmountOnExit>
                  <Box sx={(theme) => ({ backgroundColor: alpha(theme.palette.primary.main, 0.05) })}>
                    <Box
                      onClick={() => handleNavigation('/inventario/activos')}
                      sx={(theme) => ({
                        pl: 4,
                        color: 'text.primary',
                        '&:hover': { backgroundColor: alpha(theme.palette.primary.main, 0.1) },
                        display: 'flex',
                        alignItems: 'center',
                        width: '100%',
                        py: 1.5,
                        cursor: 'pointer',
                      })}
                    >
                      <AppsIcon sx={{ mr: 1.5, fontSize: 20 }} />
                      <ListItemText primary="Activos" />
                    </Box>
                  </Box>
                </Collapse>
              </ListItem>
              <ListItem disablePadding sx={{ display: 'block' }}>
                <ListItemButton
                  onClick={() => toggleMenu('security')}
                  sx={(theme) => ({ borderBottom: '1px solid rgba(0,0,0,0.1)', color: 'text.primary', '&:hover': { backgroundColor: alpha(theme.palette.primary.main, 0.05) } })}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                      <VerifiedUserIcon sx={{ mr: 2, fontSize: 20 }} />
                      <ListItemText primary="Gestión Seguridad" />
                    </Box>
                    {expandedMenu === 'security' ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                  </Box>
                </ListItemButton>
                <Collapse in={expandedMenu === 'security'} timeout="auto" unmountOnExit>
                  <Box sx={(theme) => ({ backgroundColor: alpha(theme.palette.primary.main, 0.05) })}>
                    <Box
                      onClick={() => handleNavigation('/seguridad/usuarios')}
                      sx={(theme) => ({
                        pl: 4,
                        color: 'text.primary',
                        '&:hover': { backgroundColor: alpha(theme.palette.primary.main, 0.1) },
                        display: 'flex',
                        alignItems: 'center',
                        width: '100%',
                        py: 1.5,
                        cursor: 'pointer',
                      })}
                    >
                      <PeopleIcon sx={{ mr: 1.5, fontSize: 20 }} />
                      <ListItemText primary="Usuarios" />
                    </Box>
                    <Box
                      onClick={() => handleNavigation('/seguridad/roles')}
                      sx={(theme) => ({
                        pl: 4,
                        color: 'text.primary',
                        '&:hover': { backgroundColor: alpha(theme.palette.primary.main, 0.1) },
                        display: 'flex',
                        alignItems: 'center',
                        width: '100%',
                        py: 1.5,
                        cursor: 'pointer',
                      })}
                    >
                      <VerifiedUserIcon sx={{ mr: 1.5, fontSize: 20 }} />
                      <ListItemText primary="Roles" />
                    </Box>
                  </Box>
                </Collapse>
              </ListItem>
            </List>
            <Box sx={{ p: 2, borderTop: '1px solid rgba(0,0,0,0.1)' }}>
              <Button
                fullWidth
                variant="contained"
                startIcon={<LogoutIcon />}
                onClick={() => { handleLogout(); setDrawerOpen(false); }}
                sx={{ backgroundColor: 'primary.main', color: 'primary.contrastText', '&:hover': { backgroundColor: 'primary.dark' }, fontWeight: 600 }}
              >
                Cerrar Sesión
              </Button>
            </Box>
          </Box>
        </Drawer>

        <Container maxWidth="lg" className="asset-content">
          <Box sx={{ mb: 3 }}>
            <Typography
              variant="h5"
              sx={{
                fontWeight: 700,
                color: 'primary.main',
                fontSize: '1.8rem',
                letterSpacing: '0.3px',
              }}
            >
              Lista de Activos
            </Typography>
          </Box>

          <Box className="asset-header-section" sx={{ display: 'flex', gap: 2, alignItems: 'flex-start', mb: 2, flexDirection: { xs: 'column', sm: 'row' }, width: '100%' }}>
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
                label="Categoría"
                size="small"
                value={filters.categoryFilter}
                onChange={(e) => handleFilterChange('categoryFilter', e.target.value)}
                placeholder="Buscar por categoría"
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
            className="asset-table-container"
            sx={{ backgroundColor: 'background.paper', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)' }}
          >
            <Table sx={{ '& .MuiTableCell-body': { borderColor: 'grey.300' } }}>
              <TableHead sx={{ backgroundColor: 'background.paper' }}>
                <TableRow>
                  <TableCell sx={{ fontWeight: 'bold', borderBottom: '2px solid', borderBottomColor: 'primary.main' }}>Nombre</TableCell>
                  <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid', borderBottomColor: 'primary.main' }}>
                    Categoría
                  </TableCell>
                  <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid', borderBottomColor: 'primary.main' }}>
                    Estado
                  </TableCell>
                  <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid', borderBottomColor: 'primary.main' }}>
                    Acciones
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {assets.map(asset => (
                  <TableRow key={asset.id} hover>
                    <TableCell>{asset.name}</TableCell>
                    <TableCell align="center">{asset.category}</TableCell>
                    <TableCell align="center">{asset.status}</TableCell>
                    <TableCell align="center">
                      <IconButton size="small" sx={{ color: 'primary.main' }}>
                        <EditIcon />
                      </IconButton>
                      <IconButton size="small" sx={{ color: 'error.main' }}>
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

export default AssetPage;