import { useContext } from 'react';
import { Sidebar } from '../common/components/Sidebar';
import { SidebarContext } from '../common/context/SidebarContext';
import { Box } from '@mui/material';

export function DashboardLayout({ children }) {
  const { isMinimized } = useContext(SidebarContext);
  const marginLeft = isMinimized ? '80px' : '280px';

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar />
      <Box
        component="main"
        sx={{
          flex: 1,
          marginLeft: marginLeft,
          display: 'flex',
          flexDirection: 'column',
          backgroundColor: '#f8f9fa',
          transition: 'margin-left 0.3s ease-in-out',
          '@media (max-width: 768px)': {
            marginLeft: 0,
          },
        }}
      >
        {children}
      </Box>
    </Box>
  );
}

export default DashboardLayout;
