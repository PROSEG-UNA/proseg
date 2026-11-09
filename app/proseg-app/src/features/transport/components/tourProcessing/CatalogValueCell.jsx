import { Box, Chip } from '@mui/material';
import MissingCatalogIcon from './MissingCatalogIcon.jsx';

export default function CatalogValueCell({ kind, value, missing, unavailable = false }) {
    if (!value) return '—';
    return (
        <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5, maxWidth: '100%' }}>
            <Box component="span" sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {value}
            </Box>
            {missing && <MissingCatalogIcon kind={kind} value={value} />}
            {unavailable && !missing && (
                <Chip label="No disponible" size="small" color="error" variant="outlined" sx={{ flexShrink: 0 }} />
            )}
        </Box>
    );
}
