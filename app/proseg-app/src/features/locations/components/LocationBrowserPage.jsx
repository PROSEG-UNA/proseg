import { useState } from 'react';
import { Box, Container } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { PrimaryButton } from '../../../common/components/PrimaryButton.jsx';
import { PageHeader } from '../../../common/components/index.js';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import LocationCard from './location/LocationCard.jsx';
import LocationGrid from './location/LocationGrid.jsx';
import LocationTableView from './location/LocationTableView.jsx';
import LocationTableModal from './location/LocationTableModal.jsx';
import { useLocationTable } from '../hooks/useLocationTable.jsx';
import { LOCATION_CONFIG } from './location/locationConfig.js';

export default function LocationBrowserPage({ mainEntity, cards = [], pageTitle, pageDescription }) {
    const table = useLocationTable(LOCATION_CONFIG[mainEntity]);
    const [modalEntity, setModalEntity] = useState(null);
    const { permissions } = table;

    const openModal = (entityName) => setModalEntity(entityName);
    const closeModal = () => setModalEntity(null);

    return (
        <Box>
            <Container maxWidth="xl" sx={{ pt: 1, pb: 2, mt: 2 }}>
                {permissions.canView ? (
                    <>
                        <Box sx={{ mb: 3 }}>
                            <LocationGrid>
                                {cards.map((card) => (
                                    <LocationCard
                                        key={card.entityName}
                                        title={card.title}
                                        entityName={card.entityName}
                                        icon={card.icon}
                                        description={card.description}
                                        tone="rose"
                                        onClick={openModal}
                                    />
                                ))}
                            </LocationGrid>
                        </Box>

                        <PageHeader
                            title={pageTitle}
                            description={pageDescription}
                            action={permissions.canCreate ? (
                                <PrimaryButton startIcon={<AddIcon />} onClick={table.handleCreate} sx={{ px: '28px' }}>
                                    Crear
                                </PrimaryButton>
                            ) : null}
                            sx={{ mb: 3 }}
                            titleSx={{ fontSize: '1.65rem', letterSpacing: '0.3px' }}
                        />

                        <LocationTableView table={table} />

                        <LocationTableModal
                            open={!!modalEntity}
                            onClose={closeModal}
                            config={modalEntity ? LOCATION_CONFIG[modalEntity] : null}
                        />
                    </>
                ) : (
                    <AccessDeniedState />
                )}
            </Container>
        </Box>
    );
}
