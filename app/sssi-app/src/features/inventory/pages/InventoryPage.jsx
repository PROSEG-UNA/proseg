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
import '../css/InventoryPage.css';

const mockItems = [
  { id: 1, name: 'Producto A', quantity: 50, price: 100 },
  { id: 2, name: 'Producto B', quantity: 30, price: 200 },
  { id: 3, name: 'Producto C', quantity: 20, price: 150 },
];

export function InventoryPage() {
  const navigate = useNavigate();
  const [items] = useState(mockItems);

  const handleLogout = () => {
    navigate('/login');
  };

  return (
    <Box className="inventory-page">
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
            Gestión de Inventario
          </Typography>
          <Button 
            color="inherit" 
            onClick={() => navigate('/security')}
            sx={{ mr: 2, textTransform: 'none', fontSize: '1rem' }}
          >
            Seguridad
          </Button>
          <IconButton color="inherit" onClick={handleLogout}>
            <LogoutIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" className="inventory-content">
        <Box className="inventory-header-section">
          <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
            Lista de productos
          </Typography>
          <Button 
            variant="contained" 
            startIcon={<AddIcon />}
            sx={{ backgroundColor: '#C41E3A', textTransform: 'none' }}
          >
            Agregar
          </Button>
        </Box>

        <TableContainer component={Paper} className="inventory-table-container">
          <Table>
            <TableHead sx={{ backgroundColor: '#f5f5f5' }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>Nombre</TableCell>
                <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>
                  Cantidad
                </TableCell>
                <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>
                  Precio
                </TableCell>
                <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>
                  Acciones
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map(item => (
                <TableRow key={item.id} hover>
                  <TableCell>{item.name}</TableCell>
                  <TableCell align="center">{item.quantity}</TableCell>
                  <TableCell align="center">${item.price}</TableCell>
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

export default InventoryPage;
