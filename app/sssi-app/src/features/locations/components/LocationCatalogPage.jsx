import { useState } from 'react';
import { Box, Container } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { PrimaryButton } from '../../../common/components/PrimaryButton.jsx';
import { PageHeader } from '../../../common/components/index.js';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import LocationCatalogCard from './catalog/LocationCatalogCard.jsx';
import LocationCatalogGrid from './catalog/LocationCatalogGrid.jsx';
import CatalogTableView from './catalog/CatalogTableView.jsx';
import CatalogTableModal from './catalog/CatalogTableModal.jsx';
import { useCatalogTable } from '../hooks/useCatalogTable.jsx';
import { CATALOG_CONFIG } from './catalog/catalogConfig.js';

export default function LocationCatalogPage({ mainEntity, cards = [], pageTitle, pageDescription }) {
    const table = useCatalogTable(CATALOG_CONFIG[mainEntity]);
    const [modalEntity, setModalEntity] = useState(null);
    const { permissions } = table;

    const openModal = (entityName) => setModalEntity(entityName);
    const closeModal = () => setModalEntity(null);

    return (
        <Box>
            <Container maxWidth="xl" sx={{ pt: 1, pb: 2, mt: 2 }}>
                {permissions.canViewCatalog ? (
                    <>
                        <Box sx={{ mb: 3 }}>
                            <LocationCatalogGrid>
                                {cards.map((card) => (
                                    <LocationCatalogCard
                                        key={card.entityName}
                                        title={card.title}
                                        entityName={card.entityName}
                                        icon={card.icon}
                                        description={card.description}
                                        tone="rose"
                                        onClick={openModal}
                                    />
                                ))}
                            </LocationCatalogGrid>
                        </Box>

                        <PageHeader
                            title={pageTitle}
                            description={pageDescription}
                            action={permissions.canCreateCatalog ? (
                                <PrimaryButton startIcon={<AddIcon />} onClick={table.handleCreate} sx={{ px: '28px' }}>
                                    Crear
                                </PrimaryButton>
                            ) : null}
                            sx={{ mb: 3 }}
                            titleSx={{ fontSize: '1.65rem', letterSpacing: '0.3px' }}
                        />

                        <CatalogTableView table={table} />

                        <CatalogTableModal
                            open={!!modalEntity}
                            onClose={closeModal}
                            config={modalEntity ? CATALOG_CONFIG[modalEntity] : null}
                        />
                    </>
                ) : (
                    <AccessDeniedState />
                )}
            </Container>
        </Box>
    );
}
