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
import '../css/AssetPage.css';

const mockAssets = [
  { id: 1, name: 'Laptop Dell', category: 'Electrónica', status: 'Activo' },
  { id: 2, name: 'Escritorio Madera', category: 'Mueble', status: 'Activo' },
  { id: 3, name: 'Monitor Samsung', category: 'Electrónica', status: 'Inactivo' },
];

export function AssetPage() {
  const [assets] = useState(mockAssets);
  const [filters, setFilters] = useState({
    nameFilter: '',
    categoryFilter: '',
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
      categoryFilter: '',
    });
  };

  const handleSearch = () => {
    console.log('Buscando con filtros:', filters);
  };

  return (
    <Box className="asset-page">
        <Header
          title="Gestión de Activos"
          onMenuClick={isMediumOrDown ? () => setDrawerOpen(true) : undefined}
        />
        <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />

        <Container maxWidth="lg" className="asset-content">
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

export default AssetPage;