import { useState, useContext } from 'react';
import { Sidebar, NavDrawer } from '../common/components/Sidebar';
import { Header } from '../common/components/Header';
import { SidebarContext } from '../common/context/SidebarContext';
import { Box, useMediaQuery, useTheme } from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { APP_CONFIG } from '../config/appConfig.js';

export function DashboardLayout({ children, title, pageTitle }) {
  const { isMinimized } = useContext(SidebarContext);
  const theme = useTheme();
  const isMediumOrDown = useMediaQuery(theme.breakpoints.down('md'));
  const [drawerOpen, setDrawerOpen] = useState(false);
  const resolvedPageTitle = pageTitle || title || 'Inicio';

  const marginLeft = !isMediumOrDown ? (isMinimized ? '80px' : '280px') : '0px';

  return (
    <Box sx={{ display: 'flex', minHeight: '100dvh' }}>
      <Helmet>
        <title>{`${resolvedPageTitle} - ${APP_CONFIG.name}`}</title>
      </Helmet>
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
