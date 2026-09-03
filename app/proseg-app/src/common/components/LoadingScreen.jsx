import { Box, CircularProgress } from '@mui/material';

export function LoadingScreen() {
    return (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh' }}>
            <CircularProgress />
        </Box>
    );
}

export default LoadingScreen;
