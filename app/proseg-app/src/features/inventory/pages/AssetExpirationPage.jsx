import { Box, Container } from '@mui/material';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import AssetDashboard from '../components/dashboard/AssetDashboard.jsx';
import { usePermissions } from '../../../common/hooks/index.js';
import { PERMISSIONS } from '../../../common/constants/permissions';

export function AssetExpirationPage() {
  const { hasPermission } = usePermissions();
  const canView = hasPermission(PERMISSIONS.INVENTORY.READ);

  return (
    <Box className="asset-page">
      <Container maxWidth="xl" className="asset-content">
        {canView ? <AssetDashboard /> : <AccessDeniedState />}
      </Container>
    </Box>
  );
}

export default AssetExpirationPage;
