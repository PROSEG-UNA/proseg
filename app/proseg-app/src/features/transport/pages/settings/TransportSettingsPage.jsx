import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Box, Container, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import { PrimaryButton } from '../../../../common/components/PrimaryButton.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import { queryKeys } from '../../../../common/query';
import ObservationKeywordsField from '../../components/settings/ObservationKeywordsField.jsx';
import ObservationKeywordFormModal from '../../components/settings/ObservationKeywordFormModal.jsx';

const TOUR_PERMISSIONS = [
    PERMISSIONS.TRANSPORT.TOURS.READ,
    PERMISSIONS.TRANSPORT.TOURS.IMPORT,
    PERMISSIONS.TRANSPORT.TOURS.CREATE,
    PERMISSIONS.TRANSPORT.TOURS.UPDATE,
    PERMISSIONS.TRANSPORT.TOURS.DELETE,
    PERMISSIONS.TRANSPORT.TOURS.EXPORT,
];

export default function TransportSettingsPage() {
    const [formOpen, setFormOpen] = useState(false);
    const [keywordId, setKeywordId] = useState(null);
    const queryClient = useQueryClient();
    const { hasPermission, hasAnyPermission } = usePermissions();

    if (!hasAnyPermission(TOUR_PERMISSIONS)) return <AccessDeniedState />;

    const refresh = () => {
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.observationKeywords() });
        void queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourProcessing() });
    };

    return (
        <Box className="transport-settings-page">
            <Container maxWidth="xl" sx={{ pt: 3, pb: 4 }}>
                <PageHeader
                    title="Configuraciones"
                    description="Ajustes del módulo de transporte."
                />
                <Box sx={{ mt: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2, mb: 2 }}>
                        <Box>
                            <Typography component="h2" sx={{ fontWeight: 800, fontSize: '1.15rem', mb: 0.5 }}>
                                Palabras clave en observaciones
                            </Typography>
                            <Typography sx={{ color: 'text.secondary' }}>
                                Las coincidencias aparecen junto al número de gira. Al pasar el cursor se muestra la palabra clave y puedes marcar la observación como revisada. No se distinguen mayúsculas ni tildes.
                            </Typography>
                        </Box>
                        {hasPermission(PERMISSIONS.TRANSPORT.TOURS.UPDATE) ? (
                            <PrimaryButton startIcon={<AddIcon />} onClick={() => { setKeywordId(null); setFormOpen(true); }} sx={{ px: '28px', flexShrink: 0 }}>
                                Agregar
                            </PrimaryButton>
                        ) : null}
                    </Box>
                    <ObservationKeywordsField onEditKeyword={(keyword) => { setKeywordId(keyword.id); setFormOpen(true); }} />
                </Box>
            </Container>
            {formOpen ? (
                <ObservationKeywordFormModal
                    key={keywordId ?? 'new'}
                    open={formOpen}
                    keywordId={keywordId}
                    onClose={() => setFormOpen(false)}
                    onSaved={refresh}
                />
            ) : null}
        </Box>
    );
}
