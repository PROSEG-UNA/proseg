import { useContext } from 'react';
import { Sidebar } from '../common/components/Sidebar';
import { SidebarContext } from '../common/context/SidebarContext';
import { Box, useMediaQuery, useTheme } from '@mui/material';

export function DashboardLayout({ children }) {
  const { isMinimized } = useContext(SidebarContext);
  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));

  const marginLeft = !isMediumOrDown ? (isMinimized ? '80px' : '280px') : '0px';

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Box sx={{ display: 'flex', flex: 1 }}>
        {!isMediumOrDown && <Sidebar />}

        <Box
          component="main"
          sx={{
            flex: 1,
            marginLeft: marginLeft,
            display: 'flex',
            flexDirection: 'column',
            backgroundColor: 'background.default',
            transition: 'margin-left 0.3s ease-in-out',
            overflow: 'auto',
          }}
        >
          {children}
        </Box>
      </Box>
    </Box>
  );
}

export default DashboardLayout;
