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
import '../css/UserPage.css';

const mockUsers = [
  { id: 1, name: 'Juan Pérez', email: 'juan@example.com', role: 'Administrador' },
  { id: 2, name: 'María García', email: 'maria@example.com', role: 'Usuario' },
  { id: 3, name: 'Carlos López', email: 'carlos@example.com', role: 'Usuario' },
];

export function UserPage() {
  const navigate = useNavigate();
  const [users] = useState(mockUsers);

  const handleLogout = () => {
    navigate('/login');
  };

  return (
    <Box className="user-page">
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
            Gestión de Usuarios
          </Typography>
          <IconButton color="inherit" onClick={handleLogout}>
            <LogoutIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" className="user-content">
        <Box className="user-header-section">
          <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
            Lista de Usuarios
          </Typography>
          <Button 
            variant="contained" 
            startIcon={<AddIcon />}
            sx={{ backgroundColor: '#C41E3A', textTransform: 'none' }}
          >
            Agregar
          </Button>
        </Box>

        <TableContainer component={Paper} className="user-table-container">
          <Table>
            <TableHead sx={{ backgroundColor: '#f5f5f5' }}>
              <TableRow>
                <TableCell sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>Nombre</TableCell>
                <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>
                  Email
                </TableCell>
                <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>
                  Rol
                </TableCell>
                <TableCell align="center" sx={{ fontWeight: 'bold', borderBottom: '2px solid #C41E3A' }}>
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

export default UserPage;
