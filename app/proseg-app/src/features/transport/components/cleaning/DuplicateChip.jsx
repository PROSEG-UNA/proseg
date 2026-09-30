import { Chip } from '@mui/material';
import { draftRowDuplicateLabel } from './cleaningDraftLabels.js';

export default function DuplicateChip({ row }) {
    const label = draftRowDuplicateLabel(row);
    if (!label) return null;
    const color = row.conflicting ? 'error' : (row.duplicateRank === 1 ? 'default' : 'warning');
    return <Chip label={label} color={color} size="small" variant="outlined" />;
}
