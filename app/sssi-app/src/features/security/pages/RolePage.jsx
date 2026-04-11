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
import '../css/RolePage.css';

const mockRoles = [
  { id: 1, name: 'Administrador', description: 'Acceso total al sistema', permissions: 15 },
  { id: 2, name: 'Usuario', description: 'Acceso limitado', permissions: 5 },
  { id: 3, name: 'Invitado', description: 'Solo lectura', permissions: 2 },
];

export function RolePage() {
  const navigate = useNavigate();
  const [roles] = useState(mockRoles);

  const handleLogout = () => {
    navigate('/login');
  };

  return (
    <Box className="role-page">
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
            Gestión de Roles
          </Typography>
          <IconButton color="inherit" onClick={handleLogout}>
            <LogoutIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" className="role-content">
        <Box className="role-header-section">
          <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
            Lista de Roles
          </Typography>
          <Button 
            variant="contained" 
            startIcon={<AddIcon />}
            sx={{ backgroundColor: '#C41E3A', textTransform: 'none' }}
          >
            Agregar
          </Button>
        </Box>

        <TableContainer component={Paper} className="role-table-container">
          <Table>
            <TableHead sx={{ backgroundColor: '#f5f5f5' }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>Nombre</TableCell>
                <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>
                  Descripción
                </TableCell>
                <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>
                  Permisos
                </TableCell>
                <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>
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

export default RolePage;
