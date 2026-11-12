import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Box, Container } from '@mui/material';
import { PageHeader } from '../../../../common/components/index.js';
import AccessDeniedState from '../../../../common/components/AccessDeniedState.jsx';
import { PERMISSIONS } from '../../../../common/constants/permissions';
import { usePermissions } from '../../../../common/hooks/index.js';
import { queryKeys } from '../../../../common/query';
import ObservationKeywordsField from '../../components/settings/ObservationKeywordsField.jsx';
import ObservationKeywordFormModal from '../../components/settings/ObservationKeywordFormModal.jsx';
import SettingsSectionHeader from '../../components/settings/SettingsSectionHeader.jsx';
import TourSplitKeywordsSection from '../../components/settings/TourSplitKeywordsSection.jsx';
import TourSplitKeywordFormModal from '../../components/settings/TourSplitKeywordFormModal.jsx';
import { useTourSplitSettings } from '../../hooks/useTourSplitSettings';
import { TOUR_SPLIT_MODES } from '../../services/tourSplit/tourSplitService';

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
    const [splitForm, setSplitForm] = useState(null);
    const queryClient = useQueryClient();
    const { hasAnyPermission } = usePermissions();
    const splitSettings = useTourSplitSettings();

    if (!hasAnyPermission(TOUR_PERMISSIONS)) return <AccessDeniedState />;

    const openSplitForm = (mode, splitKeywordId) => setSplitForm({ mode, keywordId: splitKeywordId });

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
                <Box sx={{ display: 'grid', gap: { xs: 2, md: 3 }, mt: 1 }}>
                    <ObservationKeywordsField
                        onAddKeyword={() => { setKeywordId(null); setFormOpen(true); }}
                        onEditKeyword={(keyword) => { setKeywordId(keyword.id); setFormOpen(true); }}
                    />
                    <Box
                        component="section"
                        aria-labelledby="tour-split-title"
                        sx={{ p: { xs: 2, sm: 3 }, border: '1px solid', borderColor: 'divider', borderRadius: 3, bgcolor: 'background.paperWarm' }}
                    >
                        <SettingsSectionHeader
                            titleId="tour-split-title"
                            title="División automática de giras que requieren varios viajes"
                            description="Cuando una gira lleva más pasajeros de los que caben en su vehículo, se divide en varios viajes. Según su destino, esos viajes los hacen choferes distintos o el mismo chofer repitiendo viajes."
                            sx={{ mb: 3 }}
                        />
                        <Box
                            sx={{
                                display: 'grid',
                                gridTemplateColumns: { xs: 'minmax(0, 1fr)', md: 'repeat(2, minmax(0, 1fr))' },
                                columnGap: { md: 6 },
                                rowGap: { xs: 4, md: 2 },
                                position: 'relative',
                                '&::before': {
                                    content: '""',
                                    display: { xs: 'none', md: 'block' },
                                    position: 'absolute',
                                    top: 0,
                                    bottom: 0,
                                    left: '50%',
                                    width: '1px',
                                    bgcolor: 'divider',
                                },
                            }}
                        >
                    <TourSplitKeywordsSection
                        mode={TOUR_SPLIT_MODES.REPEATING}
                        title="Destinos de choferes que repiten viajes"
                        description="Agrega los destinos cuyas giras, al excederse la capacidad, se dividen en viajes que hace el mismo chofer con el mismo vehículo. Las horas se cuentan una sola vez. Basta con que el destino de la gira contenga el texto que agregues."
                        switchLabel="Resto de destinos: repiten viajes"
                        defaultActive={splitSettings.defaultMode === TOUR_SPLIT_MODES.REPEATING}
                        settingsLoading={splitSettings.loading}
                        settingsSaving={splitSettings.saving}
                        onToggleDefault={(active) => splitSettings.changeDefaultMode(active ? TOUR_SPLIT_MODES.REPEATING : TOUR_SPLIT_MODES.DISTINCT)}
                        onAddKeyword={() => openSplitForm(TOUR_SPLIT_MODES.REPEATING, null)}
                        onEditKeyword={(keyword) => openSplitForm(TOUR_SPLIT_MODES.REPEATING, keyword.id)}
                    />
                    <TourSplitKeywordsSection
                        mode={TOUR_SPLIT_MODES.DISTINCT}
                        title="Destinos de choferes distintos"
                        description="Agrega los destinos cuyas giras, al excederse la capacidad, se dividen en viajes con un chofer y un vehículo distintos cada uno. Basta con que el destino de la gira contenga el texto que agregues."
                        switchLabel="Resto de destinos: choferes distintos"
                        defaultActive={splitSettings.defaultMode === TOUR_SPLIT_MODES.DISTINCT}
                        settingsLoading={splitSettings.loading}
                        settingsSaving={splitSettings.saving}
                        onToggleDefault={(active) => splitSettings.changeDefaultMode(active ? TOUR_SPLIT_MODES.DISTINCT : TOUR_SPLIT_MODES.REPEATING)}
                        onAddKeyword={() => openSplitForm(TOUR_SPLIT_MODES.DISTINCT, null)}
                        onEditKeyword={(keyword) => openSplitForm(TOUR_SPLIT_MODES.DISTINCT, keyword.id)}
                    />
                        </Box>
                    </Box>
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
            {splitForm ? (
                <TourSplitKeywordFormModal
                    key={`${splitForm.mode}-${splitForm.keywordId ?? 'new'}`}
                    open
                    mode={splitForm.mode}
                    keywordId={splitForm.keywordId}
                    onClose={() => setSplitForm(null)}
                    onSaved={() => queryClient.invalidateQueries({ queryKey: queryKeys.transport.tourSplitKeywordList(splitForm.mode) })}
                />
            ) : null}
        </Box>
    );
}
