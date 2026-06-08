import { useState, useContext } from 'react';
import { Sidebar, NavDrawer } from '../common/components/Sidebar/Sidebar';
import { Header } from '../common/components/Header';
import { SidebarContext } from '../common/context/SidebarContext';
import { Box, useMediaQuery, useTheme } from '@mui/material';

export function DashboardLayout({ children, title }) {
  const { isMinimized } = useContext(SidebarContext);
  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));
  const [drawerOpen, setDrawerOpen] = useState(false);

  const marginLeft = !isMediumOrDown ? (isMinimized ? '80px' : '280px') : '0px';

  return (
    <Box sx={{ display: 'flex', minHeight: '100dvh' }}>
      {!isMediumOrDown && <Sidebar />}
      <NavDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} />
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          flex: 1,
          marginLeft: marginLeft,
          transition: 'margin-left 0.3s ease-in-out',
          minWidth: 0,
        }}
      >
        <Header
          title={title}
          onMenuClick={isMediumOrDown ? () => setDrawerOpen(true) : undefined}
        />
        <Box
          component="main"
          sx={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            backgroundColor: 'transparent',
            overflow: 'visible',
          }}
        >
          {children}
        </Box>
      </Box>
    </Box>
  );
}

export default DashboardLayout;
