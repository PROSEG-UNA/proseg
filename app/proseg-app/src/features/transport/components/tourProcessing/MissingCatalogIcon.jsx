import { Tooltip } from '@mui/material';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import { missingCatalogMessage } from './tourProcessingCatalogMatch.js';

export default function MissingCatalogIcon({ kind, value }) {
    return (
        <Tooltip title={missingCatalogMessage(kind, value)} placement="top">
            <ErrorOutlineOutlinedIcon
                color="error"
                role="img"
                aria-hidden={false}
                sx={{ fontSize: 18, flexShrink: 0, cursor: 'help' }}
            />
        </Tooltip>
    );
}
