import { Alert, Box, Link, Stack, Typography } from '@mui/material';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import GeneralModal from '../../../../common/components/GeneralModal.jsx';
import { finalCommissionGroups } from './finalCommissionIssues.js';

function linesOf(issue) {
    const tours = issue.tours ?? [];
    if (!tours.length) return (issue.details ?? []).map((detail) => ({ label: detail, tours: [] }));
    const byLabel = new Map();
    tours.forEach((tour) => {
        const label = tour.label || `Gira ${tour.number}`;
        const group = byLabel.get(label) ?? [];
        group.push(tour);
        byLabel.set(label, group);
    });
    return [...byLabel.entries()].map(([label, group]) => ({ label, tours: group }));
}

function IssueCard({ issue, color, onOpenTour }) {
    return (
        <Box sx={{ border: '1px solid', borderColor: `${color}.main`, borderRadius: '10px', p: 1.5, bgcolor: 'background.paper' }}>
            <Typography sx={{ fontWeight: 700, fontSize: 13.5, color: `${color}.main` }}>
                {issue.title}
            </Typography>
            <Box component="ul" sx={{ m: 0, mt: 0.75, pl: 2.5, maxHeight: 220, overflowY: 'auto', fontSize: 12.5, color: 'text.secondary' }}>
                {linesOf(issue).map((line) => (
                    <li key={line.label}>
                        {line.tours.length === 1 ? (
                            <Link component="button" type="button" onClick={() => onOpenTour(line.tours[0].id)} sx={{ font: 'inherit', textAlign: 'left' }}>
                                {line.label}
                            </Link>
                        ) : (
                            <>
                                {line.label}
                                {line.tours.map((tour) => (
                                    <Link key={tour.id} component="button" type="button" onClick={() => onOpenTour(tour.id)} sx={{ ml: 1, font: 'inherit' }}>
                                        {`Gira ${tour.number}`}
                                    </Link>
                                ))}
                            </>
                        )}
                    </li>
                ))}
            </Box>
        </Box>
    );
}

export default function TourProcessingFinalCommissionDialog({
    open,
    check,
    subtitle,
    loading = false,
    fetching = false,
    error = null,
    rejecting = false,
    canRejectIncomplete = true,
    onClose,
    onOpenTour,
    onRejectIncomplete,
}) {
    const groups = finalCommissionGroups(check);
    const canReject = canRejectIncomplete && Boolean(check?.rejections);

    return (
        <GeneralModal
            open={open}
            onClose={rejecting ? undefined : onClose}
            icon={WarningAmberOutlinedIcon}
            title="Errores del borrador"
            subtitle={subtitle}
            maxWidth="lg"
            fullScreenAt="md"
            loading={loading}
            secondaryButton={{ label: 'Cerrar', onClick: onClose, disabled: rejecting }}
            primaryButton={canReject ? {
                label: rejecting ? 'Guardando...' : 'Pasar las giras sin chofer o vehículo a No aprobada',
                onClick: onRejectIncomplete,
                disabled: rejecting,
            } : undefined}
        >
            <Stack spacing={2} sx={{ p: 3 }}>
                {fetching && !loading && (
                    <Typography sx={{ fontSize: 12.5, color: 'text.secondary' }}>Actualizando...</Typography>
                )}
                {error && <Alert severity="error" variant="outlined">{error}</Alert>}
                {!loading && !error && groups.length === 0 && (
                    <Typography sx={{ fontSize: 13.5 }}>No hay reglas rotas en este borrador.</Typography>
                )}
                {groups.map((group) => (
                    <Stack key={group.key} spacing={1.25}>
                        <Typography sx={{ fontWeight: 800, fontSize: 14 }}>{`${group.title} (${group.count})`}</Typography>
                        {group.issues.map((issue) => (
                            <IssueCard
                                key={issue.code}
                                issue={issue}
                                color={group.key === 'rules' ? 'warning' : 'error'}
                                onOpenTour={onOpenTour}
                            />
                        ))}
                    </Stack>
                ))}
            </Stack>
        </GeneralModal>
    );
}
