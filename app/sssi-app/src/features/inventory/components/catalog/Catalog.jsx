import { useState } from 'react';
import { Container } from '@mui/material';
import CategoryIcon from '@mui/icons-material/Category';
import LabelIcon from '@mui/icons-material/Label';
import DevicesIcon from '@mui/icons-material/Devices';
import CatalogCard from './CatalogCard';
import CatalogGrid from './CatalogGrid';
import CatalogTableModal from './CatalogTableModal';
import { CATALOG_CONFIG } from './catalogConfig';
import { usePermissions } from '../../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../../common/constants/permissions';

export default function Catalog() {
    const [tableOpen, setTableOpen] = useState(false);
    const [tableConfig, setTableConfig] = useState(null);
    const { hasAnyPermission } = usePermissions();
    const canViewCatalog = hasAnyPermission([
        PERMISSIONS.INVENTORY.READ,
        PERMISSIONS.INVENTORY.MANAGE,
        PERMISSIONS.INVENTORY.DELETE,
    ]);

    if (!canViewCatalog) {
        return null;
    }

    const handleCatalogClick = (entityName) => {
        setTableConfig(CATALOG_CONFIG[entityName]);
        setTableOpen(true);
    };

    return (
        <>
            <Container maxWidth="xl" sx={{ pt: 1, pb: 1,  mt: 2, mb: 1}}>
                <CatalogGrid>
                    <CatalogCard
                        title="Tipos"
                        entityName="type"
                        onClick={handleCatalogClick}
                        icon={CategoryIcon}
                        tone="amber"
                        description="Categoría de activo"
                    />
                    <CatalogCard
                        title="Marcas"
                        entityName="brand"
                        onClick={handleCatalogClick}
                        icon={LabelIcon}
                        tone="amber"
                        description="Fabricantes registrados"
                    />
                    <CatalogCard
                        title="Modelos"
                        entityName="model"
                        onClick={handleCatalogClick}
                        icon={DevicesIcon}
                        tone="amber"
                        description="Modelo especifico del activo"
                    />
                </CatalogGrid>
            </Container>

            <CatalogTableModal
                open={tableOpen}
                onClose={() => setTableOpen(false)}
                config={tableConfig}
            />
        </>
    );
}
