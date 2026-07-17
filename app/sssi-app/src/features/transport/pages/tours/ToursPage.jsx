import { useState } from 'react';
import { Box, Container } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import { PrimaryButton } from '../../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import ToursTable from '../../components/tours/ToursTable.jsx';
import TourFormModal from '../../components/tours/TourFormModal.jsx';

export default function ToursPage() {
    const [tourFormOpen, setTourFormOpen] = useState(false);
    const [tourFormId, setTourFormId] = useState(null);
    const [toursRefresh, setToursRefresh] = useState(0);
    const { hasPermission, hasAnyPermission } = usePermissions();

    const canViewTours = hasAnyPermission([
        PERMISSIONS.TRANSPORT.TOURS.READ,
        PERMISSIONS.TRANSPORT.TOURS.MANAGE,
        PERMISSIONS.TRANSPORT.TOURS.DELETE,
    ]);

    const openCreateTour = () => {
        setTourFormId(null);
        setTourFormOpen(true);
    };

    const openEditTour = (tour) => {
        setTourFormId(tour.id);
        setTourFormOpen(true);
    };

    const refreshTours = () => setToursRefresh((value) => value + 1);

    if (!canViewTours) return <AccessDeniedState />;

    return (
        <Box className="transport-tours-page">
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Giras"
                    description="Gestión de giras."
                    action={hasPermission(PERMISSIONS.TRANSPORT.TOURS.MANAGE) ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={openCreateTour} sx={{ px: '28px' }}>
                            Crear
                        </PrimaryButton>
                    ) : null}
                />

                <Box sx={{ pt: 3 }}>
                    <ToursTable
                        refreshKey={toursRefresh}
                        onRefresh={refreshTours}
                        onEditTour={openEditTour}
                    />
                </Box>
            </Container>

            {tourFormOpen ? (
                <TourFormModal
                    open={tourFormOpen}
                    tourId={tourFormId}
                    onClose={() => setTourFormOpen(false)}
                    onSaved={refreshTours}
                />
            ) : null}
        </Box>
    );
}
