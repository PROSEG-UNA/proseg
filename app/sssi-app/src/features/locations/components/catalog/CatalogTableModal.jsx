import { useEffect } from 'react';
import { Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import { useCatalogTable } from '../../hooks/useCatalogTable.jsx';
import CatalogTableView from './CatalogTableView.jsx';

export default function CatalogTableModal({ open, onClose, config }) {
    const table = useCatalogTable(config, { enabled: open });
    const { title, pluralTitle, icon: Icon, loading, totalElements, permissions, handleCreate, resetState } = table;

    const resetOnOpen = () => {
        if (!open) return;
        resetState();
    };
    useEffect(resetOnOpen, [open]);

    const footerLeft = (
        <Typography sx={{ fontSize: 11.5, color: 'text.disabled', fontWeight: 500 }}>
            {totalElements > 0
                ? `${totalElements} registro${totalElements !== 1 ? 's' : ''}`
                : 'Sin registros'}
        </Typography>
    );

    return (
        <GeneralModal
            open={open}
            onClose={onClose}
            maxWidth="md"
            fullScreenAt="md"
            icon={Icon}
            title={pluralTitle}
            subtitle={`Gestión de ${pluralTitle.toLowerCase()}`}
            loading={loading}
            footerLeft={footerLeft}
            secondaryButton={{ label: 'Cerrar', onClick: onClose }}
            primaryButton={permissions.canCreateCatalog
                ? { label: `Crear ${title}`, onClick: handleCreate, startIcon: <AddIcon /> }
                : null}
        >
            <CatalogTableView table={table} />
        </GeneralModal>
    );
}
