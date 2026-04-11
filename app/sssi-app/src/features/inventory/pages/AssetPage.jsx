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
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import '../css/AssetPage.css';

const mockAssets = [
  { id: 1, name: 'Laptop Dell', category: 'Electrónica', status: 'Activo' },
  { id: 2, name: 'Escritorio Madera', category: 'Mueble', status: 'Activo' },
  { id: 3, name: 'Monitor Samsung', category: 'Electrónica', status: 'Inactivo' },
];

export function AssetPage() {
  const navigate = useNavigate();
  const [assets] = useState(mockAssets);

  const handleLogout = () => {
    navigate('/login');
  };

  return (
    <Box className="asset-page">
      <AppBar position="static" sx={{ backgroundColor: '#C41E3A' }}>
        <Toolbar>
          <Typography 
            variant="h5" 
            sx={{ 
              flexGrow: 1, 
              fontWeight: 700,
              fontSize: '1.5rem',
              letterSpacing: '0.5px',
              textShadow: '0 2px 4px rgba(0, 0, 0, 0.2)',
            }}
          >
            Gestión de Activos
          </Typography>
          <IconButton color="inherit" onClick={handleLogout}>
            <LogoutIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" className="asset-content">
        <Box className="asset-header-section">
          <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
            Lista de Activos
          </Typography>
          <Button 
            variant="contained" 
            startIcon={<AddIcon />}
            sx={{ backgroundColor: '#C41E3A', textTransform: 'none' }}
          >
            Agregar
          </Button>
        </Box>

        <TableContainer component={Paper} className="asset-table-container">
          <Table>
            <TableHead sx={{ backgroundColor: '#f5f5f5' }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>Nombre</TableCell>
                <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>
                  Categoría
                </TableCell>
                <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>
                  Estado
                </TableCell>
                <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>
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
                    <IconButton size="small" sx={{ color: '#C41E3A' }}>
                      <EditIcon />
                    </IconButton>
                    <IconButton size="small" sx={{ color: '#d32f2f' }}>
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
