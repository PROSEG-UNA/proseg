import { Box, Chip, Tooltip, Typography } from '@mui/material';
import CatalogValueCell from './CatalogValueCell.jsx';
import { checksDriverCatalog } from './tourProcessingCatalogMatch.js';

const KIND_TEXT = {
    driver: { singular: 'chofer', plural: 'choferes', unassigned: 'Sin chofer' },
    vehicle: { singular: 'vehículo', plural: 'vehículos', unassigned: 'Sin vehículo' },
};

function slotValue(kind, assignment) {
    return kind === 'driver' ? assignment.driver : assignment.borrowedVehiclePlate || assignment.vehicle;
}

function isCheckedAgainstCatalog(kind, assignment) {
    if (kind === 'driver') return checksDriverCatalog(assignment.resourceType);
    return assignment.resourceType !== 'EXTERNAL' && assignment.resourceType !== 'BORROWED_VEHICLE';
}

function describeSlot(kind, assignment, isMissing, isUnavailable) {
    const value = slotValue(kind, assignment);
    const checked = Boolean(value) && isCheckedAgainstCatalog(kind, assignment);
    return {
        value,
        unassigned: !value,
        missing: checked && isMissing(value),
        unavailable: checked && !isMissing(value) && Boolean(isUnavailable?.(value)),
    };
}

function summarizeIssues(kind, slots) {
    const text = KIND_TEXT[kind];
    const unassigned = slots.filter((slot) => slot.unassigned).length;
    const missing = slots.filter((slot) => slot.missing).length;
    return [
        unassigned && `${unassigned} de ${slots.length} viajes sin ${text.singular}`,
        missing && (missing === 1 ? `1 ${text.singular} no está en el sistema` : `${missing} ${text.plural} no están en el sistema`),
    ].filter(Boolean);
}

function slotLabel(slot, unassignedLabel) {
    if (slot.unassigned) return unassignedLabel;
    if (slot.missing) return `${slot.value} (no está en el sistema)`;
    if (slot.unavailable) return `${slot.value} (no disponible)`;
    return slot.value;
}

export default function TourProcessingAssignmentsCell({ kind, assignments, isMissing, isUnavailable }) {
    const text = KIND_TEXT[kind];
    const slots = assignments.map((assignment) => describeSlot(kind, assignment, isMissing, isUnavailable));
    const issues = summarizeIssues(kind, slots);
    const [firstSlot] = slots;

    const details = (
        <Box>
            {issues.map((issue) => (
                <Typography key={issue} sx={{ fontSize: 12, fontWeight: 600 }}>{issue}</Typography>
            ))}
            <Box component="ol" sx={{ m: 0, mt: issues.length ? 0.5 : 0, p: 0, listStyle: 'none' }}>
                {slots.map((slot, index) => (
                    <Box component="li" key={index} sx={{ display: 'flex', gap: 1.5, fontSize: 12 }}>
                        <span>{`Viaje ${index + 1}`}</span>
                        <Box component="span" sx={slot.unassigned || slot.missing ? { color: 'warning.light' } : undefined}>
                            {slotLabel(slot, text.unassigned)}
                        </Box>
                    </Box>
                ))}
            </Box>
        </Box>
    );

    return (
        <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.75, maxWidth: '100%' }}>
            <Box sx={{ minWidth: 0 }}>
                <CatalogValueCell kind={kind} value={firstSlot.value} missing={firstSlot.missing} unavailable={firstSlot.unavailable} />
            </Box>
            <Tooltip title={details} placement="top">
                <Chip
                    role="img"
                    aria-label={[`${slots.length} viajes`, ...issues].join('. ')}
                    tabIndex={0}
                    size="small"
                    variant="outlined"
                    color={issues.length ? 'warning' : 'default'}
                    label={`+${slots.length - 1}`}
                    sx={{ flexShrink: 0, height: 20, fontSize: 11.5, cursor: 'default' }}
                />
            </Tooltip>
        </Box>
    );
}
