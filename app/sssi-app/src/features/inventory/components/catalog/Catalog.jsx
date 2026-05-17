import { useState } from 'react';
import { Container } from '@mui/material';
import BusinessIcon from '@mui/icons-material/Business';
import PlaceIcon from '@mui/icons-material/Place';
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
    const { hasPermission } = usePermissions();
    const canManageCatalog = hasPermission(PERMISSIONS.INVENTORY.MANAGE);

    if (!canManageCatalog) {
        return null;
    }

    const handleCatalogClick = (entityName) => {
        setTableConfig(CATALOG_CONFIG[entityName]);
        setTableOpen(true);
    };

    return (
        <>
            <Container maxWidth="xl" sx={{ pt: 1, pb: 1,  mt: 3, mb: -1  }}>
                <CatalogGrid>
                    <CatalogCard
                        title="Sedes"
                        entityName="site"
                        onClick={handleCatalogClick}
                        icon={BusinessIcon}
                        tone="rose"
                        description="Sede o campus universitario"
                    />
                    <CatalogCard
                        title="Locaciones"
                        entityName="location"
                        onClick={handleCatalogClick}
                        icon={PlaceIcon}
                        tone="rose"
                        description="Espacio físico interno"
                    />
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