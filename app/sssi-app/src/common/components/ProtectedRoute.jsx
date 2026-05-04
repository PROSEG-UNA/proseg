import { useContext } from 'react';
import { Navigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { CircularProgress, Box } from '@mui/material';

export function ProtectedRoute({ element }) {
  const { isAuthenticated, loading } = useContext(AuthContext);

  if (loading) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '100vh',
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  return isAuthenticated ? element : <Navigate to="/login" replace />;
}

export function PublicRoute({ element }) {
  const { isAuthenticated, loading } = useContext(AuthContext);

  if (loading) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '100vh',
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  return !isAuthenticated ? element : <Navigate to="/home" replace />;
}
