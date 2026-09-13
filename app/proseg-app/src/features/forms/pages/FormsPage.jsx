import { useState } from 'react';
import { Box, Container, MenuItem, TextField } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DescriptionIcon from '@mui/icons-material/Description';
import AccessDeniedState from '../../../common/components/AccessDeniedState.jsx';
import DialogModal from '../../../common/components/DialogModal.jsx';
import { PageHeader, PrimaryButton, FeatureCard } from '../../../common/components';
import { usePermissions } from '../../../common/hooks/usePermissions';
import { PERMISSIONS } from '../../../common/constants/permissions';
import { useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '../../../common/query';
import { useFormTypes } from '../hooks/useFormTypes';
import FormsTable from '../components/FormsTable.jsx';
import FormSelectorModal from '../components/FormSelectorModal.jsx';
import FormRecordCreateModal from '../components/FormRecordCreateModal.jsx';

export default function FormsPage() {
    const queryClient = useQueryClient();
    const { hasAnyPermission, hasPermission } = usePermissions();
    const { formTypes } = useFormTypes();
    const [selectedFormTypeId, setSelectedFormTypeId] = useState('');
    const [selectorOpen, setSelectorOpen] = useState(false);
    const [selectedCreateType, setSelectedCreateType] = useState(null);
    const [alert, setAlert] = useState(null);

    const canViewForms = hasAnyPermission([
        PERMISSIONS.FORMS.READ,
        PERMISSIONS.FORMS.CREATE,
        PERMISSIONS.FORMS.DELETE,
    ]);
    const canCreateForms = hasPermission(PERMISSIONS.FORMS.CREATE);

    const handleSaved = () => {
        void queryClient.invalidateQueries({ queryKey: queryKeys.forms.root });
        setAlert({ type: 'success', message: 'Lista de formularios actualizada' });
    };

    if (!canViewForms) {
        return <AccessDeniedState />;
    }

    return (
        <Box>
            <Container maxWidth="xl" sx={{ pt: 3 }}>
                <PageHeader
                    title="Formularios"
                    description="Registro centralizado de formularios físicos de seguridad."
                    action={canCreateForms ? (
                        <PrimaryButton startIcon={<AddIcon />} onClick={() => setSelectorOpen(true)} sx={{ px: '28px' }}>
                            Registrar formulario
                        </PrimaryButton>
                    ) : null}
                />

                <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 3 }}>
                    <FeatureCard
                        icon={<DescriptionIcon fontSize="inherit" />}
                        title="Registro unificado"
                        description="Un solo módulo para múltiples tipos de formularios físicos."
                        buttonLabel="Ver listado"
                        onNavigate={() => {}}
                        sx={{ maxWidth: { xs: '100%', lg: '380px' }, cursor: 'default' }}
                    />
                </Box>

                <Box sx={{ mb: 2, maxWidth: 420 }}>
                    <TextField
                        select
                        fullWidth
                        label="Filtrar por tipo de formulario"
                        value={selectedFormTypeId}
                        onChange={(e) => setSelectedFormTypeId(e.target.value)}
                    >
                        <MenuItem value="">Todos</MenuItem>
                        {formTypes.map((type) => (
                            <MenuItem key={type.id} value={type.id}>
                                {type.name}
                            </MenuItem>
                        ))}
                    </TextField>
                </Box>

                <FormsTable selectedFormTypeId={selectedFormTypeId} />
            </Container>

            <FormSelectorModal
                open={selectorOpen}
                onClose={() => setSelectorOpen(false)}
                onSelect={(type) => {
                    setSelectedCreateType(type);
                    setSelectorOpen(false);
                }}
            />

            <FormRecordCreateModal
                open={!!selectedCreateType}
                formType={selectedCreateType}
                onClose={() => setSelectedCreateType(null)}
                onSaved={handleSaved}
            />

            <DialogModal
                open={!!alert}
                type={alert?.type}
                message={alert?.message}
                onClose={() => setAlert(null)}
            />
        </Box>
    );
}
