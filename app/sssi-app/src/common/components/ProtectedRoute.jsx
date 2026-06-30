import { useContext } from 'react';
import { Navigate, useLocation, useSearchParams } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import { resolveRedirectTarget } from '../utils';
import { CircularProgress, Box } from '@mui/material';

export function ProtectedRoute({ element }) {
  const { isAuthenticated, loading } = useContext(AuthContext);
  const location = useLocation();

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

  if (isAuthenticated) {
    return element;
  }

  const redirectTarget = encodeURIComponent(location.pathname + location.search);
  return <Navigate to={`/login?redirect=${redirectTarget}`} replace />;
}

export function PublicRoute({ element }) {
  const { isAuthenticated, loading } = useContext(AuthContext);
  const [searchParams] = useSearchParams();

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

  return !isAuthenticated
    ? element
    : <Navigate to={resolveRedirectTarget(searchParams.get('redirect'))} replace />;
}
